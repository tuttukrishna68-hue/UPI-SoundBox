package com.upisoundbox.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.upisoundbox.app.MainActivity
import java.util.concurrent.TimeUnit

class WatchdogWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val issues = mutableListOf<String>()

        // 1. Notification access enabled
        val isNotificationAccessEnabled = NotificationManagerCompat
            .getEnabledListenerPackages(context)
            .contains(context.packageName)
        if (!isNotificationAccessEnabled) {
            issues.add("Notification access is disabled")
        }

        // 2. Notification listener connected
        if (!NotificationSource.isListening && isNotificationAccessEnabled) {
            issues.add("Listener service disconnected")
        }

        // 3. Soundbox foreground service running
        if (!SoundboxService.isRunning) {
            // Attempt auto-restart
            try {
                SoundboxService.start(context)
            } catch (_: Exception) {
                issues.add("Soundbox background service is stopped")
            }
        }

        // 4. Battery optimization exemption
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                issues.add("Battery optimization is active")
            }
        }

        // 5. App notifications not blocked
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            issues.add("App notifications are blocked")
        }

        // Post high-priority alert if critical issues are found
        if (issues.isNotEmpty()) {
            postAlertNotification(issues.first())
        }

        return Result.success()
    }

    private fun postAlertNotification(issueSummary: String) {
        val channelId = "soundbox_watchdog_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reliability Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when Soundbox service encounters issues"
            }
            manager.createNotificationChannel(channel)
        }

        val fixIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                putExtra("navigate_to", "wizard")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alert = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Soundbox is not listening!")
            .setContentText("Tap to fix: $issueSummary")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(fixIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(2001, alert)
    }

    companion object {
        private const val WORK_NAME = "SoundboxWatchdogWork"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

