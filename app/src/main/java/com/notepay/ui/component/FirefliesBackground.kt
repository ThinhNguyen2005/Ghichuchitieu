package com.notepay.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FirefliesBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fireflies")
    val particleColor = MaterialTheme.colorScheme.primary
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing)),
        label = "motion",
    )
    Canvas(modifier) {
        repeat(12) { index ->
            val x = size.width * ((index * 37 % 100) / 100f) + sin((progress + index).toDouble()).toFloat() * 20.dp.toPx()
            val y = size.height * ((index * 61 % 100) / 100f) + cos((progress + index * .7f).toDouble()).toFloat() * 26.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(particleColor.copy(alpha = .10f), Color.Transparent),
                    center = Offset(x, y),
                    radius = 36.dp.toPx(),
                ),
                radius = 36.dp.toPx(),
                center = Offset(x, y),
            )
        }
    }
}
