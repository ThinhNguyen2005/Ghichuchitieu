package com.notepay.core.navigation

import android.app.ActivityManager
import android.content.Context
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Animates the back gesture. NavHost handles the transition after commitment. */
@Composable
fun AnimatedContentScope.PredictiveBackDestination(
    modifier: Modifier = Modifier,
    maxRadius: Dp = 28.dp,
    enableElevation: Boolean? = null,
    onBack: (() -> Unit)? = null,
    canGoBack: () -> Boolean = { true },
    content: @Composable () -> Unit,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    if (onBack == null || reducedMotion) {
        if (onBack != null) BackHandler { if (canGoBack()) onBack() }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
        return
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val maxShiftPx = with(density) { NotePayMotion.contentTranslation.toPx() }
    val maxRadiusPx = with(density) { maxRadius.toPx() }
    val maxElevationPx = with(density) { 10.dp.toPx() }

    val shouldEnableElevation = remember(context, enableElevation) {
        enableElevation ?: run {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.isLowRamDevice != true
        }
    }

    val offsetX = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val cornerRadius = remember { Animatable(0f) }
    val elevation = remember { Animatable(0f) }

    PredictiveBackHandler(enabled = true) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                if (!canGoBack()) return@collect
                val isLeftEdge = backEvent.swipeEdge == BackEventCompat.EDGE_LEFT
                val progress = backEvent.progress
                val eased = FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))

                val targetScale = 1f - (0.02f * eased)
                val currentRadius = maxRadiusPx * eased
                val currentElevation = if (shouldEnableElevation) maxElevationPx * eased else 0f
                val currentShift = if (isLeftEdge) maxShiftPx * eased else -maxShiftPx * eased

                scale.snapTo(targetScale)
                cornerRadius.snapTo(currentRadius)
                elevation.snapTo(currentElevation)
                offsetX.snapTo(currentShift)
            }
            if (canGoBack()) {
                onBack()
            } else {
                offsetX.snapTo(0f)
                scale.snapTo(1f)
                cornerRadius.snapTo(0f)
                elevation.snapTo(0f)
            }
        } catch (_: CancellationException) {
            withContext(NonCancellable) {
                coroutineScope {
                    launch { offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f)) }
                    launch { scale.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 500f)) }
                    launch { cornerRadius.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 650f)) }
                    launch { elevation.animateTo(0f, tween(durationMillis = 150)) } // đổ bóng không cần vật lý lò xo
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.translationX = offsetX.value
                this.scaleX = scale.value
                this.scaleY = scale.value
                val r = cornerRadius.value
                if (r > 0.5f) {
                    shape = RoundedCornerShape(r)
                    clip = true
                    shadowElevation = elevation.value
                } else {
                    clip = false
                    shadowElevation = 0f
                }
            }
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}
