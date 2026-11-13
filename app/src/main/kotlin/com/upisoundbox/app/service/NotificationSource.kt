package com.upisoundbox.app.service

import android.app.Notification
import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.parser.KeywordMatcher
import com.upisoundbox.core.parser.ParseResult
import com.upisoundbox.core.parser.ParserRouter
import com.upisoundbox.core.parser.RawNotification
import com.upisoundbox.core.parser.UpiAppConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class NotificationSource : NotificationListenerService() {

    @Inject
    lateinit var preferences: SoundboxPreferences

    @Inject
    lateinit var paymentProcessor: PaymentProcessor

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processedKeys = Collections.synchronizedSet(LinkedHashSet<String>())
    private val parserRouter = ParserRouter()
    private val keywordMatcher = KeywordMatcher()
    private val amountParser = AmountParser()

    @Volatile
    private var disabledUpiPackages: Set<String> = emptySet()

    @Volatile
    private var unknownAppLearningExpiry: Long = 0L

    override fun onCreate() {
        super.onCreate()
        loadAllowListFromAssets()
        serviceScope.launch {
            preferences.disabledUpiPackages.collect { disabledUpiPackages = it }
        }
        serviceScope.launch {
            preferences.unknownAppLearningExpiry.collect { unknownAppLearningExpiry = it }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isListening = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isListening = false
        // Per spec: call requestRebind when listener disconnects
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, NotificationSource::class.java))
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Avoid scheduling work for the phone's ordinary notifications. The
        // allow-listed UPI packages take the fast path; unknown packages are
        // still admitted while the opt-in learning window is active.
        val packageName = sbn.packageName ?: return
        if (!parserRouter.isAllowed(packageName) &&
            System.currentTimeMillis() >= unknownAppLearningExpiry
        ) return

        // NotificationListener callbacks share a system-managed binder thread.
        // Move parsing and persistence off it immediately so a slow SMS/regex/
        // database operation cannot delay the next notification update.
        serviceScope.launch {
            processNotification(sbn)
        }
    }

    private suspend fun processNotification(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        val notification = sbn.notification ?: return

        // Ignore ongoing and group summaries immediately
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (isOngoing) return

        val isGroupSummary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sbn.isGroup && (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        } else {
            false
        }
        if (isGroupSummary) return

        // Extract raw notification fields
        val extras = notification.extras
        val title = extras?.getString(Notification.EXTRA_TITLE)
            ?: extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val textLines = mutableListOf<String>()
        val linesArray = extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        linesArray?.forEach { textLines.add(it.toString()) }

        // Some UPI apps reuse a notification id and update the same notification
        // object for a later payment. Include the visible content in the
        // idempotency key so a changed payment is not mistaken for the first one.
        val contentFingerprint = listOf(title, text, bigText, subText, textLines.joinToString("\u001F"))
            .joinToString("\u001E")
            .hashCode()
        val idempotencyKey = "${sbn.key}_${sbn.postTime}_$contentFingerprint"
        if (!processedKeys.add(idempotencyKey)) {
            return // Exact notification update was already parsed
        }
        if (processedKeys.size > 1000) {
            synchronized(processedKeys) {
                val iterator = processedKeys.iterator()
                repeat(200) { if (iterator.hasNext()) { iterator.next(); iterator.remove() } }
            }
        }

        val rawNotification = RawNotification(
            key = sbn.key,
            packageName = pkg,
            postTime = sbn.postTime,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            textLines = textLines,
            isGroupSummary = isGroupSummary,
            isOngoing = isOngoing
        )

        // Check allow-list and the merchant's per-app toggle.
        val isDisabled = pkg in disabledUpiPackages
        val isAllowed = !isDisabled && parserRouter.isAllowed(pkg)
        if (!isAllowed) {
            // Check if Unknown-App Learning mode is active (opt-in 5 min)
            val expiry = preferences.unknownAppLearningExpiry.first()
            if (System.currentTimeMillis() < expiry) {
                val full = rawNotification.fullText()
                // If it looks like a payment (has credit keyword and amount)
                if (keywordMatcher.hasRejectKeyword(full) == null &&
                    keywordMatcher.findCreditKeywords(full).isNotEmpty() &&
                    amountParser.findAllAmounts(full).isNotEmpty()
                ) {
                    SoundboxEventBus.emitUnknownNotification(rawNotification)
                }
            }
            // Discard all other notifications immediately without storing
            return
        }

        // Parse notification
        when (val result = parserRouter.parse(rawNotification)) {
            is ParseResult.Success -> {
                val event = result.event
                // All sources share one application-scoped processor so a
                // notification and SMS for the same payment cannot double count.
                if (paymentProcessor.process(event)) {
                    try {
                        // Keep the persistent service alive for its live status notification.
                        // SoundboxService.start() is guarded, so repeated notification
                        // updates do not repeatedly start the foreground service.
                        SoundboxService.start(this@NotificationSource)
                    } catch (_: Exception) {}
                }
            }
            is ParseResult.Rejected, is ParseResult.Ignored -> {
                // Rejected noise or debits discarded
            }
        }
    }

    private fun loadAllowListFromAssets() {
        try {
            val jsonString = assets.open("upi_apps.json").bufferedReader().use { it.readText() }
            val apps = Json.decodeFromString<List<UpiAppConfig>>(jsonString)
            parserRouter.loadAllowList(apps)
        } catch (_: Exception) {
            // Fall back to default allow-list
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        isListening = false
        super.onDestroy()
    }

    companion object {
        @Volatile
        var isListening = false
            private set
    }
}
