package com.mathcore.app.data

/**
 * Deterministic seeding utilities — shared between [QuestionRepository] (production)
 * and unit tests in `app/src/test/`.
 *
 * Both functions are `internal` (visible within the Gradle module, including the
 * `test` source set) without leaking into the public API.
 *
 * Cross-platform equivalence with JavaScript (js/utils.js):
 *   - [webHashCode] mirrors `hashCode(str) >>> 0`  (unsigned 32-bit result returned
 *     as signed Kotlin Int — the 32-bit bit-pattern is identical)
 *   - [mulberry32]  mirrors `mulberry32(seed)` generator bit-for-bit
 *
 * The equivalence is verified in `SeedUtilsTest.kt` with cross-platform golden values.
 */

/**
 * Matches JavaScript's `hashCode(str)` from utils.js.
 *
 * Algorithm: polynomial rolling hash with base 31, standard Java/JS variant.
 * Kotlin `Int` arithmetic wraps at 32 bits (JVM semantics), mirroring
 * `Math.imul(31, h) + charCode | 0` in JavaScript.
 *
 * Note: returns a *signed* 32-bit Int, whereas JS returns unsigned (`>>> 0`).
 * The bit-pattern is identical; only the sign interpretation differs.
 * Always pass to [mulberry32] directly — it handles both interpretations correctly.
 */
internal fun webHashCode(str: String): Int {
    var h = 0
    for (c in str) h = 31 * h + c.code   // Int wraps at 2^32 like JS `| 0`
    return h
}

/**
 * Matches JavaScript's `mulberry32(seed)` generator from utils.js, bit-for-bit.
 *
 * Returns a lambda that advances internal state and produces a pseudo-random
 * Double in [0.0, 1.0) on each call. The sequence is fully deterministic:
 * same [initialSeed] → same sequence, guaranteed on both Android and web.
 *
 * Used by [QuestionRepository.getDailyQuestions] and [getDuelQuestions] to
 * ensure Android and web show identical question order.
 */
internal fun mulberry32(initialSeed: Int): () -> Double {
    var seed = initialSeed
    return {
        seed += 0x6D2B79F5.toInt()                        // step state
        var t = (seed xor (seed ushr 15)) * (1 or seed)   // avalanche 1
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t  // avalanche 2
        t = t xor (t ushr 14)                              // final mixing
        t.toUInt().toLong().toDouble() / 4294967296.0      // → [0, 1)
    }
}
