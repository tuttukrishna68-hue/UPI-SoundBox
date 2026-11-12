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
    private var initDeferred = CompletableDeferred<Boolean>()

    init {
        // Kick off asynchronous initialization on Main Looper
        CoroutineScope(Dispatchers.Main).launch {
            ensureReady()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            try {
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

    suspend fun ensureReady(): Boolean = initMutex.withLock {
        if (isTtsReady && tts != null) return true

        if (!initDeferred.isCompleted && tts != null) {
            val ready = withTimeoutOrNull(4000L) {
                initDeferred.await()
            } ?: false
            if (ready && isTtsReady) return true
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
                tts = TextToSpeech(context.applicationContext, this@AnnouncementEngine)
            }
            val ready = withTimeoutOrNull(4500L) {
                initDeferred.await()
            } ?: false
            return ready && isTtsReady
        } catch (_: Exception) {
            isTtsReady = false
            if (!initDeferred.isCompleted) initDeferred.complete(false)
            return false
        }
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
        var audioFocusRequest: AudioFocusRequest? = null
        var audioFocusRequested = false
        var focusStreamType: Int? = null
        var changedVolumeStream: Int? = null
        var originalVolume: Int? = null

        try {
            val ready = ensureReady()
            if (!ready) {
                // Still give the merchant an audible signal when speech data is unavailable.
                if (playChime) {
                    try {
                        withTimeoutOrNull(700L) {
                            chimePlayer.playChime(useAlarmStream)
                        }
                    } catch (_: Exception) {}
                }
                return@withContext false
            }
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
            focusStreamType = streamType
            // MEDIA is honoured consistently by OEM speakers and Bluetooth routes.
            // ACCESSIBILITY is ignored or muted by several merchant-phone builds.
            val audioUsage = if (useAlarmStream) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA

            // Audio Attributes
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(audioUsage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            try {
                engine.setAudioAttributes(audioAttributes)
            } catch (_: Exception) {}

            // Language resolution with graceful fallback
            val targetLocale = try {
                Locale.forLanguageTag(languageTag)
            } catch (_: Exception) {
                Locale("en", "IN")
            }

            val textToSpeak = text
            val setLangResult = try {
                engine.setLanguage(targetLocale)
            } catch (_: Exception) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }

            val langSupported = setLangResult >= TextToSpeech.LANG_AVAILABLE

            if (!langSupported) {
                // Try language-only (e.g., "hi", "ml", "ta", "te", "kn")
                val baseLocale = Locale(targetLocale.language)
                val baseResult = try {
                    engine.setLanguage(baseLocale)
                } catch (_: Exception) {
                    TextToSpeech.LANG_NOT_SUPPORTED
                }

                val baseSupported = baseResult >= TextToSpeech.LANG_AVAILABLE

                if (!baseSupported) {
                    // Fall back to Indian English or English
                    try {
                        val enInResult = engine.setLanguage(Locale("en", "IN"))
                        if (enInResult < TextToSpeech.LANG_AVAILABLE) {
                            engine.setLanguage(Locale.ENGLISH)
                        }
                    } catch (_: Exception) {
                        try { engine.setLanguage(Locale.ENGLISH) } catch (_: Exception) {}
                    }
                }
            }

            // Audio focus
            audioFocusRequest = buildAudioFocusRequest(audioUsage)
            audioFocusRequested = try {
                requestAudioFocus(audioFocusRequest, streamType)
            } catch (_: Exception) {
                false
            }

            // Play pleasant audio chime after focus is granted so it is not dropped by the OS.
            if (playChime) {
                try {
                    withTimeoutOrNull(700L) {
                        chimePlayer.playChime(useAlarmStream)
                    }
                } catch (_: Exception) {}
            }

            // Volume management: boost only when explicitly enabled, then restore
            // the merchant's original volume after speech completes.
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
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId && !speechCompletion.isCompleted) {
                        speechCompletion.complete(true)
                    }
                }
                override fun onError(id: String?) {
                    if (id == utteranceId && !speechCompletion.isCompleted) {
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

            // Await speech completion with a 6-second timeout
            val success = withTimeoutOrNull(6000L) {
                speechCompletion.await()
            } ?: false

            // Grace period so hardware speaker finishes physical output buffer
            delay(350L)

            return@withContext success
        } catch (_: Exception) {
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
