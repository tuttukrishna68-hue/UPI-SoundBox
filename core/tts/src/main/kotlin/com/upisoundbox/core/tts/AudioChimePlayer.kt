package com.upisoundbox.core.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

/**
 * Pure Kotlin/Android AudioTrack chime generator.
 * Produces a crisp, pleasant high-fidelity dual-tone chime (F5 + A5 chord)
 * without needing external asset files, zero disk I/O, zero latency.
 */
class AudioChimePlayer {

    suspend fun playChime(useAlarmStream: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val sampleRate = 44100
            val durationMs = 280
            val numSamples = (sampleRate * durationMs) / 1000
            val buffer = ShortArray(numSamples)

            val freq1 = 698.46 // F5
            val freq2 = 880.00 // A5

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Smooth exponential decay envelope
                val envelope = kotlin.math.exp(-8.0 * t / (durationMs / 1000.0))
                val wave = 0.5 * sin(2.0 * Math.PI * freq1 * t) + 0.5 * sin(2.0 * Math.PI * freq2 * t)
                buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.75).toInt().toShort()
            }

            val usage = if (useAlarmStream) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
            val contentType = AudioAttributes.CONTENT_TYPE_SONIFICATION

            val minBuffer = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val bufferSize = if (minBuffer > 0) kotlin.math.max(buffer.size * 2, minBuffer) else buffer.size * 2

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(contentType)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            try {
                if (audioTrack.state != AudioTrack.STATE_INITIALIZED) return@withContext false

                val written = audioTrack.write(buffer, 0, buffer.size)
                if (written != buffer.size) return@withContext false

                audioTrack.play()
                val started = audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING
                kotlinx.coroutines.delay(durationMs.toLong() + 50)
                return@withContext started
            } finally {
                try {
                    if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack.stop()
                    }
                } catch (_: Exception) {}
                audioTrack.release()
            }
        } catch (_: Exception) {
            // Ignore if audio device is unavailable
            return@withContext false
        }
    }
}


