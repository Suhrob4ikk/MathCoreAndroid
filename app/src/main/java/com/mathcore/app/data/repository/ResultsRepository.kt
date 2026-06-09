package com.mathcore.app.data.repository

import android.util.Log
import com.mathcore.app.data.Question
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

/**
 * Intermediate aggregation used in the global leaderboard.
 * Holds all three sort keys before the final mapping to LeaderboardEntry.
 * [БАГ-4] Needed for three-level sort: totalXp → totalCorr → bestScore
 *          (matches web stats.js: totalPts DESC → totalCorr DESC → bestPct DESC)
 */
private data class GlobalAgg(
    val username:   String,
    val totalXp:    Int,   // primary sort key — sum of all XP (stored in correctAnswers)
    val totalCorr:  Int,   // secondary sort key — sum of raw correct_answers
    val bestScore:  Int,   // tertiary sort key — best single-test percentage
    val totalTests: Int    // display only — total tests taken
)

// ── [КРИТ-2] Edge Function — server-side answer validation ────────────────────

/** One answer entry sent to the Edge Function. */
@Serializable
private data class SubmittedAnswer(
    val questionText: String,
    val selected:     String
)

/** Full request body for POST /functions/v1/submit-test. */
@Serializable
private data class SubmitTestBody(
    val answers:        List<SubmittedAnswer>,
    val section:        String,
    val difficulty:     String,
    val username:       String,
    val dailyDate:      String? = null,
    val duelSection:    String? = null,
    val duelDifficulty: String? = null
)

/** Server-computed result returned by the Edge Function. */
@Serializable
data class SubmitTestResult(
    val correctAnswers: Int,
    val totalQuestions: Int,
    val score:          Int,
    val xpGained:       Int
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
     * [КРИТ-2] Sends raw answers to the Edge Function for server-side validation.
     *
     * The server re-generates the canonical question pool, compares each submitted
     * answer text against the correct option text, computes score and XP, then
     * writes to test_results via the service role key (bypassing RLS).
     * The client never sends a pre-computed score — the server is the authority.
     *
     * @param questions    Full question list from the quiz (with correct answers).
     * @param userAnswers  Multiple-choice answers as option indices (null = unanswered).
     * @param openAnswers  Open-question answers as strings (null = unanswered).
     * @param section      DB section: "limits", "daily", "duel:XXXX", etc.
     * @param difficulty   "easy" | "medium" | "hard"
     * @param username     Display username stored in test_results.
     * @param dailyDate    "YYYY-MM-DD" required only for daily mode.
     * @param duelSection  Raw subject (e.g. "mixed") required only for duel mode.
     * @param duelDifficulty  Difficulty string required only for duel mode.
     */
    suspend fun submitTestResult(
        questions:      List<Question>,
        userAnswers:    List<Int?>,
        openAnswers:    List<String?> = emptyList(),
        section:        String,
        difficulty:     String,
        username:       String,
        dailyDate:      String? = null,
        duelSection:    String? = null,
        duelDifficulty: String? = null
    ): Result<SubmitTestResult> {
        return try {
            // Build answer entries: send option TEXT (not index) so the server can
            // validate regardless of how options were shuffled on the client.
            val answers = questions.mapIndexed { i, q ->
                val selected = when {
                    q.type == "open" -> openAnswers.getOrNull(i)?.trim() ?: ""
                    else -> {
                        val idx = userAnswers.getOrNull(i)
                        if (idx != null) q.options.getOrElse(idx) { "" } else ""
                    }
                }
                SubmittedAnswer(questionText = q.question, selected = selected)
            }
            val body = SubmitTestBody(
                answers        = answers,
                section        = section,
                difficulty     = difficulty,
                username       = username,
                dailyDate      = dailyDate,
                duelSection    = duelSection,
                duelDifficulty = duelDifficulty
            )
            val client = AppHttpClient.authenticated
            val response = client.post("${SupabaseConfig.URL}/functions/v1/submit-test") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<SubmitTestResult>())
            } else {
                val err = response.bodyAsText()
                Log.e("ResultsRepository", "submitTestResult HTTP ${response.status.value}: $err")
                Result.failure(Exception("HTTP ${response.status.value}: $err"))
            }
        } catch (e: Exception) {
            Log.e("ResultsRepository", "submitTestResult failed: ${e.message}", e)
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
                // Global leaderboard: same formula as ProfileScreen — sum XP from ALL results
                // (excluding duels), plus 50 bonus XP per daily completion.
                // totalQuestions = actual number of tests taken (not unique combos).
                all.filter { it.username.isNotBlank() && !it.section.startsWith("duel") }
                    .groupBy { it.username }
                    .map { (username, entries) ->
                        GlobalAgg(
                            username   = username,
                            totalXp    = entries.sumOf {
                                computeXp(it.correctAnswers, it.difficulty, it.score) +
                                if (it.section == "daily") 50 else 0
                            },
                            totalCorr  = entries.sumOf { it.correctAnswers },
                            bestScore  = entries.maxOf { it.score },
                            totalTests = entries.size
                        )
                    }
                    // [БАГ-4] Three-level sort matching web stats.js:
                    //   totalPts DESC → totalCorr DESC → bestPct DESC
                    .sortedWith(
                        compareByDescending<GlobalAgg> { it.totalXp }
                            .thenByDescending { it.totalCorr }
                            .thenByDescending { it.bestScore }
                    )
                    .take(50)
                    .map { e ->
                        LeaderboardEntry(
                            username       = e.username,
                            score          = e.bestScore,
                            correctAnswers = e.totalXp,
                            totalQuestions = e.totalTests,
                            section        = "all",
                            difficulty     = "mixed"
                        )
                    }
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
