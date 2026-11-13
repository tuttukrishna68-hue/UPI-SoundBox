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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val initMutex = Mutex()
    private val speakMutex = Mutex()
    private var initDeferred = CompletableDeferred<Boolean>()
    @Volatile
    private var lastFailureReason: String? = null

    init {
        // Kick off asynchronous initialization on Main Looper
        CoroutineScope(Dispatchers.Main).launch {
            ensureReady()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            lastFailureReason = null
            try {
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(0.95f)
            } catch (_: Exception) {}
            if (!initDeferred.isCompleted) {
                initDeferred.complete(true)
            }
        } else {
            isTtsReady = false
            lastFailureReason = "The phone's Text-to-Speech engine did not initialize."
            if (!initDeferred.isCompleted) {
                initDeferred.complete(false)
            }
        }
    }

    suspend fun ensureReady(): Boolean = initMutex.withLock {
        if (isTtsReady && tts != null) {
            lastFailureReason = null
            return true
        }

        if (!initDeferred.isCompleted && tts != null) {
            val ready = withTimeoutOrNull(8000L) {
                initDeferred.await()
            } ?: false
            if (ready && isTtsReady && tts != null) {
                lastFailureReason = null
                return true
            }
        }

        // Initialize fresh TextToSpeech instance on Main thread
        initDeferred = CompletableDeferred()
        isTtsReady = false
        try {
            withContext(Dispatchers.Main) {
                try {
                    tts?.stop()
                    tts?.shutdown()
                } catch (_: Exception) {}
                tts = null
                tts = TextToSpeech(context.applicationContext, this@AnnouncementEngine)
            }
            val ready = withTimeoutOrNull(8000L) {
                initDeferred.await()
            } ?: false
            if (!ready || !isTtsReady || tts == null) {
                lastFailureReason = "The phone's Text-to-Speech engine is unavailable or still loading."
                return false
            }
            lastFailureReason = null
            return true
        } catch (_: Exception) {
            isTtsReady = false
            lastFailureReason = "The phone's Text-to-Speech engine could not be opened."
            if (!initDeferred.isCompleted) initDeferred.complete(false)
            return false
        }
    }

    suspend fun isVoiceInstalled(languageTag: String): Boolean = withContext(Dispatchers.IO) {
        if (!ensureReady()) return@withContext false
        withContext(Dispatchers.Main) {
            val engine = tts ?: return@withContext false
            resolveSpeechLocale(engine, languageTag) != null
        }
    }

    fun getVoiceInstallIntent(): Intent {
        return Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getTtsSettingsIntent(): Intent {
        // The TTS settings action is not exposed as a constant in every SDK
        // revision, but is supported by the Android Settings app.
        return Intent("com.android.settings.TTS_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getLastFailureReason(): String {
        return lastFailureReason ?: "The Text-to-Speech engine rejected the audio request."
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
    ): Boolean = speakMutex.withLock {
        withContext(Dispatchers.Main) {
            var audioFocusRequest: AudioFocusRequest? = null
            var audioFocusRequested = false
            var focusStreamType: Int? = null
            var changedVolumeStream: Int? = null
            var originalVolume: Int? = null

            try {
                if (!ensureReady()) {
                    playFallbackChime(useAlarmStream, playChime)
                    return@withContext false
                }
                val engine = tts ?: run {
                    lastFailureReason = "The Text-to-Speech engine is unavailable."
                    playFallbackChime(useAlarmStream, playChime)
                    return@withContext false
                }

                if (waitCalls) {
                    var callWaitCount = 0
                    while (isPhoneCallActive() && callWaitCount < 10) {
                        delay(1000)
                        callWaitCount++
                    }
                }

                val streamType = if (useAlarmStream) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
                focusStreamType = streamType
                val audioUsage = if (useAlarmStream) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
                try {
                    engine.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(audioUsage)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                } catch (_: Exception) {}

                val voiceSelection = resolveSpeechLocale(engine, languageTag)
                if (voiceSelection == null) {
                    lastFailureReason = "No installed voice supports $languageTag. Install a voice in Android Text-to-Speech settings."
                    playFallbackChime(useAlarmStream, playChime)
                    return@withContext false
                }

                val textToSpeak = if (!voiceSelection.requestedLanguage && containsIndicScript(text)) {
                    "Payment received."
                } else {
                    text
                }

                audioFocusRequest = buildAudioFocusRequest(audioUsage)
                audioFocusRequested = try {
                    requestAudioFocus(audioFocusRequest, streamType)
                } catch (_: Exception) {
                    false
                }

                if (playChime) {
                    try {
                        withTimeoutOrNull(700L) { chimePlayer.playChime(useAlarmStream) }
                    } catch (_: Exception) {}
                }

                try {
                    if (volumeBoost) {
                        val maxVolume = audioManager.getStreamMaxVolume(streamType)
                        originalVolume = audioManager.getStreamVolume(streamType)
                        changedVolumeStream = streamType
                        val audibleVolume = (maxVolume * 0.85f).toInt().coerceAtLeast(1)
                        audioManager.setStreamVolume(streamType, audibleVolume, 0)
                    }
                } catch (_: Exception) {}

                val speechCompletion = CompletableDeferred<Boolean>()
                val utteranceId = "soundbox_${System.currentTimeMillis()}"

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = Unit

                    override fun onDone(id: String?) {
                        if ((id == null || id == utteranceId) && !speechCompletion.isCompleted) {
                            speechCompletion.complete(true)
                        }
                    }

                    @Suppress("DEPRECATION")
                    override fun onError(id: String?) {
                        if ((id == null || id == utteranceId) && !speechCompletion.isCompleted) {
                            speechCompletion.complete(false)
                        }
                    }

                    override fun onError(id: String?, errorCode: Int) {
                        if ((id == null || id == utteranceId) && !speechCompletion.isCompleted) {
                            speechCompletion.complete(false)
                        }
                    }
                })

                val normalParams = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                var speakResult = try {
                    engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, normalParams, utteranceId)
                } catch (_: Exception) {
                    TextToSpeech.ERROR
                }

                // A few OEM engines reject a request after AudioAttributes are set unless
                // the legacy stream key is also present. Retry once with that compatibility
                // parameter before declaring the speaker unavailable.
                if (speakResult != TextToSpeech.SUCCESS) {
                    try {
                        engine.stop()
                        val compatibilityParams = Bundle().apply {
                            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, streamType)
                            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                        }
                        speakResult = engine.speak(
                            textToSpeak,
                            TextToSpeech.QUEUE_FLUSH,
                            compatibilityParams,
                            utteranceId
                        )
                    } catch (_: Exception) {
                        speakResult = TextToSpeech.ERROR
                    }
                }

                if (speakResult != TextToSpeech.SUCCESS) {
                    lastFailureReason = "The Text-to-Speech engine rejected the speech request."
                    speechCompletion.complete(false)
                }

                val completionResult = withTimeoutOrNull(7000L) {
                    speechCompletion.await()
                }

                delay(350L)

                return@withContext when {
                    speakResult != TextToSpeech.SUCCESS -> false
                    completionResult == false -> {
                        lastFailureReason = "The Text-to-Speech engine could not play this voice."
                        false
                    }
                    else -> {
                        lastFailureReason = null
                        true
                    }
                }
            } catch (_: Exception) {
                lastFailureReason = "The Text-to-Speech engine could not play audio."
                return@withContext false
            } finally {
                if (audioFocusRequested) {
                    try {
                        abandonAudioFocus(audioFocusRequest, focusStreamType ?: AudioManager.STREAM_MUSIC)
                    } catch (_: Exception) {}
                }
                if (originalVolume != null && changedVolumeStream != null) {
                    try {
                        audioManager.setStreamVolume(changedVolumeStream!!, originalVolume!!, 0)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private data class VoiceSelection(
        val locale: Locale,
        val requestedLanguage: Boolean
    )

    private fun resolveSpeechLocale(engine: TextToSpeech, languageTag: String): VoiceSelection? {
        val requested = try {
            Locale.forLanguageTag(languageTag.replace('_', '-'))
        } catch (_: Exception) {
            Locale("en", "IN")
        }
        val candidates = listOf(
            requested,
            Locale(requested.language),
            Locale("en", "IN"),
            Locale.US,
            Locale.ENGLISH
        ).distinctBy { it.toLanguageTag() }

        candidates.forEachIndexed { index, locale ->
            val result = try {
                engine.setLanguage(locale)
            } catch (_: Exception) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }
            if (result >= TextToSpeech.LANG_AVAILABLE) {
                return VoiceSelection(locale, index == 0 || locale.language == requested.language)
            }
        }

        // Some engines expose a valid local voice through getVoices() even when
        // setLanguage reports missing data. Prefer an offline voice when possible.
        val voices = try { engine.voices.orEmpty() } catch (_: Exception) { emptySet() }
        val voice = voices.firstOrNull {
            it.locale.language == requested.language && !it.isNetworkConnectionRequired
        } ?: voices.firstOrNull {
            it.locale.language == Locale.ENGLISH.language && !it.isNetworkConnectionRequired
        } ?: voices.firstOrNull { !it.isNetworkConnectionRequired }

        if (voice != null) {
            return try {
                engine.voice = voice
                VoiceSelection(voice.locale, voice.locale.language == requested.language)
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    private suspend fun playFallbackChime(useAlarmStream: Boolean, enabled: Boolean) {
        if (!enabled) return
        try {
            withTimeoutOrNull(900L) { chimePlayer.playChime(useAlarmStream) }
        } catch (_: Exception) {}
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
                "com.naviapp" -> "Navi"
                "money.super.payments" -> "super.money"
                "com.whatsapp" -> "WhatsApp Pay"
                "com.mobikwik_new" -> "MobiKwik"
                "com.popclub.android" -> "POP"
                "indwin.c3.shareapp" -> "slice"
                "com.fampay.in" -> "FamApp"
                "com.nextbillion.groww" -> "Groww"
                "com.myairtelapp" -> "Airtel"
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
