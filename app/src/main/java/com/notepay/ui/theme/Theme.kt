package com.notepay.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.edit
import androidx.core.view.WindowCompat

/**
 * NotePay Theme Controller
 * Quản lý trạng thái theme (iOS Monochrome, Dynamic, Presets),
 * cấu hình thanh trạng thái (System Insets) và phân phối Theme tokens.
 * Toàn bộ định nghĩa màu sắc và ColorScheme được tổ chức tại Color.kt.
 */
object ThemeManager {
    var currentThemeColor by mutableStateOf("ios")
    var themeMode by mutableStateOf("system")

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("notepay_settings", Context.MODE_PRIVATE)
        currentThemeColor = prefs.getString("theme_color", "ios") ?: "ios"
        themeMode = prefs.getString("theme_mode", "system") ?: "system"
    }

    fun updateThemeColor(context: Context, color: String) {
        currentThemeColor = color
        val prefs = context.getSharedPreferences("notepay_settings", Context.MODE_PRIVATE)
        prefs.edit { putString("theme_color", color) }
    }

    fun updateThemeMode(context: Context, mode: String) {
        themeMode = mode
        val prefs = context.getSharedPreferences("notepay_settings", Context.MODE_PRIVATE)
        prefs.edit { putString("theme_mode", mode) }
    }
}

val LocalDarkTheme = staticCompositionLocalOf { false }

/**
 * Tiện ích kiểm tra xem giao diện hiện tại của ứng dụng có đang là Dark Theme hay không.
 * Luôn tôn trọng cài đặt Theme của ứng dụng (Sáng / Tối / Hệ thống), khắc phục triệt để lỗi
 * khi hệ thống OS là Dark nhưng người dùng chọn giao diện Sáng trong ứng dụng.
 */
@Composable
fun isAppDarkTheme(): Boolean = LocalDarkTheme.current

@Composable
fun NotePayTheme(
    darkTheme: Boolean = when (ThemeManager.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    },
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
                ?: return@SideEffect

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppColors provides appColors,
        LocalAppTypography provides appTypography,
        LocalAppShapes provides appShapes,
        LocalAppDimensions provides appDimensions
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = NotePayTypography,
            shapes = NotePayShapes,
            content = content,
        )
    }
}
