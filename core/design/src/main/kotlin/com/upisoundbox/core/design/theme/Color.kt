package com.upisoundbox.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Soundbox color tokens.
 *
 * Light: Paper #FAFAF8, Ink #141414, Accent Orange #F25C2E, Orange tint #FCE3D8,
 *        Bubble grey #E8E8EA, Muted text #8A8A8F, Success #1F9D55.
 * Dark:  Background #0F0F10, Surface #1B1B1D, Text #F5F5F2, same orange accent,
 *        bubble grey #2A2A2E, muted #8A8A8F, success #1F9D55.
 */
object SoundboxColors {
    // Light tokens
    val Paper = Color(0xFFFAFAF8)
    val Ink = Color(0xFF141414)
    val AccentOrange = Color(0xFFF25C2E)
    val OrangeTint = Color(0xFFFCE3D8)
    val BubbleGreyLight = Color(0xFFE8E8EA)
    val Muted = Color(0xFF8A8A8F)
    val Success = Color(0xFF1F9D55)

    // Dark tokens
    val BackgroundDark = Color(0xFF0F0F10)
    val SurfaceDark = Color(0xFF1B1B1D)
    val TextDark = Color(0xFFF5F5F2)
    val OrangeTintDark = Color(0xFF38231A)
    val BubbleGreyDark = Color(0xFF2A2A2E)

    // Semantic status
    val Warning = Color(0xFFE67E22)
    val Error = Color(0xFFD32F2F)
}

@Immutable
data class SoundboxColorScheme(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val accentOrange: Color,
    val orangeTint: Color,
    val bubbleGrey: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val isDark: Boolean,
    val surfaceVariant: Color = bubbleGrey
) {
    val paper: Color get() = background
    val ink: Color get() = textPrimary
    val muted: Color get() = textMuted
}

val LightSoundboxColorScheme = SoundboxColorScheme(
    background = SoundboxColors.Paper,
    surface = Color.White,
    textPrimary = SoundboxColors.Ink,
    textMuted = SoundboxColors.Muted,
    accentOrange = SoundboxColors.AccentOrange,
    orangeTint = SoundboxColors.OrangeTint,
    bubbleGrey = SoundboxColors.BubbleGreyLight,
    success = SoundboxColors.Success,
    warning = SoundboxColors.Warning,
    error = SoundboxColors.Error,
    isDark = false
)

val DarkSoundboxColorScheme = SoundboxColorScheme(
    background = SoundboxColors.BackgroundDark,
    surface = SoundboxColors.SurfaceDark,
    textPrimary = SoundboxColors.TextDark,
    textMuted = SoundboxColors.Muted,
    accentOrange = SoundboxColors.AccentOrange,
    orangeTint = SoundboxColors.OrangeTintDark,
    bubbleGrey = SoundboxColors.BubbleGreyDark,
    success = SoundboxColors.Success,
    warning = SoundboxColors.Warning,
    error = SoundboxColors.Error,
    isDark = true
)

val LocalSoundboxColors = staticCompositionLocalOf { LightSoundboxColorScheme }
