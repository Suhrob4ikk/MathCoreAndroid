package com.mathcore.app.ui.exam

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathcore.app.data.ExamResult
import com.mathcore.app.data.ExamType
import com.mathcore.app.data.Question
import com.mathcore.app.ui.quiz.MathView
import com.mathcore.app.ui.quiz.wrapMath
import com.mathcore.app.ui.theme.LargeRadius
import com.mathcore.app.ui.theme.MediumRadius
import com.mathcore.app.ui.theme.SmallRadius
import com.mathcore.app.viewmodel.ExamUiState
import com.mathcore.app.viewmodel.ExamViewModel

// ── Entry point: routes between selection → taking → result ──────────────────

@Composable
fun ExamScreen(
    examViewModel: ExamViewModel,
    onBack: () -> Unit,
    onFinished: (ExamResult) -> Unit
) {
    val state by examViewModel.uiState.collectAsState()

    when {
        // Brief loading spinner while questions are read from assets on IO thread
        state.isLoading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF7C3AED), strokeWidth = 5.dp)
        }

        state.result != null -> ExamResultScreen(
            result = state.result!!,
            onHome = { examViewModel.reset(); onBack() },
            onRetry = {
                // loadAndStart dispatches I/O to background thread
                examViewModel.loadAndStart(state.result!!.examType)
            }
        )
        state.examType != null -> ExamTakingScreen(
            state = state,
            onAnswer = examViewModel::selectAnswer,
            onNext = examViewModel::nextQuestion,
            onPrevious = examViewModel::previousQuestion,
            onFinish = {
                val result = examViewModel.finishExam()
                if (result != null) onFinished(result)
            },
            onExit = { examViewModel.reset(); onBack() }
        )
        else -> ExamSelectionScreen(
            onBack = onBack,
            // loadAndStart dispatches I/O to background thread (was main-thread block)
            onStart = { type -> examViewModel.loadAndStart(type) }
        )
    }
}

// ── Exam type selection ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSelectionScreen(
    onBack: () -> Unit,
    onStart: (ExamType) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Экзамен", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LargeRadius))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF7C3AED))))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Экзаменационный режим", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Строгий режим: таймер не останавливается, ответ нельзя изменить. Результат оценивается по 5-балльной шкале.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }

            Text("Выберите формат", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            ExamType.entries.forEach { type ->
                ExamTypeCard(type = type, onClick = { onStart(type) })
            }

            Card(
                shape = RoundedCornerShape(MediumRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Оценки", fontWeight = FontWeight.Bold)
                    GradeRow("5 — Отлично", "85–100%", Color(0xFF16A34A))
                    GradeRow("4 — Хорошо", "70–84%", Color(0xFF2563EB))
                    GradeRow("3 — Удовлетворительно", "50–69%", Color(0xFFD97706))
                    GradeRow("2 — Неудовлетворительно", "< 50%", Color(0xFFDC2626))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GradeRow(label: String, range: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
        Text(range, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ExamTypeCard(type: ExamType, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val (bg, accent) = when (type) {
        ExamType.QUICK    -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        ExamType.STANDARD -> MaterialTheme.colorScheme.primaryContainer  to MaterialTheme.colorScheme.primary
        ExamType.FULL     -> (if (isDark) Color(0xFF1A0030) else Color(0xFFF5F3FF)) to
                             (if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED))
    }
    val typeIcon = when (type) {
        ExamType.QUICK -> Icons.Default.FlashOn
        ExamType.STANDARD -> Icons.AutoMirrored.Filled.Assignment
        ExamType.FULL -> Icons.Default.School
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(LargeRadius),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(typeIcon, null, tint = accent, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(type.displayName, fontWeight = FontWeight.Bold, color = accent, fontSize = 16.sp)
                Text(type.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Все разделы · ${if (type == ExamType.FULL) "Сложный" else "Средний"}",
                    fontSize = 11.sp,
                    color = accent.copy(alpha = 0.7f)
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = accent)
        }
    }
}

// ── Exam taking screen ───────────────────────────────────────────────────────

@Composable
fun ExamTakingScreen(
    state: ExamUiState,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinish: () -> Unit,
    onExit: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }
    val question = state.currentQuestion ?: return

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Выйти из экзамена?") },
            text = { Text("Прогресс будет потерян. Вы уверены?") },
            confirmButton = {
                TextButton(onClick = onExit) { Text("Выйти", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Продолжить") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        ExamTopBar(state = state, onExit = { showExitDialog = true })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExamQuestionCard(question = question, index = state.currentIndex, total = state.questions.size)

            question.options.forEachIndexed { index, option ->
                ExamAnswerOption(
                    text = option,
                    index = index,
                    isAnswered = state.isCurrentAnswered,
                    userAnswer = state.currentAnswer,
                    correctAnswer = question.correct,
                    onClick = { onAnswer(index) }
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        ExamBottomBar(
            isFirst = state.currentIndex == 0,
            isLast = state.isLast,
            isAnswered = state.isCurrentAnswered,
            answeredCount = state.answeredCount,
            totalCount = state.questions.size,
            onPrevious = onPrevious,
            onNext = onNext,
            onFinish = onFinish
        )
    }
}

@Composable
private fun ExamTopBar(state: ExamUiState, onExit: () -> Unit) {
    val minutes = state.timeRemainingSeconds / 60
    val seconds = state.timeRemainingSeconds % 60
    val isWarning = state.timeRemainingSeconds <= 60
    val timerColor = if (isWarning) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface

    Surface(shadowElevation = 4.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, contentDescription = "Выйти из экзамена")
                    }
                    Text(
                        "Вопрос ${state.currentIndex + 1} / ${state.questions.size}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = timerColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "%02d:%02d".format(minutes, seconds),
                        fontWeight = FontWeight.Bold,
                        color = timerColor,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (state.currentIndex + 1).toFloat() / state.questions.size },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (isWarning) Color(0xFFDC2626) else Color(0xFF7C3AED),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun ExamQuestionCard(question: Question, index: Int, total: Int) {
    Card(shape = RoundedCornerShape(MediumRadius), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Вопрос ${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF7C3AED),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "из $total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            MathView(
                latex = question.question,
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 200.dp)
            )
        }
    }
}

@Composable
private fun ExamAnswerOption(
    text: String,
    index: Int,
    isAnswered: Boolean,
    userAnswer: Int?,
    correctAnswer: Int,
    onClick: () -> Unit
) {
    val isChosen = index == userAnswer
    val isCorrect = index == correctAnswer

    val correctGreen = Color(0xFF16A34A)
    val wrongRed = Color(0xFFDC2626)
    val (bgColor, borderColor) = when {
        !isAnswered -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        isCorrect -> correctGreen.copy(alpha = 0.12f) to correctGreen
        isChosen && !isCorrect -> wrongRed.copy(alpha = 0.12f) to wrongRed
        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    }

    val textColor = when {
        !isAnswered -> MaterialTheme.colorScheme.onSurface
        isCorrect -> correctGreen
        isChosen && !isCorrect -> wrongRed
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    val labels = listOf("A", "B", "C", "D")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SmallRadius))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(SmallRadius))
            .clickable(enabled = !isAnswered, onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    when {
                        isAnswered && isCorrect -> Color(0xFF16A34A)
                        isAnswered && isChosen && !isCorrect -> Color(0xFFDC2626)
                        else -> Color(0xFF7C3AED).copy(alpha = 0.15f)
                    }
                )
        ) {
            if (isAnswered && isCorrect) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            } else if (isAnswered && isChosen && !isCorrect) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
            } else {
                Text(labels[index], fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF7C3AED))
            }
        }
        Spacer(Modifier.width(12.dp))
        MathView(
            latex = wrapMath(text),
            textColor = textColor,
            modifier = Modifier.weight(1f).heightIn(min = 36.dp, max = 120.dp)
        )
    }
}

@Composable
private fun ExamBottomBar(
    isFirst: Boolean,
    isLast: Boolean,
    isAnswered: Boolean,
    answeredCount: Int,
    totalCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                "Отвечено: $answeredCount / $totalCount",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = !isFirst,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(SmallRadius)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Назад")
                }
                if (isLast) {
                    Button(
                        onClick = onFinish,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(SmallRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Сдать экзамен", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(SmallRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Text(if (isAnswered) "Следующий" else "Пропустить")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ── Exam result / certificate screen ────────────────────────────────────────

@Composable
fun ExamResultScreen(
    result: ExamResult,
    onHome: () -> Unit,
    onRetry: () -> Unit
) {
    val gradeColor = when (result.grade) {
        5 -> Color(0xFF16A34A)
        4 -> Color(0xFF2563EB)
        3 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }

    val minutes = result.timeSpentSeconds / 60
    val seconds = result.timeSpentSeconds % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Certificate card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        if (result.passed)
                            listOf(Color(0xFF1E3A8A), Color(0xFF7C3AED))
                        else
                            listOf(Color(0xFF7F1D1D), Color(0xFFB91C1C))
                    )
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "РЕЗУЛЬТАТ ЭКЗАМЕНА",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    result.examType.displayName.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(24.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        result.grade.toString(),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    result.gradeLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        // Stats
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "Результат",
                    value = "${result.percentage}%",
                    color = gradeColor,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Правильно",
                    value = "${result.correctCount}/${result.totalCount}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Время",
                    value = "%02d:%02d".format(minutes, seconds),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Per-question breakdown
            Text("Разбор вопросов", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

            result.questions.forEachIndexed { i, q ->
                val userAns = result.userAnswers.getOrNull(i)
                val correct = userAns == q.correct
                val skipped = userAns == null
                Card(
                    shape = RoundedCornerShape(SmallRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            skipped -> MaterialTheme.colorScheme.surfaceVariant
                            correct -> Color(0xFFDCFCE7)
                            else -> Color(0xFFFEE2E2)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "${i + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            MathView(
                                latex = q.question,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp, max = 80.dp)
                            )
                            if (!correct) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Правильный ответ:",
                                    fontSize = 11.sp,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Medium
                                )
                                MathView(
                                    latex = wrapMath(q.options.getOrNull(q.correct) ?: ""),
                                    textColor = Color(0xFF16A34A),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp, max = 60.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            if (correct) Icons.Default.CheckCircle else if (skipped) Icons.Default.RadioButtonUnchecked else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = when {
                                skipped -> MaterialTheme.colorScheme.onSurfaceVariant
                                correct -> Color(0xFF16A34A)
                                else -> Color(0xFFDC2626)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(SmallRadius),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Пересдать", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(SmallRadius)
            ) {
                Icon(Icons.Default.Home, null)
                Spacer(Modifier.width(8.dp))
                Text("На главную", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(SmallRadius), modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
