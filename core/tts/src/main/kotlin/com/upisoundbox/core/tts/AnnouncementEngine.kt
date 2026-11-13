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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

class AnnouncementEngine(
    private val context: Context,
    private val chimePlayer: AudioChimePlayer
) : TextToSpeech.OnInitListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var tts: TextToSpeech? = null
    @Volatile
    private var isTtsReady = false
    private val initMutex = Mutex()
    private val speakMutex = Mutex()
    private val lifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var initDeferred = CompletableDeferred<Boolean>()
    @Volatile
    private var lastFailureReason: String? = null

    init {
        // Start TTS as soon as the application creates the singleton. This
        // removes the cold-start initialization pause from the first payment,
        // including when the activity is later minimized.
        lifecycleScope.launch {
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
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            isTtsReady = false
            lastFailureReason = "TTS initialization failed: ${error.message ?: error.javaClass.simpleName}"
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
            "hi-in", "hi" -> "{app} पर {amount} प्राप्त हुए"
            "ml-in", "ml" -> "{app} വഴി {amount} ലഭിച്ചു"
            "ta-in", "ta" -> "{app} மூலம் {amount} பெறப்பட்டது"
            "te-in", "te" -> "{app} ద్వారా {amount} అందింది"
            "kn-in", "kn" -> "{app} ಮೂಲಕ {amount} ಸ್ವೀಕರಿಸಲಾಗಿದೆ"
            "mr-in", "mr" -> "{app} वर {amount} प्राप्त झाले"
            "bn-in", "bn" -> "{app} এ {amount} পাওয়া গেছে"
            "gu-in", "gu" -> "{app} પર {amount} મળ્યા"
            "pa-in", "pa" -> "{app} 'ਤੇ {amount} ਪ੍ਰਾਪਤ ਹੋਏ"
            else -> "{amount} received on {app}"
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
            var failureStage = "starting"
            var playbackErrorCode: Int? = null

            try {
                failureStage = "initializing TTS"
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
                    failureStage = "checking phone-call state"
                    var callWaitCount = 0
                    while (isPhoneCallActive() && callWaitCount < 10) {
                        delay(1000)
                        callWaitCount++
                    }
                }

                val streamType = if (useAlarmStream) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
                focusStreamType = streamType
                val audioUsage = if (useAlarmStream) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
                failureStage = "configuring audio output"
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

                failureStage = "requesting audio focus"
                audioFocusRequest = buildAudioFocusRequest(audioUsage)
                audioFocusRequested = try {
                    requestAudioFocus(audioFocusRequest, streamType)
                } catch (_: Exception) {
                    false
                }

                failureStage = "boosting output volume"
                try {
                    if (volumeBoost) {
                        val maxVolume = audioManager.getStreamMaxVolume(streamType)
                        originalVolume = audioManager.getStreamVolume(streamType)
                        changedVolumeStream = streamType
                        audioManager.setStreamVolume(streamType, maxVolume, 0)
                    }
                } catch (_: Exception) {}

                if (playChime) {
                    failureStage = "playing notification chime"
                    try {
                        // Let the bundled tune finish before starting speech,
                        // but never allow a broken media file to block audio.
                        withTimeoutOrNull(2500L) { chimePlayer.playChime(useAlarmStream) }
                    } catch (_: Exception) {}
                }

                failureStage = "preparing speech callback"
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
                            playbackErrorCode = null
                            speechCompletion.complete(false)
                        }
                    }

                    override fun onError(id: String?, errorCode: Int) {
                        if ((id == null || id == utteranceId) && !speechCompletion.isCompleted) {
                            playbackErrorCode = errorCode
                            speechCompletion.complete(false)
                        }
                    }
                })

                // Keep the first request as close as possible to Android's own
                // TTS sample. Some OEM engines reject the legacy volume bundle
                // even though the same voice works from system settings.
                failureStage = "starting speech"
                var speakResult = try {
                    engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                } catch (_: Exception) {
                    TextToSpeech.ERROR
                }

                // A few OEM engines need the legacy stream parameter. Retry once
                // without the modern-only volume parameter before declaring failure.
                if (speakResult != TextToSpeech.SUCCESS) {
                    failureStage = "retrying speech with audio stream compatibility"
                    try {
                        engine.stop()
                        val compatibilityParams = Bundle().apply {
                            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, streamType)
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
                    lastFailureReason = "TTS rejected the speech request (result $speakResult)."
                    speechCompletion.complete(false)
                }

                failureStage = "waiting for speech playback"
                val completionResult = withTimeoutOrNull(7000L) {
                    speechCompletion.await()
                }

                return@withContext when {
                    speakResult != TextToSpeech.SUCCESS -> false
                    completionResult != true -> {
                        val code = playbackErrorCode?.let { " (error code $it)" }.orEmpty()
                        lastFailureReason = if (completionResult == false) {
                            "TTS playback failed$code."
                        } else {
                            "TTS playback timed out before the engine reported completion."
                        }
                        false
                    }
                    else -> {
                        lastFailureReason = null
                        true
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val detail = error.message?.takeIf { it.isNotBlank() }
                    ?: error.javaClass.simpleName
                lastFailureReason = "TTS failed during $failureStage: $detail"
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
                // Android requires a listener when an AudioFocusRequest is
                // built. We do not use delayed focus, so keep the listener a
                // no-op and only proceed when focus is granted immediately.
                .setOnAudioFocusChangeListener { }
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
        lifecycleScope.cancel()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isTtsReady = false
    }

    companion object {
        fun mapPackageToDisplayName(pkg: String): String {
            val normalized = pkg.trim().lowercase()
            return when {
                normalized.contains("nbu.paisa") ||
                        normalized.contains("google pay") ||
                        normalized.contains("googlepay") ||
                        normalized.contains("gpay") -> {
                    if (normalized.contains("merchant") || normalized.contains("business")) {
                        "Google Pay Business"
                    } else {
                        "Google Pay"
                    }
                }
                normalized.contains("phonepe") -> "PhonePe"
                normalized.contains("paytm") || normalized.contains("net.one97") -> "Paytm"
                normalized == "in.org.npci.upiapp" || normalized.contains("bhim") -> "BHIM"
                normalized.contains("amazon") -> "Amazon Pay"
                normalized.contains("dreamplug") || normalized.contains("cred") -> "CRED"
                normalized.contains("naviapp") || normalized == "navi" -> "Navi"
                normalized.contains("super.payments") || normalized.contains("supermoney") -> "super.money"
                normalized.contains("whatsapp") -> "WhatsApp Pay"
                normalized.contains("mobikwik") -> "MobiKwik"
                normalized.contains("popclub") || normalized == "pop" -> "POP"
                normalized.contains("shareapp") || normalized.contains("slice") -> "slice"
                normalized.contains("fampay") || normalized.contains("famapp") -> "FamApp"
                normalized.contains("nextbillion.groww") || normalized.contains("groww") -> "Groww"
                normalized.contains("myairtelapp") || normalized.contains("airtel") -> "Airtel"
                normalized.startsWith("vk-") || normalized.startsWith("ax-") || normalized.startsWith("ad-") -> "Bank SMS"
                else -> "UPI"
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
