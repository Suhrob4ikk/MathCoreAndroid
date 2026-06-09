package com.mathcore.app.data.repository

import android.util.Log
import com.mathcore.app.data.model.LeaderboardEntry
import com.mathcore.app.data.model.PublicUserInfo
import com.mathcore.app.data.model.TestResult
import com.mathcore.app.util.AppHttpClient
import com.mathcore.app.util.computeXp
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ProfileAvatar(
    val username: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null
)

class ResultsRepository {

    suspend fun saveResult(result: TestResult): Result<Unit> {
        return try {
            val client = AppHttpClient.authenticated
            val response = client.post("${SupabaseConfig.REST_URL}/test_results") {
                contentType(ContentType.Application.Json)
                setBody(result)
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${response.status.value}: ${response.bodyAsText()}"))
        } catch (e: Exception) {
            Log.e("ResultsRepository", "saveResult failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Returns (leaderboard, errorMessage).
     *
     * Global (section=null): aggregates ALL test results per user → total accumulated XP.
     * Section filter: shows the user's best single result in that section.
     */
    suspend fun getLeaderboardResult(section: String? = null): Pair<List<LeaderboardEntry>, String?> {
        return try {
            val client = AppHttpClient.authenticated
            val response = client.get("${SupabaseConfig.REST_URL}/test_results") {
                parameter("select", "username,score,correct_answers,total_questions,section,difficulty,created_at")
                parameter("order", "created_at.desc")
                // For global view pull more rows so we can aggregate properly
                parameter("limit", if (section == null) "2000" else "500")
                if (section != null) parameter("section", "eq.$section")
            }
            if (!response.status.isSuccess()) {
                val body = response.bodyAsText()
                Log.e("ResultsRepository", "getLeaderboard HTTP ${response.status.value}: $body")
                return emptyList<LeaderboardEntry>() to "Ошибка сервера: ${response.status.value}"
            }
            val all = response.body<List<LeaderboardEntry>>()

            val sorted = if (section == null) {
                // Global leaderboard: best attempt per unique section+difficulty combo per user.
                // Mirrors web stats.js calcRatingPoints + per-combo dedup strategy exactly.
                all.filter { it.username.isNotBlank() && !it.section.startsWith("duel") }
                    .groupBy { it.username }
                    .map { (username, entries) ->
                        val bestPerCombo = entries
                            .groupBy { "${it.section}_${it.difficulty}" }
                            .values
                            .map { combo ->
                                combo.maxByOrNull { computeXp(it.correctAnswers, it.difficulty, it.score) }!!
                            }
                        val totalXp    = bestPerCombo.sumOf { computeXp(it.correctAnswers, it.difficulty, it.score) }
                        val totalTests = bestPerCombo.size
                        val bestScore  = bestPerCombo.maxOf { it.score }
                        LeaderboardEntry(
                            username       = username,
                            score          = bestScore,
                            correctAnswers = totalXp,
                            totalQuestions = totalTests,
                            section        = "all",
                            difficulty     = "mixed"
                        )
                    }
                    .sortedByDescending { it.correctAnswers }
                    .take(50)
            } else {
                // Section leaderboard: best single result per user in this section
                all.filter { it.username.isNotBlank() }
                    .groupBy { it.username }
                    .map { (_, entries) ->
                        entries.maxByOrNull { computeXp(it.correctAnswers, it.difficulty, it.score) }!!
                    }
                    .sortedByDescending { computeXp(it.correctAnswers, it.difficulty, it.score) }
                    .take(50)
            }

            sorted to null
        } catch (e: Exception) {
            Log.e("ResultsRepository", "getLeaderboard failed: ${e.message}", e)
            emptyList<LeaderboardEntry>() to "Ошибка загрузки: ${e.message?.take(80)}"
        }
    }

    // Keep backward-compatible version
    suspend fun getLeaderboard(section: String? = null): List<LeaderboardEntry> =
        getLeaderboardResult(section).first

    suspend fun getUserResults(userId: String): List<TestResult> {
        return try {
            val client = AppHttpClient.authenticated
            client.get("${SupabaseConfig.REST_URL}/test_results") {
                parameter("user_id", "eq.$userId")
                parameter("order", "created_at.desc")
                parameter("limit", "2000")
            }.body<List<TestResult>>()
        } catch (e: Exception) {
            Log.e("ResultsRepository", "getUserResults failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun searchUsers(query: String): List<PublicUserInfo> {
        return try {
            val client = AppHttpClient.authenticated
            client.get("${SupabaseConfig.REST_URL}/profiles") {
                parameter("select", "id,username,avatar_url,created_at,last_seen_at")
                parameter("username", "ilike.${query}%")
                parameter("limit", "20")
            }.body<List<PublicUserInfo>>()
        } catch (e: Exception) {
            Log.e("ResultsRepository", "searchUsers failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetches today's daily leaderboard entries.
     * Returns (list, errorMessage) — null error means success.
     */
    suspend fun getDailyLeaderboard(): Pair<List<TestResult>, String?> {
        return try {
            // Use local midnight converted to UTC — matches web's getDailyLeaderboard().
            val today = java.time.LocalDate.now()
            val zone  = java.time.ZoneId.systemDefault()
            val startUtc = today.atStartOfDay(zone)
                .withZoneSameInstant(java.time.ZoneOffset.UTC)
                .toInstant().toString()
            val endUtc = today.plusDays(1).atStartOfDay(zone)
                .withZoneSameInstant(java.time.ZoneOffset.UTC)
                .toInstant().toString()
            val client = AppHttpClient.authenticated
            val rows = client.get("${SupabaseConfig.REST_URL}/test_results") {
                parameter("section", "eq.daily")
                parameter("created_at", "gte.$startUtc")
                parameter("created_at", "lt.$endUtc")
                parameter("order", "score.desc,created_at.asc")
                parameter("limit", "100")
                parameter("select", "user_id,username,section,difficulty,score,correct_answers,total_questions,created_at")
            }
            if (!rows.status.isSuccess()) {
                val body = rows.bodyAsText()
                Log.e("ResultsRepository", "getDailyLeaderboard HTTP ${rows.status.value}: $body")
                return emptyList<TestResult>() to "Ошибка сервера: ${rows.status.value}"
            }
            rows.body<List<TestResult>>() to null
        } catch (e: Exception) {
            Log.e("ResultsRepository", "getDailyLeaderboard failed: ${e.message}")
            emptyList<TestResult>() to "Ошибка загрузки: ${e.message?.take(80)}"
        }
    }

    /**
     * Batch-fetches avatar_url for a list of usernames from the profiles table.
     * Returns map username → avatarUrl (null if no avatar).
     */
    suspend fun getAvatarUrls(usernames: List<String>): Map<String, String?> {
        if (usernames.isEmpty()) return emptyMap()
        return try {
            val client = AppHttpClient.authenticated
            client.get("${SupabaseConfig.REST_URL}/profiles") {
                parameter("select", "username,avatar_url")
                // PostgREST in() filter: ?username=in.(val1,val2,...)
                parameter("username", "in.(${usernames.take(100).joinToString(",")})")
            }.body<List<ProfileAvatar>>()
                .associate { it.username to it.avatarUrl }
        } catch (e: Exception) {
            Log.e("ResultsRepository", "getAvatarUrls failed: ${e.message}")
            emptyMap()
        }
    }

    suspend fun getUserPublicResults(username: String): List<TestResult> {
        return try {
            val client = AppHttpClient.authenticated
            client.get("${SupabaseConfig.REST_URL}/test_results") {
                parameter("username", "eq.$username")
                parameter("order", "created_at.desc")
                parameter("limit", "2000")
                parameter("select", "user_id,username,section,difficulty,score,correct_answers,total_questions,created_at")
            }.body<List<TestResult>>()
        } catch (e: Exception) {
            Log.e("ResultsRepository", "getUserPublicResults failed: ${e.message}")
            emptyList()
        }
    }
}
