package com.mathcore.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathcore.app.data.model.Profile
import com.mathcore.app.data.repository.AuthRepository
import com.mathcore.app.data.repository.SessionManager
import com.mathcore.app.data.repository.friendlyAuthError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val currentProfile: Profile? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    // Восстанавливаем профиль из кэша сразу — не ждём сети.
    // Иначе при запуске показывается "Студент" пока идёт загрузка.
    private val _uiState = MutableStateFlow(
        if (SessionManager.isLoggedIn() && !SessionManager.username.isNullOrBlank()) {
            AuthUiState(
                isAuthenticated = true,
                currentProfile = Profile(
                    id       = SessionManager.userId   ?: "",
                    username = SessionManager.username ?: "",
                    email    = SessionManager.email    ?: ""
                )
            )
        } else {
            AuthUiState(isAuthenticated = SessionManager.isLoggedIn())
        }
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Дополнительно обновляем профиль из сети (свежие данные)
        if (SessionManager.isLoggedIn()) refreshProfile()
    }

    fun signIn(emailOrUsername: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, info = null)
            val email = if (emailOrUsername.contains("@")) emailOrUsername
                        else repo.findEmailByUsername(emailOrUsername) ?: emailOrUsername
            val result = repo.signIn(email, password)
            if (result.isSuccess) {
                val profile = repo.getCurrentProfile() ?: Profile(
                    id = SessionManager.userId ?: "",
                    username = SessionManager.username ?: "",
                    email = email
                )
                _uiState.value = AuthUiState(isAuthenticated = true, currentProfile = profile)
            } else {
                val raw = result.exceptionOrNull()?.message
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = friendlyAuthError(raw, isLogin = true)
                )
            }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, info = null)
            val result = repo.signUp(email, password, username)
            when {
                result.isSuccess && result.getOrNull() == "OK" -> {
                    val profile = Profile(
                        id = SessionManager.userId ?: "",
                        username = username, email = email
                    )
                    _uiState.value = AuthUiState(isAuthenticated = true, currentProfile = profile)
                }
                result.isSuccess && result.getOrNull() == "EMAIL_CONFIRM" -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        info = "✉️ Аккаунт создан! Проверь почту $email и подтверди регистрацию, потом войди."
                    )
                }
                else -> {
                    val msg = result.exceptionOrNull()?.message ?: ""
                    val friendly = when {
                        msg.contains("already", ignoreCase = true) ->
                            "Этот email уже зарегистрирован — войди во вкладке «Войти»"
                        else -> msg.ifEmpty { "Ошибка регистрации" }
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = friendly)
                }
            }
        }
    }

    fun signOut() {
        // Сразу сбрасываем состояние — без этого AuthScreen видит isAuthenticated=true
        // при первом compose и немедленно зовёт onAuthSuccess(), возвращая на Home.
        _uiState.value = AuthUiState(isAuthenticated = false)
        viewModelScope.launch {
            repo.signOut()   // очищает сессию на сервере и в SharedPrefs
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null, info = null) }

    fun updateLastSeen() {
        viewModelScope.launch { repo.updateLastSeen() }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            val profile = repo.getCurrentProfile()
            if (profile != null) _uiState.value = _uiState.value.copy(currentProfile = profile)
        }
    }
}
