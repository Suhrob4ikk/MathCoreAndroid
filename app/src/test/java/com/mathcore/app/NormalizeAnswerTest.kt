package com.mathcore.app

import com.mathcore.app.data.normalizeAnswer
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [normalizeAnswer] (Question.kt).
 *
 * The function mirrors web test.js `normalizeAnswer()` exactly — every test case
 * here has an equivalent assertion in `test/utils.test.mjs` for the web build.
 *
 * Run with: ./gradlew :app:test --tests "com.mathcore.app.NormalizeAnswerTest"
 */
class NormalizeAnswerTest {

    // ── Integers ────────────────────────────────────────────────────────────────
    @Test fun integer_string_unchanged()     = assertEquals("10",   normalizeAnswer("10"))
    @Test fun zero_unchanged()               = assertEquals("0",    normalizeAnswer("0"))
    @Test fun large_integer_unchanged()      = assertEquals("1000", normalizeAnswer("1000"))

    // ── Decimal trimming ────────────────────────────────────────────────────────
    @Test fun trailing_zero_removed()        = assertEquals("1",    normalizeAnswer("1.0"))
    @Test fun zero_point_zero_collapses()    = assertEquals("0",    normalizeAnswer("0.0"))
    @Test fun multiple_trailing_zeros()      = assertEquals("2.5",  normalizeAnswer(" 2.500 "))
    @Test fun significant_decimals_kept()    = assertEquals("3.14", normalizeAnswer("3.140"))
    @Test fun no_trailing_zeros_unchanged()  = assertEquals("1.5",  normalizeAnswer("1.5"))

    // ── Comma decimal separator ─────────────────────────────────────────────────
    @Test fun comma_to_dot()                 = assertEquals("1.5",  normalizeAnswer("1,5"))
    @Test fun comma_zero_collapses()         = assertEquals("1",    normalizeAnswer("1,0"))

    // ── Whitespace ──────────────────────────────────────────────────────────────
    @Test fun leading_trailing_whitespace()  = assertEquals("42",   normalizeAnswer("  42  "))
    @Test fun internal_whitespace_removed()  = assertEquals("sinx", normalizeAnswer("sin x"))
    @Test fun uppercase_lowercased()         = assertEquals("sinx", normalizeAnswer("SIN X"))
    @Test fun mixed_case_and_space()         = assertEquals("lnx",  normalizeAnswer("Ln X"))

    // ── Edge cases ──────────────────────────────────────────────────────────────
    @Test fun empty_string()                 = assertEquals("",     normalizeAnswer(""))
    @Test fun pi_symbol_passes_through()     = assertEquals("π",    normalizeAnswer("π"))
    @Test fun only_dot_edge()                = assertEquals("",     normalizeAnswer("."))
    @Test fun dot_zero()                     = assertEquals("",     normalizeAnswer(".0"))
}
