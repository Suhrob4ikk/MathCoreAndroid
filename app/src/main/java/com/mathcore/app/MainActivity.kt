package com.mathcore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathcore.app.data.Difficulty
import com.mathcore.app.data.QuizConfig
import com.mathcore.app.data.QuizResult
import com.mathcore.app.data.QuestionRepository
import com.mathcore.app.data.Subject
import com.mathcore.app.data.normalizeAnswer
import com.mathcore.app.data.local.PreferencesManager
import com.mathcore.app.data.repository.MistakesRepository
import com.mathcore.app.data.model.LeaderboardEntry
import com.mathcore.app.data.model.TestResult
import com.mathcore.app.data.repository.AuthRepository
import com.mathcore.app.data.repository.ResultsRepository
import com.mathcore.app.data.repository.SessionManager
import com.mathcore.app.ui.auth.AuthScreen
import com.mathcore.app.ui.exam.ExamScreen
import com.mathcore.app.util.computeXp
import com.mathcore.app.ui.home.DailyLeaderboardDialog
import com.mathcore.app.ui.home.HomeScreen
import com.mathcore.app.ui.mistakes.MistakesScreen
import com.mathcore.app.ui.quiz.QuizScreen
import com.mathcore.app.ui.results.ResultsScreen
import com.mathcore.app.ui.duel.DuelScreen
import com.mathcore.app.ui.profile.ProfileScreen
import com.mathcore.app.ui.search.UserSearchScreen
import com.mathcore.app.ui.search.UserProfileScreen
import com.mathcore.app.ui.stats.StatsScreen
import com.mathcore.app.ui.theory.TheoryScreen
import com.mathcore.app.ui.theme.MathCoreTheme
import com.mathcore.app.viewmodel.AuthViewModel
import com.mathcore.app.viewmodel.DuelViewModel
import com.mathcore.app.viewmodel.ExamViewModel
import com.mathcore.app.viewmodel.QuizViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen {
    data object Auth : Screen()
    data object Home : Screen()
    data class Quiz(val config: QuizConfig) : Screen()
    data class Results(val result: QuizResult) : Screen()
    data object Duel : Screen()
    data object Profile : Screen()
    data object Stats : Screen()
    data object Exam : Screen()
    data object Mistakes : Screen()
    data object UserSearch : Screen()
    data class UserProfile(val username: String) : Screen()
    data class Theory(val subject: Subject) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = QuestionRepository(this)
        val prefs = PreferencesManager(this)
        val resultsRepo = ResultsRepository()
        setContent {
            val isDark by prefs.darkTheme.collectAsStateWithLifecycle(isSystemInDarkTheme())
            MathCoreTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MathCoreApp(repo, prefs, resultsRepo, isDark)
                }
            }
        }
    }
}

@Composable
fun MathCoreApp(
    repo: QuestionRepository,
    prefs: PreferencesManager,
    resultsRepo: ResultsRepository,
    isDark: Boolean = false
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Auth) }
    val quizViewModel: QuizViewModel = viewModel()
    val examViewModel: ExamViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val duelViewModel: DuelViewModel = viewModel()
    val mistakesRepo = remember { MistakesRepository() }
    val quizState by quizViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val xp by prefs.xp.collectAsStateWithLifecycle(0)
    val streak by prefs.streak.collectAsStateWithLifecycle(0)
    val mistakes by mistakesRepo.mistakes.collectAsStateWithLifecycle(emptyList())
    var isDuelQuiz by remember { mutableStateOf(false) }

    // Auto-navigate after auth check + subscribe to duel invites + sync XP from DB
    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated && screen == Screen.Auth) {
            screen = Screen.Home
        }
        if (authState.isAuthenticated) {
            val username = authState.currentProfile?.username ?: return@LaunchedEffect
            duelViewModel.subscribeToInvites(username)
            mistakesRepo.loadFromServer()

            // Синхронизируем XP и состояние ежедневного вызова из БД при входе.
            val userId = authState.currentProfile?.id ?: return@LaunchedEffect
            val loginResults = resultsRepo.getUserResults(userId)

            // XP: суммируем историю тестов, исключая дуэли (+50 за каждый пройденный daily)
            val dbXp = loginResults
                .filter { !it.section.startsWith("duel") }
                .sumOf { r ->
                    computeXp(r.correctAnswers, r.difficulty, r.score) +
                    if (r.section == "daily") 50 else 0
                }
            if (dbXp > 0) prefs.syncXp(dbXp)

            // Ежедневный вызов: всегда синхронизируем с БД при входе.
            // Если у текущего пользователя нет результата за сегодня — сбрасываем локальный
            // статус (иначе на устройстве остаётся "Выполнено" от предыдущего пользователя).
            val today = java.time.LocalDate.now().toString()
            val latestDaily = loginResults.firstOrNull { it.section == "daily" }
            val dailyDoneToday = latestDaily?.createdAt?.let { ts ->
                try {
                    java.time.Instant.parse(ts)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate().toString() == today
                } catch (_: Exception) { false }
            } ?: false

            if (dailyDoneToday) {
                prefs.setDailyCompleted(latestDaily!!.score)
            } else {
                // Сбрасываем устаревший/чужой статус ежедневки
                prefs.resetDailyCompleted()
            }
        }
    }

    // Update last_seen every 5 minutes while authenticated
    LaunchedEffect(authState.isAuthenticated) {
        if (!authState.isAuthenticated) return@LaunchedEffect
        while (true) {
            authViewModel.updateLastSeen()
            delay(5 * 60_000L)
        }
    }

    // Leaderboard state
    var leaderboard by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var leaderboardLoading by remember { mutableStateOf(false) }
    var leaderboardError by remember { mutableStateOf<String?>(null) }
    var leaderboardAvatarUrls by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    var userResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // Daily leaderboard state
    var dailyLeaderboardResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }
    var dailyLeaderboardLoading by remember { mutableStateOf(false) }
    var dailyLeaderboardError by remember { mutableStateOf<String?>(null) }
    var showDailyLeaderboard by remember { mutableStateOf(false) }
    var dailyAvatarUrls by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }

    // Search state
    var searchUsers by remember { mutableStateOf<List<com.mathcore.app.data.model.PublicUserInfo>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selectedUserResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }
    var selectedUserInfo by remember { mutableStateOf<com.mathcore.app.data.model.PublicUserInfo?>(null) }

    // Avatar upload
    val context = LocalContext.current
    val authRepo = remember { AuthRepository(context) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        val userId = SessionManager.userId ?: return@launch
                        authRepo.uploadAvatar(bytes, userId)
                        authViewModel.refreshProfile()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    when (val s = screen) {
        is Screen.Auth -> AuthScreen(
            onAuthSuccess = { screen = Screen.Home },
            authViewModel = authViewModel
        )

        is Screen.Home -> {
            HomeScreen(
                profile = authState.currentProfile,
                xp = xp,
                streak = streak,
                prefs = prefs,
                onStartQuiz = { config ->
                    val questions = if (config.isDailyChallenge) {
                        repo.getDailyQuestions()
                    } else {
                        repo.getShuffledQuestions(config.subject, config.difficulty, config.questionCount)
                    }
                    quizViewModel.startQuiz(questions, config)
                    screen = Screen.Quiz(config)
                },
                onDuel = { screen = Screen.Duel },
                onExam = { screen = Screen.Exam },
                onMistakes = { screen = Screen.Mistakes },
                onProfile = {
                    scope.launch {
                        val userId = authState.currentProfile?.id ?: return@launch
                        userResults = resultsRepo.getUserResults(userId)
                        val dbXp = userResults
                            .filter { !it.section.startsWith("duel") }
                            .sumOf { r ->
                                computeXp(r.correctAnswers, r.difficulty, r.score) +
                                if (r.section == "daily") 50 else 0
                            }
                        if (dbXp > 0) prefs.syncXp(dbXp)
                    }
                    screen = Screen.Profile
                },
                onStats = { screen = Screen.Stats },
                onTheory = { subject -> screen = Screen.Theory(subject) },
                mistakeCount = mistakes.size,
                dailyLeaderboardResults = dailyLeaderboardResults,
                dailyLeaderboardLoading = dailyLeaderboardLoading,
                onShowDailyLeaderboard = {
                    showDailyLeaderboard = true
                    scope.launch {
                        dailyLeaderboardLoading = true
                        dailyLeaderboardError = null
                        val (results, error) = resultsRepo.getDailyLeaderboard()
                        dailyLeaderboardResults = results
                        dailyLeaderboardError = error
                        // Batch-fetch avatars for everyone in the leaderboard
                        if (error == null && results.isNotEmpty()) {
                            val usernames = results.map { it.username }.distinct()
                            dailyAvatarUrls = resultsRepo.getAvatarUrls(usernames)
                        }
                        dailyLeaderboardLoading = false
                    }
                }
            )
            if (showDailyLeaderboard) {
                DailyLeaderboardDialog(
                    results = dailyLeaderboardResults,
                    myUsername = authState.currentProfile?.username ?: "",
                    isLoading = dailyLeaderboardLoading,
                    onDismiss = { showDailyLeaderboard = false },
                    errorMessage = dailyLeaderboardError,
                    avatarUrls = dailyAvatarUrls
                )
            }
        }

        is Screen.Theory -> TheoryScreen(
            subject = s.subject,
            onBack = { screen = Screen.Home },
            onStartQuiz = {
                val config = QuizConfig(s.subject, Difficulty.MEDIUM, 15, isStudyMode = false)
                val questions = repo.getShuffledQuestions(config.subject, config.difficulty, config.questionCount)
                quizViewModel.startQuiz(questions, config)
                screen = Screen.Quiz(config)
            }
        )

        is Screen.Quiz -> {
            val soundEnabled by prefs.soundEnabled.collectAsStateWithLifecycle(true)
            QuizScreen(
                uiState = quizState,
                onAnswer = { quizViewModel.selectAnswer(it) },
                onNext = { quizViewModel.nextQuestion() },
                onPrevious = { quizViewModel.previousQuestion() },
                onOpenAnswer = { quizViewModel.submitOpenAnswer(it) },
                soundEnabled = soundEnabled,
                onFinish = {
                    val result = quizViewModel.finishQuiz()
                    if (result != null) {
                        val wasDuel = isDuelQuiz
                        if (wasDuel) isDuelQuiz = false
                        scope.launch {
                            val user = authState.currentProfile
                            // Uses package-level normalizeAnswer from Question.kt —
                            // single canonical implementation, no local duplicate.
                            val wrongQuestions = result.questions.indices
                                .filter { i ->
                                    val q = result.questions[i]
                                    if (q.type == "open") {
                                        val userAns = result.openAnswers.getOrNull(i)?.trim() ?: return@filter true
                                        normalizeAnswer(userAns) != normalizeAnswer(q.answer)
                                    } else {
                                        result.userAnswers.getOrNull(i) != q.correct
                                    }
                                }
                                .map { result.questions[it] }

                            // Determine section and difficulty for this quiz result
                            val duelCode  = duelViewModel.uiState.value.code
                            val section   = if (wasDuel) "duel:$duelCode"
                                            else if (result.config.isDailyChallenge) "daily"
                                            else result.config.subject.name.lowercase()
                            val difficulty = result.config.difficulty.name.lowercase()

                            if (wrongQuestions.isNotEmpty()) {
                                mistakesRepo.addMistakes(wrongQuestions, section, difficulty)
                            }

                            if (!result.config.isStudyMode) {
                                if (result.config.isDailyChallenge) {
                                    prefs.setDailyCompleted(result.percentage)
                                }
                                // computeXp = единая формула (XpUtils.kt); +50 за ежедневный вызов
                                val xpGain = computeXp(
                                    result.correctCount,
                                    result.config.difficulty.name,
                                    result.percentage
                                ) + if (result.config.isDailyChallenge) 50 else 0
                                prefs.addXP(xpGain)
                                if (user != null) {
                                    resultsRepo.saveResult(
                                        TestResult(
                                            userId = user.id,
                                            username = user.username,
                                            section = section,
                                            difficulty = difficulty,
                                            score = result.percentage,
                                            correctAnswers = result.correctCount,
                                            totalQuestions = result.totalCount
                                        )
                                    )
                                }
                            }

                            if (wasDuel) {
                                // Set myScore before navigating so DuelResultsScreen
                                // never shows 0% while the state catches up.
                                duelViewModel.broadcastScore(result.percentage)
                                screen = Screen.Duel
                            } else {
                                screen = Screen.Results(result)
                            }
                        }
                    }
                },
                onExit = { screen = Screen.Home }
            )
        }

        is Screen.Results -> ResultsScreen(
            result = s.result,
            onRetry = {
                val config = s.result.config
                val questions = repo.getShuffledQuestions(config.subject, config.difficulty, config.questionCount)
                quizViewModel.startQuiz(questions, config)
                screen = Screen.Quiz(config)
            },
            onHome = { screen = Screen.Home }
        )

        is Screen.Duel -> DuelScreen(
            myName = authState.currentProfile?.username ?: "Игрок",
            // BUG-4 FIX: use subject/difficulty from DuelUiState instead of
            // hardcoded Subject.LINALG / Difficulty.MEDIUM. DuelViewModel stores
            // the host's chosen values in state and the guest receives them via WS.
            onStartQuiz = { questions, subject, difficulty ->
                val config = QuizConfig(
                    subject       = subject,
                    difficulty    = difficulty,
                    questionCount = questions.size,
                    isStudyMode   = false
                )
                quizViewModel.startQuiz(questions, config)
                isDuelQuiz = true
                screen = Screen.Quiz(config)
            },
            onBack = { screen = Screen.Home },
            duelViewModel = duelViewModel
        )

        is Screen.Profile -> ProfileScreen(
            profile = authState.currentProfile,
            results = userResults,
            xp = xp,
            streak = streak,
            isDarkTheme = isDark,
            onToggleTheme = { dark -> scope.launch { prefs.setDarkTheme(dark) } },
            onSignOut = {
                authViewModel.signOut()
                screen = Screen.Auth
            },
            onBack = { screen = Screen.Home },
            onAvatarUpload = { imagePickerLauncher.launch("image/*") }
        )

        is Screen.Stats -> {
            LaunchedEffect(Unit) {
                leaderboardLoading = true
                leaderboardError = null
                val (data, err) = resultsRepo.getLeaderboardResult()
                leaderboard = data
                leaderboardError = err
                if (err == null && data.isNotEmpty()) {
                    leaderboardAvatarUrls = resultsRepo.getAvatarUrls(data.map { it.username })
                }
                leaderboardLoading = false
            }
            StatsScreen(
                leaderboard = leaderboard,
                currentUsername = authState.currentProfile?.username ?: "",
                errorMessage = leaderboardError,
                onRefresh = { section ->
                    scope.launch {
                        leaderboardLoading = true
                        leaderboardError = null
                        val (data, err) = resultsRepo.getLeaderboardResult(section)
                        leaderboard = data
                        leaderboardError = err
                        if (err == null && data.isNotEmpty()) {
                            leaderboardAvatarUrls = resultsRepo.getAvatarUrls(data.map { it.username })
                        }
                        leaderboardLoading = false
                    }
                },
                isLoading = leaderboardLoading,
                onBack = { screen = Screen.Home },
                onUserClick = { username ->
                    scope.launch {
                        selectedUserResults = resultsRepo.getUserPublicResults(username)
                        selectedUserInfo = com.mathcore.app.data.model.PublicUserInfo(username = username)
                        screen = Screen.UserProfile(username)
                    }
                },
                onSearchClick = { screen = Screen.UserSearch },
                avatarUrls = leaderboardAvatarUrls
            )
        }

        is Screen.UserSearch -> UserSearchScreen(
            users = searchUsers,
            isLoading = searchLoading,
            errorMessage = searchError,
            onQueryChange = { query ->
                if (query.length >= 2) {
                    scope.launch {
                        searchLoading = true
                        searchError = null
                        searchUsers = resultsRepo.searchUsers(query)
                        searchLoading = false
                    }
                } else {
                    searchUsers = emptyList()
                }
            },
            onUserClick = { username ->
                scope.launch {
                    selectedUserResults = resultsRepo.getUserPublicResults(username)
                    selectedUserInfo = com.mathcore.app.data.model.PublicUserInfo(username = username)
                    screen = Screen.UserProfile(username)
                }
            },
            onBack = { screen = Screen.Stats }
        )

        is Screen.UserProfile -> UserProfileScreen(
            profile = selectedUserInfo ?: com.mathcore.app.data.model.PublicUserInfo(username = (screen as Screen.UserProfile).username),
            results = selectedUserResults,
            onBack = { screen = Screen.Stats }
        )

        is Screen.Exam -> ExamScreen(
            examViewModel = examViewModel,
            onBack = { screen = Screen.Home },
            onFinished = { result ->
                // Save exam mistakes to Supabase
                scope.launch {
                    val wrongQuestions = result.questions.indices
                        .filter { i -> result.userAnswers.getOrNull(i) != result.questions[i].correct }
                        .map { result.questions[it] }
                    if (wrongQuestions.isNotEmpty()) {
                        mistakesRepo.addMistakes(wrongQuestions, "exam", "medium")
                    }
                }
            }
        )

        is Screen.Mistakes -> MistakesScreen(
            mistakes = mistakes,
            onClearMistakes = { scope.launch { mistakesRepo.clearMistakes() } },
            onRemoveMistake = { q -> scope.launch { mistakesRepo.removeMistake(q) } },
            onPractice = { config, questions ->
                quizViewModel.startQuiz(questions, config)
                screen = Screen.Quiz(config)
            },
            onBack = { screen = Screen.Home }
        )
    }
}
