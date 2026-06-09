package com.mathcore.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.mathcore.app.data.model.TestResult
import kotlinx.coroutines.delay

@Composable
fun DailyLeaderboardDialog(
    results: List<TestResult>,
    myUsername: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    errorMessage: String? = null,
    avatarUrls: Map<String, String?> = emptyMap()
) {
    // Countdown until next midnight
    var secondsLeft by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = java.time.LocalDateTime.now()
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
            secondsLeft = java.time.Duration.between(now, midnight).seconds
            delay(1000)
        }
    }

    val hours = secondsLeft / 3600
    val minutes = (secondsLeft % 3600) / 60
    val seconds = secondsLeft % 60

    // Deduplicate (keep best per user)
    val unique = results.groupBy { it.username }
        .map { (_, entries) -> entries.maxBy { it.score } }
        .sortedByDescending { it.score }

    val medals = listOf("🥇", "🥈", "🥉")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, null,
                            tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Лидеры дня", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Countdown
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏱ Следующий через: ", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "%02d:%02d:%02d".format(hours, minutes, seconds),
                            fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (errorMessage != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            errorMessage,
                            modifier = Modifier.padding(14.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else if (unique.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center) {
                        Text("Сегодня ещё никто не прошёл вызов",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(unique) { index, result ->
                            val isMe = result.username == myUsername
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe)
                                        Color(0xFF2563EB).copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(medals.getOrNull(index) ?: "${index + 1}",
                                        fontSize = if (index < 3) 18.sp else 13.sp,
                                        modifier = Modifier.width(28.dp))
                                    // Avatar
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isMe) Color(0xFF2563EB).copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                    ) {
                                        val url = avatarUrls[result.username]
                                        if (url != null) {
                                            AsyncImage(
                                                model = url,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                                            )
                                        } else {
                                            Text(
                                                result.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                                color = if (isMe) Color(0xFF2563EB)
                                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        result.username + if (isMe) " (ты)" else "",
                                        fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f),
                                        color = if (isMe) Color(0xFF2563EB)
                                               else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text("${result.correctAnswers}/${result.totalQuestions}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 8.dp))
                                    Text("${result.score}%", fontWeight = FontWeight.Bold,
                                        color = if (result.score >= 70) Color(0xFF10B981) else Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
