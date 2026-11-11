package com.upisoundbox.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.upisoundbox.app.MainActivity
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.parser.PaymentDeduplicator
import com.upisoundbox.core.parser.PaymentEvent
import com.upisoundbox.core.tts.AnnouncementQueue
import com.upisoundbox.core.tts.AnnouncementRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Foreground Service for Soundbox Announcement & Ledger.
 *
 * Android 14+ Foreground Service Type rationale:
 * We declare `foregroundServiceType="specialUse"` in the AndroidManifest.
 * Rationale: The service operates continuously throughout the business day to perform instant
 * speech synthesis and ledger updates upon incoming UPI transactions detected via the system
 * NotificationListener and SMS receivers. It is not media playback (we do not stream music),
 * nor connectedDevice (it uses on-device speakers). `specialUse` is the honest, accurate system category
 * for dedicated shopkeeper soundbox listening services.
 */
@AndroidEntryPoint
class SoundboxService : Service() {

    @Inject
    lateinit var repository: PaymentRepository

    @Inject
    lateinit var preferences: SoundboxPreferences

    @Inject
    lateinit var announcementQueue: AnnouncementQueue

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val deduplicator = PaymentDeduplicator()

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForegroundWithNotification(0L, 0, isPaused = false)

        // Observe today's collection to keep persistent notification updated
        serviceScope.launch {
            repository.getTodayTotal().collect { total ->
                val count = repository.getTodayCount().first()
                val isPaused = !preferences.isSoundboxActive.first()
                updateNotification(total ?: 0L, count, isPaused)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                serviceScope.launch {
                    preferences.setSoundboxActive(false)
                    val total = repository.getTodayTotal().first() ?: 0L
                    val count = repository.getTodayCount().first()
                    updateNotification(total, count, isPaused = true)
                }
            }
            ACTION_RESUME -> {
                serviceScope.launch {
                    preferences.setSoundboxActive(true)
                    val total = repository.getTodayTotal().first() ?: 0L
                    val count = repository.getTodayCount().first()
                    updateNotification(total, count, isPaused = false)
                }
            }
            ACTION_PROCESS_PAYMENT -> {
                val eventJson = intent.getStringExtra(EXTRA_PAYMENT_EVENT)
                if (eventJson != null) {
                    try {
                        val event = kotlinx.serialization.json.Json.decodeFromString<PaymentEvent>(eventJson)
                        onPaymentReceived(event)
                    } catch (_: Exception) {}
                }
            }
        }
        return START_STICKY
    }

    private fun onPaymentReceived(event: PaymentEvent) {
        serviceScope.launch {
            // Check cross-source deduplication (90s window or same UPI ref)
            if (deduplicator.isDuplicate(event)) {
                return@launch
            }

            // Save to encrypted Room database
            repository.recordPayment(event, announced = true)

            // Emit to event bus for real-time UI feed
            SoundboxEventBus.emitPayment(event)

            // Enqueue for sequential voice announcement with audio focus
            announcementQueue.enqueue(AnnouncementRequest.Payment(event))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Soundbox Active Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live UPI soundbox collection status"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(totalPaise: Long, count: Int, isPaused: Boolean): Notification {
        val formattedAmount = AmountParser.formatPaise(totalPaise)
        val contentTitle = if (isPaused) "UPI Soundbox (Paused)" else "UPI Soundbox Listening"
        val contentText = "Today's Collection: $formattedAmount ($count payments)"

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleActionIntent = if (isPaused) {
            PendingIntent.getService(
                this,
                1,
                Intent(this, SoundboxService::class.java).apply { action = ACTION_RESUME },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                this,
                2,
                Intent(this, SoundboxService::class.java).apply { action = ACTION_PAUSE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val toggleActionTitle = if (isPaused) "Resume" else "Pause"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent)
            .addAction(0, toggleActionTitle, toggleActionIntent)
            .build()
    }

    private fun startForegroundWithNotification(totalPaise: Long, count: Int, isPaused: Boolean) {
        val notification = buildNotification(totalPaise, count, isPaused)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(totalPaise: Long, count: Int, isPaused: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(totalPaise, count, isPaused))
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "soundbox_foreground_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PAUSE = "com.upisoundbox.action.PAUSE"
        const val ACTION_RESUME = "com.upisoundbox.action.RESUME"
        const val ACTION_PROCESS_PAYMENT = "com.upisoundbox.action.PROCESS_PAYMENT"
        const val EXTRA_PAYMENT_EVENT = "extra_payment_event"

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, SoundboxService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun handlePayment(context: Context, event: PaymentEvent) {
            val json = kotlinx.serialization.json.Json.encodeToString(PaymentEvent.serializer(), event)
            val intent = Intent(context, SoundboxService::class.java).apply {
                action = ACTION_PROCESS_PAYMENT
                putExtra(EXTRA_PAYMENT_EVENT, json)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (_: Exception) {
                    // If started from background receiver where FGS is restricted, fallback to direct dispatch
                    SoundboxEventBus.emitPayment(event)
                }
            } else {
                context.startService(intent)
            }
        }
    }
}

