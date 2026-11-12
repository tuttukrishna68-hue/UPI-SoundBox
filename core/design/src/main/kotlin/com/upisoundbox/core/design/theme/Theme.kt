package com.upisoundbox.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SoundboxColors.EmeraldGreen,
    onPrimary = Color.White,
    primaryContainer = SoundboxColors.MintPillBgLight,
    onPrimaryContainer = SoundboxColors.MintPillTextLight,
    secondary = SoundboxColors.EmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = SoundboxColors.BubbleGreyLight,
    onSecondaryContainer = SoundboxColors.Ink,
    background = SoundboxColors.MintBackground,
    onBackground = SoundboxColors.Ink,
    surface = Color.White,
    onSurface = SoundboxColors.Ink,
    surfaceVariant = SoundboxColors.BubbleGreyLight,
    onSurfaceVariant = SoundboxColors.Muted,
    outline = SoundboxColors.CardBorderLight,
    error = SoundboxColors.Error,
    onError = Color.White
)

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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val soundboxColors = if (darkTheme) DarkSoundboxColorScheme else LightSoundboxColorScheme
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalSoundboxColors provides soundboxColors,
        LocalSoundboxTypography provides SoundboxTypography,
        LocalSoundboxSpacing provides SoundboxSpacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
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
