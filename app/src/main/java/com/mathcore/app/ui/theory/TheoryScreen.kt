package com.mathcore.app.ui.theory

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mathcore.app.data.Subject
import com.mathcore.app.data.TheoryData
import com.mathcore.app.data.TheoryStep
import com.mathcore.app.data.TheorySubject
import com.mathcore.app.data.theoryKey
import com.mathcore.app.ui.quiz.buildKatexHtml

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheoryScreen(
    subject: Subject,
    onBack: () -> Unit,
    onStartQuiz: () -> Unit
) {
    val key = subject.theoryKey()
    val data = TheoryData.subjects[key]

    if (data == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Теория") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Теория для этого раздела скоро появится")
            }
        }
        return
    }

    val accentColor = parseHexColor(data.accentColorHex)
    var detailStep by remember { mutableStateOf<TheoryStep?>(null) }
    var detailSheetOpen by remember { mutableStateOf(false) }

    if (detailSheetOpen && detailStep != null) {
        DetailBottomSheet(
            step = detailStep!!,
            accentColor = accentColor,
            onDismiss = { detailSheetOpen = false; detailStep = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(data.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${data.steps.size} шагов · Теория", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header card
            item {
                TheoryHeaderCard(data = data, accentColor = accentColor)
            }

            // Steps
            itemsIndexed(data.steps) { index, step ->
                TheoryStepCard(
                    index = index,
                    step = step,
                    accentColor = accentColor,
                    onShowDetail = {
                        detailStep = step
                        detailSheetOpen = true
                    }
                )
            }

            // Start quiz button
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onStartQuiz,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Перейти к тесту →",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun TheoryHeaderCard(data: TheorySubject, accentColor: Color) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Text(data.icon, fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(data.title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${data.steps.size} шагов · Базовые формулы и правила",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun TheoryStepCard(
    index: Int,
    step: TheoryStep,
    accentColor: Color,
    onShowDetail: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Step number + title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                ) {
                    Text(
                        "${index + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = accentColor
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    step.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Formula
            FormulaCard(
                latex = step.formula,
                accentColor = accentColor
            )

            Spacer(Modifier.height(10.dp))

            // Note
            if (step.note.isNotBlank()) {
                NoteView(note = step.note, textColor = onSurface)
            }

            Spacer(Modifier.height(10.dp))

            // Detail button
            if (step.detail.isNotBlank()) {
                TextButton(
                    onClick = onShowDetail,
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Подробнее и примеры", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FormulaCard(latex: String, accentColor: Color) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
    ) {
        MathFormulaView(
            latex = latex,
            textColor = textColor,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp, max = 180.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun NoteView(note: String, textColor: Color) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("💡", fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(8.dp))
        MathFormulaView(
            latex = note,
            textColor = textColor.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp, max = 120.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailBottomSheet(
    step: TheoryStep,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val textColor = MaterialTheme.colorScheme.onSurface

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, null,
                        tint = accentColor, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    step.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Detail HTML rendered with KaTeX
            HtmlMathView(
                html = step.detail,
                textColor = textColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 500.dp)
            )
        }
    }
}

// ─────────────────── WebView helpers ───────────────────

private data class TheoryWebState(val key: String, val colorHex: String)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MathFormulaView(
    latex: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val colorHex = remember(textColor) { "#%06X".format(textColor.toArgb() and 0xFFFFFF) }
    val html = remember(latex, colorHex) { buildKatexHtml(latex, colorHex) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                tag = TheoryWebState(latex, colorHex)
                loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
            }
        },
        update = { wv ->
            val prev = wv.tag as? TheoryWebState
            if (prev?.key != latex || prev?.colorHex != colorHex) {
                wv.tag = TheoryWebState(latex, colorHex)
                wv.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier
    )
}

// BUG-SERI-3 FIX: KaTeX loaded from local assets — same as QuizScreen.
private fun buildDetailKatexHtml(htmlContent: String, colorHex: String) = """<!DOCTYPE html>
<html><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link rel="stylesheet" href="katex/katex.min.css">
<script src="katex/katex.min.js"></script>
<script src="katex/auto-render.min.js"></script>
<style>
body{margin:12px 8px;font-family:sans-serif;font-size:15px;color:$colorHex;background:transparent;line-height:1.6;}
p{margin:0 0 10px 0;}
strong{font-weight:700;}
em{font-style:italic;}
.katex{font-size:1.05em;}
</style>
</head><body>
<div id="c">$htmlContent</div>
<script>renderMathInElement(document.getElementById('c'),{delimiters:[{left:"\\\(",right:"\\\)",display:false},{left:"\\\[",right:"\\\]",display:true}]});</script>
</body></html>"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlMathView(
    html: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val colorHex = remember(textColor) { "#%06X".format(textColor.toArgb() and 0xFFFFFF) }
    val page = remember(html, colorHex) { buildDetailKatexHtml(html, colorHex) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                tag = TheoryWebState(html, colorHex)
                loadDataWithBaseURL("file:///android_asset/", page, "text/html", "UTF-8", null)
            }
        },
        update = { wv ->
            val prev = wv.tag as? TheoryWebState
            if (prev?.key != html || prev?.colorHex != colorHex) {
                wv.tag = TheoryWebState(html, colorHex)
                wv.loadDataWithBaseURL("file:///android_asset/", page, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier
    )
}

// Parse "#rrggbb" → Color
private fun parseHexColor(hex: String): Color {
    return try {
        Color(("FF" + hex.trimStart('#')).toLong(16) or 0xFF000000.toLong())
    } catch (_: Exception) {
        Color(0xFF6366F1)
    }
}
