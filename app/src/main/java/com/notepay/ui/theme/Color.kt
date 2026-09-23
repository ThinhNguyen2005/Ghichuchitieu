package com.notepay.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ==========================================
// iOS Monochrome Design Tokens (DESIGN.md)
// ==========================================
val IosBlack = Color(0xFF000000)
val IosWhite = Color(0xFFFFFFFF)

// Light Mode Neutral Canvas
val IosLightBackground = Color(0xFFF2F2F7)
val IosLightSurface = Color(0xFFFFFFFF)
val IosLightSurfaceSubtle = Color(0xFFF8F8FA)
val IosLightSurfaceContainer = Color(0xFFE5E5EA)
val IosLightBorder = Color(0xFFD1D1D6)
val IosLightSeparator = Color(0x4A3C3C43)
val IosLightTextPrimary = Color(0xFF000000)
val IosLightTextSecondary = Color(0xFF6C6C70)
val IosLightTextMuted = Color(0xFF8E8E93)

// Dark Mode Neutral Canvas
val IosDarkBackground = Color(0xFF000000)
val IosDarkSurface = Color(0xFF1C1C1E)
val IosDarkSurfaceSubtle = Color(0xFF242426)
val IosDarkSurfaceContainer = Color(0xFF2C2C2E)
val IosDarkBorder = Color(0xFF38383A)
val IosDarkSeparator = Color(0x99545458)
val IosDarkTextPrimary = Color(0xFFFFFFFF)
val IosDarkTextSecondary = Color(0xFF8E8E93)
val IosDarkTextMuted = Color(0xFF636366)

// Semantic Tokens (HIG Standard - Only for Financial Cash Flow / Status)
val IosSuccessLight = Color(0xFF34C759)
val IosSuccessDark = Color(0xFF30D158)
val IosExpenseLight = Color(0xFFFF3B30)
val IosExpenseDark = Color(0xFFFF453A)
val IosWarningLight = Color(0xFFFF9F0A)
val IosWarningDark = Color(0xFFFFD60A)

// ==========================================
// Standard iOS Monochrome ColorSchemes
// WCAG AAA Contrast (>= 7:1 for text/background)
// ==========================================
val IosLightColorScheme: ColorScheme = lightColorScheme(
    primary = IosBlack,
    onPrimary = IosWhite,
    primaryContainer = IosLightSurfaceContainer,
    onPrimaryContainer = IosBlack,
    secondary = Color(0xFF3A3A3C),
    onSecondary = IosWhite,
    background = IosLightBackground,
    onBackground = IosBlack,
    surface = IosLightSurface,
    onSurface = IosBlack,
    surfaceVariant = IosLightSurfaceContainer,
    onSurfaceVariant = IosLightTextSecondary,
    surfaceContainer = IosLightSurfaceContainer,
    surfaceContainerHighest = IosLightBorder,
    outline = IosLightTextMuted,
    outlineVariant = IosLightBorder,
    error = IosExpenseLight,
    onError = IosWhite,
)

val IosDarkColorScheme: ColorScheme = darkColorScheme(
    primary = IosWhite,
    onPrimary = IosBlack,
    primaryContainer = IosDarkSurfaceContainer,
    onPrimaryContainer = IosWhite,
    secondary = Color(0xFF8E8E93),
    onSecondary = IosBlack,
    background = IosDarkBackground,
    onBackground = IosWhite,
    surface = IosDarkSurface,
    onSurface = IosWhite,
    surfaceVariant = IosDarkSurfaceContainer,
    onSurfaceVariant = IosDarkTextSecondary,
    surfaceContainer = IosDarkSurfaceContainer,
    surfaceContainerHighest = IosDarkBorder,
    outline = IosDarkTextMuted,
    outlineVariant = IosDarkBorder,
    error = IosExpenseDark,
    onError = IosBlack,
)

internal val LightColors = IosLightColorScheme
internal val DarkColors = IosDarkColorScheme

// ==========================================
// Theme Resolvers (Strict iOS Monochrome & Dynamic)
// ==========================================
fun getLightColorScheme(themeColor: String = "ios", context: Context? = null): ColorScheme {
    if (themeColor == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
        return dynamicLightColorScheme(context)
    }
    return IosLightColorScheme
}

fun getDarkColorScheme(themeColor: String = "ios", context: Context? = null): ColorScheme {
    if (themeColor == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
        return dynamicDarkColorScheme(context)
    }
    return IosDarkColorScheme
}

