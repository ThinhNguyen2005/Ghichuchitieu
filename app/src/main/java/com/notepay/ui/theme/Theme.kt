package com.notepay.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    themeMode: String = "system",
    themeColor: String = "ledger",
    darkTheme: Boolean = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    },
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val materialColorScheme = if (darkTheme) {
        getDarkColorScheme(themeColor, context)
    } else {
        getLightColorScheme(themeColor, context)
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
            val window = (view.context as? Activity)?.window ?: return@SideEffect

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
