package com.upisoundbox.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.upisoundbox.app.navigation.SoundboxNavGraph
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.design.SoundboxTheme
import com.upisoundbox.core.tts.AnnouncementEngine
import com.upisoundbox.core.tts.AnnouncementQueue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var repository: PaymentRepository

    @Inject
    lateinit var preferences: SoundboxPreferences

    @Inject
    lateinit var announcementQueue: AnnouncementQueue

    @Inject
    lateinit var announcementEngine: AnnouncementEngine

    private var isAppLocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Observe FLAG_SECURE (anti-screenshot) setting
        lifecycleScope.launch {
            preferences.isSecureScreenEnabled.collect { isSecure ->
                if (isSecure) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        setContent {
            val themeMode by preferences.themeMode.collectAsState(initial = "dark")
            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode.lowercase()) {
                "light" -> false
                "system" -> isSystemDark
                else -> true // Dark theme default
            }

            SoundboxTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                SoundboxNavGraph(
                    navController = navController,
                    repository = repository,
                    preferences = preferences,
                    announcementQueue = announcementQueue,
                    announcementEngine = announcementEngine
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAppLock()
    }

    private fun checkAppLock() {
        lifecycleScope.launch {
            val lockEnabled = preferences.isAppLockEnabled.first()
            if (lockEnabled && !isAppLocked) {
                showBiometricPrompt()
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isAppLocked = true
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    finish()
                }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock UPI Soundbox")
            .setSubtitle("Authenticate to view your merchant payment ledger")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        try {
            prompt.authenticate(promptInfo)
        } catch (_: Exception) {}
    }
}
