package com.mathcore.app.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathcore.app.ui.theme.LargeRadius
import com.mathcore.app.ui.theme.SmallRadius
import com.mathcore.app.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var isLogin by remember { mutableStateOf(true) }
    val uiState by authViewModel.uiState.collectAsState()

    // Shared fields — persist across tab switches so user doesn't retype
    var loginEmail    by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var regUsername   by remember { mutableStateOf("") }
    var regEmail      by remember { mutableStateOf("") }
    var regPassword   by remember { mutableStateOf("") }
    var regConfirm    by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onAuthSuccess()
    }
    // Switch to login tab when email-confirm info appears
    LaunchedEffect(uiState.info) {
        if (uiState.info != null) isLogin = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(
                colors = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF3B82F6))
            ))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()               // ← pushes content above keyboard
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(48.dp))

            // Logo
            Text("📐", fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text("MathCore", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Математика для лучших", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))

            Spacer(Modifier.height(40.dp))

            // Auth card
            Card(
                shape = RoundedCornerShape(LargeRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tab switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(SmallRadius))
                            .padding(4.dp)
                    ) {
                        listOf(true to "Войти", false to "Регистрация").forEach { (login, label) ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(
                                        if (isLogin == login) MaterialTheme.colorScheme.surface else Color.Transparent,
                                        RoundedCornerShape(SmallRadius)
                                    )
                            ) {
                                TextButton(onClick = {
                                    isLogin = login
                                    authViewModel.clearError()
                                }) {
                                    Text(
                                        label,
                                        color = if (isLogin == login) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isLogin == login) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Info banner (email confirmation)
                    if (uiState.info != null) {
                        Card(
                            shape = RoundedCornerShape(SmallRadius),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("✉️", fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(uiState.info!!, color = Color(0xFF1D4ED8), fontSize = 13.sp,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = isLogin,
                        label = "auth_form",
                        transitionSpec = {
                            if (targetState) {
                                slideInHorizontally { -it } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                            } else {
                                slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                            }
                        }
                    ) { login ->
                        if (login) {
                            LoginForm(
                                email = loginEmail,
                                password = loginPassword,
                                onEmailChange = { loginEmail = it },
                                onPasswordChange = { loginPassword = it },
                                onLogin = { authViewModel.signIn(loginEmail, loginPassword) },
                                isLoading = uiState.isLoading,
                                error = uiState.error,
                                onForgotPassword = { authViewModel.clearError() }
                            )
                        } else {
                            RegisterForm(
                                username = regUsername,
                                email = regEmail,
                                password = regPassword,
                                confirmPassword = regConfirm,
                                onUsernameChange = { regUsername = it.take(20) },
                                onEmailChange = { regEmail = it },
                                onPasswordChange = { regPassword = it },
                                onConfirmChange = { regConfirm = it },
                                onRegister = {
                                    authViewModel.signUp(regEmail, regPassword, regUsername)
                                },
                                isLoading = uiState.isLoading,
                                error = uiState.error
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Твой прогресс сохраняется в облаке",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Login Form ──────────────────────────────────────────────────────

@Composable
fun LoginForm(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onForgotPassword: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            icon = { Icon(Icons.Default.Email, null, tint = Color(0xFF2563EB)) },
            title = { Text("Восстановление пароля") },
            text = {
                Text(
                    "Для сброса пароля перейдите по ссылке:\nhttps://supabase.com/dashboard\n\n" +
                    "Или напишите нам на support@mathcore.app с темой «Восстановление пароля» и укажите ваш email.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Понятно", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email или логин") },
            leadingIcon = { Icon(Icons.Default.Email, null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(SmallRadius)
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Пароль") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Скрыть" else "Показать"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (email.isNotBlank() && password.isNotBlank()) onLogin()
                }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(SmallRadius)
        )

        if (error != null) {
            Card(
                shape = RoundedCornerShape(SmallRadius),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.ErrorOutline, null,
                        tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(error, color = Color(0xFFB71C1C), fontSize = 13.sp)
                }
            }
        }

        Button(
            onClick = { focusManager.clearFocus(); onLogin() },
            enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(SmallRadius)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.AutoMirrored.Filled.Login, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Войти", fontWeight = FontWeight.Bold)
            }
        }

        TextButton(
            onClick = { showForgotDialog = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Забыл пароль?", color = Color(0xFF2563EB), fontSize = 13.sp)
        }
    }
}

// ─── Register Form ───────────────────────────────────────────────────

@Composable
fun RegisterForm(
    username: String,
    email: String,
    password: String,
    confirmPassword: String,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onRegister: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Validation
    val usernameError = when {
        username.isEmpty() -> null
        username.length < 3 -> "Минимум 3 символа"
        username.length > 20 -> "Максимум 20 символов"
        !username.all { it.isLetterOrDigit() || it == '_' } -> "Только буквы, цифры и знак _"
        else -> null
    }
    val passwordsMatch = confirmPassword.isEmpty() || password == confirmPassword
    val canSubmit = username.length in 3..20 && usernameError == null &&
                    email.isNotBlank() && password.length >= 6 &&
                    password == confirmPassword && !isLoading

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Username
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Имя пользователя") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            isError = usernameError != null,
            supportingText = {
                if (usernameError != null) {
                    Text(usernameError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                } else if (username.isNotEmpty()) {
                    Text("✓ Доступно", color = Color(0xFF16A34A), fontSize = 12.sp)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(SmallRadius)
        )

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(SmallRadius)
        )

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Пароль") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            supportingText = {
                val len = password.length
                when {
                    password.isEmpty() -> Text("Минимум 6 символов", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    len < 6 -> Text("Ещё ${6 - len} символ${if (6-len==1) "" else "а"}...",
                        fontSize = 12.sp, color = Color(0xFFD97706))
                    else -> Text("✓ Длина ок", fontSize = 12.sp, color = Color(0xFF16A34A))
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(SmallRadius)
        )

        // Confirm password
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmChange,
            label = { Text("Повтори пароль") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            isError = !passwordsMatch,
            supportingText = {
                when {
                    confirmPassword.isEmpty() -> {}
                    !passwordsMatch -> Text("Пароли не совпадают",
                        color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    else -> Text("✓ Совпадает", color = Color(0xFF16A34A), fontSize = 12.sp)
                }
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (canSubmit) onRegister()
            }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(SmallRadius)
        )

        if (error != null) {
            Card(
                shape = RoundedCornerShape(SmallRadius),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.ErrorOutline, null,
                        tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(error, color = Color(0xFFB71C1C), fontSize = 13.sp)
                }
            }
        }

        Button(
            onClick = { focusManager.clearFocus(); onRegister() },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(SmallRadius)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Зарегистрироваться", fontWeight = FontWeight.Bold)
            }
        }
    }
}
