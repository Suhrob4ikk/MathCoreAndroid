package com.mathcore.app.data.repository

import com.mathcore.app.data.Question
import com.mathcore.app.util.AppHttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

// ── Supabase row types ────────────────────────────────────────────────────────

@Serializable
data class MistakeRow(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("question_hash") val questionHash: String? = null,
    @SerialName("question_data") val questionData: JsonObject? = null,
    val subject: String? = null,
    val difficulty: String? = null,
    @SerialName("mistake_count") val mistakeCount: Int = 1,
    val corrected: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
private data class MistakeIdRow(
    val id: String,
    @SerialName("mistake_count") val mistakeCount: Int = 1
)

@Serializable
private data class MistakeInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("question_hash") val questionHash: String,
    @SerialName("question_data") val questionData: JsonObject,
    val subject: String,
    val difficulty: String,
    @SerialName("mistake_count") val mistakeCount: Int = 1,
    val corrected: Boolean = false
)

@Serializable
private data class MistakeCountUpdate(
    @SerialName("mistake_count") val mistakeCount: Int,
    val corrected: Boolean = false,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
private data class MistakeCorrected(
    val corrected: Boolean = true,
    @SerialName("updated_at") val updatedAt: String
)

// ── Repository ────────────────────────────────────────────────────────────────

class MistakesRepository {

    private val _mistakes = MutableStateFlow<List<Question>>(emptyList())
    val mistakes: StateFlow<List<Question>> = _mistakes.asStateFlow()

    /** Load active (uncorrected) mistakes from Supabase for the signed-in user. */
    suspend fun loadFromServer() {
        val userId = SessionManager.userId ?: return
        try {
            val rows = AppHttpClient.authenticated
                .get("${SupabaseConfig.REST_URL}/user_mistakes") {
                    parameter("user_id",   "eq.$userId")
                    parameter("corrected", "eq.false")
                    parameter("select",    "*")
                    parameter("order",     "updated_at.desc")
                    parameter("limit",     "500")
                }.body<List<MistakeRow>>()
            _mistakes.value = rows.mapNotNull { it.toQuestion() }
        } catch (_: Exception) {}
    }

    /**
     * Save wrong [questions] from a test to Supabase.
     * Increments count if already present; inserts if new.
     * Also updates local StateFlow optimistically.
     */
    suspend fun addMistakes(questions: List<Question>, section: String, difficulty: String) {
        val userId = SessionManager.userId ?: return
        val existing = _mistakes.value.map { it.question }.toHashSet()

        for (q in questions) {
            val hash = hashQuestion(q.question)
            try {
                // Check if this question is already recorded
                val existing_row = AppHttpClient.authenticated
                    .get("${SupabaseConfig.REST_URL}/user_mistakes") {
                        parameter("user_id",       "eq.$userId")
                        parameter("question_hash", "eq.$hash")
                        parameter("select",        "id,mistake_count")
                        parameter("limit",         "1")
                    }.body<List<MistakeIdRow>>().firstOrNull()

                if (existing_row != null) {
                    // Update mistake count
                    AppHttpClient.authenticated
                        .patch("${SupabaseConfig.REST_URL}/user_mistakes") {
                            parameter("id", "eq.${existing_row.id}")
                            contentType(ContentType.Application.Json)
                            header("Prefer", "return=minimal")
                            setBody(MistakeCountUpdate(
                                mistakeCount = existing_row.mistakeCount + 1,
                                updatedAt    = java.time.Instant.now().toString()
                            ))
                        }
                } else {
                    // Insert new mistake
                    AppHttpClient.authenticated
                        .post("${SupabaseConfig.REST_URL}/user_mistakes") {
                            contentType(ContentType.Application.Json)
                            header("Prefer", "return=minimal")
                            setBody(MistakeInsert(
                                userId       = userId,
                                questionHash = hash,
                                questionData = q.toJsonObject(),
                                subject      = section,
                                difficulty   = difficulty
                            ))
                        }
                }
            } catch (_: Exception) {}
        }

        // Update local state: add questions not already tracked
        _mistakes.value = _mistakes.value + questions.filter { it.question !in existing }
    }

    /** Mark a mistake as corrected in Supabase and remove it from local state. */
    suspend fun removeMistake(question: Question) {
        val userId = SessionManager.userId ?: return
        val hash = hashQuestion(question.question)
        try {
            AppHttpClient.authenticated
                .patch("${SupabaseConfig.REST_URL}/user_mistakes") {
                    parameter("user_id",       "eq.$userId")
                    parameter("question_hash", "eq.$hash")
                    contentType(ContentType.Application.Json)
                    header("Prefer", "return=minimal")
                    setBody(MistakeCorrected(updatedAt = java.time.Instant.now().toString()))
                }
        } catch (_: Exception) {}
        _mistakes.value = _mistakes.value.filter { it.question != question.question }
    }

    /** Mark all mistakes as corrected in Supabase and clear local state. */
    suspend fun clearMistakes() {
        val userId = SessionManager.userId ?: return
        try {
            AppHttpClient.authenticated
                .patch("${SupabaseConfig.REST_URL}/user_mistakes") {
                    parameter("user_id",   "eq.$userId")
                    parameter("corrected", "eq.false")
                    contentType(ContentType.Application.Json)
                    header("Prefer", "return=minimal")
                    setBody(MistakeCorrected(updatedAt = java.time.Instant.now().toString()))
                }
        } catch (_: Exception) {}
        _mistakes.value = emptyList()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Matches web's hashQuestion(text) in js/mistakes.js:
     *   h = (Math.imul(31, h) + charCode) | 0  →  32-bit signed multiply
     *   Math.abs(h).toString(36)
     */
    private fun hashQuestion(text: String): String {
        val str = text.take(120)
        var h = 0
        for (c in str) {
            h = 31 * h + c.code  // Int arithmetic wraps same as JS Math.imul(31,h)|0
        }
        return Math.abs(h.toLong()).toString(36)
    }

    private fun Question.toJsonObject(): JsonObject = buildJsonObject {
        put("question", question)
        put("options",  buildJsonArray { options.forEach { add(it) } })
        put("correct",  correct)
        if (type == "open" && answer.isNotEmpty()) put("open", answer)
    }

    private fun MistakeRow.toQuestion(): Question? {
        val data = questionData ?: return null
        val questionText = data["question"]?.jsonPrimitive?.contentOrNull ?: return null
        val options = data["options"]?.jsonArray?.map {
            it.jsonPrimitive.contentOrNull ?: ""
        } ?: emptyList()
        val correct = data["correct"]?.jsonPrimitive?.intOrNull ?: 0
        val open    = data["open"]?.jsonPrimitive?.contentOrNull
        return Question(
            question = questionText,
            options  = options,
            correct  = correct,
            type     = if (open != null) "open" else "choice",
            answer   = open ?: ""
        )
    }
}
