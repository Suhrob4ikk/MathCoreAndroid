package com.mathcore.app.viewmodel

import androidx.lifecycle.ViewModel
import com.mathcore.app.data.Question
import com.mathcore.app.data.QuizConfig
import com.mathcore.app.data.QuizResult
import com.mathcore.app.data.normalizeAnswer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QuizUiState(
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val userAnswers: List<Int?> = emptyList(),
    val openAnswers: List<String?> = emptyList(),
    val isFinished: Boolean = false,
    val config: QuizConfig? = null
) {
    val currentQuestion: Question? get() = questions.getOrNull(currentIndex)
    val isLast: Boolean get() = currentIndex == questions.lastIndex
    val answeredCount: Int get() = questions.indices.count { i ->
        val q = questions.getOrNull(i) ?: return@count false
        if (q.type == "open") openAnswers.getOrNull(i) != null
        else userAnswers.getOrNull(i) != null
    }
    val currentAnswer: Int? get() = userAnswers.getOrNull(currentIndex)
    val isCurrentAnswered: Boolean get() {
        val q = currentQuestion ?: return false
        return if (q.type == "open") openAnswers.getOrNull(currentIndex) != null
        else userAnswers.getOrNull(currentIndex) != null
    }
    val currentOpenAnswer: String? get() = openAnswers.getOrNull(currentIndex)
}

class QuizViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    fun startQuiz(questions: List<Question>, config: QuizConfig) {
        _uiState.value = QuizUiState(
            questions = questions,
            currentIndex = 0,
            userAnswers = List(questions.size) { null },
            openAnswers = List(questions.size) { null },
            isFinished = false,
            config = config
        )
    }

    fun selectAnswer(answerIndex: Int) {
        val state = _uiState.value
        if (state.isCurrentAnswered) return
        val newAnswers = state.userAnswers.toMutableList()
        newAnswers[state.currentIndex] = answerIndex
        _uiState.value = state.copy(userAnswers = newAnswers)
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state.currentIndex < state.questions.lastIndex) {
            _uiState.value = state.copy(currentIndex = state.currentIndex + 1)
        }
    }

    fun previousQuestion() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            _uiState.value = state.copy(currentIndex = state.currentIndex - 1)
        }
    }

    fun submitOpenAnswer(text: String) {
        val state = _uiState.value
        if (state.isCurrentAnswered) return
        val newAnswers = state.openAnswers.toMutableList()
        newAnswers[state.currentIndex] = text.trim()
        _uiState.value = state.copy(openAnswers = newAnswers)
    }

    fun finishQuiz(): QuizResult? {
        val state = _uiState.value
        val config = state.config ?: return null
        val correctCount = state.questions.indices.count { i ->
            val q = state.questions[i]
            if (q.type == "open") {
                val userAns = state.openAnswers.getOrNull(i)?.trim() ?: return@count false
                normalizeAnswer(userAns) == normalizeAnswer(q.answer)
            } else {
                state.userAnswers.getOrNull(i) == q.correct
            }
        }
        _uiState.value = state.copy(isFinished = true)
        return QuizResult(
            config = config,
            correctCount = correctCount,
            totalCount = state.questions.size,
            userAnswers = state.userAnswers,
            openAnswers = state.openAnswers,
            questions = state.questions
        )
    }

    fun goToQuestion(index: Int) {
        val state = _uiState.value
        if (index in state.questions.indices) {
            _uiState.value = state.copy(currentIndex = index)
        }
    }
}
