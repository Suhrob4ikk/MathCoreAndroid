package com.mathcore.app.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mathcore.app.data.model.LeaderboardEntry
import com.mathcore.app.util.computeXp

private val sectionTabs = listOf(
    null to "Все",
    "integrals"   to "Интегралы",
    "derivatives" to "Производные",
    "limits"      to "Пределы",
    "series"      to "Ряды",
    "ode"         to "ОДУ",
    "probability" to "Вероятность",
    "linalg"      to "Лин. алгебра"
)

private val sectionEmoji = mapOf(
    "integrals" to "∫", "derivatives" to "∂", "limits" to "lim",
    "series" to "Σ", "ode" to "dy/dx", "probability" to "P", "linalg" to "A",
    "duel" to "⚔️", "daily" to "📅"
)

private val sectionName = mapOf(
    "integrals" to "Интегралы", "derivatives" to "Производные", "limits" to "Пределы",
    "series" to "Ряды", "ode" to "ОДУ", "probability" to "Вероятность", "linalg" to "Лин. алгебра",
    "duel" to "Дуэль", "daily" to "Ежедневный"
)

/** For aggregated global row section=="all": XP is already stored in correctAnswers. */
fun calcXp(entry: LeaderboardEntry): Int {
    if (entry.section == "all") return entry.correctAnswers   // pre-computed total
    return computeXp(entry.correctAnswers, entry.difficulty, entry.score)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    leaderboard: List<LeaderboardEntry>,
    currentUsername: String,
    errorMessage: String? = null,
    onRefresh: (String?) -> Unit,
    isLoading: Boolean,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    avatarUrls: Map<String, String?> = emptyMap()
) {
    var selectedSection by remember { mutableStateOf<String?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showXpGuide by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSection) { onRefresh(selectedSection) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFF59E0B))
                        Spacer(Modifier.width(8.dp))
                        Text("Рейтинг", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, "Поиск пользователей")
                    }
                    IconButton(onClick = { onRefresh(selectedSection) }) {
                        Icon(Icons.Default.Refresh, "Обновить")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 8.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                sectionTabs.forEachIndexed { index, (key, label) ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            selectedSection = key
                        },
                        text = { Text(label, fontSize = 13.sp) }
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF7C3AED))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // XP guide card
                    item {
                        XpGuideCard(
                            expanded = showXpGuide,
                            onToggle = { showXpGuide = !showXpGuide }
                        )
                    }

                    // Error banner
                    if (errorMessage != null) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Не удалось загрузить рейтинг",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            errorMessage,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f)
                                        )
                                    }
                                    TextButton(onClick = { onRefresh(selectedSection) }) {
                                        Text("Повтор", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    if (leaderboard.isEmpty() && errorMessage == null) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🏆", fontSize = 56.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "Рейтинг пуст",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Пройди тест — и ты появишься здесь!",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (leaderboard.isNotEmpty()) {
                        itemsIndexed(leaderboard) { index, entry ->
                            LeaderboardRow(
                                position = index + 1,
                                entry = entry,
                                isCurrentUser = entry.username == currentUsername,
                                onClick = { onUserClick(entry.username) },
                                avatarUrl = avatarUrls[entry.username]
                            )
                        }
                    }

                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun XpGuideCard(expanded: Boolean, onToggle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A).copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚡", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Как начислять XP-баллы",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF1E3A8A)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF1E3A8A),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color(0xFF1E3A8A).copy(alpha = 0.15f))
                    Spacer(Modifier.height(10.dp))
                    XpGuideRow("🟢", "Лёгкий", "+10 XP за правильный ответ", Color(0xFF16A34A))
                    Spacer(Modifier.height(6.dp))
                    XpGuideRow("🟡", "Средний", "+20 XP за правильный ответ", Color(0xFFD97706))
                    Spacer(Modifier.height(6.dp))
                    XpGuideRow("🔴", "Сложный", "+30 XP за правильный ответ", Color(0xFFDC2626))
                    Spacer(Modifier.height(6.dp))
                    XpGuideRow("🏆", "Бонус 100%", "+25 XP за идеальный результат", Color(0xFF7C3AED))
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF1E3A8A).copy(alpha = 0.15f))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "В общем рейтинге учитывается суммарный XP со всех тестов.\nВ разделе — лучший результат в этом разделе.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun XpGuideRow(emoji: String, label: String, desc: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 16.sp, modifier = Modifier.width(28.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = color)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LeaderboardRow(
    position: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
    onClick: () -> Unit = {},
    avatarUrl: String? = null
) {
    val xp = calcXp(entry)
    val isGlobal = entry.section == "all"

    val medalBg = when (position) {
        1 -> Color(0xFFFFD700); 2 -> Color(0xFFC0C0C0); 3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val cardBg = when {
        isCurrentUser -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        position == 1 -> Color(0xFFFFFBEB)
        position == 2 -> MaterialTheme.colorScheme.surface
        position == 3 -> Color(0xFFFFF8F0)
        else -> MaterialTheme.colorScheme.surface
    }
    val xpGradient = when {
        xp >= 1000 -> listOf(Color(0xFFDC2626), Color(0xFF9333EA))
        xp >= 500  -> listOf(Color(0xFF7C3AED), Color(0xFF4F46E5))
        xp >= 200  -> listOf(Color(0xFF2563EB), Color(0xFF0EA5E9))
        else       -> listOf(Color(0xFF16A34A), Color(0xFF059669))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(if (position <= 3) 4.dp else 1.dp),
        border = if (isCurrentUser)
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        else null,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Medal / rank
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(medalBg)
            ) {
                Text(
                    when (position) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "$position" },
                    fontWeight = FontWeight.Bold,
                    fontSize = if (position <= 3) 16.sp else 13.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            // Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(
                        entry.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.username,
                        fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    if (isCurrentUser) {
                        Spacer(Modifier.width(6.dp))
                        Text("(ты)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(3.dp))
                if (isGlobal) {
                    // Global aggregate row
                    Text(
                        "📊 ${entry.totalQuestions} тест${pluralTests(entry.totalQuestions)}  ·  лучший ${entry.score}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Section-specific row
                    val diffColor = when (entry.difficulty.lowercase()) {
                        "easy" -> Color(0xFF16A34A); "medium" -> Color(0xFFD97706); else -> Color(0xFFDC2626)
                    }
                    val diffLabel = when (entry.difficulty.lowercase()) {
                        "easy" -> "Лёгкий"; "medium" -> "Средний"; else -> "Сложный"
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${sectionEmoji[entry.section] ?: "📝"} ${sectionName[entry.section] ?: entry.section}",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            modifier = Modifier.background(diffColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) { Text(diffLabel, fontSize = 10.sp, color = diffColor, fontWeight = FontWeight.SemiBold) }
                    }
                    Spacer(Modifier.height(1.dp))
                    Text("✓ ${entry.correctAnswers}/${entry.totalQuestions}  ·  ${entry.score}%",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // XP badge
            Box(
                modifier = Modifier
                    .background(Brush.horizontalGradient(xpGradient), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$xp XP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (isGlobal) Text("всего", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                }
            }
        }
    }
}

private fun pluralTests(n: Int): String = when {
    n % 100 in 11..19 -> "ов"
    n % 10 == 1       -> ""
    n % 10 in 2..4    -> "а"
    else              -> "ов"
}
