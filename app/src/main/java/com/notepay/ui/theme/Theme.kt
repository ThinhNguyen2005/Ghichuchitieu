package com.notepay.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider

object ThemeManager {
    var currentThemeColor by mutableStateOf("green")

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("notepay_settings", Context.MODE_PRIVATE)
        currentThemeColor = prefs.getString("theme_color", "green") ?: "green"
    }

    fun updateThemeColor(context: Context, color: String) {
        currentThemeColor = color
        val prefs = context.getSharedPreferences("notepay_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("theme_color", color).apply()
    }
}

fun getLightColorScheme(themeColor: String, context: Context? = null): ColorScheme {
    if (themeColor == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
        return dynamicLightColorScheme(context)
    }
    return when (themeColor) {
        "ios" -> LightColors.copy(
            primary = Color(0xFF000000),
            primaryContainer = Color(0xFFE5E5EA),
            onPrimaryContainer = Color(0xFF000000),
            surface = Color(0xFFFFFFFF),
            background = Color(0xFFF2F2F7),
            surfaceContainer = Color(0xFFE5E5EA),
            outlineVariant = Color(0xFFD1D1D6),
        )
        "blue" -> LightColors.copy(
            primary = Color(0xFF1976D2),
            primaryContainer = Color(0xFFD1E4FF),
            onPrimaryContainer = Color(0xFF001D36),
            surface = Color(0xFFF3F6FA),
            background = Color(0xFFF8FAFC),
            surfaceContainer = Color(0xFFE8EEF5),
            outlineVariant = Color(0xFFD9E2EC),
        )
        "red" -> LightColors.copy(
            primary = Color(0xFFC2185B),
            primaryContainer = Color(0xFFFFD9E2),
            onPrimaryContainer = Color(0xFF3F0015),
            surface = Color(0xFFFAF3F5),
            background = Color(0xFFFCF8F9),
            surfaceContainer = Color(0xFFF5E7EB),
            outlineVariant = Color(0xFFE5D5D9),
        )
        "orange" -> LightColors.copy(
            primary = Color(0xFFE65100),
            primaryContainer = Color(0xFFFFDBCF),
            onPrimaryContainer = Color(0xFF341100),
            surface = Color(0xFFFAF4F2),
            background = Color(0xFFFCF9F7),
            surfaceContainer = Color(0xFFF5E8E3),
            outlineVariant = Color(0xFFE8D7D0),
        )
        "teal" -> LightColors.copy(
            primary = Color(0xFF00796B),
            primaryContainer = Color(0xFFCCEBE7),
            onPrimaryContainer = Color(0xFF00201B),
            surface = Color(0xFFF2FAF8),
            background = Color(0xFFF7FCFC),
            surfaceContainer = Color(0xFFE3F5F2),
            outlineVariant = Color(0xFFD0E5E1),
        )
        "gold" -> LightColors.copy(
            primary = Color(0xFF8A6600),
            primaryContainer = Color(0xFFFFE19F),
            onPrimaryContainer = Color(0xFF2B1F00),
            surface = Color(0xFFFAF9F2),
            background = Color(0xFFFCFCF7),
            surfaceContainer = Color(0xFFF5F3E3),
            outlineVariant = Color(0xFFE6E2CE),
        )
        "brown" -> LightColors.copy(
            primary = Color(0xFF8D4F38),
            primaryContainer = Color(0xFFFFDBD0),
            onPrimaryContainer = Color(0xFF370B00),
            surface = Color(0xFFFAF4F2),
            background = Color(0xFFFCF8F7),
            surfaceContainer = Color(0xFFF5E8E4),
            outlineVariant = Color(0xFFE8D6D1),
        )
        "gray" -> LightColors.copy(
            primary = Color(0xFF566066),
            primaryContainer = Color(0xFFDAE5EC),
            onPrimaryContainer = Color(0xFF131D22),
            surface = Color(0xFFF4F6F7),
            background = Color(0xFFF8FAFB),
            surfaceContainer = Color(0xFFEAECEE),
            outlineVariant = Color(0xFFDDE1E3),
        )
        else -> LightColors.copy(
            primary = Color(0xFF1B7F4F),
            primaryContainer = Color(0xFFB6F2CE),
            onPrimaryContainer = Color(0xFF002113),
            surface = Color(0xFFF2FAF5),
            background = Color(0xFFF7FCFA),
            surfaceContainer = Color(0xFFE3F5EB),
            outlineVariant = Color(0xFFD0E5D7),
        )
    }
}

fun getDarkColorScheme(themeColor: String, context: Context? = null): ColorScheme {
    if (themeColor == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
        return dynamicDarkColorScheme(context)
    }
    return when (themeColor) {
        "ios" -> DarkColors.copy(
            primary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF2C2C2E),
            onPrimaryContainer = Color(0xFFFFFFFF),
            surface = Color(0xFF1C1C1E),
            background = Color(0xFF000000),
            surfaceContainer = Color(0xFF2C2C2E),
            outlineVariant = Color(0xFF3A3A3C),
        )
        "blue" -> DarkColors.copy(
            primary = Color(0xFF90CAF9),
            primaryContainer = Color(0xFF004881),
            onPrimaryContainer = Color(0xFFD1E4FF),
            surface = Color(0xFF0F172A),
            background = Color(0xFF0B0F19),
            surfaceContainer = Color(0xFF1E293B),
            outlineVariant = Color(0xFF334155),
        )
        "red" -> DarkColors.copy(
            primary = Color(0xFFF48FB1),
            primaryContainer = Color(0xFF8C003B),
            onPrimaryContainer = Color(0xFFFFD9E2),
            surface = Color(0xFF240E14),
            background = Color(0xFF1A0A0F),
            surfaceContainer = Color(0xFF3B1622),
            outlineVariant = Color(0xFF5C2630),
        )
        "orange" -> DarkColors.copy(
            primary = Color(0xFFFFB74D),
            primaryContainer = Color(0xFF8B2C00),
            onPrimaryContainer = Color(0xFFFFDBCF),
            surface = Color(0xFF24140E),
            background = Color(0xFF1A0E0A),
            surfaceContainer = Color(0xFF3B1F16),
            outlineVariant = Color(0xFF5C2D26),
        )
        "teal" -> DarkColors.copy(
            primary = Color(0xFF80CBC4),
            primaryContainer = Color(0xFF005047),
            onPrimaryContainer = Color(0xFFCCEBE7),
            surface = Color(0xFF0E2420),
            background = Color(0xFF0A1A17),
            surfaceContainer = Color(0xFF163B35),
            outlineVariant = Color(0xFF265C50),
        )
        "gold" -> DarkColors.copy(
            primary = Color(0xFFF3C244),
            primaryContainer = Color(0xFF5A4200),
            onPrimaryContainer = Color(0xFFFFE19F),
            surface = Color(0xFF24200E),
            background = Color(0xFF1A170A),
            surfaceContainer = Color(0xFF3B3516),
            outlineVariant = Color(0xFF5C5426),
        )
        "brown" -> DarkColors.copy(
            primary = Color(0xFFFFB59C),
            primaryContainer = Color(0xFF703823),
            onPrimaryContainer = Color(0xFFFFDBD0),
            surface = Color(0xFF24140E),
            background = Color(0xFF1A0E0A),
            surfaceContainer = Color(0xFF3B1E16),
            outlineVariant = Color(0xFF5C2F26),
        )
        "gray" -> DarkColors.copy(
            primary = Color(0xFFBECAFF),
            primaryContainer = Color(0xFF3E484D),
            onPrimaryContainer = Color(0xFFDAE5EC),
            surface = Color(0xFF1F2937),
            background = Color(0xFF111827),
            surfaceContainer = Color(0xFF374151),
            outlineVariant = Color(0xFF4B5563),
        )
        else -> DarkColors.copy(
            primary = Color(0xFF9BD6B0),
            primaryContainer = Color(0xFF005233),
            onPrimaryContainer = Color(0xFFB6F2CE),
            surface = Color(0xFF0E2419),
            background = Color(0xFF0A1A12),
            surfaceContainer = Color(0xFF163B29),
            outlineVariant = Color(0xFF265C3E),
        )
    }
}

@Composable
fun NotePayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val currentTheme = ThemeManager.currentThemeColor

    val materialColorScheme = if (darkTheme) {
        getDarkColorScheme(currentTheme, context)
    } else {
        getLightColorScheme(currentTheme, context)
    }

    val baseAppColors = if (darkTheme) DarkAppColors else LightAppColors
    val appColors = baseAppColors.copy(
        primary = materialColorScheme.primary,
        secondary = materialColorScheme.secondary,
        background = materialColorScheme.background,
        surface = materialColorScheme.surface
    )

    val appTypography = DefaultAppTypography
    val appShapes = DefaultAppShapes
    val appDimensions = DefaultAppDimensions

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalAppTypography provides appTypography,
        LocalAppShapes provides appShapes,
        LocalAppDimensions provides appDimensions
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = NotePayTypography,
            content = content,
        )
    }
}
