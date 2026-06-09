package com.mathcore.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mathcore.app.data.model.PublicUserInfo
import com.mathcore.app.util.computeXp
import com.mathcore.app.data.model.TestResult

/** Единая формула уровня — совпадает с ProfileScreen */
private fun userLevel(totalTests: Int, avgScore: Double): String = when {
    totalTests >= 20 && avgScore >= 85.0 -> "Эксперт"
    totalTests >= 10 && avgScore >= 75.0 -> "Продвинутый"
    totalTests >= 5  && avgScore >= 60.0 -> "Практик"
    totalTests >= 1  -> "Студент"
    else -> "Новичок"
}

private val sectionName = mapOf(
    "integrals" to "Интегралы",
    "derivatives" to "Производные",
    "limits" to "Пределы",
    "series" to "Ряды",
    "ode" to "ОДУ",
    "probability" to "Вероятность",
    "linalg" to "Лин. алгебра",
    "duel" to "Дуэль",
    "daily" to "Ежедневный"
)

private val diffLabel = mapOf(
    "easy" to "Лёгкий",
    "medium" to "Средний",
    "hard" to "Сложный"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    profile: PublicUserInfo,
    results: List<TestResult>,
    onBack: () -> Unit
) {
    val initial = profile.username.firstOrNull()?.uppercaseChar() ?: '?'
    val avatarColor = Color(
        (profile.username.hashCode().toLong() and 0xFFFFFF).toInt() or 0xFF000000.toInt()
    ).let {
        Color(
            red = (it.red * 0.7f + 0.2f).coerceIn(0.2f, 0.8f),
            green = (it.green * 0.7f + 0.2f).coerceIn(0.2f, 0.8f),
            blue = (it.blue * 0.7f + 0.2f).coerceIn(0.2f, 0.8f)
        )
    }

    // Exclude duels from all stats — same filter as global leaderboard and web profile.
    val nonDuelResults = results.filter { !it.section.startsWith("duel") }
    val totalTests = nonDuelResults.size
    val avgScore = if (nonDuelResults.isEmpty()) 0.0 else nonDuelResults.map { it.score }.average()
    val bestScore = nonDuelResults.maxOfOrNull { it.score } ?: 0
    val level = userLevel(totalTests, avgScore)
    val totalXp = nonDuelResults.sumOf { r ->
        computeXp(r.correctAnswers, r.difficulty, r.score) +
        if (r.section == "daily") 50 else 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile.username, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar + level
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(avatarColor)
                    ) {
                        val avatarUrl = profile.avatarUrl
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(
                                "$initial",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        profile.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF7C3AED).copy(alpha = 0.12f)
                    ) {
                        Text(
                            level,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7C3AED)
                        )
                    }
                    if (profile.lastSeenText.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (profile.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                profile.lastSeenText,
                                fontSize = 12.sp,
                                color = if (profile.isOnline) Color(0xFF10B981)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Stats row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard("Тестов", "$totalTests", Modifier.weight(1f))
                    StatCard("Лучший %", "$bestScore%", Modifier.weight(1f))
                    StatCard("Средний %", "${avgScore.toInt()}%", Modifier.weight(1f))
                }
            }

            // XP row
            if (totalXp > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF7C3AED).copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$totalXp XP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF7C3AED)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "всего",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Recent results section
            if (results.isNotEmpty()) {
                item {
                    Text(
                        "Последние результаты",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(results.take(20)) { result ->
                    ResultRow(result)
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Нет результатов",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultRow(result: TestResult) {
    val scoreColor = when {
        result.score >= 80 -> Color(0xFF16A34A)
        result.score >= 60 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sectionName[result.section] ?: result.section,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    diffLabel[result.difficulty] ?: result.difficulty,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${result.score}%",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = scoreColor
            )
        }
    }
}
