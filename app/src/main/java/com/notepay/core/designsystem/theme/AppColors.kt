package com.notepay.core.designsystem.theme

import androidx.compose.material3.ColorScheme
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
    separator = Color(0xFFE0E0E0),
    isLight = true,
    ink = SandInk,
    inkRaised = SandDarkInk,
    inkBorder = Color.Transparent,
    onInk = Color(0xFFFFFFFF),
    onInkMuted = Color(0x99FFFFFF),
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
    separator = Color(0xFF2A2A2A),
    isLight = false,
    ink = SandDarkInk,
    inkRaised = Color(0xFF303030),
    inkBorder = SandDarkInkBorder,
    onInk = SandDarkTextPrimary,
    onInkMuted = Color(0x99FFFFFF),
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

fun resolveAppColors(
    themeColor: String,
    darkTheme: Boolean,
    materialColorScheme: ColorScheme,
): AppColors = when (themeColor) {
    "emerald" -> emeraldAppColors(darkTheme)
    "ocean" -> oceanAppColors(darkTheme)
    "lavender" -> lavenderAppColors(darkTheme)
    "ios" -> iosAppColors(darkTheme)
    "dynamic" -> dynamicAppColors(darkTheme, materialColorScheme)
    else -> if (darkTheme) DarkAppColors else LightAppColors
}

private fun emeraldAppColors(darkTheme: Boolean): AppColors = if (darkTheme) {
    DarkAppColors.copy(
        background = EmeraldDarkBg,
        surface = EmeraldDarkSurface,
        surfaceSunken = EmeraldDarkLow,
        surfaceRaised = EmeraldDarkCard,
        secondary = EmeraldDarkTextSecondary,
        primary = EmeraldDarkAccent,
        textPrimary = EmeraldDarkText,
        textSecondary = EmeraldDarkTextSecondary,
        border = EmeraldDarkBorder,
        ink = EmeraldDarkInk,
        inkRaised = EmeraldDarkCard,
        inkBorder = EmeraldDarkBorder,
        onInk = EmeraldDarkText,
        sand = EmeraldDarkAccent,
        onSand = EmeraldDarkOnAccent,
        chartSeries = listOf(EmeraldDarkAccent, EmeraldSecondary, SandSky, SandDarkClay),
    )
} else {
    LightAppColors.copy(
        background = EmeraldLightBg,
        surface = EmeraldLightSurface,
        surfaceSunken = EmeraldLightField,
        surfaceRaised = EmeraldLightSurface,
        secondary = EmeraldSecondary,
        primary = EmeraldInk,
        border = EmeraldLightBorder,
        ink = EmeraldInk,
        inkRaised = EmeraldDarkInk,
        onInk = EmeraldOnInk,
        sand = EmeraldAccentLight,
        onSand = EmeraldOnAccentLight,
        chartSeries = listOf(EmeraldInk, EmeraldAccentLight, EmeraldSecondary, SandSky),
    )
}

private fun oceanAppColors(darkTheme: Boolean): AppColors = if (darkTheme) {
    DarkAppColors.copy(
        background = OceanDarkBg,
        surface = OceanDarkSurface,
        surfaceSunken = OceanDarkLow,
        surfaceRaised = OceanDarkCard,
        secondary = OceanDarkTextSecondary,
        primary = OceanDarkAccent,
        textPrimary = OceanDarkText,
        textSecondary = OceanDarkTextSecondary,
        border = OceanDarkBorder,
        ink = OceanDarkInk,
        inkRaised = OceanDarkCard,
        inkBorder = OceanDarkBorder,
        onInk = OceanDarkText,
        sand = OceanDarkAccent,
        onSand = OceanDarkOnAccent,
        chartSeries = listOf(OceanDarkAccent, OceanSecondary, SandSky, EmeraldDarkAccent),
    )
} else {
    LightAppColors.copy(
        background = OceanLightBg,
        surface = OceanLightSurface,
        surfaceSunken = OceanLightField,
        surfaceRaised = OceanLightSurface,
        secondary = OceanSecondary,
        primary = OceanInk,
        border = OceanLightBorder,
        ink = OceanInk,
        inkRaised = OceanDarkInk,
        onInk = OceanOnInk,
        sand = OceanAccentLight,
        onSand = OceanOnAccentLight,
        chartSeries = listOf(OceanInk, OceanAccentLight, OceanSecondary, EmeraldSecondary),
    )
}

private fun lavenderAppColors(darkTheme: Boolean): AppColors = if (darkTheme) {
    DarkAppColors.copy(
        background = LavenderDarkBg,
        surface = LavenderDarkSurface,
        surfaceSunken = LavenderDarkLow,
        surfaceRaised = LavenderDarkCard,
        secondary = LavenderDarkTextSecondary,
        primary = LavenderDarkAccent,
        textPrimary = LavenderDarkText,
        textSecondary = LavenderDarkTextSecondary,
        border = LavenderDarkBorder,
        ink = LavenderDarkInk,
        inkRaised = LavenderDarkCard,
        inkBorder = LavenderDarkBorder,
        onInk = LavenderDarkText,
        sand = LavenderDarkAccent,
        onSand = LavenderDarkOnAccent,
        chartSeries = listOf(LavenderDarkAccent, LavenderSecondary, OceanDarkAccent, SandDarkClay),
    )
} else {
    LightAppColors.copy(
        background = LavenderLightBg,
        surface = LavenderLightSurface,
        surfaceSunken = LavenderLightField,
        surfaceRaised = LavenderLightSurface,
        secondary = LavenderSecondary,
        primary = LavenderInk,
        border = LavenderLightBorder,
        ink = LavenderInk,
        inkRaised = LavenderDarkInk,
        onInk = LavenderOnInk,
        sand = LavenderAccentLight,
        onSand = LavenderOnAccentLight,
        chartSeries = listOf(LavenderInk, LavenderAccentLight, LavenderSecondary, SandSky),
    )
}

private fun iosAppColors(darkTheme: Boolean): AppColors = if (darkTheme) {
    DarkAppColors.copy(
        background = IosDarkBackground,
        surface = IosDarkSurface,
        surfaceSunken = IosDarkBackground,
        surfaceRaised = IosDarkSurfaceContainer,
        secondary = IosDarkTextSecondary,
        primary = IosWhite,
        textPrimary = IosWhite,
        textSecondary = IosDarkTextSecondary,
        textMuted = IosDarkTextMuted,
        border = IosDarkBorder,
        ink = IosDarkSurfaceContainer,
        inkRaised = IosDarkBorder,
        inkBorder = IosDarkBorder,
        onInk = IosWhite,
        sand = IosWhite,
        onSand = IosBlack,
        chartSeries = listOf(IosWhite, IosDarkTextSecondary, IosSuccessDark, IosDarkTextMuted),
    )
} else {
    LightAppColors.copy(
        background = IosLightBackground,
        surface = IosLightSurface,
        surfaceSunken = IosLightSurfaceContainer,
        surfaceRaised = IosLightSurface,
        secondary = IosLightTextSecondary,
        primary = IosBlack,
        textPrimary = IosBlack,
        textSecondary = IosLightTextSecondary,
        textMuted = IosLightTextMuted,
        border = IosLightBorder,
        ink = IosBlack,
        inkRaised = IosDarkSurfaceContainer,
        onInk = IosWhite,
        sand = IosLightSurfaceContainer,
        onSand = IosBlack,
        chartSeries = listOf(IosBlack, IosLightTextSecondary, IosSuccessLight, IosLightTextMuted),
    )
}

private fun dynamicAppColors(
    darkTheme: Boolean,
    materialColorScheme: ColorScheme,
): AppColors {
    val base = if (darkTheme) DarkAppColors else LightAppColors
    return base.copy(
        background = materialColorScheme.background,
        surface = materialColorScheme.surface,
        surfaceSunken = materialColorScheme.surfaceContainerLow,
        surfaceRaised = materialColorScheme.surfaceContainerHigh,
        primary = materialColorScheme.primary,
        secondary = materialColorScheme.secondary,
        border = materialColorScheme.outlineVariant,
        ink = materialColorScheme.primary,
        onInk = materialColorScheme.onPrimary,
        sand = materialColorScheme.primaryContainer,
        onSand = materialColorScheme.onPrimaryContainer,
        chartSeries = listOf(
            materialColorScheme.primary,
            materialColorScheme.secondary,
            materialColorScheme.tertiary,
            materialColorScheme.primaryContainer,
        ),
    )
}

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
