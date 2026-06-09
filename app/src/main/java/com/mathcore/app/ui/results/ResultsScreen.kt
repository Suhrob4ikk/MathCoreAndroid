package com.mathcore.app.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
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
import com.mathcore.app.data.QuizResult
import com.mathcore.app.data.normalizeAnswer
import com.mathcore.app.ui.quiz.MathView
import com.mathcore.app.ui.quiz.wrapMath

@Composable
fun ResultsScreen(
    result: QuizResult,
    currentUsername: String? = null,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    val pct = result.percentage
    val isPerfect = pct == 100

    val scoreColor = when {
        pct >= 90 -> Color(0xFF10B981)
        pct >= 70 -> Color(0xFF3B82F6)
        pct >= 50 -> Color(0xFFF59E0B)
        else      -> Color(0xFFEF4444)
    }

    val comment = when {
        pct == 100 -> if (!currentUsername.isNullOrBlank()) "$currentUsername, феноменально! Все баллы!" else "Феноменально! Все баллы!"
        pct >= 90  -> "Отлично! Почти идеально."
        pct >= 70  -> "Хорошо! Можно ещё лучше."
        pct >= 50  -> "Неплохо, но есть над чем поработать."
        else       -> "Не отчаивайся, попробуй ещё раз!"
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Score hero card
        item {
            if (isPerfect) {
                // Perfect score — dark gradient card like the web's special treatment
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E3A8A), Color(0xFF7C3AED))
                            )
                        )
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "PERFECT",
                            color = Color(0xFFD4AF37),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 4.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${result.correctCount}/${result.totalCount}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp
                                )
                                Text(
                                    "$pct%",
                                    color = Color(0xFFD4AF37),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            comment,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (result.config.isDailyChallenge) "Ежедневный вызов · ${result.config.difficulty.displayName}"
                            else if (result.config.isStudyMode) "Работа над ошибками"
                            else "${result.config.subject.displayName} · ${result.config.difficulty.displayName}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Standard result card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = scoreColor.copy(alpha = 0.08f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(scoreColor)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${result.correctCount}/${result.totalCount}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                                Text(
                                    "$pct%",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            comment,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = scoreColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (result.config.isDailyChallenge) "Ежедневный вызов · ${result.config.difficulty.displayName}"
                            else if (result.config.isStudyMode) "Работа над ошибками"
                            else "${result.config.subject.displayName} · ${result.config.difficulty.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Buttons
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Главная")
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Ещё раз")
                }
            }
        }

        val hasErrors = result.correctCount < result.totalCount

        if (hasErrors) {
            item {
                Text(
                    "Разбор ошибок",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        itemsIndexed(result.questions) { index, question ->
            val isOpen = question.type == "open"

            val isCorrect = if (isOpen) {
                val ans = result.openAnswers.getOrNull(index)?.trim()
                ans != null && normalizeAnswer(ans) == normalizeAnswer(question.answer)
            } else {
                result.userAnswers.getOrNull(index) == question.correct
            }

            if (!isCorrect) {
                val userAnswerText = if (isOpen) {
                    result.openAnswers.getOrNull(index)
                        ?.takeIf { it.isNotBlank() } ?: "Не отвечено"
                } else {
                    val idx = result.userAnswers.getOrNull(index)
                    if (idx != null) question.options.getOrNull(idx) ?: "?" else "Не отвечено"
                }
                val correctAnswerText = if (isOpen) {
                    question.answer.ifBlank { "?" }
                } else {
                    question.options.getOrNull(question.correct) ?: "?"
                }

                ResultQuestionCard(
                    index = index,
                    question = question.question,
                    userAnswerText = userAnswerText,
                    correctAnswerText = correctAnswerText,
                    isCorrect = false
                )
            }
        }

        item {
            if (result.correctCount > 0) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Правильно отвечено: ${result.correctCount} вопросов",
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun ResultQuestionCard(
    index: Int,
    question: String,
    userAnswerText: String,
    correctAnswerText: String,
    isCorrect: Boolean
) {
    val (bgColor, borderColor) = if (isCorrect)
        Color(0xFFDCFCE7) to Color(0xFF16A34A)
    else
        Color(0xFFFEE2E2) to Color(0xFFDC2626)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Вопрос ${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = borderColor
            )
            Spacer(Modifier.height(6.dp))
            MathView(
                latex = question,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp, max = 100.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ваш ответ: ", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFFB91C1C))
                MathView(latex = wrapMath(userAnswerText), modifier = Modifier.weight(1f).heightIn(min = 28.dp, max = 60.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Правильно: ", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFF15803D))
                MathView(latex = wrapMath(correctAnswerText), modifier = Modifier.weight(1f).heightIn(min = 28.dp, max = 60.dp))
            }
        }
    }
}
