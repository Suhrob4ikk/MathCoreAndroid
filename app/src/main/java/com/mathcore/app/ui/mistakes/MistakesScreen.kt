package com.mathcore.app.ui.mistakes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathcore.app.data.Difficulty
import com.mathcore.app.data.Question
import com.mathcore.app.data.QuizConfig
import com.mathcore.app.data.Subject
import com.mathcore.app.ui.theme.MediumRadius
import com.mathcore.app.ui.quiz.MathView
import com.mathcore.app.ui.quiz.wrapMath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakesScreen(
    mistakes: List<Question>,
    onClearMistakes: () -> Unit,
    onRemoveMistake: (Question) -> Unit,
    onPractice: (QuizConfig, List<Question>) -> Unit,
    onBack: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистить все ошибки?") },
            text = { Text("Список всех сохранённых ошибок будет удалён.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearMistakes()
                    showClearDialog = false
                }) { Text("Очистить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои ошибки", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (mistakes.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Очистить всё", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (mistakes.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        onPractice(
                            QuizConfig(Subject.LINALG, Difficulty.MEDIUM, mistakes.size, isStudyMode = true),
                            mistakes.shuffled()
                        )
                    },
                    icon = { Icon(Icons.Default.PlayArrow, null) },
                    text = { Text("Отработать всё") },
                    containerColor = Color(0xFFDC2626)
                )
            }
        }
    ) { padding ->
        if (mistakes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Ошибок нет!", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ошибки из тестов автоматически\nпопадают сюда для повторения",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "${mistakes.size} вопрос${when {
                            mistakes.size % 100 in 11..19 -> "ов"
                            mistakes.size % 10 == 1 -> ""
                            mistakes.size % 10 in 2..4 -> "а"
                            else -> "ов"
                        }} для повторения",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                itemsIndexed(mistakes) { index, question ->
                    MistakeCard(
                        question = question,
                        number = index + 1,
                        onRemove = { onRemoveMistake(question) },
                        onPractice = {
                            onPractice(
                                QuizConfig(Subject.LINALG, Difficulty.MEDIUM, 1, isStudyMode = true),
                                listOf(question)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MistakeCard(
    question: Question,
    number: Int,
    onRemove: () -> Unit,
    onPractice: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(MediumRadius),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "#$number",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626),
                    fontSize = 13.sp
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onPractice, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Повторить", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Done, contentDescription = "Убрать", tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                }
            }
            MathView(
                latex = question.question,
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp, max = 120.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Правильный ответ:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            MathView(
                latex = wrapMath(question.options.getOrElse(question.correct) { "" }),
                textColor = Color(0xFF16A34A),
                modifier = Modifier.fillMaxWidth().heightIn(min = 28.dp, max = 60.dp)
            )
        }
    }
}
