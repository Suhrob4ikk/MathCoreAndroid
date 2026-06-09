package com.mathcore.app.util

import com.mathcore.app.data.repository.SessionManager
import com.mathcore.app.data.repository.SupabaseConfig
import com.mathcore.app.data.repository.json
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

/**
 * Application-wide shared HttpClient singletons.
 *
 * NEVER call buildHttpClient() directly in repositories or ViewModels.
 * Use [default] for public (anon-key) requests and [authenticated] for
 * requests that require a user session.
 *
 * Both clients are created lazily on first access and reused for the
 * entire application lifetime — this avoids the connection-pool and
 * thread-pool leaks that occur when a new client is created per request.
 *
 * BUG-CRIT-4 FIX: Access token refresh on 401.
 *   The [authenticated] client uses Ktor's `Auth` bearer plugin which:
 *   - Eagerly attaches the current access token to every request
 *   - On a 401 response, calls the `refreshTokens` block exactly once
 *   - The block exchanges [SessionManager.refreshToken] for new tokens,
 *     updates [SessionManager], and retries the original request automatically
 *   - Thread-safe — Ktor's bearer provider uses its own internal mutex
 *   If the refresh itself fails (no refresh token, network error, expired
 *   refresh token), the 401 is propagated to the caller and
 *   [SessionManager.clear] is called so the app can redirect to login.
 */
object AppHttpClient {

    // ── Public clients ─────────────────────────────────────────────────────────

    /**
     * Unauthenticated client — uses only the Supabase anon key.
     * Suitable for public endpoints (sign-up, sign-in, public profiles).
     */
    val default: HttpClient by lazy { buildDefaultClient() }

    /**
     * Authenticated client — reads [SessionManager.accessToken] via the
     * Ktor bearer Auth plugin. On 401 the token is refreshed transparently.
     */
    val authenticated: HttpClient by lazy { buildAuthenticatedClient() }

    /**
     * Release underlying resources on application shutdown.
     * Called from [Application.onTerminate()].
     */
    fun close() {
        runCatching { default.close() }
        runCatching { authenticated.close() }
    }

    // ── Private factories ──────────────────────────────────────────────────────

    private fun buildDefaultClient(): HttpClient = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
        install(WebSockets)
        engine { connectTimeout = 15_000; socketTimeout = 30_000 }
        defaultRequest {
            if (!headers.contains("apikey"))
                headers.append("apikey", SupabaseConfig.KEY)
            if (!headers.contains(HttpHeaders.Authorization))
                headers.append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.KEY}")
        }
    }

    private fun buildAuthenticatedClient(): HttpClient = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
        install(WebSockets)
        engine { connectTimeout = 15_000; socketTimeout = 30_000 }

        // Add Supabase apikey to every request (REST + WS handshake).
        // The Auth plugin adds Authorization: Bearer separately.
        defaultRequest {
            if (!headers.contains("apikey"))
                headers.append("apikey", SupabaseConfig.KEY)
        }

        // BUG-CRIT-4 FIX: bearer token + automatic refresh on 401
        install(Auth) {
            bearer {
                // Called once per request to supply the current tokens.
                loadTokens {
                    BearerTokens(
                        accessToken  = SessionManager.accessToken  ?: SupabaseConfig.KEY,
                        refreshToken = SessionManager.refreshToken ?: ""
                    )
                }

                // Called automatically when the server returns 401.
                // Exchanges the stored refresh token for a fresh access token.
                // Returns null (and clears the session) if the exchange fails.
                refreshTokens {
                    val rt = SessionManager.refreshToken
                    if (rt.isNullOrBlank()) {
                        SessionManager.clear()
                        return@refreshTokens null
                    }
                    try {
                        val resp = client.post(
                            "${SupabaseConfig.URL}/auth/v1/token?grant_type=refresh_token"
                        ) {
                            contentType(ContentType.Application.Json)
                            setBody(buildJsonObject { put("refresh_token", rt) })
                            markAsRefreshTokenRequest()   // prevents Auth plugin re-entry
                        }
                        if (!resp.status.isSuccess()) {
                            SessionManager.clear()
                            return@refreshTokens null
                        }
                        val obj = runCatching {
                            json.parseToJsonElement(resp.bodyAsText()).jsonObject
                        }.getOrNull() ?: run { SessionManager.clear(); return@refreshTokens null }
                        val newAccess  = obj["access_token"]?.jsonPrimitive?.content
                            ?: run { SessionManager.clear(); return@refreshTokens null }
                        val newRefresh = obj["refresh_token"]?.jsonPrimitive?.content ?: rt
                        SessionManager.updateToken(newAccess, newRefresh)
                        BearerTokens(newAccess, newRefresh)
                    } catch (_: Exception) {
                        null
                    }
                }

                // Always send token proactively (don't wait for a 401 challenge).
                sendWithoutRequest { true }
            }
        }
    }
}
