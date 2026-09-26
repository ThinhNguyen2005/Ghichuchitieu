package com.notepay.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceSunken: Color,
    val surfaceRaised: Color,
    val secondary: Color,
    val primary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val borderStrong: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val separator: Color,
    val isLight: Boolean
)

val LightAppColors = AppColors(
    background = LedgerLightBackground,
    surface = LedgerLightSurface,
    surfaceSunken = LedgerLightSurfaceSunken,
    surfaceRaised = LedgerLightSurfaceRaised,
    secondary = LedgerSecondary,
    primary = LedgerPrimary,
    textPrimary = LedgerLightTextPrimary,
    textSecondary = LedgerLightTextSecondary,
    textMuted = LedgerLightTextMuted,
    border = LedgerLightBorder,
    borderStrong = LedgerLightBorderStrong,
    success = LedgerIncome,
    warning = LedgerWarning,
    error = LedgerExpense,
    separator = LedgerLightBorder,
    isLight = true
)

val DarkAppColors = AppColors(
    background = LedgerDarkBackground,
    surface = LedgerDarkSurface,
    surfaceSunken = LedgerDarkSurfaceSunken,
    surfaceRaised = LedgerDarkSurfaceRaised,
    secondary = LedgerDarkSecondary,
    primary = LedgerDarkPrimary,
    textPrimary = LedgerDarkTextPrimary,
    textSecondary = LedgerDarkTextSecondary,
    textMuted = LedgerDarkTextMuted,
    border = LedgerDarkBorder,
    borderStrong = LedgerDarkBorderStrong,
    success = LedgerDarkIncome,
    warning = LedgerDarkWarning,
    error = LedgerDarkExpense,
    separator = LedgerDarkBorder,
    isLight = false
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current

    val typography: AppTypography
        @Composable
        get() = LocalAppTypography.current

    val shapes: AppShapes
        @Composable
        get() = LocalAppShapes.current

    val dimensions: AppDimensions
        @Composable
        get() = LocalAppDimensions.current
}
