package com.upisoundbox.core.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Plays the merchant-provided professional chime before speech. */
class AudioChimePlayer(private val context: Context) {

    suspend fun playChime(useAlarmStream: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        var player: MediaPlayer? = null
        try {
            player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(if (useAlarmStream) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            context.resources.openRawResourceFd(R.raw.tune).use { descriptor ->
                player.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            }
            player.prepare()
            player.start()

            // Waiting for the real duration keeps the tune and announcement
            // in the intended order without guessing a fixed chime length.
            val durationMs = player.duration.coerceAtLeast(0).toLong()
            kotlinx.coroutines.delay(durationMs + 50L)
            true
        } catch (_: Exception) {
            false
        } finally {
            try {
                player?.stop()
            } catch (_: Exception) {}
            try {
                player?.release()
            } catch (_: Exception) {}
        }
    }
}

