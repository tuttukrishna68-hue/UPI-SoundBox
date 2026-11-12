package com.upisoundbox.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
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

        try {
            com.upisoundbox.app.service.SoundboxService.start(this)
        } catch (_: Exception) {}

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
            SoundboxTheme {
                val navController = rememberNavController()
                SoundboxNavGraph(
                    navController = navController,
                    repository = repository,
                    preferences = preferences,
                    announcementQueue = announcementQueue,
                    announcementEngine = announcementEngine,
                    onRequestBiometricConfirmation = { onSuccess ->
                        showBiometricPromptForSecurityChange(onSuccess)
                    }
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
        showBiometricPrompt(
            title = "Unlock UPI Soundbox",
            subtitle = "Authenticate to view your merchant payment ledger",
            onSuccess = { isAppLocked = true },
            onError = { errorCode, _ ->
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    finish()
                }
            }
        )
    }

    private fun showBiometricPromptForSecurityChange(onSuccess: () -> Unit) {
        showBiometricPrompt(
            title = "Confirm security change",
            subtitle = "Verify your fingerprint or device PIN to change this lock",
            onSuccess = onSuccess,
            onError = { _, _ ->
                // A cancelled or failed confirmation leaves the existing setting intact.
            }
        )
    }

    private fun showBiometricPrompt(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, message: CharSequence) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString)
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        try {
            prompt.authenticate(promptInfo)
        } catch (_: Exception) {}
    }
}
