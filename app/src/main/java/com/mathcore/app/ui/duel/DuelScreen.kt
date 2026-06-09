package com.mathcore.app.ui.duel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathcore.app.data.Difficulty
import com.mathcore.app.data.Question
import com.mathcore.app.data.Subject
import com.mathcore.app.data.model.PublicUserInfo
import com.mathcore.app.viewmodel.DuelPhase
import com.mathcore.app.viewmodel.DuelViewModel
import com.mathcore.app.viewmodel.PendingInvite

@Composable
fun DuelScreen(
    myName: String,
    // BUG-1 / BUG-4 FIX: pass Subject + Difficulty so MainActivity can build
    // QuizConfig with the correct section instead of hardcoding Subject.LINALG.
    onStartQuiz: (List<Question>, Subject, Difficulty) -> Unit,
    onBack: () -> Unit,
    duelViewModel: DuelViewModel = viewModel()
) {
    val state by duelViewModel.uiState.collectAsState()
    val userSuggestions by duelViewModel.userSuggestions.collectAsState()

    LaunchedEffect(state.phase) {
        if (state.phase == DuelPhase.ACTIVE && state.questions.isNotEmpty()) {
            onStartQuiz(state.questions, state.selectedSection, state.selectedDiff)
        }
    }

    DisposableEffect(Unit) {
        onDispose { duelViewModel.leaveDuel() }
    }

    // Incoming invite dialog
    state.pendingInvite?.let { invite ->
        IncomingInviteDialog(
            invite = invite,
            onAccept = { duelViewModel.acceptInvite(invite, myName) },
            onDecline = { duelViewModel.dismissInvite() }
        )
    }

    // Incoming rematch-request dialog (shown over the results screen)
    state.pendingRematch?.let { opponentName ->
        RematchRequestDialog(
            opponentName = opponentName,
            onAccept = { duelViewModel.acceptRematch() },
            onDecline = { duelViewModel.declineRematch() }
        )
    }

    when (state.phase) {
        DuelPhase.IDLE -> DuelLobbyScreen(
            myName = myName,
            onCreateDuel = { section, diff, target ->
                duelViewModel.createDuel(myName, section, diff, target)
            },
            onJoinDuel = { code -> duelViewModel.joinDuel(myName, code) },
            onBack = onBack,
            userSuggestions = userSuggestions,
            onSearchUsers = { duelViewModel.searchUsers(it) },
            onClearSuggestions = { duelViewModel.clearUserSuggestions() }
        )
        DuelPhase.WAITING -> DuelWaitingScreen(
            code = state.code,
            myName = myName,
            onCancel = { duelViewModel.leaveDuel(); onBack() }
        )
        DuelPhase.COUNTDOWN -> DuelCountdownScreen(
            countdown = state.countdown,
            opponentName = state.opponentName
        )
        DuelPhase.LOADING -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(color = Color(0xFF7C3AED), strokeWidth = 5.dp)
            Spacer(Modifier.height(16.dp))
            Text("Загружаем вопросы…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // ACTIVE: LaunchedEffect(state.phase) above starts the quiz immediately.
        DuelPhase.ACTIVE -> Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        // Quiz done, waiting for opponent's score event (up to 60 s timeout).
        DuelPhase.WAITING_FOR_SCORE -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            CircularProgressIndicator(color = Color(0xFF7C3AED), strokeWidth = 5.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Ожидаем результата ${state.opponentName.ifEmpty { "соперника" }}…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        DuelPhase.FINISHED -> DuelResultsScreen(
            myName = myName,
            myScore = state.myScore,
            opponentName = state.opponentName,
            opponentScore = state.opponentScore,
            rematchRequested = state.rematchRequested,
            onRematch = { duelViewModel.requestRematch() },
            onHome = { duelViewModel.leaveDuel(); onBack() }
        )
    }
}

// ─── Lobby ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelLobbyScreen(
    myName: String,
    onCreateDuel: (Subject, Difficulty, String) -> Unit,
    onJoinDuel: (String) -> Unit,
    onBack: () -> Unit,
    userSuggestions: List<PublicUserInfo> = emptyList(),
    onSearchUsers: (String) -> Unit = {},
    onClearSuggestions: () -> Unit = {}
) {
    var selectedSubject by remember { mutableStateOf(Subject.MIXED) }
    var selectedDiff    by remember { mutableStateOf(Difficulty.MEDIUM) }
    var targetUsername  by remember { mutableStateOf("") }
    var joinCode        by remember { mutableStateOf("") }
    var showJoin        by remember { mutableStateOf(false) }
    var showInvite      by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Дуэль 1 на 1", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF7C3AED),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsEsports, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Дуэль в реальном времени", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Соревнуйся с другом онлайн", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                    }
                }
            }

            // Section selector
            Text("Раздел", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Subject.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { subject ->
                            FilterChip(
                                selected = selectedSubject == subject,
                                onClick = { selectedSubject = subject },
                                label = { Text("${subject.emoji} ${subject.displayName}", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            // Difficulty
            Text("Сложность", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { diff ->
                    FilterChip(
                        selected = selectedDiff == diff,
                        onClick = { selectedDiff = diff },
                        label = { Text(diff.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Optional: invite specific user
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Пригласить конкретного игрока",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f))
                Switch(
                    checked = showInvite,
                    onCheckedChange = { showInvite = it; if (!it) targetUsername = "" }
                )
            }
            AnimatedVisibility(visible = showInvite) {
                Column {
                    OutlinedTextField(
                        value = targetUsername,
                        onValueChange = { v ->
                            targetUsername = v.take(30)
                            onSearchUsers(v.trim())
                        },
                        label = { Text("Имя пользователя") },
                        placeholder = { Text("например: ivan123") },
                        leadingIcon = { Icon(Icons.Default.PersonSearch, null) },
                        trailingIcon = {
                            if (targetUsername.isNotEmpty()) {
                                IconButton(onClick = {
                                    targetUsername = ""
                                    onClearSuggestions()
                                }) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    // Autocomplete dropdown
                    if (userSuggestions.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                userSuggestions.take(5).forEachIndexed { index, user ->
                                    if (index > 0) HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                targetUsername = user.username
                                                onClearSuggestions()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val initial = user.username.firstOrNull()?.uppercaseChar() ?: '?'
                                        val color = Color(
                                            (user.username.hashCode().toLong() and 0xFFFFFF)
                                                .toInt() or 0xFF000000.toInt()
                                        )
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(color.copy(
                                                    red = (color.red * 0.7f + 0.2f).coerceIn(0.2f, 0.8f),
                                                    green = (color.green * 0.7f + 0.2f).coerceIn(0.2f, 0.8f),
                                                    blue = (color.blue * 0.7f + 0.2f).coerceIn(0.2f, 0.8f)
                                                ))
                                        ) {
                                            Text("$initial", color = Color.White,
                                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text(user.username, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    } else if (targetUsername.length >= 2 && showInvite) {
                        Text(
                            "Пользователи не найдены",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Create duel button
            Button(
                onClick = { onCreateDuel(selectedSubject, selectedDiff, targetUsername.trim()) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (targetUsername.isBlank()) "Создать дуэль" else "Пригласить ${targetUsername.trim()}",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            }

            HorizontalDivider()

            // Join duel
            if (!showJoin) {
                OutlinedButton(
                    onClick = { showJoin = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Link, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Войти по коду дуэли")
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Введи код дуэли", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { joinCode = it.uppercase().take(6) },
                            label = { Text("Код (6 символов)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showJoin = false }, modifier = Modifier.weight(1f)) {
                                Text("Отмена")
                            }
                            Button(
                                onClick = { if (joinCode.length == 6) onJoinDuel(joinCode) },
                                enabled = joinCode.length == 6,
                                modifier = Modifier.weight(1f)
                            ) { Text("Войти") }
                        }
                    }
                }
            }
        }
    }
}

// ─── Waiting screen (with copy button) ─────────────────────────────

@Composable
fun DuelWaitingScreen(code: String, myName: String, onCancel: () -> Unit) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp), color = Color(0xFF7C3AED), strokeWidth = 6.dp)
        Spacer(Modifier.height(24.dp))
        Text("Ожидание соперника...", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Твой код дуэли:", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp)
            ) {
                Text(
                    code,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    letterSpacing = 8.sp
                )
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("duel_code", code))
                        copied = true
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (copied) "Скопировано!" else "Скопировать код", fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Поделись этим кодом с другом\nили отправь приглашение по имени",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onCancel) { Text("Отмена") }
    }
}

// ─── Countdown ──────────────────────────────────────────────────────

@Composable
fun DuelCountdownScreen(countdown: Int, opponentName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Соперник: $opponentName", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Text("Начало через", fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))
        AnimatedContent(targetState = countdown, label = "countdown") { count ->
            Text(count.toString(), fontSize = 96.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
        }
        Spacer(Modifier.height(32.dp))
        Text("Приготовься!", fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── Results ─────────────────────────────────────────────────────────

@Composable
fun DuelResultsScreen(
    myName: String,
    myScore: Int,
    opponentName: String,
    opponentScore: Int,   // -1 = opponent disconnected / timed out
    rematchRequested: Boolean = false,
    onRematch: () -> Unit,
    onHome: () -> Unit
) {
    val timedOut  = opponentScore == -1
    val displayOppScore = if (timedOut) 0 else opponentScore
    val isWin = myScore > displayOppScore || timedOut
    val isTie = !timedOut && myScore == displayOppScore
    val resultEmoji = when { timedOut -> "🏆"; isTie -> "🤝"; isWin -> "🏆"; else -> "💪" }
    val resultText  = when { timedOut -> "Соперник отключился — ты победил!"; isTie -> "Ничья!"; isWin -> "Ты победил!"; else -> "Соперник победил" }
    val resultColor = when { timedOut || isWin -> Color(0xFF10B981); isTie -> Color(0xFFF59E0B); else -> Color(0xFFEF4444) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text(resultEmoji, fontSize = 72.sp)
        Spacer(Modifier.height(16.dp))
        Text(resultText, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = resultColor, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerScore(name = myName, score = myScore, isWinner = isWin || isTie)
                Text("VS", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF94A3B8))
                if (timedOut) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(opponentName.ifEmpty { "Соперник" }, fontSize = 13.sp, color = Color(0xFF94A3B8))
                        Text("—", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Text("отключился", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                } else {
                    PlayerScore(name = opponentName.ifEmpty { "Соперник" }, score = displayOppScore, isWinner = !isWin || isTie)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRematch,
            enabled = !rematchRequested && !timedOut,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text(if (rematchRequested) "Ожидаем ответа…" else "Реванш", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.Home, null)
            Spacer(Modifier.width(8.dp))
            Text("Главная")
        }
    }
}

// ─── Rematch-request dialog ──────────────────────────────────────────

@Composable
fun RematchRequestDialog(
    opponentName: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        icon = { Text("⚔️", fontSize = 32.sp) },
        title = { Text("Реванш!", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
        text = { Text("$opponentName хочет сыграть ещё раз. Принять реванш?", textAlign = TextAlign.Center) },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) { Text("Принять") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDecline) { Text("Отклонить") }
        }
    )
}

// ─── Incoming invite dialog ──────────────────────────────────────────

@Composable
fun IncomingInviteDialog(
    invite: PendingInvite,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val sectionName = mapOf(
        "INTEGRALS" to "Интегралы", "DERIVATIVES" to "Производные", "LIMITS" to "Пределы",
        "SERIES" to "Ряды", "ODE" to "Дифф. уравнения", "PROBABILITY" to "Вероятность", "LINALG" to "Лин. алгебра"
    )
    val diffName = mapOf("EASY" to "Лёгкий", "MEDIUM" to "Средний", "HARD" to "Сложный")

    AlertDialog(
        onDismissRequest = onDecline,
        icon = { Text("⚔️", fontSize = 32.sp) },
        title = {
            Text("Приглашение на дуэль", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${invite.fromUsername} вызывает тебя на дуэль!",
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row {
                            Text("Раздел: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text(sectionName[invite.section] ?: invite.section, fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Row {
                            Text("Сложность: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text(diffName[invite.difficulty] ?: invite.difficulty, fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Row {
                            Text("Код дуэли: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text(invite.code, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                letterSpacing = 2.sp, color = Color(0xFF7C3AED))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Icon(Icons.Default.SportsEsports, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Принять", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text("Отклонить") }
        }
    )
}

// ─── Player score widget ─────────────────────────────────────────────

@Composable
fun PlayerScore(name: String, score: Int, isWinner: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp).clip(CircleShape)
                .background(if (isWinner) Color(0xFF10B981) else Color(0xFFEF4444))
        ) {
            Text("${score}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(name, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
