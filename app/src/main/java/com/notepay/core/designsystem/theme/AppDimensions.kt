package com.notepay.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Thang kích thước khoảng cách (spacing) và công thái học của NotePay theo chuẩn 4dp (DESIGN.md).
 * Xóa sự trùng lặp giữa space và padding, bổ sung mốc 12dp và 48dp (minTouchTarget theo WCAG).
 */
@Immutable
data class AppDimensions(
    val spaceExtraSmall: Dp = 4.dp,
    val spaceSmall: Dp = 8.dp,
    val spaceMedium: Dp = 12.dp,
    val spaceLarge: Dp = 16.dp,
    val spaceExtraLarge: Dp = 24.dp,
    val spaceMassive: Dp = 32.dp,
    val minTouchTarget: Dp = 48.dp,
) {
    // Thuộc tính tiện ích và tương thích ngược
    val spaceTiny: Dp get() = spaceExtraSmall
    val paddingSmall: Dp get() = spaceSmall
    val paddingMedium: Dp get() = spaceLarge
    val paddingLarge: Dp get() = spaceExtraLarge
}

val DefaultAppDimensions = AppDimensions(
    spaceExtraSmall = 4.dp,
    spaceSmall = 8.dp,
    spaceMedium = 12.dp,
    spaceLarge = 16.dp,
    spaceExtraLarge = 24.dp,
    spaceMassive = 32.dp,
    minTouchTarget = 48.dp,
)

val LocalAppDimensions = staticCompositionLocalOf { DefaultAppDimensions }
