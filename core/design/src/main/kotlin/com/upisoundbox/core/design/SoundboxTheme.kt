package com.upisoundbox.core.design

import androidx.compose.runtime.Composable
import com.upisoundbox.core.design.theme.SoundboxTheme as InternalSoundboxTheme

@Composable
fun SoundboxTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    InternalSoundboxTheme(darkTheme = darkTheme, content = content)
}
