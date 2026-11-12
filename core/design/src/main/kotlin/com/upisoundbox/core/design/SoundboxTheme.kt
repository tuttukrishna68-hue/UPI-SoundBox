package com.upisoundbox.core.design

import androidx.compose.runtime.Composable
import com.upisoundbox.core.design.theme.SoundboxTheme as InternalSoundboxTheme

@Composable
fun SoundboxTheme(
    content: @Composable () -> Unit
) {
    InternalSoundboxTheme(content = content)
}
