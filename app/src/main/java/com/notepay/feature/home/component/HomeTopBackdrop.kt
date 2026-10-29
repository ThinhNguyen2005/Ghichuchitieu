package com.notepay.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.notepay.ui.theme.AppTheme

/**
 * Nền trang trí phía trên trang Home: một mảng màu rất mềm (đọc như ảnh bị làm mờ) chiếm
 * khoảng 1/3 màn hình, tan dần vào nền `background` của app nên chữ và thẻ phía dưới luôn
 * đủ tương phản.
 *
 * Không dùng ảnh bitmap: toàn bộ là gradient dựng từ token sand/ink nên nhẹ, không tốn
 * asset và tự đổi theo Light/Dark. Muốn thay bằng ảnh thật thì chỉ cần đổi các brush dưới
 * đây sang `drawImage`/`AsyncImage`.
 */
@Composable
internal fun HomeTopBackdrop(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors

    val brushes = remember(colors) {
        val isLight = colors.isLight
        val wash = Brush.verticalGradient(
            0f to colors.sand.copy(alpha = if (isLight) 0.62f else 0.16f),
            0.45f to colors.sand.copy(alpha = if (isLight) 0.20f else 0.06f),
            1f to Color.Transparent,
        )
        // Vệt cát chéo: tạo cảm giác "ảnh" thay vì một dải màu phẳng.
        val sheen = Brush.linearGradient(
            colors = listOf(
                colors.clay.copy(alpha = if (isLight) 0.32f else 0.20f),
                Color.Transparent,
                colors.sand.copy(alpha = if (isLight) 0.28f else 0.10f),
            ),
            start = Offset.Zero,
            end = Offset.Infinite,
        )
        // Điểm sáng mờ ở giữa khối, giống vùng lấy nét của một bức ảnh blur.
        val glow = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isLight) 0.55f else 0.06f),
                Color.Transparent,
            ),
        )
        val inkShade = Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to colors.ink.copy(alpha = if (isLight) 0.03f else 0.18f),
            1f to Color.Transparent,
        )
        // Lớp cuối: hoà hẳn vào nền trang để không có đường cắt ngang.
        val fade = Brush.verticalGradient(
            0f to Color.Transparent,
            0.62f to Color.Transparent,
            0.88f to colors.background,
            1f to colors.background,
        )
        listOf(wash, sheen, glow, inkShade, fade)
    }

    // Xếp chồng bằng modifier để không sinh thêm node layout: brush sau vẽ đè lên brush trước.
    var stacked: Modifier = modifier.fillMaxWidth()
    brushes.forEach { brush -> stacked = stacked.background(brush) }

    Box(modifier = stacked)
}
