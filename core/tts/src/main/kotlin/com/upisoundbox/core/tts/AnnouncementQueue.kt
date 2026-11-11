package com.upisoundbox.core.tts

import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.parser.NumberToWords
import com.upisoundbox.core.parser.PaymentEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface AnnouncementRequest {
    data class Payment(val event: PaymentEvent) : AnnouncementRequest
    data class Test(val sampleAmountPaise: Long = 10000L, val samplePayer: String = "Rahul") : AnnouncementRequest
    data class Custom(val text: String, val languageTag: String) : AnnouncementRequest
}

class AnnouncementQueue(
    private val engine: AnnouncementEngine,
    private val preferences: SoundboxPreferences,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val queueChannel = Channel<AnnouncementRequest>(Channel.UNLIMITED)
    private var lastAnnouncedText: String? = null
    private var lastAnnouncedLang: String = "en-IN"

    // Burst buffer tracking
    private val burstBuffer = mutableListOf<PaymentEvent>()

    init {
        scope.launch {
            processQueue()
        }
    }

    fun enqueue(request: AnnouncementRequest) {
        queueChannel.trySend(request)
    }

    suspend fun replayLast(): Boolean {
        val text = lastAnnouncedText ?: return false
        return engine.speak(text = text, languageTag = lastAnnouncedLang)
    }

    private suspend fun processQueue() {
        for (request in queueChannel) {
            handleRequest(request)
        }
    }

    private suspend fun handleRequest(request: AnnouncementRequest) {
        when (request) {
            is AnnouncementRequest.Test -> {
                val lang = preferences.languageTag.first()
                val template = preferences.phraseTemplate.first()
                val rendered = engine.renderPhrase(
                    template = template,
                    amountPaise = request.sampleAmountPaise,
                    payer = request.samplePayer,
                    appPackage = "com.google.android.apps.nbu.paisa.user",
                    languageTag = lang
                )
                val testText = "Test announcement: $rendered"
                val useAlarm = preferences.isAlarmStreamEnabled.first()
                val boost = preferences.isVolumeBoostEnabled.first()
                val chime = preferences.isChimeEnabled.first()
                val waitCalls = preferences.waitDuringCalls.first()

                lastAnnouncedText = testText
                lastAnnouncedLang = lang
                engine.speak(testText, lang, useAlarm, boost, chime, waitCalls)
            }

            is AnnouncementRequest.Custom -> {
                lastAnnouncedText = request.text
                lastAnnouncedLang = request.languageTag
                engine.speak(request.text, request.languageTag)
            }

            is AnnouncementRequest.Payment -> {
                // 1. Check if soundbox is active
                if (!preferences.isSoundboxActive.first()) return

                // 2. Check minimum amount
                val minAmount = preferences.minAmountPaise.first()
                if (request.event.amountPaise < minAmount) return

                // 3. Check quiet hours
                if (isInsideQuietHours()) return

                // 4. Burst handling: collect any pending payments
                val isBurstEnabled = preferences.isBurstModeEnabled.first()
                val burstThreshold = preferences.burstThresholdCount.first()

                burstBuffer.add(request.event)
                delay(300) // Small debounce window to detect bursts

                // Drain any additional events arrived in the channel
                while (true) {
                    val next = queueChannel.tryReceive().getOrNull() ?: break
                    if (next is AnnouncementRequest.Payment) {
                        burstBuffer.add(next.event)
                    } else {
                        // Re-dispatch non-payment requests
                        scope.launch { queueChannel.send(next) }
                    }
                }

                val lang = preferences.languageTag.first()
                val useAlarm = preferences.isAlarmStreamEnabled.first()
                val boost = preferences.isVolumeBoostEnabled.first()
                val chime = preferences.isChimeEnabled.first()
                val waitCalls = preferences.waitDuringCalls.first()
                val repeats = preferences.repeatCount.first()

                if (isBurstEnabled && burstBuffer.size >= burstThreshold) {
                    val count = burstBuffer.size
                    val totalPaise = burstBuffer.sumOf { it.amountPaise }
                    val totalWords = NumberToWords.convertPaise(totalPaise, lang)
                    val formatted = if (totalWords.isNotBlank()) totalWords else AmountParser.formatPaise(totalPaise)
                    val burstSummary = renderBurstSummary(count, formatted, lang)

                    lastAnnouncedText = burstSummary
                    lastAnnouncedLang = lang

                    repeat(repeats) {
                        engine.speak(burstSummary, lang, useAlarm, boost, chime, waitCalls)
                    }
                    burstBuffer.clear()
                } else {
                    for (event in burstBuffer) {
                        val template = preferences.phraseTemplate.first()
                        val text = engine.renderPhrase(
                            template = template,
                            amountPaise = event.amountPaise,
                            payer = event.payer,
                            appPackage = event.appPackage,
                            languageTag = lang
                        )

                        lastAnnouncedText = text
                        lastAnnouncedLang = lang

                        repeat(repeats) {
                            engine.speak(text, lang, useAlarm, boost, chime, waitCalls)
                        }
                    }
                    burstBuffer.clear()
                }
            }
        }
    }

    private suspend fun isInsideQuietHours(): Boolean {
        if (!preferences.isQuietHoursEnabled.first()) return false
        val now = Calendar.getInstance()
        val curMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val startMinutes = preferences.quietHoursStartHour.first() * 60 + preferences.quietHoursStartMinute.first()
        val endMinutes = preferences.quietHoursEndHour.first() * 60 + preferences.quietHoursEndMinute.first()

        return if (startMinutes <= endMinutes) {
            curMinutes in startMinutes..endMinutes
        } else {
            // Spans midnight (e.g. 22:00 to 07:00)
            curMinutes >= startMinutes || curMinutes <= endMinutes
        }
    }

    private fun renderBurstSummary(count: Int, totalStr: String, lang: String): String {
        return when (lang.lowercase()) {
            "hi-in", "hi" -> "$count भुगतान प्राप्त हुए, कुल $totalStr"
            "ml-in", "ml" -> "$count പേയ്‌മെന്റുകൾ ലഭിച്ചു, ആകെ $totalStr"
            "ta-in", "ta" -> "$count பரிவர்த்தனைகள் பெறப்பட்டன, மொத்தம் $totalStr"
            "te-in", "te" -> "$count చెల్లింపులు అందాయి, మొత్తం $totalStr"
            "kn-in", "kn" -> "$count ಪಾವತಿಗಳು ಸ್ವೀಕರಿಸಲ್ಪಟ್ಟಿವೆ, ಒಟ್ಟು $totalStr"
            else -> "$count payments received, total $totalStr"
        }
    }
}

