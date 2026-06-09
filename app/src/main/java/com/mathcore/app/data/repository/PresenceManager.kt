package com.mathcore.app.data.repository

import android.util.Log
import com.mathcore.app.util.AppHttpClient
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

/**
 * Joins the Supabase Realtime presence channel `realtime:user-active:{userId}` after
 * a successful sign-in so that the web client's setupSessionGuard() can detect an
 * active Android session and warn the web user.
 *
 * Usage:
 *   PresenceManager.track(userId, username)   // after sign-in / session restore
 *   PresenceManager.untrack()                 // on sign-out
 *
 * This is a best-effort, fire-and-forget feature — any WebSocket errors are logged
 * and swallowed so they never surface to the user. The feature is strictly additive:
 * if the channel cannot be joined, normal app operation is unaffected.
 *
 * Protocol overview (Supabase Realtime / Phoenix channels):
 *   1. Connect to wss://{host}/realtime/v1/websocket?apikey=…&vsn=1.0.0
 *   2. Send phx_join to topic "realtime:user-active:{userId}"
 *   3. On phx_reply {status:ok} → send presence track event with username + platform
 *   4. Send heartbeat {"topic":"phoenix","event":"heartbeat",...} every 25 s
 *   5. On untrack() — cancel the coroutine (WebSocket closes automatically)
 */
object PresenceManager {
    private const val TAG = "PresenceManager"

    /**
     * Dedicated IO scope for the long-running WebSocket coroutine.
     * Uses SupervisorJob so one failed job does not cancel the scope itself.
     */
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var presenceJob: Job? = null

    /**
     * Joins the presence channel for [userId] and tracks presence with [username].
     * Any previously running session is cancelled before starting a new one.
     */
    fun track(userId: String, username: String) {
        untrack()   // cancel stale session (e.g. previous account)
        presenceJob = managerScope.launch {
            runTracking(userId, username)
        }
    }

    /** Cancels the active presence session (WebSocket closes gracefully). */
    fun untrack() {
        presenceJob?.cancel()
        presenceJob = null
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun runTracking(userId: String, username: String) {
        val topic = "realtime:user-active:$userId"
        var ref   = 1

        try {
            AppHttpClient.authenticated.webSocket(
                urlString = "${SupabaseConfig.REALTIME_WS}?apikey=${SupabaseConfig.KEY}&vsn=1.0.0"
            ) {
                // ── Step 1: Join the presence channel ────────────────────────
                val joinPayload = buildJsonObject {
                    put("config", buildJsonObject {
                        put("broadcast", buildJsonObject {
                            put("ack", false)
                            put("self", false)
                        })
                        put("presence", buildJsonObject {
                            put("key", "")
                        })
                    })
                }
                send(msg(topic, "phx_join", joinPayload, "${ref++}", joinRef = "1"))

                // ── Step 2: Start heartbeat loop (required by Supabase) ───────
                val heartbeatJob = launch {
                    while (isActive) {
                        delay(25_000)
                        send(msg("phoenix", "heartbeat", JsonObject(emptyMap()), "${ref++}"))
                    }
                }

                // ── Step 3: Wait for join_ok, then send presence track ────────
                var joined = false
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val obj = runCatching {
                        json.parseToJsonElement(frame.readText()).jsonObject
                    }.getOrNull() ?: continue

                    val event  = (obj["event"]  as? JsonPrimitive)?.content
                    val status = ((obj["payload"] as? JsonObject)
                        ?.get("status") as? JsonPrimitive)?.content

                    if (!joined && event == "phx_reply" && status == "ok") {
                        joined = true
                        val trackPayload = buildJsonObject {
                            put("event", "track")
                            put("payload", buildJsonObject {
                                put("online_at", java.time.Instant.now().toString())
                                put("username",  username)
                                put("platform",  "android")
                            })
                        }
                        send(msg(topic, "presence", trackPayload, "${ref++}", joinRef = "1"))
                        Log.d(TAG, "Presence tracked: user=$username, channel=$topic")
                    }
                    // Continue reading to keep the connection alive (heartbeat replies, etc.)
                }
                heartbeatJob.cancel()
            }
        } catch (_: CancellationException) {
            // Normal — cancelled by untrack() or app shutdown
            Log.d(TAG, "Presence session cancelled for userId=$userId")
        } catch (e: Exception) {
            // Non-fatal — WebSocket may fail on poor network; app works fine without it
            Log.w(TAG, "Presence tracking error: ${e.message}")
        }
    }

    /**
     * Builds a Phoenix-channel protocol message as a JSON string.
     * [joinRef] is only included in channel messages (not heartbeats).
     */
    private fun msg(
        topic:   String,
        event:   String,
        payload: JsonElement,
        ref:     String,
        joinRef: String? = null
    ): String = json.encodeToString(buildJsonObject {
        put("topic",   topic)
        put("event",   event)
        put("payload", payload)
        put("ref",     ref)
        if (joinRef != null) put("join_ref", joinRef)
    })
}
