package com.notepay.ui.component

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modifier hỗ trợ bo góc mượt mà cho các màn hình con trong NavHost khi người dùng
 * thực hiện thao tác vuốt lùi (Predictive Back Gesture).
 *
 * - Khi ở trạng thái tĩnh (toàn màn hình): cornerRadius = 0.dp (tràn viền tự nhiên, phẳng).
 * - Khi đang vuốt back hoặc chuyển cảnh: cornerRadius tự động bo tròn mượt mà đến [maxRadius]
 *   theo tiến trình tuyến tính (LinearEasing) bám sát tuyệt đối theo ngón tay người dùng.
 * - [enableElevation]: Tự động bật bóng nổi cao cấp (Material 3) trên thiết bị tiêu chuẩn/mạnh,
 *   và tự động tắt trên thiết bị yếu (Low-RAM / Android Go) để đảm bảo 60fps/120fps không bao giờ giật lag.
 */
@Composable
fun Modifier.predictiveBackCorners(
    scope: AnimatedContentScope,
    maxRadius: Dp = 28.dp,
    enableElevation: Boolean? = null,
): Modifier {
    val context = LocalContext.current
    val shouldEnableElevation = remember(context, enableElevation) {
        enableElevation ?: run {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.isLowRamDevice != true
        }
    }

    val cornerRadius by scope.transition.animateDp(
        label = "predictiveBackCorners",
        transitionSpec = { tween(durationMillis = 250, easing = LinearEasing) }
    ) { state ->
        when (state) {
            EnterExitState.Visible -> 0.dp
            EnterExitState.PreEnter,
            EnterExitState.PostExit -> maxRadius
        }
    }

    return this.graphicsLayer {
        val radiusPx = cornerRadius.toPx()
        val maxRadiusPx = maxRadius.toPx()
        if (radiusPx > 0.5f) {
            shape = RoundedCornerShape(radiusPx)
            clip = true
            if (shouldEnableElevation) {
                val progress = if (maxRadiusPx > 0f) (radiusPx / maxRadiusPx).coerceIn(0f, 1f) else 0f
                shadowElevation = 8.dp.toPx() * progress
            } else {
                shadowElevation = 0f
            }
        } else {
            clip = false
            shadowElevation = 0f
        }
    }
}

/**
 * Container bọc màn hình đích trong NavGraph composable(...) với hiệu ứng bo góc Predictive Back.
 */
@Composable
fun AnimatedContentScope.PredictiveBackDestination(
    modifier: Modifier = Modifier,
    maxRadius: Dp = 28.dp,
    enableElevation: Boolean? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .predictiveBackCorners(this, maxRadius, enableElevation)
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}
