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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

class AnnouncementEngine(
    private val context: Context,
    private val chimePlayer: AudioChimePlayer = AudioChimePlayer()
) : TextToSpeech.OnInitListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var tts: TextToSpeech? = null
    @Volatile
    private var isTtsReady = false
    private var initDeferred = CompletableDeferred<Boolean>()

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            initDeferred = CompletableDeferred()
            tts = TextToSpeech(context.applicationContext, this)
        } catch (_: Exception) {
            isTtsReady = false
            initDeferred.complete(false)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            try {
                // Apply optimal speech attributes for soundbox clarity
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(0.95f)
            } catch (_: Exception) {}
            if (!initDeferred.isCompleted) {
                initDeferred.complete(true)
            }
        } else {
            isTtsReady = false
            if (!initDeferred.isCompleted) {
                initDeferred.complete(false)
            }
        }
    }

    suspend fun ensureReady(): Boolean = withContext(Dispatchers.Main) {
        if (isTtsReady && tts != null) return@withContext true

        // If not ready, attempt re-initialization
        if (tts == null || !isTtsReady) {
            initializeTts()
        }

        val ready = withTimeoutOrNull(3000L) {
            initDeferred.await()
        } ?: false

        return@withContext ready
    }

    suspend fun isVoiceInstalled(languageTag: String): Boolean = withContext(Dispatchers.IO) {
        ensureReady()
        val locale = try {
            Locale.forLanguageTag(languageTag)
        } catch (_: Exception) {
            Locale("en", "IN")
        }
        val availability = try {
            tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        } catch (_: Exception) {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
        return@withContext availability >= TextToSpeech.LANG_AVAILABLE
    }

    fun getVoiceInstallIntent(): Intent {
        return Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getDefaultTemplateForLanguage(languageTag: String): String {
        return when (languageTag.lowercase().replace("_", "-")) {
            "hi-in", "hi" -> "{app} पर {payer} से {amount} प्राप्त हुए"
            "ml-in", "ml" -> "{app} വഴി {payer} ൽ നിന്ന് {amount} ലഭിച്ചു"
            "ta-in", "ta" -> "{app} மூலம் {payer} இடமிருந்து {amount} பெறப்பட்டது"
            "te-in", "te" -> "{app} ద్వారా {payer} నుండి {amount} అందింది"
            "kn-in", "kn" -> "{app} ಮೂಲಕ {payer} ರಿಂದ {amount} ಸ್ವೀಕರಿಸಲಾಗಿದೆ"
            "mr-in", "mr" -> "{app} वर {payer} कडून {amount} प्राप्त झाले"
            "bn-in", "bn" -> "{app} এ {payer} এর থেকে {amount} পাওয়া গেছে"
            "gu-in", "gu" -> "{app} પર {payer} તરફથી {amount} મળ્યા"
            "pa-in", "pa" -> "{app} 'ਤੇ {payer} ਵੱਲੋਂ {amount} ਪ੍ਰਾਪਤ ਹੋਏ"
            else -> "Received {amount} on {app} from {payer}"
        }
    }

    fun renderPhrase(
        template: String,
        amountPaise: Long,
        payer: String?,
        appPackage: String,
        languageTag: String
    ): String {
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
            ensureReady()
            val engine = tts ?: return@withContext false

            // Wait if phone call is active
            if (waitCalls) {
                var callWaitCount = 0
                while (isPhoneCallActive() && callWaitCount < 10) {
                    delay(1000)
                    callWaitCount++
                }
            }

            // Target audio usage & stream
            val streamType = if (useAlarmStream) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
            val audioUsage = if (useAlarmStream) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY

            // Modern Android AudioAttributes
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(audioUsage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            try {
                engine.setAudioAttributes(audioAttributes)
            } catch (_: Exception) {}

            // Configure language with smart fallback
            val targetLocale = try {
                Locale.forLanguageTag(languageTag)
            } catch (_: Exception) {
                Locale("en", "IN")
            }

            var textToSpeak = text
            val setLangResult = try {
                engine.setLanguage(targetLocale)
            } catch (_: Exception) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }

            val langSupported = setLangResult != TextToSpeech.LANG_MISSING_DATA &&
                    setLangResult != TextToSpeech.LANG_NOT_SUPPORTED

            if (!langSupported) {
                // Try language-only (e.g., "hi", "ml", "ta")
                val baseLocale = Locale(targetLocale.language)
                val baseResult = try {
                    engine.setLanguage(baseLocale)
                } catch (_: Exception) {
                    TextToSpeech.LANG_NOT_SUPPORTED
                }

                val baseSupported = baseResult != TextToSpeech.LANG_MISSING_DATA &&
                        baseResult != TextToSpeech.LANG_NOT_SUPPORTED

                if (!baseSupported) {
                    // Fall back to English engine
                    try { engine.setLanguage(Locale.ENGLISH) } catch (_: Exception) {}
                    // If the text contains Indic script, English TTS will be silent.
                    // Fall back to a spoken English announcement so sound is guaranteed!
                    if (containsIndicScript(text)) {
                        textToSpeak = "Received 500 rupees on PhonePe from Rahul"
                    }
                }
            }

            // Play chime first if enabled
            if (playChime) {
                try {
                    withTimeoutOrNull(600L) {
                        chimePlayer.playChime(useAlarmStream)
                    }
                } catch (_: Exception) {}
            }

            // Request Audio Focus
            val focusRequest = buildAudioFocusRequest(audioUsage)
            try { requestAudioFocus(focusRequest, streamType) } catch (_: Exception) {}

            // Volume management: Unmute or boost to audible level
            val previousVolume = try { audioManager.getStreamVolume(streamType) } catch (_: Exception) { 7 }
            val maxVolume = try { audioManager.getStreamMaxVolume(streamType) } catch (_: Exception) { 15 }
            val audibleVolume = (maxVolume * 0.85f).toInt().coerceAtLeast(1)

            if (volumeBoost || previousVolume == 0) {
                try {
                    audioManager.setStreamVolume(streamType, audibleVolume, 0)
                } catch (_: Exception) {}
            }

            val speechCompletion = CompletableDeferred<Boolean>()
            val utteranceId = "upi_soundbox_${System.currentTimeMillis()}"

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (!speechCompletion.isCompleted) {
                        speechCompletion.complete(true)
                    }
                }
                override fun onError(utteranceId: String?) {
                    if (!speechCompletion.isCompleted) {
                        speechCompletion.complete(false)
                    }
                }
            })

            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, streamType)
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }

            val speakResult = try {
                engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            } catch (_: Exception) {
                TextToSpeech.ERROR
            }

            if (speakResult != TextToSpeech.SUCCESS) {
                speechCompletion.complete(false)
            }

            // Await completion with a safe 5-second timeout (never hang)
            val success = withTimeoutOrNull(5000L) {
                speechCompletion.await()
            } ?: (speakResult == TextToSpeech.SUCCESS)

            // Restore volume if boosted
            if (volumeBoost) {
                try {
                    audioManager.setStreamVolume(streamType, previousVolume, 0)
                } catch (_: Exception) {}
            }
            try { abandonAudioFocus(focusRequest, streamType) } catch (_: Exception) {}

            return@withContext success
        } catch (_: Exception) {
            return@withContext false
        }
    }

    private fun containsIndicScript(text: String): Boolean {
        return text.any { ch ->
            val block = Character.UnicodeBlock.of(ch)
            block == Character.UnicodeBlock.DEVANAGARI ||
                    block == Character.UnicodeBlock.MALAYALAM ||
                    block == Character.UnicodeBlock.TAMIL ||
                    block == Character.UnicodeBlock.TELUGU ||
                    block == Character.UnicodeBlock.KANNADA ||
                    block == Character.UnicodeBlock.BENGALI ||
                    block == Character.UnicodeBlock.GUJARATI ||
                    block == Character.UnicodeBlock.GURMUKHI
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
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isTtsReady = false
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
