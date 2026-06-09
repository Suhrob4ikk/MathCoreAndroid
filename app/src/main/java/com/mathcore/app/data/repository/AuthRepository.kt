package com.mathcore.app.data.repository

import android.content.Context
import com.mathcore.app.data.model.Profile
import com.mathcore.app.util.AppHttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class SignUpBody(val email: String, val password: String, val data: UserMetaBody)
@Serializable data class UserMetaBody(val username: String)
@Serializable data class SignInBody(val email: String, val password: String)

@Serializable
data class AuthResponse(
    @SerialName("access_token")  val accessToken: String?  = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: AuthUser? = null,
    @SerialName("error_description") val errorDescription: String? = null,
    val error: String? = null,
    val msg: String? = null,
    val message: String? = null
) {
    fun errorMessage() = errorDescription ?: message ?: msg ?: error
}

@Serializable
data class AuthUser(
    val id: String,
    val email: String? = null,
    @SerialName("user_metadata") val userMetadata: UserMeta? = null
)
@Serializable data class UserMeta(val username: String? = null)
@Serializable data class ProfileEmailRow(val email: String? = null)

class AuthRepository(private val context: Context) {

    private suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            val rows = AppHttpClient.default.get("${SupabaseConfig.REST_URL}/profiles") {
                parameter("username", "ilike.$username")
                parameter("select", "id")
                parameter("limit", "1")
            }.body<List<Map<String, String>>>()
            rows.isEmpty()
        } catch (_: Exception) { true }
    }

    suspend fun signUp(email: String, password: String, username: String): Result<String> {
        if (!isUsernameAvailable(username))
            return Result.failure(Exception("Это имя пользователя уже занято"))
        return try {
            val httpResp = AppHttpClient.default.post("${SupabaseConfig.AUTH_URL}/signup") {
                contentType(ContentType.Application.Json)
                setBody(SignUpBody(email, password, UserMetaBody(username)))
            }
            val body = httpResp.bodyAsText()
            if (!httpResp.status.isSuccess()) {
                val raw = runCatching { json.decodeFromString<AuthResponse>(body).errorMessage() }
                    .getOrNull() ?: "Ошибка ${httpResp.status.value}"
                return Result.failure(Exception(friendlyAuthError(raw, isLogin = false)))
            }
            val resp = runCatching { json.decodeFromString<AuthResponse>(body) }.getOrNull()
                ?: return Result.failure(Exception("Ошибка ответа сервера"))

            when {
                resp.accessToken != null && resp.user != null -> {
                    applySession(resp.accessToken, resp.refreshToken, resp.user.id, username, email)
                    upsertProfile(resp.user.id, username, email)
                    Result.success("OK")
                }
                resp.user != null -> Result.success("EMAIL_CONFIRM")
                else -> Result.failure(Exception(friendlyAuthError(resp.errorMessage(), isLogin = false)))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка сети: ${e.message}"))
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            val httpResp = AppHttpClient.default.post(
                "${SupabaseConfig.AUTH_URL}/token?grant_type=password"
            ) {
                contentType(ContentType.Application.Json)
                setBody(SignInBody(email, password))
            }
            if (!httpResp.status.isSuccess()) {
                val body = httpResp.bodyAsText()
                val raw = runCatching { json.decodeFromString<AuthResponse>(body).errorMessage() }
                    .getOrNull() ?: ""
                return Result.failure(Exception(friendlyAuthError(raw, isLogin = true)))
            }
            val resp = httpResp.body<AuthResponse>()
            if (resp.accessToken != null && resp.user != null) {
                val name = resp.user.userMetadata?.username ?: email.substringBefore("@")
                applySession(resp.accessToken, resp.refreshToken, resp.user.id, name, email)
                // Гарантируем существование профиля — на случай если при регистрации
                // было включено email-подтверждение и профиль тогда не создавался
                upsertProfile(resp.user.id, name, email)
                Result.success(Unit)
            } else {
                Result.failure(Exception(friendlyAuthError(resp.errorMessage(), isLogin = true)))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка сети: ${e.message}"))
        }
    }

    suspend fun signOut() {
        try { AppHttpClient.authenticated.post("${SupabaseConfig.AUTH_URL}/logout") }
        catch (_: Exception) {}
        SessionManager.clearAndSave(context)
    }

    suspend fun findEmailByUsername(username: String): String? {
        return try {
            AppHttpClient.default.post("${SupabaseConfig.REST_URL}/rpc/get_email_by_username") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("p_username" to username))
            }.bodyAsText().trim('"').ifEmpty { null }
        } catch (_: Exception) { null }
    }

    suspend fun getCurrentProfile(): Profile? {
        val userId = SessionManager.userId ?: return null
        return try {
            AppHttpClient.authenticated
                .get("${SupabaseConfig.REST_URL}/profiles") {
                    parameter("id", "eq.$userId")
                    parameter("select", "*")
                    parameter("limit", "1")
                }.body<List<Profile>>()
                .firstOrNull()?.also { SessionManager.username = it.username }
        } catch (_: Exception) { null }
    }

    private fun applySession(token: String, refresh: String?, userId: String, username: String, email: String) {
        SessionManager.accessToken  = token
        SessionManager.refreshToken = refresh
        SessionManager.userId       = userId
        SessionManager.username     = username
        SessionManager.email        = email
        SessionManager.save(context)    // ← сохраняем на диск для авто-входа
    }

    suspend fun updateLastSeen() {
        val userId = SessionManager.userId ?: return
        try {
            AppHttpClient.authenticated
                .patch("${SupabaseConfig.REST_URL}/profiles") {
                    parameter("id", "eq.$userId")
                    contentType(ContentType.Application.Json)
                    header("Prefer", "return=minimal")
                    setBody(mapOf("last_seen_at" to java.time.Instant.now().toString()))
                }
        } catch (_: Exception) {}
    }

    suspend fun uploadAvatar(imageBytes: ByteArray, userId: String): String? {
        return try {
            val uploadUrl = "${SupabaseConfig.URL}/storage/v1/object/avatars/$userId.jpg"
            val response = AppHttpClient.authenticated
                .put(uploadUrl) {
                    contentType(ContentType.parse("image/jpeg"))
                    setBody(imageBytes)
                    header("x-upsert", "true")
                }
            if (response.status.isSuccess()) {
                val publicUrl = "${SupabaseConfig.URL}/storage/v1/object/public/avatars/$userId.jpg"
                // Save avatar_url to profile
                AppHttpClient.authenticated
                    .patch("${SupabaseConfig.REST_URL}/profiles") {
                        parameter("id", "eq.$userId")
                        contentType(ContentType.Application.Json)
                        header("Prefer", "return=minimal")
                        setBody(mapOf("avatar_url" to publicUrl))
                    }
                publicUrl
            } else null
        } catch (_: Exception) { null }
    }

    private suspend fun upsertProfile(userId: String, username: String, email: String) {
        try {
            AppHttpClient.authenticated
                .post("${SupabaseConfig.REST_URL}/profiles") {
                    header("Prefer", "resolution=merge-duplicates")
                    contentType(ContentType.Application.Json)
                    setBody(Profile(id = userId, username = username, email = email))
                }
        } catch (_: Exception) {}
    }
}

/** Converts raw Supabase / network error strings to user-friendly Russian. */
internal fun friendlyAuthError(raw: String?, isLogin: Boolean): String {
    val s = raw?.lowercase() ?: ""
    return when {
        s.isEmpty()                                   -> if (isLogin) "Неверный логин или пароль" else "Ошибка регистрации"
        s.contains("invalid login credentials")       -> "Неверный email/логин или пароль"
        s.contains("invalid credentials")             -> "Неверный email/логин или пароль"
        s.contains("email not confirmed")             -> "Email не подтверждён — проверь почту"
        s.contains("user not found")                  -> "Пользователь не найден"
        s.contains("already registered") ||
            s.contains("user already exists") ||
            s.contains("already")                     -> "Этот email уже зарегистрирован — войди во вкладке «Войти»"
        s.contains("password") &&
            s.contains("characters")                  -> "Пароль слишком короткий — минимум 6 символов"
        s.contains("rate limit") ||
            s.contains("too many requests")           -> "Слишком много попыток — подожди несколько минут"
        s.contains("network") ||
            s.contains("unable to connect") ||
            s.contains("connection")                  -> "Нет подключения к интернету"
        s.contains("email") && s.contains("valid")    -> "Некорректный email-адрес"
        s.contains("signup disabled")                 -> "Регистрация временно недоступна"
        s.contains("weak password")                   -> "Слишком простой пароль — придумай сложнее"
        else                                          -> raw ?: if (isLogin) "Ошибка входа" else "Ошибка регистрации"
    }
}
