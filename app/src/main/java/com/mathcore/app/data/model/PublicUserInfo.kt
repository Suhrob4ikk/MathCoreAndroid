package com.mathcore.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicUserInfo(
    val id: String = "",
    val username: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null
) {
    val isOnline: Boolean get() {
        val ls = lastSeenAt ?: return false
        return try {
            val seen = java.time.Instant.parse(ls).toEpochMilli()
            System.currentTimeMillis() - seen < 5 * 60_000
        } catch (_: Exception) { false }
    }
    val lastSeenText: String get() {
        val ls = lastSeenAt ?: return ""
        return try {
            val diff = System.currentTimeMillis() - java.time.Instant.parse(ls).toEpochMilli()
            when {
                diff < 60_000 -> "Онлайн"
                diff < 3_600_000 -> "Был ${diff / 60_000} мин. назад"
                diff < 86_400_000 -> "Был ${diff / 3_600_000} ч. назад"
                else -> "Давно не заходил"
            }
        } catch (_: Exception) { "" }
    }
}
