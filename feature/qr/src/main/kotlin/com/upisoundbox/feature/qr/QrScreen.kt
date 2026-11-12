package com.upisoundbox.feature.qr

import androidx.compose.runtime.Composable
import com.upisoundbox.core.data.preferences.SoundboxPreferences

@Composable
fun QrScreen(
    preferences: SoundboxPreferences,
    onBack: () -> Unit = {}
) {
    MyQrScreen(
        preferences = preferences,
        onNavigateBack = onBack
    )
}
