package com.upisoundbox.core.tts

import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.parser.NumberToWords
import com.upisoundbox.core.parser.PaymentEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface AnnouncementRequest {
    data class Payment(val event: PaymentEvent) : AnnouncementRequest
    data class Test(
        val sampleAmountPaise: Long = 50000L,
        val samplePayer: String = "Rahul Sharma",
        val languageTag: String? = null
    ) : AnnouncementRequest
    data class Custom(val text: String, val languageTag: String) : AnnouncementRequest
}

class AnnouncementQueue(
    private val engine: AnnouncementEngine,
    private val preferences: SoundboxPreferences,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private data class QueuedRequest(
        val request: AnnouncementRequest,
        val completion: CompletableDeferred<Boolean>? = null
    )

    private val queueChannel = Channel<QueuedRequest>(Channel.UNLIMITED)
    private var lastAnnouncedText: String? = null
    private var lastAnnouncedLang: String = "en-IN"

    // Burst buffer tracking
    private val burstBuffer = mutableListOf<PaymentEvent>()

    init {
        scope.launch {
            processQueue()
        }
    }

    fun enqueue(request: AnnouncementRequest): Boolean {
        return queueChannel.trySend(QueuedRequest(request)).isSuccess
    }

    /**
     * Enqueues a request and waits until the audio engine has actually attempted it.
     * UI test buttons use this path so they never report success before playback starts.
     */
    suspend fun enqueueAndAwait(request: AnnouncementRequest): Boolean {
        val completion = CompletableDeferred<Boolean>()
        queueChannel.send(QueuedRequest(request, completion))
        return completion.await()
    }

    suspend fun replayLast(): Boolean {
        val text = lastAnnouncedText ?: return false
        return engine.speak(text = text, languageTag = lastAnnouncedLang)
    }

    private suspend fun processQueue() {
        for (queuedRequest in queueChannel) {
            val success = try {
                handleRequest(queuedRequest.request)
            } catch (_: Exception) {
                false
            }
            queuedRequest.completion?.complete(success)
        }
    }

    private suspend fun handleRequest(request: AnnouncementRequest): Boolean {
        when (request) {
            is AnnouncementRequest.Test -> {
                val lang = request.languageTag ?: preferences.languageTag.first()
                val template = engine.getDefaultTemplateForLanguage(lang)
                val rendered = engine.renderPhrase(
                    template = template,
                    amountPaise = request.sampleAmountPaise,
                    payer = request.samplePayer,
                    appPackage = "com.phonepe.app",
                    languageTag = lang
                )
                val useAlarm = preferences.isAlarmStreamEnabled.first()
                val boost = preferences.isVolumeBoostEnabled.first()
                val chime = preferences.isChimeEnabled.first()
                val waitCalls = preferences.waitDuringCalls.first()

                lastAnnouncedText = rendered
                lastAnnouncedLang = lang
                return engine.speak(rendered, lang, useAlarm, boost, chime, waitCalls)
            }

            is AnnouncementRequest.Custom -> {
                lastAnnouncedText = request.text
                lastAnnouncedLang = request.languageTag
                return engine.speak(request.text, request.languageTag)
            }

            is AnnouncementRequest.Payment -> {
                // 1. Check if soundbox is active
                if (!preferences.isSoundboxActive.first()) return false

                // 2. Check minimum amount
                val minAmount = preferences.minAmountPaise.first()
                if (request.event.amountPaise < minAmount) return false

                // 3. Check quiet hours
                if (isInsideQuietHours()) return false

                // 4. Burst handling: collect any pending payments
                val isBurstEnabled = preferences.isBurstModeEnabled.first()
                val burstThreshold = preferences.burstThresholdCount.first()

                if (isBurstEnabled && burstThreshold > 1) {
                    burstBuffer.add(request.event)
                    // Wait a short window (e.g. 1.5 seconds) to see if more arrive
                    delay(1500)
                    if (burstBuffer.size >= burstThreshold) {
                        val success = announceBurst(burstBuffer.toList())
                        burstBuffer.clear()
                        return success
                    } else if (burstBuffer.isNotEmpty()) {
                        val single = burstBuffer.removeAt(0)
                        return announceSingle(single)
                    }
                } else {
                    return announceSingle(request.event)
                }
            }
        }
        return false
    }

    private suspend fun announceSingle(event: PaymentEvent): Boolean {
        val lang = preferences.languageTag.first()
        val template = preferences.phraseTemplate.first()
        val repeat = preferences.repeatCount.first()
        val useAlarm = preferences.isAlarmStreamEnabled.first()
        val boost = preferences.isVolumeBoostEnabled.first()
        val chime = preferences.isChimeEnabled.first()
        val waitCalls = preferences.waitDuringCalls.first()

        val text = engine.renderPhrase(
            template = template,
            amountPaise = event.amountPaise,
            payer = event.payer,
            appPackage = event.appPackage,
            languageTag = lang
        )

        lastAnnouncedText = text
        lastAnnouncedLang = lang

        var success = true
        repeat(repeat) { index ->
            // Only play chime on first repetition
            val attempt = engine.speak(
                text = text,
                languageTag = lang,
                useAlarmStream = useAlarm,
                volumeBoost = boost,
                playChime = chime && index == 0,
                waitCalls = waitCalls
            )
            success = attempt && success
            if (index < repeat - 1) {
                delay(800)
            }
        }
        return success
    }

    private suspend fun announceBurst(events: List<PaymentEvent>): Boolean {
        val lang = preferences.languageTag.first()
        val totalPaise = events.sumOf { it.amountPaise }
        val count = events.size
        val words = NumberToWords.convertPaise(totalPaise, lang)
        val spokenAmount = if (words.isNotBlank()) words else AmountParser.formatPaise(totalPaise)

        val burstText = when (lang.lowercase()) {
            "hi-in", "hi" -> "कुल $count भुगतानों में $spokenAmount प्राप्त हुए"
            "ml-in", "ml" -> "$count പേയ്‌മെന്റുകളിലായി ആകെ $spokenAmount ലഭിച്ചു"
            "ta-in", "ta" -> "$count பரிவர்த்தனைகளில் மொத்தம் $spokenAmount பெறப்பட்டது"
            "te-in", "te" -> "$count చెల్లింపులలో మొత్తం $spokenAmount అందింది"
            "kn-in", "kn" -> "$count ಪಾವತಿಗಳಲ್ಲಿ ಒಟ್ಟು $spokenAmount ಸ್ವೀಕರಿಸಲಾಗಿದೆ"
            else -> "Received $count payments totaling $spokenAmount"
        }

        val useAlarm = preferences.isAlarmStreamEnabled.first()
        val boost = preferences.isVolumeBoostEnabled.first()
        val chime = preferences.isChimeEnabled.first()
        val waitCalls = preferences.waitDuringCalls.first()

        lastAnnouncedText = burstText
        lastAnnouncedLang = lang

        return engine.speak(
            text = burstText,
            languageTag = lang,
            useAlarmStream = useAlarm,
            volumeBoost = boost,
            playChime = chime,
            waitCalls = waitCalls
        )
    }

    private suspend fun isInsideQuietHours(): Boolean {
        if (!preferences.isQuietHoursEnabled.first()) return false

        val startH = preferences.quietHoursStartHour.first()
        val startM = preferences.quietHoursStartMinute.first()
        val endH = preferences.quietHoursEndHour.first()
        val endM = preferences.quietHoursEndMinute.first()

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = startH * 60 + startM
        val endMinutes = endH * 60 + endM

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
}
