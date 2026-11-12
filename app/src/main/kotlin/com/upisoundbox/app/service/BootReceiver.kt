package com.upisoundbox.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // Schedule watchdog immediately
            WatchdogWorker.schedule(context)

            // On Android 14/15, direct startForegroundService from background broadcast receiver
            // throws ForegroundServiceStartNotAllowedException unless scheduled or triggered via WorkManager.
            // We use WorkManager OneTimeWorkRequest as the reliable trampoline.
            val startWork = OneTimeWorkRequestBuilder<WatchdogWorker>().build()
            WorkManager.getInstance(context).enqueue(startWork)

            // The worker owns the restart so Android's background-start limits
            // are respected and the service is not started before onboarding.
        }
    }
}

