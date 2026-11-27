package com.notepay.core.navigation

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object NotePayMotion {
    const val tabIndicatorDurationMillis = 200
    const val contentDurationMillis = 200
    const val contentFadeOutDurationMillis = 200
    const val contentFadeDelayMillis = 0
    val contentTranslation: Dp = 12.dp
}

/** Easing chuẩn Material 3 cho phần tử xuất hiện trên màn hình. */
internal val NotePayEmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

/** Cài đặt bật/tắt phản hồi rung toàn cục, được cung cấp từ [NotePayNavHost]. */
internal val LocalHapticFeedbackEnabled = compositionLocalOf { true }

/**
 * Helper kích hoạt Haptic thống nhất toàn ứng dụng.
 * Luôn kiểm tra cài đặt [LocalHapticFeedbackEnabled] trước khi phát rung.
 */
@Composable
internal fun rememberNotePayHaptic(): (HapticFeedbackType) -> Unit {
    val haptic = LocalHapticFeedback.current
    val enabled = LocalHapticFeedbackEnabled.current
    return remember(haptic, enabled) {
        { type ->
            if (enabled) {
                haptic.performHapticFeedback(type)
            }
        }
    }
}

/**
 * Rung ngang nhẹ trên trục X kèm phản hồi xúc giác mỗi khi [trigger] tăng (> 0).
 * Tôn trọng cả [rememberNotePayReducedMotion] và [LocalHapticFeedbackEnabled].
 */
internal fun Modifier.shakeOnError(trigger: Int): Modifier = composed {
    val reducedMotion = rememberNotePayReducedMotion()
    val performHaptic = rememberNotePayHaptic()
    val offsetX = remember { Animatable(0f) }
    val amplitudePx = with(LocalDensity.current) { 10.dp.toPx() }

    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        performHaptic(HapticFeedbackType.LongPress)
        if (reducedMotion) {
            offsetX.snapTo(0f)
            return@LaunchedEffect
        }
        offsetX.snapTo(0f)
        offsetX.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 360
                -amplitudePx at 60
                amplitudePx at 120
                (-amplitudePx * 0.65f) at 180
                (amplitudePx * 0.65f) at 240
                (-amplitudePx * 0.3f) at 300
                0f at 360
            },
        )
    }

    graphicsLayer {
        translationX = offsetX.value
    }
}

/**
 * Hiệu ứng xuất hiện tuần tự cho các khối nội dung khi mới mở màn hình.
 * Tôn trọng [rememberNotePayReducedMotion] và chỉ chạy một lần mỗi phiên hiển thị.
 */
@Composable
internal fun NotePayStaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    if (reducedMotion) {
        Box(modifier = modifier) { content() }
        return
    }

    var hasEntered by rememberSaveable(index) { mutableStateOf(false) }
    val visibleState = remember { MutableTransitionState(hasEntered) }

    LaunchedEffect(Unit) {
        if (!hasEntered) {
            hasEntered = true
            visibleState.targetState = true
        }
    }

    val delayMs = (index * 45).coerceAtMost(180)
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 260,
                delayMillis = delayMs,
                easing = NotePayEmphasizedDecelerate,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 280,
                delayMillis = delayMs,
                easing = NotePayEmphasizedDecelerate,
            ),
            initialOffsetY = { it / 10 },
        ),
        exit = fadeOut(animationSpec = tween(140)),
        modifier = modifier,
    ) {
        content()
    }
}

private val rootTabRoutes = listOf(
    Route.Home.path,
    Route.Stats.path,
    Route.Assets.path,
    Route.Utilities.path,
)

internal fun rootTabIndexForRoute(route: String?): Int? {
    val baseRoute = route?.substringBefore('?') ?: return null
    return rootTabRoutes.indexOf(baseRoute).takeIf { it >= 0 }
}

internal fun tabTransitionDirection(initialRoute: String?, targetRoute: String?): Int? {
    val initialIndex = rootTabIndexForRoute(initialRoute) ?: return null
    val targetIndex = rootTabIndexForRoute(targetRoute) ?: return null
    return (targetIndex - initialIndex).sign.takeIf { it != 0 }
}

internal fun tabIndicatorOffsetPx(
    selectedIndex: Int,
    tabCount: Int,
    tabWidthPx: Float,
): Float {
    if (tabCount <= 0) return 0f
    return selectedIndex.coerceIn(0, tabCount - 1) * tabWidthPx
}

@Composable
internal fun rememberNotePayReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

private val Int.sign: Int
    get() = when {
        this < 0 -> -1
        this > 0 -> 1
        else -> 0
    }
