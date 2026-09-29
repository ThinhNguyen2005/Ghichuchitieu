package com.notepay.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

data class AppShapes(
    val corner8: Shape,
    val corner10: Shape,
    val corner12: Shape,
    val corner14: Shape,
    val corner16: Shape,
    val corner20: Shape,
    val corner24: Shape,
    val capsule: Shape,
    val circle: Shape,
    // NotePay Ledger Ink & Warm Paper - Chữ ký hình khối (Asymmetrical Folded Corners)
    val card: Shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp),
    val cardAlt: Shape = RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
    val chip: Shape = RoundedCornerShape(10.dp),
    val input: Shape = RoundedCornerShape(14.dp),
    val row: Shape = RoundedCornerShape(16.dp),
    val sheetTop: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
)

val DefaultAppShapes = AppShapes(
    corner8 = RoundedCornerShape(8.dp),
    corner10 = RoundedCornerShape(10.dp),
    corner12 = RoundedCornerShape(12.dp),
    corner14 = RoundedCornerShape(14.dp),
    corner16 = RoundedCornerShape(16.dp),
    corner20 = RoundedCornerShape(20.dp),
    corner24 = RoundedCornerShape(24.dp),
    capsule = RoundedCornerShape(50),
    circle = CircleShape,
    card = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp),
    cardAlt = RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
    chip = RoundedCornerShape(10.dp),
    input = RoundedCornerShape(14.dp),
    row = RoundedCornerShape(16.dp),
    sheetTop = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
)

val NotePayShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp),
    extraLarge = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
)

val LocalAppShapes = staticCompositionLocalOf { DefaultAppShapes }
