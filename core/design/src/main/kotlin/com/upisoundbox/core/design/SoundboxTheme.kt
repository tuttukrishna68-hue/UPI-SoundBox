package com.upisoundbox.core.design

import androidx.compose.runtime.Composable
import com.upisoundbox.core.design.theme.SoundboxTheme as InternalSoundboxTheme

@Composable
fun SoundboxTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    InternalSoundboxTheme(darkTheme = darkTheme, content = content)
}
