package com.notepay.ui.component

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class CradleShape(
    private val cradleRadius: Dp = 38.dp, // Bán kính đường cong ôm nút +
    private val cornerRadius: Dp = 24.dp  // Góc bo của thanh điều hướng dưới
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val r = with(density) { cornerRadius.toPx() }
            val cradleR = with(density) { cradleRadius.toPx() }
            val centerX = size.width / 2

            // Vẽ góc bo tròn trái trên
            moveTo(0f, r)
            arcTo(
                rect = Rect(0f, 0f, r * 2, r * 2),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Vẽ đường thẳng đến điểm bắt đầu đường cong khuyết ở giữa
            val clearance = with(density) { 8.dp.toPx() } // Độ giãn cách an toàn
            lineTo(centerX - cradleR - clearance, 0f)

            // Vẽ đường cong khuyết ôm nút + (Cradle cutout)
            cubicTo(
                x1 = centerX - cradleR,
                y1 = 0f,
                x2 = centerX - cradleR,
                y2 = cradleR,
                x3 = centerX,
                y3 = cradleR
            )
            cubicTo(
                x1 = centerX + cradleR,
                y1 = cradleR,
                x2 = centerX + cradleR,
                y2 = 0f,
                x3 = centerX + cradleR + clearance,
                y3 = 0f
            )

            // Vẽ đường thẳng đến góc phải trên
            lineTo(size.width - r, 0f)
            arcTo(
                rect = Rect(size.width - r * 2, 0f, size.width, r * 2),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Vẽ phần còn lại của hình chữ nhật
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}
