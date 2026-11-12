package com.upisoundbox.core.tts

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.parser.NumberToWords
import com.upisoundbox.core.parser.PaymentEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

class AnnouncementEngine(
    private val context: Context,
    private val chimePlayer: AudioChimePlayer = AudioChimePlayer()
) : TextToSpeech.OnInitListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val initDeferred = CompletableDeferred<Boolean>()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            initDeferred.complete(true)
        } else {
            isTtsReady = false
            initDeferred.complete(false)
        }
    }

    suspend fun isVoiceInstalled(languageTag: String): Boolean {
        initDeferred.await()
        val locale = Locale.forLanguageTag(languageTag)
        val availability = tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        return availability >= TextToSpeech.LANG_AVAILABLE
    }

    fun getVoiceInstallIntent(): Intent {
        return Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun renderPhrase(
        template: String,
        amountPaise: Long,
        payer: String?,
        appPackage: String,
        languageTag: String
    ): String {
        // Resolve words or formatted amount
        val words = NumberToWords.convertPaise(amountPaise, languageTag)
        val amountSpoken = if (words.isNotBlank()) words else AmountParser.formatPaise(amountPaise)

        val friendlyApp = mapPackageToDisplayName(appPackage)
        val friendlyPayer = payer ?: getFallbackPayerForLanguage(languageTag)

        return template
            .replace("{amount}", amountSpoken)
            .replace("{payer}", friendlyPayer)
            .replace("{app}", friendlyApp)
    }

    suspend fun speak(
        text: String,
        languageTag: String,
        useAlarmStream: Boolean = false,
        volumeBoost: Boolean = true,
        playChime: Boolean = true,
        waitCalls: Boolean = true
    ): Boolean = withContext(Dispatchers.Main) {
        try {
            initDeferred.await()
            if (!isTtsReady || tts == null) return@withContext false

            // Wait if phone call is active
            if (waitCalls) {
                var callWaitCount = 0
                while (isPhoneCallActive() && callWaitCount < 30) {
                    delay(1000)
                    callWaitCount++
                }
            }

            // Configure language with fallback to English
            val targetLocale = Locale.forLanguageTag(languageTag)
            val availability = try {
                tts?.isLanguageAvailable(targetLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            } catch (_: Exception) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }
            if (availability >= TextToSpeech.LANG_AVAILABLE) {
                try { tts?.language = targetLocale } catch (_: Exception) {}
            } else {
                try { tts?.language = Locale.ENGLISH } catch (_: Exception) {}
            }

            // Play chime first if enabled
            if (playChime) {
                try { chimePlayer.playChime(useAlarmStream) } catch (_: Exception) {}
            }

            // Request Audio Focus with ducking
            val streamType = if (useAlarmStream) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
            val audioUsage = if (useAlarmStream) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
            val focusRequest = buildAudioFocusRequest(audioUsage)

            val focusGranted = try { requestAudioFocus(focusRequest, streamType) } catch (_: Exception) { false }

            // Volume boost safely
            val previousVolume = try { audioManager.getStreamVolume(streamType) } catch (_: Exception) { 7 }
            val maxVolume = try { audioManager.getStreamMaxVolume(streamType) } catch (_: Exception) { 15 }
            if (volumeBoost) {
                try {
                    audioManager.setStreamVolume(streamType, maxVolume, 0)
                } catch (_: Exception) {}
            }

            val speechCompletion = CompletableDeferred<Boolean>()
            val utteranceId = "upi_soundbox_${System.currentTimeMillis()}"

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    speechCompletion.complete(true)
                }
                override fun onError(utteranceId: String?) {
                    speechCompletion.complete(false)
                }
            })

            // Audio Attributes for TTS
            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, streamType)
            }

            val speakResult = try {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            } catch (e: Exception) {
                TextToSpeech.ERROR
            }

            if (speakResult != TextToSpeech.SUCCESS) {
                speechCompletion.complete(false)
            }

            // Await completion or timeout after 15 seconds
            val success = try {
                kotlinx.coroutines.withTimeout(15000L) {
                    speechCompletion.await()
                }
            } catch (_: Exception) {
                false
            }

            // Restore volume and abandon audio focus
            if (volumeBoost) {
                try {
                    audioManager.setStreamVolume(streamType, previousVolume, 0)
                } catch (_: Exception) {}
            }
            try { abandonAudioFocus(focusRequest, streamType) } catch (_: Exception) {}

            success
        } catch (e: Exception) {
            false
        }
    }

    private fun isPhoneCallActive(): Boolean {
        return audioManager.mode == AudioManager.MODE_IN_CALL ||
                audioManager.mode == AudioManager.MODE_IN_COMMUNICATION
    }

    private fun buildAudioFocusRequest(audioUsage: Int): AudioFocusRequest? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(audioUsage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .build()
        } else {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun requestAudioFocus(focusRequest: AudioFocusRequest?, streamType: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            audioManager.requestAudioFocus(null, streamType, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    @Suppress("DEPRECATION")
    private fun abandonAudioFocus(focusRequest: AudioFocusRequest?, streamType: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest)
        } else {
            audioManager.abandonAudioFocus(null)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        fun mapPackageToDisplayName(pkg: String): String {
            return when (pkg) {
                "com.google.android.apps.nbu.paisa.user" -> "Google Pay"
                "com.google.android.apps.nbu.paisa.merchant" -> "Google Pay Business"
                "com.phonepe.app", "com.phonepe.business", "com.phonepe.app.business" -> "PhonePe"
                "net.one97.paytm", "net.one97.paytm.merchant", "com.paytm.business" -> "Paytm"
                "in.org.npci.upiapp" -> "BHIM"
                "in.amazon.mShop.android.shopping" -> "Amazon Pay"
                "com.dreamplug.androidapp" -> "CRED"
                else -> if (pkg.startsWith("VK-") || pkg.startsWith("AX-") || pkg.startsWith("AD-")) "Bank SMS" else "UPI"
            }
        }

        private fun getFallbackPayerForLanguage(lang: String): String {
            return when (lang.lowercase()) {
                "hi-in", "hi" -> "ग्राहक"
                "ml-in", "ml" -> "കസ്റ്റമർ"
                "ta-in", "ta" -> "வாடிக்கையாளர்"
                "te-in", "te" -> "కస్టమర్"
                "kn-in", "kn" -> "ಗ್ರಾಹಕ"
                else -> "customer"
            }
        }
    }
}

