package com.mathcore.app.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mathcore.app.data.Difficulty
import com.mathcore.app.data.QuizConfig
import com.mathcore.app.data.Subject
import com.mathcore.app.data.local.PreferencesManager
import com.mathcore.app.data.model.Profile
import com.mathcore.app.data.model.TestResult
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profile: Profile?,
    xp: Int,
    streak: Int,
    prefs: PreferencesManager,
    onStartQuiz: (QuizConfig) -> Unit,
    onDuel: () -> Unit,
    onExam: () -> Unit,
    onMistakes: () -> Unit,
    onProfile: () -> Unit,
    onStats: () -> Unit,
    onTheory: (com.mathcore.app.data.Subject) -> Unit = {},
    mistakeCount: Int = 0,
    dailyLeaderboardResults: List<TestResult> = emptyList(),
    dailyLeaderboardLoading: Boolean = false,
    onShowDailyLeaderboard: () -> Unit = {}
) {
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var selectedDifficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var questionCount by remember { mutableIntStateOf(15) }
    var isStudyMode by remember { mutableStateOf(false) }
    val dailyCompleted by prefs.dailyCompleted.collectAsStateWithLifecycle(false)
    val dailyScore by prefs.dailyScore.collectAsStateWithLifecycle(0)

    var countdown by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = java.time.LocalDateTime.now()
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
            countdown = java.time.Duration.between(now, midnight).seconds
            delay(1000)
        }
    }

    val level = xp / 3000 + 1   // 3000 XP на уровень; согласовано с ProfileScreen

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Главная") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onStats,
                    icon = { Icon(Icons.Default.EmojiEvents, null) },
                    label = { Text("Рейтинг") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onExam,
                    icon = { Icon(Icons.Default.School, null) },
                    label = { Text("Экзамен") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onProfile,
                    icon = { Icon(Icons.Default.Person, null) },
                    label = { Text("Профиль") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF2563EB)))
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Привет, ${profile?.username ?: "Студент"}! 👋",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Уровень $level · $xp XP",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                            if (streak > 0) {
                                Spacer(Modifier.height(4.dp))
                                Text("🔥 Стрик: $streak дней", color = Color(0xFFFCD34D), fontSize = 13.sp)
                            }
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(onClick = onProfile)
                        ) {
                            val avatarUrl = profile?.avatarUrl
                            if (avatarUrl != null) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Аватар",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(
                                    (profile?.username?.firstOrNull()?.uppercaseChar() ?: "?").toString(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Daily challenge card
            item {
                Spacer(Modifier.height(16.dp))
                DailyChallengeCard(
                    completed = dailyCompleted,
                    score = dailyScore,
                    countdown = countdown,
                    onStart = {
                        // subject is ignored when isDailyChallenge=true;
                        // MainActivity uses getDailyQuestions() instead.
                        onStartQuiz(QuizConfig(
                            subject = Subject.LINALG,
                            difficulty = Difficulty.MEDIUM,
                            questionCount = 10,
                            isStudyMode = false,
                            isDailyChallenge = true
                        ))
                    },
                    onShowLeaderboard = onShowDailyLeaderboard,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Quick-action cards row — Дуэль + Ошибки (Экзамен вынесен в навбар)
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // Duel
                    Card(
                        onClick = onDuel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⚔️", fontSize = 32.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("Дуэль", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("1 vs 1 онлайн", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                        }
                    }
                    // Mistakes
                    Card(
                        onClick = onMistakes,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (mistakeCount == 0) Color(0xFF374151)
                                            else Color(0xFFDC2626)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(if (mistakeCount == 0) "✅" else "❌", fontSize = 32.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("Ошибки", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            val cnt = mistakeCount
                            Text(if (cnt > 0) "$cnt вопрос${when { cnt % 100 in 11..19 -> "ов"; cnt % 10 == 1 -> ""; cnt % 10 in 2..4 -> "а"; else -> "ов" }}" else "Чисто!", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                        }
                    }
                }
            }

            // Subject selection
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Выбери раздел",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            items(Subject.entries.filter { it != Subject.MIXED }.chunked(2)) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    row.forEach { subject ->
                        SubjectCard(
                            subject = subject,
                            isSelected = selectedSubject == subject,
                            onClick = { selectedSubject = if (selectedSubject == subject) null else subject },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // Quiz settings (shown when subject selected)
            if (selectedSubject != null) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Настройки теста", fontWeight = FontWeight.Bold)

                            // Difficulty
                            Text("Сложность", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Difficulty.entries.forEach { diff ->
                                    FilterChip(
                                        selected = selectedDifficulty == diff,
                                        onClick = { selectedDifficulty = diff },
                                        label = { Text(diff.displayName, fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Count
                            Text("Вопросов: $questionCount", style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = questionCount.toFloat(),
                                onValueChange = { questionCount = it.toInt() },
                                valueRange = 5f..25f,
                                steps = 3
                            )

                            // Study mode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Режим изучения", fontWeight = FontWeight.Medium)
                                    Text("Без таймера и сохранения", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = isStudyMode, onCheckedChange = { isStudyMode = it })
                            }

                            // Buttons row: Theory + Start
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { selectedSubject?.let { onTheory(it) } },
                                    modifier = Modifier.weight(0.45f).height(52.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Теория", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                }
                                Button(
                                    onClick = {
                                        selectedSubject?.let { subj ->
                                            onStartQuiz(QuizConfig(subj, selectedDifficulty, questionCount, isStudyMode))
                                        }
                                    },
                                    modifier = Modifier.weight(0.55f).height(52.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Начать", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun DailyChallengeCard(
    completed: Boolean,
    score: Int,
    countdown: Long = 0L,
    onStart: () -> Unit,
    onShowLeaderboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (completed) Color(0xFFDCFCE7) else Color(0xFFFFFBEB)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (completed) "✅" else "📅", fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Ежедневный вызов",
                        fontWeight = FontWeight.Bold,
                        color = if (completed) Color(0xFF15803D) else Color(0xFF92400E)
                    )
                    Text(
                        if (completed) "Выполнено! Результат: $score%" else "10 вопросов из всех разделов",
                        fontSize = 12.sp,
                        color = if (completed) Color(0xFF166534) else Color(0xFF78350F)
                    )
                }
                if (!completed) {
                    Button(
                        onClick = onStart,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) { Text("Начать", fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🕐 Следующий через %02d:%02d:%02d".format(
                        countdown / 3600, (countdown % 3600) / 60, countdown % 60
                    ),
                    fontSize = 11.sp,
                    color = if (completed) Color(0xFF166534) else Color(0xFF78350F)
                )
                TextButton(
                    onClick = onShowLeaderboard,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Лидеры дня 🏆", fontSize = 12.sp,
                        color = Color(0xFFD97706), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun SubjectCard(
    subject: Subject,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant

    Card(
        onClick = onClick,
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (isSelected) 6.dp else 1.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(subject.emoji, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                subject.displayName,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}
