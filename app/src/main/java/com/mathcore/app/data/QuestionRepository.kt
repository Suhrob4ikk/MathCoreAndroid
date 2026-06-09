package com.mathcore.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class QuestionRepository(private val context: Context) {

    private val gson = Gson()

    fun getQuestions(subject: Subject, difficulty: Difficulty): List<Question> {
        val fileName = "${subject.name.lowercase()}_${difficulty.name.lowercase()}.json"
        return try {
            val json = context.assets.open("questions/$fileName").bufferedReader().readText()
            val type = object : TypeToken<List<Question>>() {}.type
            val raw: List<Question> = gson.fromJson(json, type)
            // Normalize nulls from Gson (it doesn't use Kotlin default values)
            raw.map { q ->
                q.copy(
                    options = q.options ?: emptyList(),
                    type = q.type ?: "choice",
                    answer = q.answer ?: ""
                )
            }
        } catch (e: Exception) {
            getFallbackQuestions(subject, difficulty)
        }
    }

    fun getShuffledQuestions(subject: Subject, difficulty: Difficulty, count: Int, choiceOnly: Boolean = false): List<Question> {
        val questions = getQuestions(subject, difficulty)
            .let { if (choiceOnly) it.filter { q -> q.type != "open" && q.options.size == 4 } else it }
        return questions.shuffled().take(count).map { q ->
            if (q.type == "open" || q.options.isEmpty()) {
                q
            } else {
                val maxIdx = q.options.size - 1
                val shuffledIndices = (0..maxIdx).shuffled()
                val newOptions = shuffledIndices.map { q.options[it] }
                val newCorrect = shuffledIndices.indexOf(q.correct)
                q.copy(options = newOptions, correct = newCorrect)
            }
        }
    }

    private fun getFallbackQuestions(subject: Subject, difficulty: Difficulty): List<Question> {
        return when (subject) {
            Subject.LIMITS -> getLimitsFallback(difficulty)
            Subject.INTEGRALS -> getIntegralsFallback(difficulty)
            Subject.DERIVATIVES -> getDerivativesFallback(difficulty)
            Subject.SERIES -> getSeriesFallback(difficulty)
            Subject.ODE -> getOdeFallback(difficulty)
            Subject.PROBABILITY -> getProbabilityFallback(difficulty)
            Subject.LINALG -> getLinalgFallback(difficulty)
            Subject.MIXED -> getLinalgFallback(difficulty)  // MIXED is handled in getDuelQuestions; fallback to LINALG
        }
    }

    private fun getLimitsFallback(difficulty: Difficulty) = when (difficulty) {
        Difficulty.EASY -> listOf(
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\sin x}{x}\\)", listOf("0", "∞", "π", "1"), 3),
            Question("Найдите \\(\\lim_{x \\to \\infty} \\dfrac{1}{x}\\)", listOf("1", "∞", "0", "-1"), 2),
            Question("Найдите \\(\\lim_{x \\to 2} (3x + 1)\\)", listOf("5", "6", "7", "8"), 2),
            Question("Найдите \\(\\lim_{x \\to \\infty} \\left(1 + \\dfrac{1}{x}\\right)^x\\)", listOf("1", "2", "e", "∞"), 2),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{e^x - 1}{x}\\)", listOf("0", "e", "∞", "1"), 3),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\tan x}{x}\\)", listOf("0", "π/2", "∞", "1"), 3),
            Question("Найдите \\(\\lim_{x \\to 1} \\dfrac{x^2-1}{x-1}\\)", listOf("0", "1", "2", "∞"), 2),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\ln(1+x)}{x}\\)", listOf("0", "ln 2", "∞", "1"), 3),
            Question("Найдите \\(\\lim_{x \\to \\infty} \\dfrac{2x^2+1}{x^2-1}\\)", listOf("-1", "1", "2", "∞"), 2),
            Question("Найдите \\(\\lim_{x \\to 0} \\cos(x)\\)", listOf("0", "1", "-1", "π"), 1),
            Question("Найдите \\(\\lim_{x \\to 5} (2x - 3)\\)", listOf("5", "7", "10", "12"), 1),
            Question("Найдите \\(\\lim_{x \\to 3} x^2\\)", listOf("6", "8", "9", "12"), 2),
            Question("Найдите \\(\\lim_{x \\to 1} (x^2 + x + 1)\\)", listOf("1", "2", "3", "4"), 2),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{2x}{x}\\)", listOf("0", "1", "2", "∞"), 2),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\sin 3x}{x}\\)", listOf("1", "3", "1/3", "0"), 1),
            Question("Найдите \\(\\lim_{x \\to \\infty} \\dfrac{3x+2}{x+1}\\)", listOf("1", "2", "3", "∞"), 2),
            Question("Найдите \\(\\lim_{x \\to 1} \\dfrac{x^3-1}{x-1}\\)", listOf("1", "2", "3", "∞"), 2),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\sin 4x}{\\sin 2x}\\)", listOf("1/2", "1", "2", "4"), 2),
            Question("Найдите \\(\\lim_{x \\to \\infty} \\dfrac{\\ln x}{x}\\)", listOf("∞", "1", "0", "-1"), 2),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{e^{2x}-1}{x}\\)", listOf("0", "1", "2", "e²"), 2)
        )
        Difficulty.MEDIUM -> listOf(
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{x - \\sin x}{x^3}\\)", listOf("0", "\\frac{1}{6}", "\\frac{1}{3}", "1"), 1),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{1-\\cos x}{x^2}\\)", listOf("0", "\\frac{1}{2}", "1", "\\infty"), 1),
            Question("Найдите \\(\\lim_{x \\to \\infty} \\left(\\dfrac{x+1}{x-1}\\right)^x\\)", listOf("1", "e", "e^2", "\\infty"), 2),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\arctan x}{x}\\)", listOf("0", "\\frac{\\pi}{4}", "\\frac{\\pi}{2}", "1"), 3),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{e^x - 1 - x}{x^2}\\)", listOf("0", "\\frac{1}{2}", "1", "e"), 1),
            Question("Найдите \\(\\lim_{x \\to \\infty} x\\sin\\dfrac{1}{x}\\)", listOf("0", "\\frac{1}{2}", "1", "\\infty"), 2),
            Question("Найдите \\(\\lim_{x \\to 0^+} x^x\\)", listOf("0", "1", "e", "\\infty"), 1),
            Question("Найдите \\(\\lim_{x \\to \\infty} \\sqrt{x^2+x} - x\\)", listOf("0", "\\frac{1}{2}", "1", "\\infty"), 1),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\tan x - x}{x^3}\\)", listOf("0", "\\frac{1}{3}", "\\frac{1}{2}", "1"), 1),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\sqrt{1+x}-1}{x}\\)", listOf("0", "\\frac{1}{2}", "1", "2"), 1),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\sin 5x}{\\sin 3x}\\)", listOf("3/5", "1", "5/3", "0"), 2),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{e^x - e^{-x}}{2x}\\)", listOf("0", "1/2", "1", "∞"), 2),
            Question("Найдите \\(\\lim_{x \\to 1} \\dfrac{x^n - 1}{x - 1}\\)", listOf("0", "1", "n", "n-1"), 2),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{1-\\cos x}{x\\sin x}\\)", listOf("0", "1/2", "1", "∞"), 1),
            Question("Найдите \\(\\lim_{x \\to \\pi} \\dfrac{\\sin x}{x-\\pi}\\)", listOf("-π", "-1", "0", "1"), 1),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{e^{3x}-1}{\\sin 2x}\\)", listOf("0", "2/3", "3/2", "∞"), 2),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{a^x - 1}{x}\\), \\(a>0\\)", listOf("0", "1", "a", "ln a"), 3),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\ln(1+x^2)}{x^2}\\)", listOf("0", "1/2", "1", "2"), 2),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\sqrt{1+x}-1}{x}\\)", listOf("0", "1/2", "1", "2"), 1),
            Question("Найдите \\(\\lim_{x \\to \\infty} x\\left(1 - \\cos\\dfrac{1}{x}\\right)\\)", listOf("0", "1/2", "1", "∞"), 0)
        )
        Difficulty.HARD -> listOf(
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\sin x \\cdot \\ln(1+x) - x^2\\cos x}{x^4}\\)", listOf("-\\frac{5}{6}", "-\\frac{1}{6}", "\\frac{1}{6}", "\\frac{5}{6}"), 1),
            Question("Найдите \\(\\lim_{x \\to 0} \\left(\\dfrac{\\sin x}{x}\\right)^{1/x^2}\\)", listOf("e^{-1/6}", "e^{-1/3}", "e^{1/6}", "1"), 0),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\tan x - x}{x - \\sin x}\\)", listOf("\\frac{1}{2}", "1", "2", "\\infty"), 2),
            Question("Найдите \\(\\lim_{x \\to 1} \\dfrac{x^3-3x+2}{x^3-x^2-x+1}\\)", listOf("0", "\\frac{1}{2}", "1", "\\frac{3}{2}"), 3),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\ln(\\cos x)}{x^2}\\)", listOf("-1", "-\\frac{1}{2}", "0", "\\frac{1}{2}"), 1),
            Question("Найдите \\(\\lim_{x \\to 0} (\\cos x)^{1/\\sin^2 x}\\)", listOf("e^{-1/2}", "e^{-1}", "1", "e^{1/2}"), 0),
            Question("Найдите \\(\\lim_{x \\to \\infty} x^{1/(\\ln x)}\\)", listOf("0", "1", "e", "\\infty"), 2),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{e^{\\tan x}-e^x}{x^3}\\)", listOf("-\\frac{1}{6}", "0", "\\frac{1}{6}", "\\frac{1}{3}"), 2),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{\\arcsin x - x}{x^3}\\)", listOf("0", "\\frac{1}{6}", "\\frac{1}{3}", "\\frac{1}{2}"), 1),
            Question("Найдите \\(\\lim_{x \\to 0} \\dfrac{x\\cos x - \\sin x}{x^3}\\)", listOf("-\\frac{1}{6}", "-\\frac{1}{3}", "0", "\\frac{1}{3}"), 1)
        )
    }

    private fun getIntegralsFallback(difficulty: Difficulty) = when (difficulty) {
        Difficulty.EASY -> listOf(
            Question("Вычислите \\(\\int x^2\\,dx\\)", listOf("x^2+C", "\\frac{x^3}{3}+C", "2x+C", "\\frac{x^2}{2}+C"), 1),
            Question("Вычислите \\(\\int e^x\\,dx\\)", listOf("e^x+C", "xe^x+C", "e^{x+1}+C", "e^{x-1}+C"), 0),
            Question("Вычислите \\(\\int \\cos x\\,dx\\)", listOf("-\\sin x+C", "\\cos x+C", "\\sin x+C", "-\\cos x+C"), 2),
            Question("Вычислите \\(\\int \\sin x\\,dx\\)", listOf("\\cos x+C", "-\\cos x+C", "\\sin x+C", "-\\sin x+C"), 1),
            Question("Вычислите \\(\\int \\dfrac{1}{x}\\,dx\\)", listOf("x+C", "\\ln|x|+C", "\\frac{1}{x^2}+C", "x^2+C"), 1),
            Question("\\(\\int_0^1 x\\,dx = ?\\)", listOf("0", "\\frac{1}{4}", "\\frac{1}{2}", "1"), 2),
            Question("\\(\\int_0^{\\pi} \\sin x\\,dx = ?\\)", listOf("0", "1", "2", "\\pi"), 2),
            Question("Вычислите \\(\\int 3x^2\\,dx\\)", listOf("x^3+C", "6x+C", "x^2+C", "3x^3+C"), 0),
            Question("Вычислите \\(\\int (x+1)^2\\,dx\\)", listOf("\\frac{(x+1)^3}{3}+C", "\\frac{(x+1)^2}{2}+C", "2(x+1)+C", "(x+1)^2+C"), 0),
            Question("\\(\\int_0^1 2x\\,dx = ?\\)", listOf("0", "1", "2", "4"), 1)
        )
        Difficulty.MEDIUM -> listOf(
            Question("Вычислите \\(\\int x e^x\\,dx\\)", listOf("e^x+C", "xe^x-e^x+C", "xe^x+e^x+C", "\\frac{x^2 e^x}{2}+C"), 1),
            Question("Вычислите \\(\\int \\ln x\\,dx\\)", listOf("\\frac{1}{x}+C", "x\\ln x+C", "x\\ln x - x+C", "\\frac{x}{\\ln x}+C"), 2),
            Question("Вычислите \\(\\int \\dfrac{dx}{1+x^2}\\)", listOf("\\ln(1+x^2)+C", "\\arctan x+C", "\\arcsin x+C", "\\frac{2x}{(1+x^2)^2}+C"), 1),
            Question("Вычислите \\(\\int \\dfrac{dx}{\\sqrt{1-x^2}}\\)", listOf("\\arctan x+C", "\\arcsin x+C", "\\arccos x+C", "\\ln x+C"), 1),
            Question("Вычислите \\(\\int \\sin^2 x\\,dx\\)", listOf("\\sin x\\cos x+C", "\\frac{x}{2}-\\frac{\\sin 2x}{4}+C", "-\\frac{\\cos 2x}{2}+C", "\\cos^2 x+C"), 1),
            Question("\\(\\int_1^e \\ln x\\,dx = ?\\)", listOf("0", "1", "e-1", "e"), 1),
            Question("Вычислите \\(\\int \\dfrac{x}{x^2+1}\\,dx\\)", listOf("\\arctan x+C", "\\frac{\\ln(x^2+1)}{2}+C", "\\frac{1}{x^2+1}+C", "2x+C"), 1),
            Question("Вычислите \\(\\int x\\sin x\\,dx\\)", listOf("\\sin x - x\\cos x+C", "\\cos x+C", "-x\\cos x + \\sin x+C", "x\\sin x+C"), 2),
            Question("\\(\\int_0^{\\pi/2} \\sin x\\cos x\\,dx = ?\\)", listOf("0", "\\frac{1}{4}", "\\frac{1}{2}", "1"), 2),
            Question("Вычислите \\(\\int \\dfrac{2x+1}{x^2+x}\\,dx\\)", listOf("2\\ln|x|+C", "\\ln|x^2+x|+C", "\\frac{1}{x^2+x}+C", "2x+1+C"), 1)
        )
        Difficulty.HARD -> listOf(
            Question("\\(\\int_0^{+\\infty} e^{-x}\\,dx = ?\\)", listOf("0", "1", "\\infty", "e"), 1),
            Question("\\(\\int_1^{+\\infty} \\dfrac{dx}{x^2} = ?\\)", listOf("0", "1", "2", "\\infty"), 1),
            Question("\\(\\int_0^1 \\dfrac{dx}{\\sqrt{x}} = ?\\)", listOf("1", "2", "\\frac{1}{2}", "\\infty"), 1),
            Question("Вычислите \\(\\int e^x \\sin x\\,dx\\)", listOf("\\frac{e^x(\\sin x - \\cos x)}{2}+C", "e^x\\sin x+C", "e^x\\cos x+C", "\\frac{\\sin x + \\cos x}{2}+C"), 0),
            Question("Вычислите \\(\\int \\dfrac{dx}{x^2-1}\\)", listOf("\\arctan x+C", "\\ln|x-1|+C", "\\frac{1}{2}\\ln\\left|\\frac{x-1}{x+1}\\right|+C", "\\frac{1}{x^2-1}+C"), 2),
            Question("\\(\\int_{-\\infty}^{+\\infty} e^{-x^2}\\,dx = ?\\)", listOf("1", "\\pi", "\\sqrt{\\pi}", "2"), 2),
            Question("Вычислите \\(\\int \\dfrac{x^2}{x^2+1}\\,dx\\)", listOf("x+C", "x - \\arctan x+C", "\\arctan x+C", "\\frac{x^2}{2}+C"), 1),
            Question("\\(\\int_0^{\\pi} x\\sin x\\,dx = ?\\)", listOf("0", "1", "\\pi", "2\\pi"), 2),
            Question("Вычислите \\(\\int \\sec^2 x\\,dx\\)", listOf("\\sin x+C", "\\tan x+C", "\\sec x+C", "\\sec x\\tan x+C"), 1),
            Question("\\(\\int_0^{\\pi/4} \\tan^2 x\\,dx = ?\\)", listOf("0", "1 - \\frac{\\pi}{4}", "\\frac{\\pi}{4}", "\\frac{\\pi}{4} - 1"), 3)
        )
    }

    private fun getDerivativesFallback(difficulty: Difficulty) = when (difficulty) {
        Difficulty.EASY -> listOf(
            Question("Найдите производную \\(f(x) = x^3\\)", listOf("x²", "3x²", "3x", "x³"), 1),
            Question("Найдите производную \\(f(x) = e^x\\)", listOf("xe^{x-1}", "e^{x+1}", "e^x", "e"), 2),
            Question("Найдите производную \\(f(x) = \\sin x\\)", listOf("-sin x", "cos x", "-cos x", "sin x"), 1),
            Question("Найдите производную \\(f(x) = \\ln x\\)", listOf("e^x", "1/x", "ln x", "x"), 1),
            Question("Найдите производную \\(f(x) = x^2 + 2x\\)", listOf("x²", "2x", "2x+2", "2x²"), 2),
            Question("Найдите производную \\(f(x) = \\cos x\\)", listOf("sin x", "-sin x", "cos x", "-cos x"), 1),
            Question("Найдите производную \\(f(x) = 5\\)", listOf("5", "0", "1", "x"), 1),
            Question("Найдите производную \\(f(x) = \\sqrt{x}\\)", listOf("1/(2√x)", "√x/2", "2√x", "1/√x"), 0),
            Question("Найдите производную \\(f(x) = \\tan x\\)", listOf("sin x", "1/cos²x", "cos x", "cot x"), 1),
            Question("Найдите производную \\(f(x) = x^n\\)", listOf("x^{n-1}", "nx^{n-1}", "(n-1)x^n", "nx^n"), 1),
            Question("Найдите производную \\(f(x) = x^4\\)", listOf("x³", "4x³", "4x", "x⁴"), 1),
            Question("Найдите производную \\(f(x) = 3x^2 + 2x\\)", listOf("6x", "6x+2", "3x+2", "6x²"), 1),
            Question("Найдите производную \\(f(x) = \\sin(2x)\\)", listOf("cos(2x)", "2cos(2x)", "-cos(2x)", "-2cos(2x)"), 1),
            Question("Найдите производную \\(f(x) = e^{3x}\\)", listOf("e^{3x}", "3e^{3x}", "e^{3x}/3", "3xe^{3x}"), 1),
            Question("Найдите производную \\(f(x) = \\ln(2x)\\)", listOf("1/(2x)", "2/x", "1/x", "2ln(x)"), 2),
            Question("Найдите производную \\(f(x) = x^2 \\sin x\\)", listOf("2x sin x", "x² cos x", "2x sin x + x² cos x", "2x cos x"), 2),
            Question("Найдите производную \\(f(x) = \\dfrac{1}{x^2}\\)", listOf("1/(2x)", "-2/x³", "-1/x²", "2/x³"), 1),
            Question("Найдите производную \\(f(x) = \\cos^2 x\\)", listOf("2cos x", "-2cos x sin x", "2sin x", "-sin(2x)"), 1),
            Question("Производная суммы функций равна...", listOf("произведению производных", "сумме производных", "частному производных", "нулю"), 1),
            Question("Найдите производную \\(f(x) = \\sqrt{3x+1}\\)", listOf("1/(2√(3x+1))", "3/(2√(3x+1))", "√3/(2√x)", "3/(√(3x+1))"), 1)
        )
        Difficulty.MEDIUM -> listOf(
            Question("Найдите производную \\(f(x) = x e^x\\)", listOf("e^x", "xe^x", "(1+x)e^x", "xe^{x-1}"), 2),
            Question("Найдите производную \\(f(x) = \\sin(x^2)\\)", listOf("cos(x²)", "2x sin(x²)", "2x cos(x²)", "cos(2x)"), 2),
            Question("Найдите производную \\(f(x) = \\ln(\\sin x)\\)", listOf("1/sin x", "cos x/sin x", "-cot x", "1/x"), 1),
            Question("Найдите производную \\(f(x) = x^2 \\ln x\\)", listOf("2x ln x", "x + 2x ln x", "2x ln x + x", "2x/x"), 2),
            Question("Найдите производную \\(f(x) = \\dfrac{x}{x+1}\\)", listOf("1/(x+1)", "1/(x+1)²", "-1/(x+1)²", "x/(x+1)²"), 1),
            Question("Найдите производную \\(f(x) = e^{\\sin x}\\)", listOf("e^{sin x}", "cos x · e^{sin x}", "sin x · e^{sin x}", "e^{cos x}"), 1),
            Question("Найдите производную \\(f(x) = \\arctan x\\)", listOf("1/(1+x²)", "-1/(1+x²)", "1/x", "arctan x"), 0),
            Question("Найдите производную \\(f(x) = \\ln(x^2 + 1)\\)", listOf("1/(x²+1)", "2x ln(x²+1)", "2x/(x²+1)", "2/(x²+1)"), 2),
            Question("Найдите производную \\(f(x) = (2x+1)^5\\)", listOf("5(2x+1)⁴", "10(2x+1)⁴", "(2x+1)⁵/5", "5(2x+1)"), 1),
            Question("Критическая точка функции — это точка, где производная...", listOf("Равна бесконечности", "Равна нулю или не существует", "Строго положительна", "Строго отрицательна"), 1)
        )
        Difficulty.HARD -> listOf(
            Question("Найдите производную \\(f(x) = x^x\\)", listOf("x^x ln x", "x^{x-1}", "x^x(1+ln x)", "x^{x+1}/(x+1)"), 2),
            Question("Найдите производную \\(f(x) = \\arcsin(\\sqrt{x})\\)", listOf("1/(2√(x(1-x)))", "1/√(1-x)", "1/(2√x)", "-1/√(1-x)"), 0),
            Question("Вторая производная \\(f(x) = x^3\\) при \\(x=2\\) равна...", listOf("6", "12", "3", "9"), 1),
            Question("Найдите производную \\(f(x) = \\ln\\tan\\dfrac{x}{2}\\)", listOf("1/sin x", "1/cos x", "cot x", "tan x/2"), 0),
            Question("Производная \\(f(x) = a^x\\) равна...", listOf("xa^{x-1}", "a^x", "a^x ln a", "x ln a"), 2),
            Question("Найдите \\(y'\\) из уравнения \\(x^2 + y^2 = 1\\) (неявное дифференцирование)", listOf("y/x", "-x/y", "x/y", "-y/x"), 1),
            Question("Формула Лейбница для \\(n\\)-й производной произведения \\((uv)^{(n)} = ?\\)", listOf("u^{(n)}v + uv^{(n)}", "u^{(n)}v^{(n)}", "∑_{k=0}^{n} C_n^k u^{(k)}v^{(n-k)}", "nu^{(n-1)}v'"), 2),
            Question("Найдите производную \\(y = \\ln|\\cos x|\\)", listOf("sin x/cos x", "-tan x", "tan x", "1/cos x"), 1),
            Question("Логарифмическое дифференцирование применяется для...", listOf("Дробно-рациональных функций", "Степенно-показательных функций", "Тригонометрических функций", "Полиномов"), 1),
            Question("Производная n-го порядка от \\(\\sin x\\) равна...", listOf("sin(x+nπ/2)", "sin(x+nπ)", "n·sin x", "(-1)^n sin x"), 0)
        )
    }

    private fun getSeriesFallback(difficulty: Difficulty) = when (difficulty) {
        Difficulty.EASY -> listOf(
            Question("Геометрический ряд \\(\\sum r^n\\) сходится при...", listOf("r > 1", "r = 1", "r ≥ 0", "|r| < 1"), 3),
            Question("Сумма \\(\\sum_{n=0}^{\\infty} \\dfrac{1}{2^n} = ?\\)", listOf("1", "∞", "2", "1/2"), 2),
            Question("Если ряд сходится, то его общий член...", listOf("Стремится к ∞", "Постоянен", "Может не стремиться к 0", "Стремится к 0"), 3),
            Question("Ряд \\(\\sum \\dfrac{1}{n}\\) называется...", listOf("Геометрическим", "Обобщённо-гармоническим", "Гармоническим", "Знакоположительным"), 2),
            Question("Ряд \\(\\sum_{n=1}^{\\infty} \\dfrac{1}{n^p}\\) сходится при...", listOf("p < 1", "p > 1", "p = 1", "p ≥ 0"), 1),
            Question("Необходимое условие сходимости ряда \\(\\sum a_n\\):", listOf("a_n > 0", "a_n → 0", "S_n → ∞", "a_n = const"), 1),
            Question("Ряд \\(\\sum_{n=1}^{\\infty}(-1)^n\\) ...", listOf("Сходится", "Сходится к 0", "Расходится", "Сходится к 1"), 2),
            Question("Что такое частичная сумма \\(S_n\\)?", listOf("Сумма всего ряда", "Сумма чётных членов", "Сумма нечётных членов", "Сумма первых n членов"), 3),
            Question("Ряд \\(\\sum_{n=0}^{\\infty}\\left(\\frac{1}{3}\\right)^n\\) равен...", listOf("1/2", "3/2", "3", "2/3"), 1),
            Question("Сумма \\(\\sum_{n=1}^{\\infty}\\dfrac{1}{3^n} = ?\\)", listOf("1/2", "1/3", "1", "3"), 0)
        )
        Difficulty.MEDIUM -> listOf(
            Question("Признак Даламбера: ряд сходится если \\(\\lim\\dfrac{a_{n+1}}{a_n} = L\\) и...", listOf("L > 1", "L < 1", "L = 1", "L = 0"), 1),
            Question("Ряд \\(\\sum \\dfrac{(-1)^n}{n}\\) сходится...", listOf("Абсолютно", "Расходится", "К бесконечности", "Условно"), 3),
            Question("Радиус сходимости ряда \\(\\sum \\dfrac{x^n}{n!}\\) равен...", listOf("1", "0", "e", "∞"), 3),
            Question("Если \\(L < 1\\) в признаке Коши \\(L = \\limsup\\sqrt[n]{|a_n|}\\), то ряд...", listOf("Расходится", "Сходится абсолютно", "Сходится условно", "Неизвестно"), 1),
            Question("Ряд \\(\\sum \\dfrac{1}{n^2}\\) сходится к...", listOf("π²/6", "π²/12", "1", "π/4"), 0),
            Question("Признак Лейбница применим к...", listOf("Знакочередующимся рядам", "Знакоположительным", "Степенным", "Расходящимся"), 0),
            Question("Интервал сходимости ряда \\(\\sum x^n\\) это...", listOf("(-∞, +∞)", "(-1, 1)", "[0, 1]", "(-1, 1]"), 1),
            Question("Ряд Тейлора для \\(e^x\\) в точке 0 — это...", listOf("∑xⁿ/n!", "∑(-1)ⁿxⁿ/n!", "∑x²ⁿ/(2n)!", "∑xⁿ"), 0),
            Question("Ряд \\(\\sum \\dfrac{n^2}{2^n}\\) по признаку Даламбера...", listOf("Расходится (L>1)", "Сходится (L<1)", "Признак не работает (L=1)", "Знакочередующийся"), 1),
            Question("Остаток знакочередующегося ряда по признаку Лейбница: \\(|R_n|\\) ...", listOf("\\leq a_{n+1}", "> a_n", "= 0", "\\geq a_{n+1}"), 0)
        )
        Difficulty.HARD -> listOf(
            Question("Сумма \\(\\sum_{n=1}^{\\infty}\\dfrac{1}{n(n+1)} = ?\\)", listOf("1", "1/2", "ln 2", "∞"), 0),
            Question("Является ли ряд \\(\\sum \\dfrac{\\sin n}{n^2}\\) абсолютно сходящимся?", listOf("Да, |sin n|/n² ≤ 1/n²", "Нет, условно сходится", "Нет, расходится", "Зависит от начала"), 0),
            Question("Сумма \\(\\sum_{n=0}^{\\infty}\\dfrac{(-1)^n}{(2n)!} = ?\\)", listOf("cos 1", "sin 1", "e^{-1}", "cosh 1"), 0),
            Question("Ряд \\(\\sum_{n=2}^{\\infty}\\dfrac{1}{n\\ln n}\\) ...", listOf("Расходится", "Абсолютно сходится", "Условно сходится", "Знакочередующийся"), 0),
            Question("Ряд \\(\\sum_{n=1}^{\\infty}\\dfrac{(-1)^{n+1}}{n}\\) сходится к...", listOf("ln 2", "-ln 2", "1", "0"), 0),
            Question("Равномерно ли сходится \\(\\sum x^n\\) на (-1,1)?", listOf("Нет, только на [-r,r] при r<1", "Да, равномерно", "Нет, расходится", "Да, при всех x"), 0),
            Question("Сумма \\(\\sum_{n=1}^{\\infty} n x^{n-1}\\) при |x|<1 равна...", listOf("1/(1-x)²", "1/(1-x)", "x/(1-x)²", "1/(1+x)²"), 0),
            Question("Производящая функция ряда — это...", listOf("Частичная сумма", "Степенной ряд ∑aₙxⁿ", "Сумма ряда", "Характеристический многочлен"), 1),
            Question("Ряд \\(\\sum_{n=1}^{\\infty}\\dfrac{(-1)^n}{\\sqrt{n}}\\) является...", listOf("Абсолютно сходящимся", "Условно сходящимся", "Расходящимся", "Знакоположительным"), 1),
            Question("Радиус сходимости ряда \\(\\sum_{n=0}^{\\infty}\\dfrac{(2x)^n}{n^2}\\) равен...", listOf("1/2", "2", "1", "∞"), 0)
        )
    }

    private fun getOdeFallback(difficulty: Difficulty) = when (difficulty) {
        Difficulty.EASY -> listOf(
            Question("Уравнение \\(y' = ky\\) имеет общее решение...", listOf("y = Cx", "y = Ce^{kx}", "y = kx + C", "y = C/x"), 1),
            Question("Задача Коши: \\(y' = 2x, y(0)=1 \\Rightarrow y(2)=?\\)", listOf("5", "3", "4", "9"), 0),
            Question("ОДУ \\(y' = f(x)/g(y)\\) называется...", listOf("Линейным", "С разделяющимися переменными", "Бернулли", "Однородным"), 1),
            Question("Интегрирующий множитель для \\(y' + P(x)y = Q(x)\\) равен...", listOf("e^{∫P dx}", "e^{-∫P dx}", "∫P dx", "P(x)"), 0),
            Question("Задача Коши: \\(y' = y, y(0)=1 \\Rightarrow y(1)=?\\)", listOf("e", "1", "e²", "2"), 0),
            Question("Число произвольных постоянных в общем решении ОДУ n-го порядка:", listOf("n-1", "n", "n+1", "2n"), 1),
            Question("Задача Коши: \\(y' = -y, y(0)=2 \\Rightarrow y(1)=?\\)", listOf("e^{-1}", "2e^{-1}", "2e", "e^{-2}"), 1),
            Question("ОДУ \\(y'' + 4y = 0\\) имеет характеристические корни...", listOf("±2", "±2i", "0, 4", "-4, 0"), 1),
            Question("Общее решение \\(y'' - y = 0\\) есть...", listOf("C₁eˣ + C₂e^{-x}", "C₁cos x + C₂sin x", "C₁x + C₂", "e^{C₁x}"), 0),
            Question("Вронскиан двух функций = 0 означает что они...", listOf("Ортогональны", "Линейно зависимы", "Непрерывны", "Линейно независимы"), 1)
        )
        Difficulty.MEDIUM -> listOf(
            Question("Характеристическое уравнение для \\(y'' - 5y' + 6y = 0\\):", listOf("r² - 5r + 6 = 0", "r² + 5r + 6 = 0", "r - 5r + 6 = 0", "r² - 5 + 6r = 0"), 0),
            Question("Корни \\(r² - 5r + 6 = 0\\) равны...", listOf("r=1, r=6", "r=2, r=3", "r=-2, r=-3", "r=-1, r=-6"), 1),
            Question("Общее решение \\(y'' + y = 0\\) есть...", listOf("C₁eˣ + C₂e^{-x}", "C₁ + C₂x", "C₁cos x + C₂sin x", "C₁e^{ix}"), 2),
            Question("При комплексных корнях \\(r = α ± iβ\\) общее решение содержит...", listOf("e^{αx}(C₁cos βx + C₂sin βx)", "e^{αx} + e^{iβx}", "C₁e^{αx} + C₂e^{βx}", "cos βx + sin αx"), 0),
            Question("Частное решение \\(y'' + 4y = 8\\) равно...", listOf("y* = 2", "y* = 8", "y* = 4", "y* = 1"), 0),
            Question("Задача Коши: \\(y' + y = 1, y(0)=0 \\Rightarrow y(1)=?\\)", listOf("1-e^{-1}", "e-1", "1", "e^{-1}"), 0),
            Question("Уравнение \\(y'' + ω²y = 0\\) описывает...", listOf("Затухающие колебания", "Гармонические колебания", "Экспоненциальный рост", "Случайные колебания"), 1),
            Question("Однородное уравнение \\(y' = f(y/x)\\) решается заменой...", listOf("u = y + x", "u = y/x", "u = xy", "u = e^y"), 1),
            Question("Общее решение \\(y'' + 4y' + 4y = 0\\) (кратный корень r=-2):", listOf("C₁e^{-2x} + C₂e^{-2x}", "(C₁ + C₂x)e^{-2x}", "C₁e^{-4x} + C₂", "e^{-2x}(C₁cos 2x + C₂sin 2x)"), 1),
            Question("Условие существования решения задачи Коши: f и f'_y должны быть...", listOf("Монотонными", "Периодическими", "Непрерывными в некоторой области", "Аналитическими"), 2)
        )
        Difficulty.HARD -> listOf(
            Question("Задача Коши: \\(y'' + 4y = 0, y(0)=0, y'(0)=2 \\Rightarrow y(π/4)=?\\)", listOf("0", "1", "√2", "2"), 1),
            Question("Метод вариации постоянных для \\(y'' + p(x)y' + q(x)y = f(x)\\) даёт частное решение в виде...", listOf("y* = Ae^x", "y* = u₁y₁ + u₂y₂", "y* = C₁y₁", "y* = f(x)/q(x)"), 1),
            Question("Задача Коши: \\(y'' - 4y = 0, y(0)=1, y'(0)=2 \\Rightarrow y(1)=?\\)", listOf("e²", "e^{-2}", "cosh 2", "e²/2+e^{-2}/2"), 0),
            Question("Уравнение Бернулли \\(y' + P(x)y = Q(x)y^n\\) при \\(n≠0,1\\) решается заменой...", listOf("v = y^n", "v = y^{1-n}", "v = 1/y", "v = ln y"), 1),
            Question("Формула Абеля для вронскиана: \\(W(x) = W(x_0)e^{-\\int_{x_0}^x p\\,dt}\\) где \\(p(t)\\) — это...", listOf("Коэффициент при y''", "Коэффициент при y'", "Свободный член", "Коэффициент при y"), 1),
            Question("Задача Коши: \\(y' = y/x, y(1)=3 \\Rightarrow y(e)=?\\)", listOf("3", "3e", "e", "3e²"), 1),
            Question("Уравнение Эйлера \\(x²y'' + xy' + y = 0\\) решается заменой...", listOf("x = e^t", "y = xe^x", "x = ln t", "y = x^r"), 0),
            Question("Система ОДУ \\(x' = Ax\\) имеет решение \\(x = e^{At}x_0\\). При диагональной A это...", listOf("e^{A}x₀", "x₀ + At·x₀", "Диагональная матричная экспонента", "Невозможно вычислить"), 2),
            Question("Преобразование Лапласа \\(\\mathcal{L}\\{f'\\} = ?\\)", listOf("sF(s)", "sF(s) - f(0)", "F(s)/s", "sF(s) + f(0)"), 1),
            Question("Задача Коши: \\(y' = x^2, y(0)=0 \\Rightarrow y(3) = ?\\)", listOf("3", "6", "9", "27"), 2)
        )
    }

    private fun getProbabilityFallback(difficulty: Difficulty) = when (difficulty) {
        Difficulty.EASY -> listOf(
            Question("Вероятность события — это число в диапазоне...", listOf("[0, ∞)", "(-1, 1)", "[0, 1]", "(0, 1)"), 2),
            Question("Вероятность достоверного события равна...", listOf("0", "0.5", "1", "∞"), 2),
            Question("Вероятность невозможного события равна...", listOf("1", "0.5", "-1", "0"), 3),
            Question("Бросают монету. Вероятность орла равна...", listOf("1/4", "1/3", "1/2", "2/3"), 2),
            Question("Бросают кубик. Вероятность выпадения 6 равна...", listOf("1/3", "1/4", "1/2", "1/6"), 3),
            Question("Формула классической вероятности: P(A) = ?", listOf("n/N", "m/N", "N/m", "m·N"), 1),
            Question("Формула сложения P(A+B) для несовместных событий:", listOf("P(A)·P(B)", "P(A)+P(B)", "P(A)+P(B)−P(AB)", "1−P(A)"), 1),
            Question("Вероятность дополнения: P(Ā) = ?", listOf("P(A)+1", "1+P(A)", "1−P(A)", "P(A)−1"), 2),
            Question("Формула умножения для независимых событий P(AB) = ?", listOf("P(A)+P(B)", "P(A)·P(B)", "P(A|B)·P(A)", "P(B|A)+P(A)"), 1),
            Question("Условная вероятность P(A|B) = ?", listOf("P(A)·P(B)", "P(AB)/P(B)", "P(A)/P(B)", "P(A)+P(B)"), 1)
        )
        Difficulty.MEDIUM -> listOf(
            Question("Формула полной вероятности: P(A) = ?", listOf("∑P(A|Hᵢ)", "∑P(Hᵢ)P(A|Hᵢ)", "P(A∩B)/P(B)", "1 - P(Ā)"), 1),
            Question("Формула Байеса применяется для...", listOf("Нахождения суммы вероятностей", "Пересчёта вероятностей гипотез после наблюдения", "Умножения вероятностей", "Нахождения дополнения"), 1),
            Question("Математическое ожидание \\(M[X]\\) дискретной СВ:", listOf("∑xᵢ", "∑xᵢpᵢ", "∑pᵢ", "max xᵢ"), 1),
            Question("Дисперсия \\(D[X] = ?\\)", listOf("M[X]", "M[X²] − (M[X])²", "(M[X])²", "M[X²]"), 1),
            Question("Стандартное отклонение σ = ?", listOf("D[X]", "M[X]²", "√D[X]", "D[X]²"), 2),
            Question("Биномиальный закон: вероятность k успехов в n испытаниях:", listOf("Cₙᵏ·pᵏ·qⁿ⁻ᵏ", "n·p·q", "pᵏ/k!", "Cₙᵏ·p·q"), 0),
            Question("Нормальное распределение имеет форму...", listOf("Прямоугольника", "Треугольника", "Колоколообразной кривой", "Экспоненты"), 2),
            Question("При n→∞ биномиальное распределение стремится к...", listOf("Пуассоновскому при np=const", "Равномерному", "Геометрическому", "Гипергеометрическому"), 0),
            Question("Если X~N(0,1), то P(X∈(−1,1)) приближённо равно...", listOf("0.5", "0.683", "0.954", "0.997"), 1),
            Question("Корреляция Пирсона принимает значения в...", listOf("[0, 1]", "[0, ∞)", "[−1, 1]", "(−∞, ∞)"), 2)
        )
        Difficulty.HARD -> listOf(
            Question("Момент порядка k СВ X: \\(\\mu_k = ?\\)", listOf("M[X^k]", "M[(X−M[X])^k]", "k·M[X]", "M[X]^k"), 0),
            Question("Центральный момент 2-го порядка равен...", listOf("M[X]", "D[X]", "σ", "M[X²]"), 1),
            Question("Характеристическая функция СВ X: φ(t) = ?", listOf("M[e^{tX}]", "M[e^{itX}]", "M[e^{−tX}]", "E[e^X]"), 1),
            Question("Закон больших чисел Чебышёва утверждает, что...", listOf("Средняя сходится к M[X] по вероятности", "Дисперсия → 0", "Все СВ одинаковы", "Сумма → ∞"), 0),
            Question("ЦПТ: нормированная сумма n независимых СВ при n→∞...", listOf("→ N(0,1) по распределению", "→ 0", "→ M[X]", "→ Пуассону"), 0),
            Question("Неравенство Чебышёва: P(|X−M[X]| ≥ ε) ≤ ?", listOf("D[X]/ε", "D[X]/ε²", "σ/ε", "1/ε"), 1),
            Question("Оценка параметра θ называется несмещённой если...", listOf("θ̂ = θ всегда", "M[θ̂] = θ", "D[θ̂] → 0", "θ̂ → θ п.н."), 1),
            Question("Метод максимального правдоподобия максимизирует...", listOf("P(θ)", "L(θ|x) = ∏f(xᵢ|θ)", "−logP(θ)", "∑(xᵢ−θ)²"), 1),
            Question("Критерий Пирсона χ² применяется для проверки...", listOf("Нормальности", "Гипотезы о виде распределения", "Равенства средних", "Независимости событий"), 1),
            Question("p-значение — это вероятность...", listOf("Что H₀ верна", "Получить результат ≥ наблюдаемого при H₀", "Что H₁ верна", "Ошибки 1-го рода"), 1)
        )
    }

    // ── Daily challenge ────────────────────────────────────────────────────────

    /**
     * Returns the same 10 questions for ALL users on the same calendar day.
     *
     * Algorithm mirrors the web version (daily.js):
     *   1. Seed = webHashCode(today as "YYYY-MM-DD")
     *   2. Pool = all easy + medium choice questions from every subject
     *   3. Seeded Fisher-Yates shuffle → take first 10
     *   4. Each question's 4 options shuffled with the same continuing RNG
     */
    fun getDailyQuestions(): List<Question> {
        val dateStr = java.time.LocalDate.now().toString()
        val rng = mulberry32(webHashCode(dateStr))

        // Order must match web questions.js QUESTIONS object key order:
        // integrals → derivatives → series → limits → ode → probability → linalg
        val webSubjectOrder = listOf(
            Subject.INTEGRALS, Subject.DERIVATIVES, Subject.SERIES,
            Subject.LIMITS, Subject.ODE, Subject.PROBABILITY, Subject.LINALG
        )
        val all = mutableListOf<Question>()
        for (subject in webSubjectOrder) {
            all += getQuestions(subject, Difficulty.EASY).filter {
                it.type != "open" && it.options.size == 4
            }
            all += getQuestions(subject, Difficulty.MEDIUM).filter {
                it.type != "open" && it.options.size == 4
            }
        }

        // Seeded Fisher-Yates
        for (i in all.lastIndex downTo 1) {
            val j = (rng() * (i + 1)).toInt()
            val tmp = all[i]; all[i] = all[j]; all[j] = tmp
        }

        return all.take(10).map { q ->
            val order = (0..3).toMutableList()
            for (i in order.lastIndex downTo 1) {
                val j = (rng() * (i + 1)).toInt()
                order[i] = order[j].also { order[j] = order[i] }
            }
            q.copy(options = order.map { q.options[it] }, correct = order.indexOf(q.correct))
        }
    }

    /**
     * Seeded duel question generator — mirrors web's getDuelQuestions(code, section, difficulty).
     * Seed: webHashCode(code + '_duel_' + sectionLower + '_' + diffLower)
     * Both pool shuffle and per-question option shuffle use the SAME continuing RNG,
     * so Android and web produce identical question lists for the same duel code.
     */
    fun getDuelQuestions(code: String, subject: Subject, difficulty: Difficulty, count: Int = 10): List<Question> {
        val sectionStr = subject.name.lowercase()
        val diffStr    = difficulty.name.lowercase()
        val seed = webHashCode("${code}_duel_${sectionStr}_${diffStr}")
        val rng  = mulberry32(seed)

        val pool: MutableList<Question>
        if (subject == Subject.MIXED) {
            // Web's "mixed" algorithm: pick 2 questions from each section using the shared RNG,
            // then shuffle the 14 candidates, take first 10.
            val sectionOrder = listOf(
                Subject.INTEGRALS, Subject.DERIVATIVES, Subject.SERIES,
                Subject.LIMITS, Subject.ODE, Subject.PROBABILITY, Subject.LINALG
            )
            val selected = mutableListOf<Question>()
            for (subj in sectionOrder) {
                val sPool = getQuestions(subj, difficulty)
                    .filter { it.type != "open" && it.options.size == 4 }
                    .toMutableList()
                for (i in sPool.lastIndex downTo 1) {
                    val j = (rng() * (i + 1)).toInt()
                    val tmp = sPool[i]; sPool[i] = sPool[j]; sPool[j] = tmp
                }
                selected += sPool.take(2)
            }
            // Shuffle the 14 selected together
            for (i in selected.lastIndex downTo 1) {
                val j = (rng() * (i + 1)).toInt()
                val tmp = selected[i]; selected[i] = selected[j]; selected[j] = tmp
            }
            pool = selected
        } else {
            // Single-section: shuffle full pool, take first `count`
            val rawPool = getQuestions(subject, difficulty)
                .filter { it.type != "open" && it.options.size == 4 }
                .toMutableList()
            for (i in rawPool.lastIndex downTo 1) {
                val j = (rng() * (i + 1)).toInt()
                val tmp = rawPool[i]; rawPool[i] = rawPool[j]; rawPool[j] = tmp
            }
            pool = rawPool
        }

        // Shuffle each question's options (matches web: for i from 3 downTo 1)
        return pool.take(count).map { q ->
            val order = (0..3).toMutableList()
            for (i in order.lastIndex downTo 1) {
                val j = (rng() * (i + 1)).toInt()
                order[i] = order[j].also { order[j] = order[i] }
            }
            q.copy(options = order.map { q.options[it] }, correct = order.indexOf(q.correct))
        }
    }

    // ── Fallback questions ─────────────────────────────────────────────────────

    private fun getLinalgFallback(difficulty: Difficulty) = when (difficulty) {
        Difficulty.EASY -> listOf(
            Question("Определитель матрицы \\(\\begin{pmatrix}a&b\\\\c&d\\end{pmatrix}\\) равен...", listOf("a+d-b-c", "ad+bc", "ac-bd", "ad-bc"), 3),
            Question("Если \\(\\det(A) = 0\\), то матрица...", listOf("Единичная", "Вырожденная (необратимая)", "Симметричная", "Диагональная"), 1),
            Question("Найдите \\(\\det\\begin{pmatrix}3&1\\\\2&4\\end{pmatrix}\\)", listOf("14", "10", "5", "7"), 1),
            Question("Найдите \\(\\det\\begin{pmatrix}5&3\\\\2&4\\end{pmatrix}\\)", listOf("26", "14", "8", "22"), 1),
            Question("Матрица \\(A = \\begin{pmatrix}1&0\\\\0&1\\end{pmatrix}\\) называется...", listOf("Нулевой", "Единичной", "Диагональной", "Симметричной"), 1),
            Question("При транспонировании матрицы...", listOf("Элементы умножаются на -1", "Строки и столбцы меняются местами", "Обнуляется диагональ", "Матрица инвертируется"), 1),
            Question("Размер матрицы \\(A^T\\), если \\(A\\) имеет размер \\(2\\times3\\):", listOf("2×3", "3×3", "3×2", "2×2"), 2),
            Question("Матрица A симметрична если...", listOf("A = -A^T", "A = A^T", "A = A^{-1}", "A = E"), 1),
            Question("Вектор \\((3,4)\\) имеет длину...", listOf("5", "7", "12", "√7"), 0),
            Question("Скалярное произведение \\((1,2)\\cdot(3,4)\\) равно...", listOf("10", "11", "12", "14"), 1)
        )
        Difficulty.MEDIUM -> listOf(
            Question("Собственный вектор v матрицы A удовлетворяет...", listOf("Av = 0", "Av = Ev", "Av = λv", "Av = λA"), 2),
            Question("Характеристическое уравнение матрицы A:", listOf("det(A) = 0", "det(A - λE) = 0", "det(A + λE) = 0", "tr(A) = λ"), 1),
            Question("Ранг матрицы — это...", listOf("Число строк", "Порядок матрицы", "Максимальный порядок ненулевого минора", "Определитель"), 2),
            Question("Матрица обратима тогда и только тогда, когда...", listOf("Она квадратная", "det(A) ≠ 0", "det(A) = 1", "Она симметричная"), 1),
            Question("Правило Крамера применяется для решения...", listOf("Интегралов", "Систем линейных уравнений", "Дифференциальных уравнений", "Неравенств"), 1),
            Question("Теорема Гамильтона-Кэли: матрица A удовлетворяет...", listOf("A = E", "Своему характеристическому уравнению", "det(A) = 0", "A² = A"), 1),
            Question("Матрицы A и B перестановочны (коммутируют) если...", listOf("AB = 0", "AB = BA", "A = B^T", "AB = E"), 1),
            Question("След матрицы (trace) — это...", listOf("Определитель", "Сумма элементов главной диагонали", "Произведение диагональных элементов", "Сумма всех элементов"), 1),
            Question("Норма вектора \\(\\mathbf{v} = (v_1,\\ldots,v_n)\\) равна...", listOf("∑vᵢ", "max|vᵢ|", "√(∑vᵢ²)", "∑vᵢ²"), 2),
            Question("Два вектора ортогональны если их скалярное произведение равно...", listOf("1", "-1", "0", "|a||b|"), 2)
        )
        Difficulty.HARD -> listOf(
            Question("SVD разложение матрицы A: \\(A = U\\Sigma V^T\\). Что такое \\(\\Sigma\\)?", listOf("Квадратная матрица", "Матрица из собственных векторов", "Диагональная матрица сингулярных значений", "Обратная матрица"), 2),
            Question("LU-разложение применяется для...", listOf("Нахождения собственных значений", "Эффективного решения систем Ax=b", "Вычисления интегралов", "Нахождения ранга"), 1),
            Question("Метод Гаусса сводит матрицу к...", listOf("Нулевой", "Единичной", "Ступенчатому виду", "Диагональной"), 2),
            Question("Пространство ядра матрицы A: Ker(A) = {x: Ax = ?}", listOf("Ax = E", "Ax = b", "Ax = 0", "Ax = λx"), 2),
            Question("Ортогональная матрица Q удовлетворяет...", listOf("Q² = E", "Q^T = Q^{-1}", "det(Q) = 0", "QQ = 0"), 1),
            Question("Число обусловленности матрицы cond(A) показывает...", listOf("Ранг матрицы", "Чувствительность решения к погрешностям данных", "Число итераций", "det(A)"), 1),
            Question("Метод итерации по степени матрицы сходится при...", listOf("det(A) > 1", "Спектральном радиусе ρ(A) < 1", "tr(A) > 0", "Симметричности A"), 1),
            Question("QR-разложение: A = QR, где R — это...", listOf("Симметричная матрица", "Нижнетреугольная матрица", "Верхнетреугольная матрица", "Диагональная матрица"), 2),
            Question("Положительно определённая матрица A: для всех x≠0...", listOf("x^T A x = 0", "x^T A x < 0", "x^T A x > 0", "Ax = 0"), 2),
            Question("Теорема Перрона-Фробениуса применяется к...", listOf("Антисимметричным матрицам", "Матрицам с неотрицательными элементами", "Вырожденным матрицам", "Комплексным матрицам"), 1)
        )
    }
}
