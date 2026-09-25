package com.notepay.ui.component

import android.app.ActivityManager
import android.content.Context
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Container bọc màn hình con trong NavGraph với cơ chế Predictive Back chuẩn Telegram:
 *
 * 1. Khi vuốt từ mép trái: Màn hình thu nhỏ nhẹ (scale ~0.92), bo góc tròn (28.dp),
 *    đổ bóng nổi và bị đẩy nhẹ sang phải (~32.dp).
 * 2. Khi vuốt từ mép phải: Tương tự, màn hình thu nhỏ và bị đẩy nhẹ sang trái (~ -32.dp).
 * 3. Khi buông tay ra (Gesture confirmed): Trang lướt/trượt dứt khoát 100% sang bên phải ra khỏi màn hình.
 * 4. Khi kéo ngược lại để hủy (Cancelled): Đàn hồi mượt mà về trạng thái toàn màn hình ban đầu.
 */
@Composable
fun AnimatedContentScope.PredictiveBackDestination(
    modifier: Modifier = Modifier,
    maxRadius: Dp = 28.dp,
    enableElevation: Boolean? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (onBack == null) {
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
    val configuration = LocalConfiguration.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val targetFixedShiftPx = with(density) { 32.dp.toPx() }
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
    val alpha = remember { Animatable(1f) }

    PredictiveBackHandler(enabled = true) { progressFlow ->
        var isLeftEdge = true
        try {
            progressFlow.collect { backEvent ->
                isLeftEdge = backEvent.swipeEdge == BackEventCompat.EDGE_LEFT
                val progress = backEvent.progress

                // Telegram gesture: Nhanh chóng đạt trạng thái preview cố định (trong ~12% đầu của thao tác vuốt)
                // và giữ cố định (không bám đuổi theo ngón tay nữa khi ngón tay di chuyển tiếp)
                val previewFactor = FastOutSlowInEasing.transform((progress / 0.12f).coerceIn(0f, 1f))

                val targetScale = 1f - (0.08f * previewFactor)
                val currentRadius = maxRadiusPx * previewFactor
                val currentElevation = if (shouldEnableElevation) maxElevationPx * previewFactor else 0f
                val currentShift = if (isLeftEdge) {
                    targetFixedShiftPx * previewFactor
                } else {
                    -targetFixedShiftPx * previewFactor
                }

                scale.snapTo(targetScale)
                cornerRadius.snapTo(currentRadius)
                elevation.snapTo(currentElevation)
                offsetX.snapTo(currentShift)
            }

            // Khi buông tay xác nhận: Trượt dứt khoát 100% sang phải ra khỏi màn hình (Telegram physics)
            coroutineScope {
                launch {
                    offsetX.animateTo(
                        targetValue = screenWidthPx,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 180)
                    )
                }
            }
            onBack()
        } catch (e: CancellationException) {
            // Khi hủy thao tác vuốt (kéo ngược về mép hoặc huỷ gesture):
            // Phục hồi lại toàn màn hình với spring mượt mà, bọc NonCancellable để hoàn tất an toàn
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                coroutineScope {
                    launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                    launch { scale.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow)) }
                    launch { cornerRadius.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                    launch { elevation.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                    launch { alpha.animateTo(1f, tween(durationMillis = 150)) }
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
                this.alpha = alpha.value
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
