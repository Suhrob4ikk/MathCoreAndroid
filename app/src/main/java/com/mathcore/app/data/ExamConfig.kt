package com.mathcore.app.data

enum class ExamType(
    val displayName: String,
    val questionCount: Int,
    val timeLimitMinutes: Int,
    val emoji: String,
    val description: String
) {
    QUICK("Быстрый", 10, 15, "⚡", "10 вопросов · 15 минут"),
    STANDARD("Стандартный", 20, 30, "📝", "20 вопросов · 30 минут"),
    FULL("Полный", 30, 45, "🎓", "30 вопросов · 45 минут")
}

data class ExamResult(
    val examType: ExamType,
    val correctCount: Int,
    val totalCount: Int,
    val timeSpentSeconds: Int,
    val questions: List<Question>,
    val userAnswers: List<Int?>
) {
    val percentage: Int get() = if (totalCount > 0) (correctCount * 100) / totalCount else 0
    val grade: Int get() = when {
        percentage >= 85 -> 5
        percentage >= 70 -> 4
        percentage >= 50 -> 3
        else -> 2
    }
    val gradeLabel: String get() = when (grade) {
        5 -> "Отлично"
        4 -> "Хорошо"
        3 -> "Удовлетворительно"
        else -> "Неудовлетворительно"
    }
    val passed: Boolean get() = grade >= 3
}
