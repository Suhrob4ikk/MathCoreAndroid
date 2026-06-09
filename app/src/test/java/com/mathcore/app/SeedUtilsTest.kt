package com.mathcore.app

import com.mathcore.app.data.mulberry32
import com.mathcore.app.data.webHashCode
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [webHashCode] and [mulberry32] (SeedUtils.kt).
 *
 * Cross-platform guarantee: every assertion labelled "cross-platform" has a
 * matching assertion in the web test `test/utils.test.mjs`.  Running both test
 * suites on the same inputs must produce identical numbers; otherwise the daily
 * challenge or duel question order will diverge between Android and web.
 *
 * Run with: ./gradlew :app:test --tests "com.mathcore.app.SeedUtilsTest"
 */
class SeedUtilsTest {

    // ══════════════════════════════════════════════════════════════════════════════
    // webHashCode
    // ══════════════════════════════════════════════════════════════════════════════

    @Test fun hash_empty_string_is_zero() =
        assertEquals(0, webHashCode(""))

    @Test fun hash_single_char_a() =
        // 'a' = ASCII 97 → h = 31×0 + 97 = 97  (no overflow)
        assertEquals(97, webHashCode("a"))

    @Test fun hash_two_chars_ab() =
        // h = 31×97 + 98 = 3105  (no overflow)
        assertEquals(3105, webHashCode("ab"))

    /**
     * Cross-platform golden value.
     *
     * Verified manually (5-char ASCII, no 32-bit overflow):
     *   h(h) = 104
     *   h(e) = 31×104 + 101 = 3325
     *   h(l) = 31×3325 + 108 = 103183
     *   h(l) = 31×103183 + 108 = 3198781
     *   h(o) = 31×3198781 + 111 = 99162322
     *
     * JS equivalent: `hashCode("hello") >>> 0 === 99162322`
     * (see test/utils.test.mjs for the matching web assertion)
     */
    @Test fun hash_hello_cross_platform_golden() =
        assertEquals(99162322, webHashCode("hello"))

    @Test fun hash_same_input_is_deterministic() {
        val a = webHashCode("calculus")
        val b = webHashCode("calculus")
        assertEquals(a, b)
    }

    @Test fun hash_different_inputs_differ() {
        assertNotEquals(webHashCode("abc"), webHashCode("abd"))
        assertNotEquals(webHashCode("daily"), webHashCode("duel"))
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // mulberry32 — range and determinism
    // ══════════════════════════════════════════════════════════════════════════════

    @Test fun prng_outputs_in_unit_interval() {
        val rng = mulberry32(42)
        repeat(200) { i ->
            val v = rng()
            assertTrue("Expected [0,1) at step $i but got $v", v >= 0.0 && v < 1.0)
        }
    }

    @Test fun prng_same_seed_same_sequence() {
        val a = mulberry32(12345)
        val b = mulberry32(12345)
        val samplesA = DoubleArray(20) { a() }
        val samplesB = DoubleArray(20) { b() }
        assertArrayEquals("Same seed must produce identical sequence", samplesA, samplesB, 0.0)
    }

    @Test fun prng_different_seeds_differ() {
        // It is astronomically unlikely these are equal (and would be a bug)
        assertNotEquals("Seed 1 and 2 must produce different first values",
            mulberry32(1)(), mulberry32(2)(), 0.0)
    }

    @Test fun prng_successive_calls_differ() {
        val rng = mulberry32(99)
        assertNotEquals("Successive calls must differ", rng(), rng(), 0.0)
    }

    @Test fun prng_floor_indices_stay_in_range() {
        val rng = mulberry32(777)
        repeat(500) { i ->
            val idx = (rng() * 10).toInt()
            assertTrue("Index out of [0,9] at step $i: $idx", idx in 0..9)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Cross-platform integration
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Integration smoke-test: seeds the PRNG from the "hello" hash (cross-platform
     * golden = 99162322) and checks that 100 shuffle indices are valid.
     *
     * The matching web assertion in `test/utils.test.mjs` seeds mulberry32 with the
     * same value and checks the same invariant, ensuring Fisher-Yates shuffles on
     * both platforms select from the same index distribution.
     */
    @Test fun integration_prng_seeded_from_hello_hash() {
        val seed = webHashCode("hello")
        assertEquals("Hash guard failed — cross-platform golden broken", 99162322, seed)

        val rng = mulberry32(seed)
        repeat(100) { i ->
            val idx = (rng() * 10).toInt()
            assertTrue("Fisher-Yates index out of range at step $i: $idx", idx in 0..9)
        }
    }

    /**
     * Verifies that signed/unsigned overflow seeds work correctly.
     *
     * webHashCode() returns a signed Int; JS hashCode() returns unsigned (>>> 0).
     * Both platforms see the same 32-bit bit-pattern inside mulberry32 after
     * its first `seed |= 0` / `seed += ...` step, so overflow is transparent.
     *
     * "2026-06" (7 chars) produces a negative hash (verified: h = -1_447_427_407
     * after the overflowing multiplication at position 7).  mulberry32 must still
     * produce a deterministic sequence from this negative seed.
     */
    @Test fun integration_overflow_seed_determinism() {
        // 7-char prefix that is verified to overflow to a negative hash
        val seed = webHashCode("2026-06")
        assertTrue("Expected negative (overflow) hash for '2026-06', got $seed", seed < 0)

        val rng1 = mulberry32(seed)
        val rng2 = mulberry32(seed)
        val seq1 = DoubleArray(10) { rng1() }
        val seq2 = DoubleArray(10) { rng2() }
        assertArrayEquals("Overflow seed must still yield a deterministic sequence", seq1, seq2, 0.0)
    }
}
