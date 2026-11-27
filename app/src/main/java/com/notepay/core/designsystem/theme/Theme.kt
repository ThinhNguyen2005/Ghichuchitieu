package com.notepay.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.notepay.core.platform.LocaleHelper

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
    themeColor: String = "sand",
    darkTheme: Boolean = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    },
    appLanguage: String = LocaleHelper.LANG_SYSTEM,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val context = remember(baseContext, appLanguage) {
        LocaleHelper.localizedContext(baseContext, appLanguage)
    }

    // remember: dynamic color scheme cấp phát đối tượng mới mỗi lần gọi, không được chạy lại mỗi recomposition.
    val materialColorScheme = remember(darkTheme, themeColor, context) {
        if (darkTheme) {
            getDarkColorScheme(themeColor, context)
        } else {
            getLightColorScheme(themeColor, context)
        }
    }

    val baseAppColors = remember(darkTheme, themeColor, materialColorScheme) {
        resolveAppColors(themeColor, darkTheme, materialColorScheme)
    }
    val animationSpec: FiniteAnimationSpec<androidx.compose.ui.graphics.Color> =
        if (reduceMotion) snap() else tween(durationMillis = 250)
    val animatedBackground by animateColorAsState(
        baseAppColors.background,
        animationSpec = animationSpec,
        label = "theme-background",
    )
    val animatedSurface by animateColorAsState(
        baseAppColors.surface,
        animationSpec = animationSpec,
        label = "theme-surface",
    )
    val animatedSurfaceSunken by animateColorAsState(
        baseAppColors.surfaceSunken,
        animationSpec = animationSpec,
        label = "theme-surface-sunken",
    )
    val animatedSurfaceContainer by animateColorAsState(
        materialColorScheme.surfaceContainer,
        animationSpec = animationSpec,
        label = "theme-surface-container",
    )
    val animatedPrimary by animateColorAsState(
        baseAppColors.primary,
        animationSpec = animationSpec,
        label = "theme-primary",
    )
    val animatedInk by animateColorAsState(
        baseAppColors.ink,
        animationSpec = animationSpec,
        label = "theme-ink",
    )
    val animatedOnInk by animateColorAsState(
        baseAppColors.onInk,
        animationSpec = animationSpec,
        label = "theme-on-ink",
    )
    val animatedSand by animateColorAsState(
        baseAppColors.sand,
        animationSpec = animationSpec,
        label = "theme-sand",
    )
    val animatedOnSand by animateColorAsState(
        baseAppColors.onSand,
        animationSpec = animationSpec,
        label = "theme-on-sand",
    )
    val animatedColorScheme = materialColorScheme.copy(
        primary = animatedPrimary,
        background = animatedBackground,
        surface = animatedSurface,
        surfaceContainer = animatedSurfaceContainer,
    )
    val appColors = baseAppColors.copy(
        primary = animatedPrimary,
        background = animatedBackground,
        surface = animatedSurface,
        surfaceSunken = animatedSurfaceSunken,
        ink = animatedInk,
        onInk = animatedOnInk,
        sand = animatedSand,
        onSand = animatedOnSand,
    )

    val appTypography = DefaultAppTypography
    val appShapes = DefaultAppShapes
    val appDimensions = DefaultAppDimensions

    CompositionLocalProvider(
        LocalContext provides context,
        LocalConfiguration provides context.resources.configuration,
    ) {
        CompositionLocalProvider(
            LocalDarkTheme provides darkTheme,
            LocalAppColors provides appColors,
            LocalAppTypography provides appTypography,
            LocalAppShapes provides appShapes,
            LocalAppDimensions provides appDimensions
        ) {
            MaterialTheme(
                colorScheme = animatedColorScheme,
                typography = NotePayTypography,
                shapes = NotePayShapes,
                content = content,
            )
        }
    }
}
