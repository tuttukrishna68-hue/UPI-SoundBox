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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class NotificationSource : NotificationListenerService() {

    @Inject
    lateinit var preferences: SoundboxPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val processedKeys = Collections.synchronizedSet(LinkedHashSet<String>())
    private val parserRouter = ParserRouter()
    private val keywordMatcher = KeywordMatcher()
    private val amountParser = AmountParser()

    override fun onCreate() {
        super.onCreate()
        loadAllowListFromAssets()
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

        // Idempotency check: sbn.key + postTime
        val idempotencyKey = "${sbn.key}_${sbn.postTime}"
        if (!processedKeys.add(idempotencyKey)) {
            return // Already processed
        }
        if (processedKeys.size > 1000) {
            val iterator = processedKeys.iterator()
            repeat(200) { if (iterator.hasNext()) { iterator.next(); iterator.remove() } }
        }

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

        // Check allow-list
        val isAllowed = parserRouter.isAllowed(pkg)
        if (!isAllowed) {
            // Check if Unknown-App Learning mode is active (opt-in 5 min)
            serviceScope.launch {
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
            }
            // Discard all other notifications immediately without storing
            return
        }

        // Parse notification
        when (val result = parserRouter.parse(rawNotification)) {
            is ParseResult.Success -> {
                // Forward to Soundbox foreground service for deduplication, announcement, and DB write
                SoundboxService.handlePayment(this, result.event)
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

    companion object {
        @Volatile
        var isListening = false
            private set
    }
}

