package com.mathcore.app

import com.mathcore.app.util.computeXp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [computeXp] (XpUtils.kt).
 *
 * Formula:  correctAnswers × mult  +  (if score == 100 then 25 else 0)
 *   easy   → mult = 10
 *   medium → mult = 20
 *   hard   → mult = 30
 *   else   → mult = 20 (safe fallback)
 *
 * Run with: ./gradlew :app:test --tests "com.mathcore.app.ComputeXpTest"
 */
class ComputeXpTest {

    // ── easy (×10) ──────────────────────────────────────────────────────────────
    @Test fun easy_no_bonus()       = assertEquals(50,  computeXp(5,  "easy",   70))
    @Test fun easy_perfect_bonus()  = assertEquals(75,  computeXp(5,  "easy",  100))
    @Test fun easy_zero_correct()   = assertEquals(0,   computeXp(0,  "easy",    0))

    // ── medium (×20) ────────────────────────────────────────────────────────────
    @Test fun medium_no_bonus()     = assertEquals(200, computeXp(10, "medium",  80))
    @Test fun medium_perfect()      = assertEquals(225, computeXp(10, "medium", 100))
    @Test fun medium_one_correct()  = assertEquals(20,  computeXp(1,  "medium",  10))

    // ── hard (×30) ──────────────────────────────────────────────────────────────
    @Test fun hard_no_bonus()       = assertEquals(210, computeXp(7,  "hard",    70))
    @Test fun hard_perfect()        = assertEquals(325, computeXp(10, "hard",   100))

    // ── perfect-score bonus only (25) ───────────────────────────────────────────
    @Test fun bonus_is_exactly_25() = assertEquals(25,  computeXp(0,  "easy",  100))

    // ── case-insensitivity ───────────────────────────────────────────────────────
    @Test fun uppercase_easy()      = assertEquals(75,  computeXp(5,  "EASY",  100))
    @Test fun uppercase_medium()    = assertEquals(225, computeXp(10, "MEDIUM",100))
    @Test fun uppercase_hard()      = assertEquals(325, computeXp(10, "HARD",  100))
    @Test fun mixed_case()          = assertEquals(225, computeXp(10, "Medium",100))

    // ── unknown difficulty falls back to medium ──────────────────────────────────
    @Test fun unknown_fallback_medium()  = assertEquals(100, computeXp(5,  "expert",  50))
    @Test fun empty_fallback_medium()    = assertEquals(225, computeXp(10, "",        100))

    // ── score boundary: 99 vs 100 ───────────────────────────────────────────────
    @Test fun score_99_no_bonus()   = assertEquals(200, computeXp(10, "medium",  99))
    @Test fun score_100_bonus()     = assertEquals(225, computeXp(10, "medium", 100))
}
