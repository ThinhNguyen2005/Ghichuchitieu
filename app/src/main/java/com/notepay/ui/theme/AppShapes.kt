package com.notepay.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

data class AppShapes(
    val corner8: Shape,
    val corner12: Shape,
    val corner16: Shape,
    val corner20: Shape,
    val corner24: Shape,
    val capsule: Shape,
    val circle: Shape
)

val DefaultAppShapes = AppShapes(
    corner8 = RoundedCornerShape(8.dp),
    corner12 = RoundedCornerShape(12.dp),
    corner16 = RoundedCornerShape(16.dp),
    corner20 = RoundedCornerShape(20.dp),
    corner24 = RoundedCornerShape(24.dp),
    capsule = RoundedCornerShape(50),
    circle = CircleShape
)

val LocalAppShapes = staticCompositionLocalOf { DefaultAppShapes }
