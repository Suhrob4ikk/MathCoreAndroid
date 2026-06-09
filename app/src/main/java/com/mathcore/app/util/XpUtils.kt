package com.mathcore.app.util

/**
 * Single source of truth for XP calculation.
 *
 * All five previous call-sites used slightly different formulas:
 *   – ProfileScreen / MainActivity sync: "hard"→30, else→15
 *   – ResultsRepository / StatsScreen:   else→30
 *   – MainActivity save:                 uppercase "EASY"/"MEDIUM", else→30
 *
 * Now every site calls this function, so the numbers are always identical.
 *
 * Formula (matches the XP guide card displayed in StatsScreen):
 *   correct answers × (10 / 20 / 30) for easy / medium / hard
 *   +25 bonus for a perfect score (100%)
 */
fun computeXp(correctAnswers: Int, difficulty: String, score: Int): Int {
    val mult = when (difficulty.lowercase().trim()) {
        "easy"   -> 10
        "medium" -> 20
        "hard"   -> 30
        else     -> 20   // safe fallback: treat unknown as medium
    }
    return correctAnswers * mult + if (score == 100) 25 else 0
}
