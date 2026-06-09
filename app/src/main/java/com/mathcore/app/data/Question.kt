package com.mathcore.app.data

/**
 * Canonical answer normaliser shared by [QuizViewModel], [MainActivity], and any
 * future answer-checking logic. Mirrors web test.js normalizeAnswer() exactly.
 *
 * Rules:
 *  - Trim outer whitespace, lowercase
 *  - Remove ALL internal whitespace (so "sin x" → "sinx", matches web)
 *  - Replace comma decimal separator with dot
 *  - Trim trailing zeros only when a decimal point is present ("10" stays "10")
 *  - Trim a trailing dot left over after zero-stripping ("1." → "1")
 */
fun normalizeAnswer(text: String): String {
    val s = text.trim().lowercase().replace(Regex("\\s+"), "").replace(",", ".")
    return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
}

data class Question(
    val question: String,
    val options: List<String> = emptyList(),
    val correct: Int = 0,
    val type: String = "choice",   // "choice" | "open"
    val answer: String = ""        // correct answer for open questions
)

enum class Subject(val displayName: String, val emoji: String) {
    MIXED("Смешанный", "🔀"),
    INTEGRALS("Интегралы", "∫"),
    DERIVATIVES("Производные", "∂"),
    LIMITS("Пределы", "lim"),
    SERIES("Ряды", "Σ"),
    ODE("Дифф. уравнения", "dy/dx"),
    PROBABILITY("Вероятность", "P"),
    LINALG("Линейная алгебра", "A")
}

enum class Difficulty(val displayName: String, val color: Long) {
    EASY("Лёгкий", 0xFF4CAF50),
    MEDIUM("Средний", 0xFFFF9800),
    HARD("Сложный", 0xFFF44336)
}

data class QuizConfig(
    val subject: Subject,
    val difficulty: Difficulty,
    val questionCount: Int,
    val isStudyMode: Boolean = false,
    val isDailyChallenge: Boolean = false
)

data class QuizResult(
    val config: QuizConfig,
    val correctCount: Int,
    val totalCount: Int,
    val userAnswers: List<Int?>,
    val openAnswers: List<String?> = emptyList(),
    val questions: List<Question>
) {
    val percentage: Int get() = if (totalCount > 0) (correctCount * 100) / totalCount else 0
}
