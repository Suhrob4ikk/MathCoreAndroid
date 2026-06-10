package com.mathcore.app.ui.quiz

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mathcore.app.data.Question
import com.mathcore.app.data.normalizeAnswer
import com.mathcore.app.ui.theme.LargeRadius
import com.mathcore.app.ui.theme.MediumRadius
import com.mathcore.app.ui.theme.SmallRadius
import com.mathcore.app.util.SoundManager
import com.mathcore.app.viewmodel.QuizUiState

@Composable
fun QuizScreen(
    uiState: QuizUiState,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinish: () -> Unit,
    onExit: () -> Unit,
    onOpenAnswer: (String) -> Unit = {},
    soundEnabled: Boolean = false
) {
    val question = uiState.currentQuestion ?: return
    val progress = (uiState.currentIndex + 1).toFloat() / uiState.questions.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        QuizHeader(
            currentIndex = uiState.currentIndex,
            totalQuestions = uiState.questions.size,
            answeredCount = uiState.answeredCount,
            progress = progress,
            onExit = onExit
        )

        // Question + options
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Question card with math rendering
            QuestionCard(question = question)

            // Answer options
            if (question.type == "open") {
                OpenAnswerInput(
                    onSubmit = onOpenAnswer,
                    isAnswered = uiState.isCurrentAnswered,
                    userAnswer = uiState.currentOpenAnswer,
                    correctAnswer = question.answer,
                    soundEnabled = soundEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                question.options.forEachIndexed { index, option ->
                    AnswerOption(
                        text = option,
                        index = index,
                        isAnswered = uiState.isCurrentAnswered,
                        userAnswer = uiState.currentAnswer,
                        correctAnswer = question.correct,
                        soundEnabled = soundEnabled,
                        onClick = { onAnswer(index) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // Bottom navigation
        QuizBottomBar(
            isFirst = uiState.currentIndex == 0,
            isLast = uiState.isLast,
            isAnswered = uiState.isCurrentAnswered,
            onPrevious = onPrevious,
            onNext = onNext,
            onFinish = onFinish
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizHeader(
    currentIndex: Int,
    totalQuestions: Int,
    answeredCount: Int,
    progress: Float,
    onExit: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, contentDescription = "Выйти")
                    }
                    Column {
                        Text(
                            "Вопрос ${currentIndex + 1} из $totalQuestions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Отвечено: $answeredCount из $totalQuestions",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (answeredCount == totalQuestions)
                                Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Answered pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(LargeRadius))
                        .background(
                            if (answeredCount == totalQuestions) Color(0xFF10B981).copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "$answeredCount / $totalQuestions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (answeredCount == totalQuestions)
                            Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF10B981),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun QuestionCard(question: Question) {
    Card(
        shape = RoundedCornerShape(MediumRadius),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Задание:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            // Math rendering via WebView with KaTeX
            MathView(
                latex = question.question,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 280.dp)
            )
        }
    }
}

// Tracks the last loaded content and color so we can update color via JS without reloading.
private data class MathViewState(val contentKey: String, val colorHex: String)

/** Wraps bare LaTeX expressions in \(...\) so KaTeX renders them. Plain text is HTML-encoded. */
fun wrapMath(text: String): String {
    // Уже содержит разметку — возвращаем как есть
    if (text.contains("\\(") || text.contains("\\[") || text.contains("\$\$")) return text
    // Содержит $...$ — KaTeX сам отрендерит после нашего фикса делимитеров
    if (text.contains("\$")) return text
    return if (text.contains("^") || text.contains("_") || text.contains("\\")) {
        "\\($text\\)"
    } else {
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
}

// BUG-SERI-3 FIX: KaTeX loaded from local assets (app/src/main/assets/katex/)
// instead of CDN, so formulas render without internet and 2-3x faster.
// baseUrl = "file:///android_asset/" tells the WebView to resolve relative paths
// against the assets root, so "katex/katex.min.css" maps to assets/katex/.
internal fun buildKatexHtml(content: String, colorHex: String) = """<!DOCTYPE html>
<html><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link rel="stylesheet" href="katex/katex.min.css">
<script src="katex/katex.min.js"></script>
<script src="katex/auto-render.min.js"></script>
<style>body{margin:8px;font-family:sans-serif;font-size:16px;color:$colorHex;background:transparent;word-wrap:break-word;}.katex{font-size:1.1em;}</style>
</head><body><div id="c">$content</div>
<script>renderMathInElement(document.getElementById('c'),{delimiters:[{left:"${'$'}${'$'}",right:"${'$'}${'$'}",display:true},{left:"${'$'}",right:"${'$'}",display:false},{left:"\\(",right:"\\)",display:false},{left:"\\[",right:"\\]",display:true}]});</script>
</body></html>"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MathView(
    latex: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val colorHex = remember(textColor) { "#%06X".format(textColor.toArgb() and 0xFFFFFF) }
    val html = remember(latex, colorHex) { buildKatexHtml(latex, colorHex) }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        val state = view.tag as? MathViewState ?: return
                        view.evaluateJavascript(
                            "document.body.style.color='${state.colorHex}'", null
                        )
                    }
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                tag = MathViewState(latex, colorHex)
                loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            val prev = webView.tag as? MathViewState
            when {
                prev?.contentKey != latex -> {
                    webView.tag = MathViewState(latex, colorHex)
                    webView.loadDataWithBaseURL(
                        "file:///android_asset/", html, "text/html", "UTF-8", null
                    )
                }
                prev?.colorHex != colorHex -> {
                    webView.tag = MathViewState(latex, colorHex)
                    webView.evaluateJavascript(
                        "document.body.style.color='$colorHex'", null
                    )
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun AnswerOption(
    text: String,
    index: Int,
    isAnswered: Boolean,
    userAnswer: Int?,
    correctAnswer: Int,
    soundEnabled: Boolean = false,
    onClick: () -> Unit
) {
    val isCorrect = index == correctAnswer
    val isChosen = index == userAnswer

    val correctGreen = Color(0xFF16A34A)
    val wrongRed = Color(0xFFDC2626)
    val (bgColor, borderColor, textColor) = when {
        !isAnswered -> Triple(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.onSurface
        )
        isCorrect -> Triple(
            correctGreen.copy(alpha = 0.12f),
            correctGreen,
            correctGreen
        )
        isChosen && !isCorrect -> Triple(
            wrongRed.copy(alpha = 0.12f),
            wrongRed,
            wrongRed
        )
        else -> Triple(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }

    val labels = listOf("A", "B", "C", "D")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SmallRadius))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(SmallRadius))
            .clickable(enabled = !isAnswered, onClick = {
                if (soundEnabled) {
                    if (index == correctAnswer) SoundManager.playCorrect() else SoundManager.playWrong()
                }
                onClick()
            })
            .padding(12.dp)
    ) {
        // Label bubble
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (isAnswered && isCorrect) Color(0xFF16A34A)
                    else if (isAnswered && isChosen && !isCorrect) Color(0xFFDC2626)
                    else MaterialTheme.colorScheme.primaryContainer
                )
        ) {
            if (isAnswered && isCorrect) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            } else if (isAnswered && isChosen && !isCorrect) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            } else {
                Text(labels[index], fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Spacer(Modifier.width(12.dp))

        // Option text — auto-wrap bare LaTeX in \(...\) so KaTeX renders it
        MathView(
            latex = wrapMath(text),
            textColor = textColor,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 36.dp, max = 120.dp)
        )
    }
}


@Composable
fun OpenAnswerInput(
    onSubmit: (String) -> Unit,
    isAnswered: Boolean,
    userAnswer: String?,
    correctAnswer: String,
    soundEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val isCorrect = userAnswer != null && normalizeAnswer(userAnswer) == normalizeAnswer(correctAnswer)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = if (isAnswered) userAnswer ?: "" else inputText,
            onValueChange = { if (!isAnswered) inputText = it },
            label = { Text("Введи ответ") },
            singleLine = true,
            enabled = !isAnswered,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                if (!isAnswered && inputText.isNotBlank()) {
                    val correct = normalizeAnswer(inputText) == normalizeAnswer(correctAnswer)
                    if (soundEnabled) { if (correct) SoundManager.playCorrect() else SoundManager.playWrong() }
                    onSubmit(inputText)
                }
            }),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(SmallRadius),
            colors = if (isAnswered) OutlinedTextFieldDefaults.colors(
                disabledContainerColor = if (isCorrect) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFDC2626).copy(alpha = 0.1f),
                disabledBorderColor = if (isCorrect) Color(0xFF10B981) else Color(0xFFDC2626),
                disabledTextColor = if (isCorrect) Color(0xFF10B981) else Color(0xFFDC2626),
                disabledLabelColor = if (isCorrect) Color(0xFF10B981) else Color(0xFFDC2626)
            ) else OutlinedTextFieldDefaults.colors()
        )

        if (!isAnswered) {
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val correct = normalizeAnswer(inputText) == normalizeAnswer(correctAnswer)
                        if (soundEnabled) { if (correct) SoundManager.playCorrect() else SoundManager.playWrong() }
                        onSubmit(inputText)
                    }
                },
                enabled = inputText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(SmallRadius)
            ) { Text("Проверить ответ", fontWeight = FontWeight.Bold) }
        } else {
            Card(
                shape = RoundedCornerShape(SmallRadius),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect) Color(0xFF10B981).copy(alpha = 0.12f)
                                    else Color(0xFFDC2626).copy(alpha = 0.12f)
                )
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isCorrect) "✓" else "✗", fontSize = 20.sp,
                        color = if (isCorrect) Color(0xFF10B981) else Color(0xFFDC2626))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(if (isCorrect) "Верно!" else "Неверно",
                            fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            color = if (isCorrect) Color(0xFF10B981) else Color(0xFFDC2626))
                        if (!isCorrect) Text("Ответ: $correctAnswer", fontSize = 13.sp, color = Color(0xFF10B981))
                    }
                }
            }
        }
    }
}

@Composable
fun QuizBottomBar(
    isFirst: Boolean,
    isLast: Boolean,
    isAnswered: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = !isFirst,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(SmallRadius)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Назад")
            }

            if (isLast) {
                Button(
                    onClick = onFinish,
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(SmallRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Завершить тест", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(SmallRadius)
                ) {
                    Text(if (isAnswered) "Следующий" else "Пропустить")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
