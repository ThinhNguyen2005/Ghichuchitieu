package com.notepay.core.navigation

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal object NotePayMotion {
    const val tabIndicatorDurationMillis = 200
    const val contentDurationMillis = 200
    const val contentFadeOutDurationMillis = 200
    const val contentFadeDelayMillis = 0
    const val pageEnterDurationMillis = 280
    const val pageExitDurationMillis = 180
    const val modalEnterDurationMillis = 320
    const val modalExitDurationMillis = 220
    const val contentEnterDurationMillis = 220
    const val contentExitDurationMillis = 140
    val contentTranslation: Dp = 12.dp
}

/** Easing chuẩn Material 3 cho phần tử xuất hiện trên màn hình. */
internal val NotePayEmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

/** Easing chuẩn Material 3 cho phần tử rời khỏi màn hình. */
internal val NotePayEmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

/** Easing chuẩn Material 3 cho chuyển đổi trạng thái nội dung tại chỗ. */
internal val NotePayStandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

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
    visible: Boolean = true,
    delayStepMillis: Int = 45,
    maxDelayMillis: Int = 180,
    content: @Composable () -> Unit,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    if (reducedMotion) {
        if (visible) {
            Box(modifier = modifier) { content() }
        }
        return
    }

    var hasEntered by rememberSaveable(index) { mutableStateOf(false) }
    val visibleState = remember { MutableTransitionState(hasEntered && visible) }

    LaunchedEffect(visible) {
        if (visible && !hasEntered) {
            hasEntered = true
        }
        visibleState.targetState = visible
    }

    val delayMs = (index * delayStepMillis).coerceAtMost(maxDelayMillis)
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
        exit = fadeOut(animationSpec = tween(NotePayMotion.contentExitDurationMillis, easing = NotePayEmphasizedAccelerate)),
        modifier = modifier,
    ) {
        content()
    }
}

/**
 * Staggered entrance modifier that animates `alpha` and `translationY` via `graphicsLayer`
 * without altering measurement or collapsing height on frame 0 (safe inside sheets/lists).
 */
internal fun Modifier.notePayStaggeredEntrance(
    index: Int,
    visible: Boolean = true,
    delayStepMillis: Int = 30,
    maxDelayMillis: Int = 150,
): Modifier = composed {
    val reducedMotion = rememberNotePayReducedMotion()
    val transitionState = remember(index) {
        MutableTransitionState(if (reducedMotion) visible else false)
    }
    transitionState.targetState = visible
    val transition = rememberTransition(transitionState = transitionState, label = "staggeredModifier$index")
    val delayMs = (index * delayStepMillis).coerceAtMost(maxDelayMillis)
    val alpha by transition.animateFloat(
        transitionSpec = {
            if (reducedMotion) snap()
            else tween(
                durationMillis = if (targetState) 160 else 100,
                delayMillis = if (targetState) 30 + delayMs else 0,
                easing = if (targetState) NotePayEmphasizedDecelerate else NotePayEmphasizedAccelerate,
            )
        },
        label = "staggeredAlpha",
    ) { if (it) 1f else 0f }
    val offsetYFactor by transition.animateFloat(
        transitionSpec = {
            if (reducedMotion) snap()
            else tween(
                durationMillis = if (targetState) 180 else 100,
                delayMillis = if (targetState) 30 + delayMs else 0,
                easing = if (targetState) NotePayEmphasizedDecelerate else NotePayEmphasizedAccelerate,
            )
        },
        label = "staggeredOffsetY",
    ) { if (it) 0f else 1f }
    val translationPx = with(LocalDensity.current) { 14.dp.toPx() }
    graphicsLayer {
        this.alpha = alpha
        this.translationY = offsetYFactor * translationPx
    }
}

internal fun isModalOpeningRoute(route: String?): Boolean =
    route != null && (
        route.startsWith(Route.AddTransaction.path) ||
            route.startsWith(Route.AddWallet.path)
        )

internal fun notePayForwardEnterTransition(
    initialRoute: String?,
    targetRoute: String?,
    reducedMotion: Boolean,
    tabDistancePx: Int,
): EnterTransition {
    if (reducedMotion) return EnterTransition.None
    val tabDirection = tabTransitionDirection(initialRoute, targetRoute)
    return when {
        isModalOpeningRoute(targetRoute) -> fadeIn(
            animationSpec = tween(durationMillis = 240, easing = NotePayEmphasizedDecelerate),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = NotePayMotion.modalEnterDurationMillis,
                easing = NotePayEmphasizedDecelerate,
            ),
            initialOffsetY = { (it * 0.22f).roundToInt() },
        ) + scaleIn(
            initialScale = 0.96f,
            transformOrigin = TransformOrigin(0.5f, 0.65f),
            animationSpec = tween(
                durationMillis = NotePayMotion.modalEnterDurationMillis,
                easing = NotePayEmphasizedDecelerate,
            ),
        )

        tabDirection != null -> fadeIn(
            tween(
                durationMillis = NotePayMotion.contentDurationMillis,
                delayMillis = NotePayMotion.contentFadeDelayMillis,
                easing = NotePayEmphasizedDecelerate,
            ),
        ) + slideInHorizontally(
            animationSpec = tween(NotePayMotion.contentDurationMillis, easing = NotePayEmphasizedDecelerate),
            initialOffsetX = { tabDirection * tabDistancePx },
        )

        else -> fadeIn(
            animationSpec = tween(durationMillis = 220, easing = NotePayEmphasizedDecelerate),
        ) + slideInHorizontally(
            animationSpec = tween(
                durationMillis = NotePayMotion.pageEnterDurationMillis,
                easing = NotePayEmphasizedDecelerate,
            ),
            initialOffsetX = { (it * 0.16f).roundToInt() },
        )
    }
}

internal fun notePayForwardExitTransition(
    initialRoute: String?,
    targetRoute: String?,
    reducedMotion: Boolean,
    tabDistancePx: Int,
): ExitTransition {
    if (reducedMotion) return ExitTransition.None
    val tabDirection = tabTransitionDirection(initialRoute, targetRoute)
    return when {
        isModalOpeningRoute(targetRoute) -> scaleOut(
            targetScale = 0.96f,
            transformOrigin = TransformOrigin(0.5f, 0.5f),
            animationSpec = tween(durationMillis = 260, easing = NotePayStandardEasing),
        ) + fadeOut(
            animationSpec = tween(durationMillis = 200, easing = NotePayEmphasizedAccelerate),
        )

        tabDirection != null -> fadeOut(
            tween(NotePayMotion.contentFadeOutDurationMillis, easing = NotePayEmphasizedAccelerate),
        ) + slideOutHorizontally(
            animationSpec = tween(NotePayMotion.contentFadeOutDurationMillis, easing = NotePayEmphasizedAccelerate),
            targetOffsetX = { -tabDirection * tabDistancePx },
        )

        else -> slideOutHorizontally(
            animationSpec = tween(durationMillis = 240, easing = NotePayStandardEasing),
            targetOffsetX = { -(it * 0.06f).roundToInt() },
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = NotePayMotion.pageExitDurationMillis,
                easing = NotePayEmphasizedAccelerate,
            ),
        )
    }
}

internal fun notePayContentTransform(
    reducedMotion: Boolean,
    clipSize: Boolean = false,
): ContentTransform = ContentTransform(
    targetContentEnter = if (reducedMotion) {
        EnterTransition.None
    } else {
        fadeIn(
            animationSpec = tween(
                durationMillis = NotePayMotion.contentEnterDurationMillis,
                easing = NotePayEmphasizedDecelerate,
            ),
        )
    },
    initialContentExit = if (reducedMotion) {
        ExitTransition.None
    } else {
        fadeOut(
            animationSpec = tween(
                durationMillis = NotePayMotion.contentExitDurationMillis,
                easing = NotePayEmphasizedAccelerate,
            ),
        )
    },
    sizeTransform = SizeTransform(clip = clipSize),
)

@Composable
internal fun <S> NotePayAnimatedContent(
    targetState: S,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    label: String = "NotePayAnimatedContent",
    contentKey: (S) -> Any? = { it },
    content: @Composable AnimatedContentScope.(targetState: S) -> Unit,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = { notePayContentTransform(reducedMotion) },
        contentAlignment = contentAlignment,
        label = label,
        contentKey = contentKey,
        content = content,
    )
}

@Composable
internal fun NotePayAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    label: String = "NotePayAnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    val enter = if (reducedMotion) {
        EnterTransition.None
    } else {
        fadeIn(tween(durationMillis = 200, easing = NotePayEmphasizedDecelerate)) +
            expandVertically(tween(durationMillis = 220, easing = NotePayEmphasizedDecelerate))
    }
    val exit = if (reducedMotion) {
        ExitTransition.None
    } else {
        fadeOut(tween(durationMillis = 140, easing = NotePayEmphasizedAccelerate)) +
            shrinkVertically(tween(durationMillis = 160, easing = NotePayEmphasizedAccelerate))
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        label = label,
        content = content,
    )
}

internal fun Modifier.notePayPressScale(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
): Modifier = composed {
    val reducedMotion = rememberNotePayReducedMotion()
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reducedMotion) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "notePayPressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
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
