package com.notepay.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Thang bo góc v4 "Sand & Ink": 12 / 16 / 20 / 24 / 28 và pill.
 * Toàn bộ giá trị là bội số của 4 (lưới 4dp của dự án); không còn bo góc bất đối xứng 18/6 của v3.
 */
data class AppShapes(
    val corner12: Shape,
    val corner16: Shape,
    val corner20: Shape,
    val corner24: Shape,
    val corner28: Shape,
    val capsule: Shape,
    val circle: Shape,
    val card: Shape = RoundedCornerShape(28.dp),
    val cardAlt: Shape = RoundedCornerShape(24.dp),
    val chip: Shape = capsule,
    val input: Shape = RoundedCornerShape(16.dp),
    val row: Shape = RoundedCornerShape(16.dp),
    val sheetTop: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
)

val DefaultAppShapes = AppShapes(
    corner12 = RoundedCornerShape(12.dp),
    corner16 = RoundedCornerShape(16.dp),
    corner20 = RoundedCornerShape(20.dp),
    corner24 = RoundedCornerShape(24.dp),
    corner28 = RoundedCornerShape(28.dp),
    capsule = RoundedCornerShape(50),
    circle = CircleShape,
    card = RoundedCornerShape(28.dp),
    cardAlt = RoundedCornerShape(24.dp),
    chip = RoundedCornerShape(50),
    input = RoundedCornerShape(16.dp),
    row = RoundedCornerShape(16.dp),
    sheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
)

val NotePayShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LocalAppShapes = staticCompositionLocalOf { DefaultAppShapes }
