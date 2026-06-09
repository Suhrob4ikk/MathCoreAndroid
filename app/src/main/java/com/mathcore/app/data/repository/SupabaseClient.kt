package com.mathcore.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.mathcore.app.BuildConfig
import kotlinx.serialization.json.Json

// ── Supabase configuration ─────────────────────────────────────────────────
// Keys are read from BuildConfig, which is populated from local.properties
// at build time. The local.properties file must NOT be committed to VCS.
object SupabaseConfig {
    val URL: String         get() = BuildConfig.SUPABASE_URL
    val KEY: String         get() = BuildConfig.SUPABASE_KEY
    val REST_URL: String    get() = "$URL/rest/v1"
    val AUTH_URL: String    get() = "$URL/auth/v1"
    val REALTIME_WS: String get() = "wss://${URL.removePrefix("https://")}/realtime/v1/websocket"
}

// ── Shared Json instance (used by AppHttpClient and repositories) ──────────
val json = Json {
    ignoreUnknownKeys = true
    isLenient          = true
    // encodeDefaults = false so that fields like id=null are NOT serialized on INSERT,
    // which would otherwise cause Supabase to reject the request (NOT NULL constraint).
    encodeDefaults     = false
}

// ── Session storage with SharedPreferences persistence ────────────────────
object SessionManager {
    var accessToken:  String? = null
    var refreshToken: String? = null
    var userId:       String? = null
    var username:     String? = null
    var email:        String? = null

    private const val PREFS = "mathcore_session_enc"

    /**
     * Stored on [init] so [updateToken] can persist refreshed tokens without
     * requiring a Context parameter. Holds applicationContext — no leak.
     */
    private var appContext: Context? = null

    /**
     * Returns an EncryptedSharedPreferences instance backed by AES256-GCM.
     * Falls back to plain SharedPreferences if the device doesn't support the
     * required key scheme (very rare; only affects old API 23 devices without HSM).
     */
    private fun prefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Extremely rare fallback — device keystore unavailable
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    /** Restore session on app start. Call from Application.onCreate(). */
    fun init(context: Context) {
        appContext = context.applicationContext
        val p = prefs(context)
        accessToken  = p.getString("access_token",  null)
        refreshToken = p.getString("refresh_token", null)
        userId       = p.getString("user_id",       null)
        username     = p.getString("username",      null)
        email        = p.getString("email",         null)
        // [БАГ-7] Re-join Realtime presence channel on session restore (cold start / process kill).
        // Ensures web's setupSessionGuard() sees the Android session even after app restart.
        if (isLoggedIn()) {
            PresenceManager.track(userId!!, username ?: "unknown")
        }
    }

    /** Persist session to disk. Call after a successful sign-in. */
    fun save(context: Context) {
        prefs(context).edit().apply {
            putString("access_token",  accessToken)
            putString("refresh_token", refreshToken)
            putString("user_id",       userId)
            putString("username",      username)
            putString("email",         email)
            apply()
        }
    }

    /** Clear session on sign-out. */
    fun clearAndSave(context: Context) {
        clear()
        prefs(context).edit().clear().apply()
    }

    fun isLoggedIn() = !accessToken.isNullOrBlank() && !userId.isNullOrBlank()

    /**
     * Update the in-memory token after a successful refresh.
     * Also persists the new tokens immediately so they survive a process kill.
     * The appContext stored in [init] is the applicationContext — safe to hold statically.
     */
    fun updateToken(newAccess: String, newRefresh: String? = null) {
        accessToken = newAccess
        if (newRefresh != null) refreshToken = newRefresh
        appContext?.let { ctx ->
            prefs(ctx).edit().apply {
                putString("access_token", newAccess)
                if (newRefresh != null) putString("refresh_token", newRefresh)
                apply()
            }
        }
    }

    fun clear() {
        accessToken  = null; refreshToken = null
        userId       = null; username     = null; email = null
    }
}
