package com.mathcore.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mathcore.app.data.Difficulty
import com.mathcore.app.data.Question
import com.mathcore.app.data.QuestionRepository
import com.mathcore.app.data.Subject
import com.mathcore.app.data.model.PublicUserInfo
import com.mathcore.app.data.repository.ResultsRepository
import com.mathcore.app.data.repository.SupabaseConfig
import com.mathcore.app.data.repository.json
import com.mathcore.app.util.AppHttpClient
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

// ── Phases ────────────────────────────────────────────────────────────────────
enum class DuelPhase {
    IDLE,              // Lobby — choosing section / difficulty
    WAITING,           // Waiting for opponent to connect
    LOADING,           // Loading questions from local JSON (brief; shows spinner)
    COUNTDOWN,         // 3-2-1 countdown
    ACTIVE,            // Quiz is running (hands off to QuizScreen)
    WAITING_FOR_SCORE, // Quiz done, waiting for opponent's score event
    FINISHED           // Results screen
}

// ── Data classes ─────────────────────────────────────────────────────────────
data class PendingInvite(
    val code: String,
    val fromUsername: String,
    val section: String = "",
    val difficulty: String = ""
)

data class DuelUiState(
    val phase: DuelPhase          = DuelPhase.IDLE,
    val code: String              = "",       // current duel code (questions + DB key)
    val opponentName: String      = "",
    val myScore: Int              = 0,
    val opponentScore: Int        = 0,        // -1 = opponent timed out / disconnected
    val opponentScoreReceived: Boolean = false,  // true once opponent's score event arrives
    val countdown: Int            = 3,
    val questions: List<Question> = emptyList(),
    val error: String?            = null,
    val pendingInvite: PendingInvite? = null,
    val selectedSection: Subject  = Subject.MIXED,
    val selectedDiff: Difficulty  = Difficulty.MEDIUM,
    // Rematch state
    val rematchRequested: Boolean = false,    // we sent rematch_request, waiting for reply
    val pendingRematch: String?   = null      // opponent's name if they requested rematch
)

// ── ViewModel ─────────────────────────────────────────────────────────────────
class DuelViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DuelUiState())
    val uiState: StateFlow<DuelUiState> = _uiState.asStateFlow()

    private val _userSuggestions = MutableStateFlow<List<PublicUserInfo>>(emptyList())
    val userSuggestions: StateFlow<List<PublicUserInfo>> = _userSuggestions.asStateFlow()

    private val questionRepo = QuestionRepository(application)
    private val resultsRepo  = ResultsRepository()

    private var searchJob: Job? = null
    private var inviteJob: Job? = null
    private var opponentTimeoutJob: Job? = null

    private var wsSession: DefaultClientWebSocketSession? = null
    private var myName = ""
    private var myRole = ""   // "host" | "guest"

    // wsChannelCode is the topic we JOINED — stays constant for lifetime of the WS session.
    // After a rematch the new code goes into DuelUiState.code (for questions + DB) but
    // WS messages are still routed via the original joined channel.
    private var wsChannelCode = ""
    private val duelTopic get() = "realtime:duel:${wsChannelCode}"

    // ── User autocomplete ──────────────────────────────────────────────────────

    fun searchUsers(query: String) {
        searchJob?.cancel()
        if (query.length < 2) { _userSuggestions.value = emptyList(); return }
        searchJob = viewModelScope.launch {
            delay(300)
            try { _userSuggestions.value = resultsRepo.searchUsers(query) }
            catch (_: Exception) {}
        }
    }

    fun clearUserSuggestions() {
        _userSuggestions.value = emptyList()
        searchJob?.cancel()
    }

    // ── Incoming invite subscription ───────────────────────────────────────────

    /**
     * Joins the shared 'duel-invites' Realtime channel and listens for invites
     * addressed to [myUsername]. Reconnects automatically on WS drop.
     */
    fun subscribeToInvites(myUsername: String) {
        if (myUsername.isBlank() || inviteJob?.isActive == true) return
        inviteJob = viewModelScope.launch {
            while (isActive) {
                try {
                    AppHttpClient.authenticated.webSocket(
                        urlString = "${SupabaseConfig.REALTIME_WS}?apikey=${SupabaseConfig.KEY}&vsn=1.0.0"
                    ) {
                        sendRaw(this, "realtime:duel-invites", "phx_join", buildJsonObject {
                            put("config", buildJsonObject {
                                put("broadcast", buildJsonObject { put("self", false) })
                            })
                        }, joinRef = "1")

                        launch {
                            while (isActive) {
                                delay(25_000)
                                try { sendRaw(this@webSocket, "phoenix", "heartbeat", buildJsonObject {}, ref = "hb") }
                                catch (_: Exception) {}
                            }
                        }

                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            val obj = runCatching {
                                json.parseToJsonElement(frame.readText()).jsonObject
                            }.getOrNull() ?: continue

                            if (obj["event"]?.jsonPrimitive?.content != "broadcast") continue
                            val outerPayload = obj["payload"]?.jsonObject ?: continue
                            if (outerPayload["event"]?.jsonPrimitive?.content != "invite") continue

                            val p = outerPayload["payload"]?.jsonObject ?: continue
                            val invitedUsername = p["invitedUsername"]?.jsonPrimitive?.content ?: continue
                            // Case-insensitive match: web lowercases the target username
                            if (!invitedUsername.equals(myUsername, ignoreCase = true)) continue

                            val code        = p["code"]?.jsonPrimitive?.content       ?: continue
                            val inviterName = p["inviterName"]?.jsonPrimitive?.content ?: "Игрок"
                            val section     = p["section"]?.jsonPrimitive?.content    ?: ""
                            val difficulty  = p["difficulty"]?.jsonPrimitive?.content ?: ""

                            _uiState.update {
                                it.copy(pendingInvite = PendingInvite(code, inviterName, section, difficulty))
                            }
                        }
                    }
                } catch (_: Exception) {}
                if (isActive) delay(3_000)   // reconnect after 3 s on any drop
            }
        }
    }

    fun dismissInvite() = _uiState.update { it.copy(pendingInvite = null) }

    fun acceptInvite(invite: PendingInvite, myUsername: String) {
        _uiState.update { it.copy(pendingInvite = null, opponentName = invite.fromUsername) }
        joinDuel(myUsername, invite.code)
    }

    // ── Create / Join ──────────────────────────────────────────────────────────

    fun createDuel(
        name: String,
        section: Subject,
        diff: Difficulty,
        targetUsername: String = ""
    ) {
        myName = name
        myRole = "host"
        val code = generateCode()
        _uiState.update {
            it.copy(
                phase = DuelPhase.WAITING,
                code  = code,
                selectedSection = section,
                selectedDiff    = diff
            )
        }
        viewModelScope.launch { connectWS(code) }
        if (targetUsername.isNotBlank()) {
            viewModelScope.launch { sendInviteMessage(targetUsername, code, name, section, diff) }
        }
    }

    fun joinDuel(name: String, code: String) {
        myName = name
        myRole = "guest"
        _uiState.update { it.copy(phase = DuelPhase.WAITING, code = code) }
        // Send "join" from INSIDE connectWS so wsSession is guaranteed non-null.
        // The old approach (separate coroutine + fixed 800 ms delay) was a race
        // condition: wsSession could still be null when sendBroadcast ran.
        viewModelScope.launch {
            connectWS(code, onConnected = {
                sendBroadcast("join", buildJsonObject { put("name", name) })
            })
        }
    }

    /** Broadcast final score to the opponent after the quiz finishes. */
    fun broadcastScore(score: Int) {
        viewModelScope.launch {
            val oppAlreadySent = _uiState.value.opponentScoreReceived
            _uiState.update { it.copy(
                myScore = score,
                phase   = if (oppAlreadySent) DuelPhase.FINISHED else DuelPhase.WAITING_FOR_SCORE
            ) }
            sendBroadcast("score", buildJsonObject {
                put("score", score)
                put("name",  myName)
            })

            if (!oppAlreadySent) {
                // Timeout: if opponent doesn't send score within 60 s, show results anyway
                opponentTimeoutJob?.cancel()
                opponentTimeoutJob = launch {
                    delay(60_000)
                    if (_uiState.value.phase == DuelPhase.WAITING_FOR_SCORE) {
                        _uiState.update { it.copy(opponentScore = -1, phase = DuelPhase.FINISHED) }
                    }
                }
            }
        }
    }

    /** Request a rematch — uses the web protocol (rematch_request on the existing channel). */
    fun requestRematch() {
        _uiState.update { it.copy(rematchRequested = true) }
        viewModelScope.launch {
            sendBroadcast("rematch_request", buildJsonObject { put("name", myName) })
        }
    }

    /** Accept incoming rematch from opponent. */
    fun acceptRematch() {
        val pendingName = _uiState.value.pendingRematch ?: return
        _uiState.update { it.copy(pendingRematch = null) }
        viewModelScope.launch {
            sendBroadcast("rematch_accept", buildJsonObject {})
        }
    }

    /** Decline incoming rematch from opponent. */
    fun declineRematch() {
        _uiState.update { it.copy(pendingRematch = null) }
        viewModelScope.launch {
            sendBroadcast("rematch_decline", buildJsonObject {})
        }
    }

    fun leaveDuel() {
        opponentTimeoutJob?.cancel()
        viewModelScope.launch {
            try { wsSession?.close(CloseReason(CloseReason.Codes.NORMAL, "user left")) }
            catch (_: Exception) {}
            wsSession = null
        }
        _uiState.update {
            it.copy(
                phase = DuelPhase.IDLE, code = "", opponentName = "",
                myScore = 0, opponentScore = 0, opponentScoreReceived = false,
                countdown = 3, questions = emptyList(), error = null,
                rematchRequested = false, pendingRematch = null
            )
        }
    }

    // ── Core: load questions + countdown ─────────────────────────────────────

    private fun startDuel() {
        viewModelScope.launch {
            _uiState.update { it.copy(phase = DuelPhase.LOADING) }
            val questions = withContext(Dispatchers.IO) {
                // Use seeded generation — same code produces identical questions on web + Android
                questionRepo.getDuelQuestions(
                    _uiState.value.code,
                    _uiState.value.selectedSection,
                    _uiState.value.selectedDiff,
                    count = 10
                )
            }
            _uiState.update { it.copy(questions = questions) }
            for (i in 3 downTo 1) {
                _uiState.update { it.copy(phase = DuelPhase.COUNTDOWN, countdown = i) }
                delay(1000)
            }
            _uiState.update { it.copy(phase = DuelPhase.ACTIVE) }
        }
    }

    // ── WebSocket helpers ──────────────────────────────────────────────────────

    private suspend fun connectWS(code: String, onConnected: (suspend () -> Unit)? = null) {
        wsChannelCode = code   // fix duelTopic for this channel
        try {
            AppHttpClient.authenticated.webSocket(
                urlString = "${SupabaseConfig.REALTIME_WS}?apikey=${SupabaseConfig.KEY}&vsn=1.0.0"
            ) {
                wsSession = this

                sendRaw(this, duelTopic, "phx_join", buildJsonObject {
                    put("config", buildJsonObject {
                        put("broadcast", buildJsonObject { put("self", false) })
                    })
                }, joinRef = "1")

                // If a post-connect callback is provided (guest "join" broadcast),
                // run it in a child coroutine after a brief pause so the server
                // can process the phx_join and send back the phx_reply ack.
                if (onConnected != null) {
                    launch {
                        delay(600)
                        onConnected()
                    }
                }

                launch {
                    while (isActive) {
                        delay(25_000)
                        try { sendRaw(this@webSocket, "phoenix", "heartbeat", buildJsonObject {}, ref = "hb") }
                        catch (_: Exception) {}
                    }
                }

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        handleMessage(json.parseToJsonElement(frame.readText()).jsonObject)
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Ошибка подключения: ${e.message}") }
        }
    }

    /**
     * Handles incoming Supabase Realtime broadcast frames.
     *
     * Rematch protocol (matches web duel.js):
     *   requester  → rematch_request  → waits for rematch_accept
     *   responder  → rematch_accept   → requester generates newCode + sends rematch_start
     *   both start new quiz with newCode (WS channel unchanged, code used only for seed+DB)
     */
    private fun handleMessage(msg: JsonObject) {
        val outerEvent = msg["event"]?.jsonPrimitive?.content ?: return
        if (outerEvent != "broadcast") return

        val outerPayload = msg["payload"]?.jsonObject ?: return
        val event   = outerPayload["event"]?.jsonPrimitive?.content   ?: return
        val payload = outerPayload["payload"]?.jsonObject             ?: return

        viewModelScope.launch {
            when (event) {
                "join" -> {
                    val guestName = payload["name"]?.jsonPrimitive?.content ?: "Гость"
                    _uiState.update { it.copy(opponentName = guestName) }
                    if (myRole == "host") {
                        sendBroadcast("start", buildJsonObject {
                            put("section",    _uiState.value.selectedSection.name.lowercase())
                            put("difficulty", _uiState.value.selectedDiff.name.lowercase())
                        })
                        startDuel()
                    }
                }

                "start" -> {
                    if (myRole == "guest") {
                        val sectionStr = payload["section"]?.jsonPrimitive?.content    ?: "linalg"
                        val diffStr    = payload["difficulty"]?.jsonPrimitive?.content ?: "medium"
                        val section = runCatching {
                            Subject.valueOf(sectionStr.uppercase())
                        }.getOrDefault(Subject.LINALG)
                        val diff = runCatching {
                            Difficulty.valueOf(diffStr.uppercase())
                        }.getOrDefault(Difficulty.MEDIUM)
                        _uiState.update { it.copy(selectedSection = section, selectedDiff = diff) }
                        startDuel()
                    }
                }

                "score" -> {
                    opponentTimeoutJob?.cancel()
                    val oppScore = payload["score"]?.jsonPrimitive?.int ?: 0
                    _uiState.update { state ->
                        state.copy(
                            opponentScore         = oppScore,
                            opponentScoreReceived = true,
                            // Only advance to FINISHED if we already sent our own score
                            phase = if (state.phase == DuelPhase.WAITING_FOR_SCORE) DuelPhase.FINISHED
                                    else state.phase
                        )
                    }
                }

                // ── Rematch protocol ─────────────────────────────────────────
                "rematch_request" -> {
                    val name = payload["name"]?.jsonPrimitive?.content ?: _uiState.value.opponentName
                    _uiState.update { it.copy(pendingRematch = name) }
                }

                "rematch_accept" -> {
                    // We sent rematch_request; now we become host of the new round.
                    if (!_uiState.value.rematchRequested) return@launch
                    val newCode = generateCode()
                    val section = _uiState.value.selectedSection
                    val diff    = _uiState.value.selectedDiff
                    // Send rematch_start on the CURRENT channel (wsChannelCode unchanged)
                    // so the guest (still on old channel) receives it.
                    sendBroadcast("rematch_start", buildJsonObject {
                        put("code",       newCode)
                        put("section",    section.name.lowercase())
                        put("difficulty", diff.name.lowercase())
                    })
                    _uiState.update {
                        it.copy(
                            code = newCode, rematchRequested = false, pendingRematch = null,
                            myScore = 0, opponentScore = 0, opponentScoreReceived = false,
                            countdown = 3, questions = emptyList()
                        )
                    }
                    startDuel()
                }

                "rematch_decline" -> {
                    _uiState.update { it.copy(rematchRequested = false) }
                }

                "rematch_start" -> {
                    // We are the guest for this rematch round.
                    if (_uiState.value.rematchRequested) return@launch
                    val newCode    = payload["code"]?.jsonPrimitive?.content       ?: return@launch
                    val sectionStr = payload["section"]?.jsonPrimitive?.content    ?: "linalg"
                    val diffStr    = payload["difficulty"]?.jsonPrimitive?.content ?: "medium"
                    val section = runCatching { Subject.valueOf(sectionStr.uppercase()) }.getOrDefault(Subject.LINALG)
                    val diff    = runCatching { Difficulty.valueOf(diffStr.uppercase()) }.getOrDefault(Difficulty.MEDIUM)
                    _uiState.update {
                        it.copy(
                            code = newCode, selectedSection = section, selectedDiff = diff,
                            myScore = 0, opponentScore = 0, opponentScoreReceived = false,
                            countdown = 3, questions = emptyList()
                        )
                    }
                    startDuel()
                }
            }
        }
    }

    /**
     * Sends a broadcast on the current duel Realtime channel (wsChannelCode).
     * Supabase expects: { topic, event:"broadcast", payload:{ type:"broadcast", event, payload }, ref, join_ref }
     */
    private suspend fun sendBroadcast(event: String, payload: JsonObject) {
        try {
            val msg = buildJsonObject {
                put("topic",   duelTopic)
                put("event",   "broadcast")
                put("payload", buildJsonObject {
                    put("type",    "broadcast")
                    put("event",   event)
                    put("payload", payload)
                })
                put("ref",      "")
                put("join_ref", "1")
            }
            wsSession?.send(json.encodeToString(msg))
        } catch (_: Exception) {}
    }

    /**
     * Opens a short-lived WS to deliver an invite on the shared 'duel-invites' channel.
     * Matches web's invitesChannel.send({ event:'invite', payload:{...} }).
     */
    private suspend fun sendInviteMessage(
        targetUsername: String,
        code: String,
        from: String,
        section: Subject,
        diff: Difficulty
    ) {
        try {
            AppHttpClient.authenticated.webSocket(
                urlString = "${SupabaseConfig.REALTIME_WS}?apikey=${SupabaseConfig.KEY}&vsn=1.0.0"
            ) {
                sendRaw(this, "realtime:duel-invites", "phx_join", buildJsonObject {
                    put("config", buildJsonObject {
                        put("broadcast", buildJsonObject { put("self", false) })
                    })
                }, joinRef = "1")
                delay(600)

                val broadcastMsg = buildJsonObject {
                    put("topic", "realtime:duel-invites")
                    put("event", "broadcast")
                    put("payload", buildJsonObject {
                        put("type",  "broadcast")
                        put("event", "invite")
                        put("payload", buildJsonObject {
                            put("invitedUsername", targetUsername.lowercase())
                            put("code",            code)
                            put("inviterName",     from)
                            put("section",         section.name.lowercase())
                            put("difficulty",      diff.name.lowercase())
                        })
                    })
                    put("ref",      "")
                    put("join_ref", "1")
                }
                send(json.encodeToString(broadcastMsg))
                delay(300)
                close()
            }
        } catch (_: Exception) {}
    }

    /** Low-level Phoenix protocol frame sender. */
    private suspend fun sendRaw(
        session: DefaultClientWebSocketSession,
        topic: String,
        event: String,
        payload: JsonObject,
        ref: String = "1",
        joinRef: String? = null
    ) {
        val msg = buildJsonObject {
            put("topic",   topic)
            put("event",   event)
            put("payload", payload)
            put("ref",     ref)
            if (joinRef != null) put("join_ref", joinRef)
        }
        session.send(json.encodeToString(msg))
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        inviteJob?.cancel()
        searchJob?.cancel()
        opponentTimeoutJob?.cancel()
        val sessionToClose = wsSession
        wsSession = null
        CoroutineScope(Dispatchers.IO).launch {
            try { sessionToClose?.close(CloseReason(CloseReason.Codes.NORMAL, "ViewModel cleared")) }
            catch (_: Exception) {}
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
