package com.mathcore.app.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates and plays short PCM tones for quiz feedback.
 *
 * Each play call dispatches to [Dispatchers.Default] via a fire-and-forget
 * coroutine so the main thread is never blocked.  Raw [Thread] was replaced
 * to avoid untracked thread accumulation if the user answers rapidly.
 *
 * [AudioTrack.Builder] is used instead of the deprecated 7-arg constructor
 * (deprecated since API 21).
 */
object SoundManager {

    private val scope = CoroutineScope(Dispatchers.Default)

    fun playCorrect() {
        // Two-note ding: C5 (523 Hz) → E5 (659 Hz)
        scope.launch {
            playToneBlocking(523, 180)
            playToneBlocking(659, 200)
        }
    }

    fun playWrong() {
        // Low buzz: 200 Hz
        scope.launch { playToneBlocking(200, 280, 0.3) }
    }

    fun playPerfect() {
        // Ascending arpeggio C5-E5-G5-C6
        scope.launch {
            listOf(523, 659, 784, 1047).forEach { freq ->
                playToneBlocking(freq, 140)
            }
        }
    }

    fun playFinish() {
        scope.launch {
            playToneBlocking(440, 220)
            playToneBlocking(554, 280)
        }
    }

    private fun playToneBlocking(frequency: Int, durationMs: Int, volume: Double = 0.35) {
        try {
            val sampleRate = 44100
            val numSamples = sampleRate * durationMs / 1000
            val audioData = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val angle = 2.0 * PI * frequency * i / sampleRate
                val fadeStart = numSamples * 0.75
                val env = if (i >= fadeStart)
                    1.0 - (i - fadeStart) / (numSamples - fadeStart)
                else 1.0
                audioData[i] = (sin(angle) * 32767 * volume * env).toInt().toShort()
            }
            // AudioTrack.Builder replaces the deprecated 7-arg constructor (deprecated API 21)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(numSamples * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(audioData, 0, numSamples)
            track.play()
            Thread.sleep(durationMs.toLong() + 50)
            track.stop()
            track.release()
        } catch (_: Exception) {}
    }
}
