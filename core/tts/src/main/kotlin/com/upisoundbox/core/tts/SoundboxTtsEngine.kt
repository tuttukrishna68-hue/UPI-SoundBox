package com.upisoundbox.core.tts

/**
 * Placeholder TTS engine interface.
 */
interface SoundboxTtsEngine {
    suspend fun speak(text: String)
    fun stop()
}
