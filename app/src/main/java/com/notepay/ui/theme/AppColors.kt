package com.notepay.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color,
    val surface: Color,
    val secondary: Color,
    val primary: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val separator: Color,
    val isLight: Boolean
)

val LightAppColors = AppColors(
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    secondary = Color(0xFFF8F8FA),
    primary = Color(0xFF0A84FF),
    success = Color(0xFF30D158),
    warning = Color(0xFFFFD60A),
    error = Color(0xFFFF453A),
    separator = Color(0x4A3C3C43), // #3C3C434A in RGBA maps to 0x4A3C3C43 in ARGB
    isLight = true
)

val DarkAppColors = AppColors(
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    secondary = Color(0xFF2C2C2E),
    primary = Color(0xFF0A84FF),
    success = Color(0xFF30D158),
    warning = Color(0xFFFFD60A),
    error = Color(0xFFFF453A),
    separator = Color(0x99545458), // #54545899 in RGBA maps to 0x99545458 in ARGB
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
