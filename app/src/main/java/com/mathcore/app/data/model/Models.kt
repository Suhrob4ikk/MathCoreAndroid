package com.mathcore.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val xp: Int = 0,
    val streak: Int = 0,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class TestResult(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val username: String,
    val section: String,
    val difficulty: String,
    val score: Int,
    @SerialName("correct_answers") val correctAnswers: Int,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class LeaderboardEntry(
    val username: String = "",
    val score: Int = 0,
    @SerialName("correct_answers") val correctAnswers: Int = 0,
    @SerialName("total_questions") val totalQuestions: Int = 0,
    val section: String = "",
    val difficulty: String = "medium",
    @SerialName("created_at") val createdAt: String? = null
)
