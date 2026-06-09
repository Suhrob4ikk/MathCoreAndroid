package com.mathcore.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathcore.app.data.Difficulty
import com.mathcore.app.data.ExamResult
import com.mathcore.app.data.ExamType
import com.mathcore.app.data.Question
import com.mathcore.app.data.QuestionRepository
import com.mathcore.app.data.Subject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ExamUiState(
    val examType: ExamType? = null,
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val userAnswers: List<Int?> = emptyList(),
    val timeRemainingSeconds: Int = 0,
    val isFinished: Boolean = false,
    val result: ExamResult? = null,
    val startTimeMillis: Long = 0L,
    /** True while questions are being loaded from assets on the IO thread. */
    val isLoading: Boolean = false
) {
    val currentQuestion: Question? get() = questions.getOrNull(currentIndex)
    val currentAnswer: Int? get() = userAnswers.getOrNull(currentIndex)
    val isCurrentAnswered: Boolean get() = currentAnswer != null
    val isLast: Boolean get() = currentIndex >= questions.size - 1
    val answeredCount: Int get() = userAnswers.count { it != null }
    val timerProgress: Float get() {
        val total = (examType?.timeLimitMinutes ?: 1) * 60
        return timeRemainingSeconds.toFloat() / total
    }
}

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val questionRepo: QuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    /**
     * Build the question list on the IO thread, then start the exam.
     * Shows [ExamUiState.isLoading] during the brief loading phase so the UI
     * can display a spinner instead of freezing the main thread.
     */
    fun loadAndStart(type: ExamType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val questions = withContext(Dispatchers.IO) { buildExamQuestions(type) }
            startExam(questions, type)   // startExam replaces state → isLoading reset to false
        }
    }

    private fun buildExamQuestions(type: ExamType): List<Question> {
        val subjects = Subject.entries.filter { it != Subject.MIXED }
        val perSubject = type.questionCount / subjects.size
        val extra = type.questionCount % subjects.size
        val difficulty = if (type == ExamType.FULL) Difficulty.HARD else Difficulty.MEDIUM
        val questions = mutableListOf<Question>()
        subjects.forEachIndexed { i, subj ->
            val count = if (i < extra) perSubject + 1 else perSubject
            if (count > 0) questions += questionRepo.getShuffledQuestions(subj, difficulty, count, choiceOnly = true)
        }
        return questions.shuffled()
    }

    fun startExam(questions: List<Question>, examType: ExamType) {
        timerJob?.cancel()
        _uiState.value = ExamUiState(
            examType = examType,
            questions = questions,
            userAnswers = List(questions.size) { null },
            timeRemainingSeconds = examType.timeLimitMinutes * 60,
            startTimeMillis = System.currentTimeMillis()
        )
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val remaining = _uiState.value.timeRemainingSeconds - 1
                if (remaining <= 0) {
                    _uiState.update { it.copy(timeRemainingSeconds = 0) }
                    finishExam()
                    break
                }
                _uiState.update { it.copy(timeRemainingSeconds = remaining) }
            }
        }
    }

    fun selectAnswer(index: Int) {
        val state = _uiState.value
        if (state.isCurrentAnswered || state.isFinished) return
        val answers = state.userAnswers.toMutableList()
        answers[state.currentIndex] = index
        _uiState.update { it.copy(userAnswers = answers) }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state.currentIndex < state.questions.size - 1)
            _uiState.update { it.copy(currentIndex = state.currentIndex + 1) }
    }

    fun previousQuestion() {
        val state = _uiState.value
        if (state.currentIndex > 0)
            _uiState.update { it.copy(currentIndex = state.currentIndex - 1) }
    }

    fun finishExam(): ExamResult? {
        timerJob?.cancel()
        val state = _uiState.value
        val examType = state.examType ?: return null
        if (state.isFinished) return state.result
        val timeSpent = ((System.currentTimeMillis() - state.startTimeMillis) / 1000).toInt()
        val correct = state.questions.indices.count { i ->
            state.userAnswers.getOrNull(i) == state.questions[i].correct
        }
        val result = ExamResult(
            examType = examType,
            correctCount = correct,
            totalCount = state.questions.size,
            timeSpentSeconds = timeSpent,
            questions = state.questions,
            userAnswers = state.userAnswers
        )
        _uiState.update { it.copy(isFinished = true, result = result) }
        return result
    }

    fun reset() {
        timerJob?.cancel()
        _uiState.value = ExamUiState()
    }
}
