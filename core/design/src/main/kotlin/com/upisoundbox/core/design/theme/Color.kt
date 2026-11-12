package com.upisoundbox.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Soundbox color tokens matching the reference emerald fintech design.
 *
 * Light: MintBackground #F7FAF8, Ink #0F172A, Emerald #10B981, MintPill #DCFCE7 / #15803D.
 * Dark:  Obsidian #090D0B, Text #FFFFFF, DarkEmerald #0E1511 / #1B2B21, Neon #34D399.
 */
object SoundboxColors {
    // Light tokens
    val MintBackground = Color(0xFFF7FAF8)
    val MintBackgroundGradientStart = Color(0xFFEBF7F0)
    val CardBackgroundLight = Color(0xFFFFFFFF)
    val CardBorderLight = Color(0xFFE5E7EB)
    val Ink = Color(0xFF0F172A)
    val Muted = Color(0xFF64748B)

    // Primary Emerald Brand
    val EmeraldGreen = Color(0xFF10B981)
    val EmeraldDark = Color(0xFF059669)
    val MintPillBgLight = Color(0xFFDCFCE7)
    val MintPillTextLight = Color(0xFF15803D)

    // Dark tokens
    val BackgroundDark = Color(0xFF090D0B)
    val CardBackgroundDark = Color(0xFF0E1511)
    val CardBorderDark = Color(0xFF1B2B21)
    val TextDark = Color(0xFFFFFFFF)
    val MutedDark = Color(0xFF94A3B8)
    val MintPillBgDark = Color(0xFF102E1E)
    val MintPillTextDark = Color(0xFF34D399)
    val NeonGreen = Color(0xFF34D399)

    // UPI Brand Logos
    val GooglePayBlue = Color(0xFF1A73E8)
    val PhonePePurple = Color(0xFF5F259F)
    val PaytmBlue = Color(0xFF00BAF2)
    val BhimTeal = Color(0xFF00897B)

    // Hero Cards
    val HeroLightGradientStart = Color(0xFFF0FDF4)
    val HeroLightGradientEnd = Color(0xFFE7F7ED)
    val HeroLightBorder = Color(0xFFC6F6D5)

    val HeroDarkGradientStart = Color(0xFF0C2016)
    val HeroDarkGradientEnd = Color(0xFF08170F)
    val HeroDarkBorder = Color(0xFF1B4D34)

    // Legacy Aliases
    val Paper = MintBackground
    val AccentOrange = EmeraldGreen
    val OrangeTint = MintPillBgLight
    val OrangeTintDark = MintPillBgDark
    val BubbleGreyLight = Color(0xFFF1F5F9)
    val BubbleGreyDark = Color(0xFF161F1A)
    val SurfaceDark = CardBackgroundDark

    // Semantic status
    val Success = EmeraldGreen
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
}

@Immutable
data class SoundboxColorScheme(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val emeraldGreen: Color,
    val pillBackground: Color,
    val pillText: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val heroBorder: Color,
    val statIconBg: Color,
    val statIconTint: Color,
    val divider: Color,
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
    background = SoundboxColors.MintBackground,
    surface = SoundboxColors.CardBackgroundLight,
    textPrimary = SoundboxColors.Ink,
    textMuted = SoundboxColors.Muted,
    cardBackground = SoundboxColors.CardBackgroundLight,
    cardBorder = SoundboxColors.CardBorderLight,
    emeraldGreen = SoundboxColors.EmeraldGreen,
    pillBackground = SoundboxColors.MintPillBgLight,
    pillText = SoundboxColors.MintPillTextLight,
    heroGradientStart = SoundboxColors.HeroLightGradientStart,
    heroGradientEnd = SoundboxColors.HeroLightGradientEnd,
    heroBorder = SoundboxColors.HeroLightBorder,
    statIconBg = SoundboxColors.MintPillBgLight,
    statIconTint = SoundboxColors.MintPillTextLight,
    divider = Color(0xFFF1F5F9),
    accentOrange = SoundboxColors.EmeraldGreen,
    orangeTint = SoundboxColors.MintPillBgLight,
    bubbleGrey = SoundboxColors.BubbleGreyLight,
    success = SoundboxColors.EmeraldGreen,
    warning = SoundboxColors.Warning,
    error = SoundboxColors.Error,
    isDark = false
)

val DarkSoundboxColorScheme = SoundboxColorScheme(
    background = SoundboxColors.BackgroundDark,
    surface = SoundboxColors.CardBackgroundDark,
    textPrimary = SoundboxColors.TextDark,
    textMuted = SoundboxColors.MutedDark,
    cardBackground = SoundboxColors.CardBackgroundDark,
    cardBorder = SoundboxColors.CardBorderDark,
    emeraldGreen = SoundboxColors.NeonGreen,
    pillBackground = SoundboxColors.MintPillBgDark,
    pillText = SoundboxColors.MintPillTextDark,
    heroGradientStart = SoundboxColors.HeroDarkGradientStart,
    heroGradientEnd = SoundboxColors.HeroDarkGradientEnd,
    heroBorder = SoundboxColors.HeroDarkBorder,
    statIconBg = SoundboxColors.MintPillBgDark,
    statIconTint = SoundboxColors.NeonGreen,
    divider = Color(0xFF1A251E),
    accentOrange = SoundboxColors.NeonGreen,
    orangeTint = SoundboxColors.MintPillBgDark,
    bubbleGrey = SoundboxColors.BubbleGreyDark,
    success = SoundboxColors.NeonGreen,
    warning = SoundboxColors.Warning,
    error = SoundboxColors.Error,
    isDark = true
)

val LocalSoundboxColors = staticCompositionLocalOf { DarkSoundboxColorScheme }
