package com.upisoundbox.core.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SoundboxColors.NeonGreen,
    onPrimary = Color.Black,
    primaryContainer = SoundboxColors.MintPillBgDark,
    onPrimaryContainer = SoundboxColors.NeonGreen,
    secondary = SoundboxColors.EmeraldGreen,
    onSecondary = Color.Black,
    secondaryContainer = SoundboxColors.BubbleGreyDark,
    onSecondaryContainer = SoundboxColors.TextDark,
    background = SoundboxColors.BackgroundDark,
    onBackground = SoundboxColors.TextDark,
    surface = SoundboxColors.CardBackgroundDark,
    onSurface = SoundboxColors.TextDark,
    surfaceVariant = SoundboxColors.BubbleGreyDark,
    onSurfaceVariant = SoundboxColors.MutedDark,
    outline = SoundboxColors.CardBorderDark,
    error = SoundboxColors.Error,
    onError = Color.White
)

@Composable
fun SoundboxTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSoundboxColors provides DarkSoundboxColorScheme,
        LocalSoundboxTypography provides SoundboxTypography,
        LocalSoundboxSpacing provides SoundboxSpacing()
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = SoundboxTypography,
            shapes = MaterialShapes,
            content = content
        )
    }
}

object SoundboxTheme {
    val colors: SoundboxColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalSoundboxColors.current

    val typography: androidx.compose.material3.Typography
        @Composable
        @ReadOnlyComposable
        get() = LocalSoundboxTypography.current

    val spacing: SoundboxSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSoundboxSpacing.current

    val shapes: SoundboxShapes
        get() = SoundboxShapes
}
