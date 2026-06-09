package com.mathcore.app.data.repository

import android.content.Context
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

    private const val PREFS = "mathcore_session"

    /**
     * Stored on [init] so [updateToken] can persist refreshed tokens without
     * requiring a Context parameter. Holds applicationContext — no leak.
     */
    private var appContext: Context? = null

    /** Restore session on app start. Call from Application.onCreate(). */
    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        accessToken  = p.getString("access_token",  null)
        refreshToken = p.getString("refresh_token", null)
        userId       = p.getString("user_id",       null)
        username     = p.getString("username",      null)
        email        = p.getString("email",         null)
    }

    /** Persist session to disk. Call after a successful sign-in. */
    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
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
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun isLoggedIn() = !accessToken.isNullOrBlank() && !userId.isNullOrBlank()

    /**
     * BUG-CRIT-4 FIX: Update the in-memory token after a successful refresh.
     * Also persists the new tokens to SharedPreferences immediately so they
     * survive a process kill between refreshes. The appContext stored in [init]
     * is the applicationContext — safe to hold statically.
     */
    fun updateToken(newAccess: String, newRefresh: String? = null) {
        accessToken = newAccess
        if (newRefresh != null) refreshToken = newRefresh
        // Persist immediately — avoids extra network round-trip after process restart
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.apply {
            putString("access_token", newAccess)
            if (newRefresh != null) putString("refresh_token", newRefresh)
            apply()
        }
    }

    fun clear() {
        accessToken  = null; refreshToken = null
        userId       = null; username     = null; email = null
    }
}
