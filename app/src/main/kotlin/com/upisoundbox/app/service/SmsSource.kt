package com.upisoundbox.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.parser.ParseResult
import com.upisoundbox.core.parser.RawSms
import com.upisoundbox.core.parser.SmsParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsSource : BroadcastReceiver() {

    @Inject
    lateinit var preferences: SoundboxPreferences

    private val smsParser = SmsParser()
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        scope.launch {
            // Check if SMS source is enabled by user
            val isEnabled = preferences.isSmsSourceEnabled.first()
            if (!isEnabled) return@launch

            val allowedSenders = preferences.allowedSmsSenders.first()
                .split(",")
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() }

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return@launch
            for (sms in messages) {
                val sender = sms.originatingAddress ?: continue
                val senderUpper = sender.uppercase()

                // Check sender ID allow-list (e.g. HDFCBK, SBIINB, VK-HDFCBK)
                val matchesSender = allowedSenders.any { allowed ->
                    senderUpper.contains(allowed)
                }
                if (!matchesSender) continue

                val body = sms.messageBody ?: continue
                val rawSms = RawSms(
                    sender = sender,
                    body = body,
                    receivedAt = sms.timestampMillis
                )

                when (val result = smsParser.parse(rawSms)) {
                    is ParseResult.Success -> {
                        SoundboxService.handlePayment(context, result.event)
                    }
                    else -> {}
                }
            }
        }
    }
}

