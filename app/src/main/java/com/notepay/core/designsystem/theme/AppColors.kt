package com.notepay.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Ngữ nghĩa màu của NotePay.
 *
 * `MaterialTheme.colorScheme` là bề mặt màu lúc chạy (DESIGN.md §10); lớp này bổ sung
 * các ngữ nghĩa riêng của v4 "Sand & Ink" như thẻ ink, màu cát, thanh phân bổ và chuỗi dữ liệu.
 */
@Immutable
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
    val isLight: Boolean,
    // ==== v4 Sand & Ink ====
    val ink: Color,
    val inkRaised: Color,
    val inkBorder: Color,
    val onInk: Color,
    val onInkMuted: Color,
    val onInkIncome: Color,
    val onInkExpense: Color,
    val sand: Color,
    val onSand: Color,
    val graphite: Color,
    val clay: Color,
    val hairline: Color,
    val chartSeries: List<Color>
)

val LightAppColors = AppColors(
    background = SandLightBackground,
    surface = SandLightSurface,
    surfaceSunken = SandLightSurfaceField,
    surfaceRaised = SandLightSurface,
    secondary = SandGraphite,
    primary = SandInk,
    textPrimary = SandLightTextPrimary,
    textSecondary = SandLightTextSecondary,
    textMuted = SandLightTextMuted,
    border = SandLightBorder,
    borderStrong = SandLightBorderStrong,
    success = SandIncome,
    warning = SandWarning,
    error = SandExpense,
    separator = SandLightBorder,
    isLight = true,
    ink = SandInk,
    inkRaised = SandDarkInk,
    inkBorder = Color.Transparent,
    onInk = Color(0xFFFFFFFF),
    onInkMuted = SandLightBorder,
    onInkIncome = SandIncomeOnInk,
    onInkExpense = SandExpenseOnInk,
    sand = Sand,
    onSand = SandOnSand,
    graphite = SandGraphite,
    clay = SandClay,
    hairline = SandLightHairline,
    chartSeries = listOf(Sand, SandGraphite, SandClay, SandSky)
)

val DarkAppColors = AppColors(
    background = SandDarkBackground,
    surface = SandDarkSurface,
    surfaceSunken = SandDarkSurfaceLow,
    surfaceRaised = SandDarkCard,
    secondary = SandDarkSecondary,
    primary = Sand,
    textPrimary = SandDarkTextPrimary,
    textSecondary = SandDarkTextSecondary,
    textMuted = SandDarkTextMuted,
    border = SandDarkBorder,
    borderStrong = SandDarkBorderStrong,
    success = SandDarkIncome,
    warning = SandDarkWarning,
    error = SandDarkExpense,
    separator = SandDarkBorder,
    isLight = false,
    ink = SandDarkInk,
    inkRaised = Color(0xFF303030),
    inkBorder = SandDarkInkBorder,
    onInk = SandDarkTextPrimary,
    onInkMuted = SandDarkTextSecondary,
    onInkIncome = SandDarkIncome,
    onInkExpense = SandDarkExpense,
    sand = Sand,
    onSand = SandOnSand,
    graphite = SandDarkGraphite,
    clay = SandDarkClay,
    hairline = SandDarkHairline,
    chartSeries = listOf(Sand, SandDarkGraphite, SandDarkClay, SandSky)
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
