package com.upisoundbox.app

import android.app.Application
import com.upisoundbox.app.service.WatchdogWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SoundboxApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("sqlcipher")
        } catch (_: Throwable) {}

        if (BuildConfig.DEBUG) {
            Timber.plant(RedactingDebugTree())
        }
        // Schedule periodic 15-min watchdog
        try {
            WatchdogWorker.schedule(this)
        } catch (_: Throwable) {}
    }

    /**
     * Timber tree that redacts customer names in log output per privacy rules.
     */
    private class RedactingDebugTree : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val redacted = message.replace(Regex("(?i)(from|by)\\s+([A-Za-z\\s]{3,20})"), "$1 [REDACTED]")
            super.log(priority, tag, redacted, t)
        }
    }

    companion object {
        const val APP_NAME = "UPI Soundbox"
    }
}
