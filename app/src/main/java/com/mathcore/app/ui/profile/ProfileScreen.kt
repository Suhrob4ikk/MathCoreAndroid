package com.mathcore.app.ui.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mathcore.app.data.model.Profile
import com.mathcore.app.data.model.TestResult
import com.mathcore.app.util.computeXp

private val sectionNames = mapOf(
    "integrals"   to "Интегралы",
    "derivatives" to "Производные",
    "limits"      to "Пределы",
    "series"      to "Ряды",
    "ode"         to "Дифф. уравнения",
    "probability" to "Вероятность",
    "linalg"      to "Линейная алгебра",
    "duel"        to "Дуэль",
    "daily"       to "Ежедневный вызов",
    "mixed"       to "Все разделы"
)

private val sectionEmoji = mapOf(
    "integrals" to "∫", "derivatives" to "∂", "limits" to "lim",
    "series" to "Σ", "ode" to "dy/dx", "probability" to "P",
    "linalg" to "A", "duel" to "VS", "daily" to "D", "mixed" to "∗"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: Profile?,
    results: List<TestResult>,
    xp: Int,
    streak: Int,
    isDarkTheme: Boolean = false,
    onToggleTheme: (Boolean) -> Unit = {},
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    onAvatarUpload: (() -> Unit)? = null
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    // XP считаем из реальной истории тестов из БД — не из локального DataStore.
    // Локальный xp (prefs) сбрасывается при переустановке, а история в БД — нет.
    // computeXp() — единая формула (см. util/XpUtils.kt), одинаковая с рейтингом.
    val displayXp = results
        .filter { !it.section.startsWith("duel") }
        .sumOf { r ->
            computeXp(r.correctAnswers, r.difficulty, r.score) +
            if (r.section == "daily") 50 else 0
        }.let { dbXp -> if (dbXp > 0) dbXp else xp }   // fallback на prefs если история ещё не загружена
    val xpLevel = displayXp / 3000 + 1              // 3000 XP на уровень — согласовано с HomeScreen
    val xpInLevel = displayXp % 3000

    // Уровень по системе тестов (единая система с публичным профилем)
    val totalTestsCount = results.size
    val avgScoreAll = if (results.isEmpty()) 0.0
                     else results.map { it.score }.average()
    val levelLabel = when {
        totalTestsCount >= 20 && avgScoreAll >= 85.0 -> "Эксперт"
        totalTestsCount >= 10 && avgScoreAll >= 75.0 -> "Продвинутый"
        totalTestsCount >= 5  && avgScoreAll >= 60.0 -> "Практик"
        totalTestsCount >= 1  -> "Студент"
        else -> "Новичок"
    }

    // Бейджи
    val badges = computeBadges(results, streak)

    // Диалог подтверждения выхода
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Выйти из аккаунта?") },
            text = { Text("Ты выйдешь из аккаунта ${profile?.username ?: ""}. При следующем запуске потребуется войти снова.") },
            confirmButton = {
                TextButton(
                    onClick = { showLogoutDialog = false; onSignOut() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Выйти", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showLogoutDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Выйти", fontSize = 14.sp)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Профиль карточка
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    ) {
                        Box(modifier = Modifier.size(80.dp)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(80.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
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
                                        fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White
                                    )
                                }
                            }
                            if (onAvatarUpload != null) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                        .clickable { onAvatarUpload() }
                                ) {
                                    Icon(Icons.Default.CameraAlt, null,
                                        tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(profile?.username ?: "—", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(profile?.email ?: "", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Уровень $xpLevel · $levelLabel",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { xpInLevel / 3000f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF10B981),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("$xpInLevel/3000 XP до следующего уровня", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }

            // Статистика
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(Icons.Default.Whatshot, "$streak", "Стрик дней", Modifier.weight(1f))
                    StatCard(Icons.Default.Star, "$displayXp", "Всего XP", Modifier.weight(1f))
                    StatCard(Icons.AutoMirrored.Filled.Assignment, "${results.size}", "Тестов", Modifier.weight(1f))
                }
            }

            if (results.isNotEmpty()) {
                item {
                    val avgScore = results.map { it.score }.average().toInt()
                    val bestScore = results.maxOf { it.score }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard(Icons.Default.BarChart, "$avgScore%", "Средний балл", Modifier.weight(1f))
                        StatCard(Icons.Default.EmojiEvents, "$bestScore%", "Лучший балл", Modifier.weight(1f))
                    }
                }
            }

            // Бейджи
            if (badges.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Достижения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
                    BadgesRow(badges = badges)
                }
            }

            // Per-subject stats
            if (results.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("По разделам", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
                    SubjectStatsCard(results = results)
                }
                item {
                    Text("Последние результаты", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(results.take(20)) { result -> ResultRow(result) }
            }

            // Settings
            item {
                Spacer(Modifier.height(8.dp))
                Text("Настройки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (isDarkTheme) "Тёмная тема" else "Светлая тема",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Medium
                        )
                        Switch(checked = isDarkTheme, onCheckedChange = onToggleTheme)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(16.dp),
         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
         modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

data class Badge(val icon: ImageVector, val label: String, val description: String, val earned: Boolean)

fun computeBadges(results: List<TestResult>, streak: Int): List<Badge> {
    val count = results.size
    val avg = if (results.isEmpty()) 0.0 else results.map { it.score }.average()
    val best = if (results.isEmpty()) 0 else results.maxOf { it.score }
    val subjectCount = results.map { it.section }
        .filter { it !in listOf("duel", "daily", "mixed") }
        .toSet().size

    return listOf(
        Badge(Icons.Default.Adjust,                       "Первый шаг",    "Пройти первый тест",      count >= 1),
        Badge(Icons.AutoMirrored.Filled.MenuBook,         "5 тестов",      "Пройти 5 тестов",         count >= 5),
        Badge(Icons.Default.Whatshot,                     "10 тестов",     "Пройти 10 тестов",        count >= 10),
        Badge(Icons.Default.Star,                         "20 тестов",     "Пройти 20 тестов",        count >= 20),
        Badge(Icons.Default.Grade,                        "Перфекционист", "Получить 100%",            best == 100),
        Badge(Icons.Default.Star,                         "Отличник",      "Средний балл ≥ 90%",      avg >= 90.0),
        Badge(Icons.Default.CheckCircle,                  "Хорошист",      "Средний балл ≥ 70%",      avg >= 70.0),
        Badge(Icons.Default.Stars,                        "Всесторонний",  "Изучить 4+ раздела",      subjectCount >= 4),
        Badge(Icons.Default.FlashOn,                      "Стрик 5",       "Стрик 5 дней подряд",    streak >= 5),
        Badge(Icons.Default.School,                       "Стрик 10",      "Стрик 10 дней подряд",   streak >= 10)
    ).filter { it.earned }
}

@Composable
fun BadgesRow(badges: List<Badge>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(badges) { badge ->
            BadgeChip(badge = badge)
        }
    }
}

@Composable
fun BadgeChip(badge: Badge) {
    var showTooltip by remember { mutableStateOf(false) }

    Card(
        onClick = { showTooltip = !showTooltip },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                badge.icon,
                contentDescription = badge.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                badge.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SubjectStatsCard(results: List<TestResult>) {
    val subjectKeys = listOf("integrals", "derivatives", "limits", "series", "ode", "probability", "linalg")
    val subjectEmojis = mapOf(
        "integrals" to "∫", "derivatives" to "∂", "limits" to "lim",
        "series" to "Σ", "ode" to "dy/dx", "probability" to "P", "linalg" to "A"
    )
    val subjectLabels = mapOf(
        "integrals" to "Интегралы", "derivatives" to "Производные", "limits" to "Пределы",
        "series" to "Ряды", "ode" to "ДУ", "probability" to "Вероятность", "linalg" to "Лин. алгебра"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            subjectKeys.forEach { key ->
                val subjectResults = results.filter { it.section == key }
                if (subjectResults.isNotEmpty()) {
                    val avg = subjectResults.map { it.score }.average().toInt()
                    val count = subjectResults.size
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            subjectEmojis[key] ?: "📝",
                            fontSize = 16.sp,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    subjectLabels[key] ?: key,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "$avg%  ·  $count тест${if (count % 10 == 1 && count % 100 != 11) "" else if (count % 10 in 2..4 && count % 100 !in 12..14) "а" else "ов"}",
                                    fontSize = 12.sp,
                                    color = if (avg >= 70) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { avg / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (avg >= 70) Color(0xFF10B981) else Color(0xFFF59E0B),
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultRow(result: TestResult) {
    val diffColor = when (result.difficulty) {
        "easy" -> Color(0xFF10B981); "medium" -> Color(0xFFF59E0B); else -> Color(0xFFEF4444)
    }
    val diffName = when (result.difficulty) {
        "easy" -> "Лёгкий"; "medium" -> "Средний"; else -> "Сложный"
    }
    val scoreColor = if (result.score >= 70) Color(0xFF10B981) else Color(0xFFEF4444)
    val name = sectionNames[result.section] ?: result.section.replaceFirstChar { it.uppercase() }
    val emoji = sectionEmoji[result.section] ?: "📝"

    Card(shape = RoundedCornerShape(12.dp),
         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.background(diffColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(diffName, fontSize = 11.sp, color = diffColor)
                    }
                    Text("${result.correctAnswers}/${result.totalQuestions}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("${result.score}%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = scoreColor)
        }
    }
}
