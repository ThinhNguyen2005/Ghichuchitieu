package com.notepay.core.designsystem.component.effect

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FirefliesBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fireflies")
    val particleColor = MaterialTheme.colorScheme.primary
    val progressState = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing)),
        label = "motion",
    )
    Spacer(
        modifier = modifier.drawWithCache {
            val radiusPx = 36.dp.toPx()
            val driftXPx = 20.dp.toPx()
            val driftYPx = 26.dp.toPx()
            val cachedBrush = Brush.radialGradient(
                colors = listOf(particleColor.copy(alpha = .10f), Color.Transparent),
                center = Offset.Zero,
                radius = radiusPx,
            )
            onDrawBehind {
                val progress = progressState.value
                repeat(12) { index ->
                    val x = size.width * ((index * 37 % 100) / 100f) +
                        sin((progress + index).toDouble()).toFloat() * driftXPx
                    val y = size.height * ((index * 61 % 100) / 100f) +
                        cos((progress + index * .7f).toDouble()).toFloat() * driftYPx
                    translate(left = x, top = y) {
                        drawCircle(
                            brush = cachedBrush,
                            radius = radiusPx,
                            center = Offset.Zero,
                        )
                    }
                }
            }
        },
    )
}
