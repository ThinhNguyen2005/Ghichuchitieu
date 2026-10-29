package com.notepay.ui.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * Bậc kích thước cửa sổ theo Material 3, rút gọn về 3 ngưỡng mà app thực sự dùng
 * (600dp = Medium, 840dp = Expanded). Các bậc Large/ExtraLarge của WindowSizeClass
 * được gộp vào [Expanded] vì NotePay chỉ thay đổi layout ở hai mốc 600dp và 840dp.
 */
enum class WindowLayout { Compact, Medium, Expanded }

object NotePayAdaptive {
    /**
     * Trần chiều rộng nội dung một cột. Rộng hơn mức này (tablet nằm ngang) thì dòng chữ
     * và card trở nên khó đọc, nên phần dư được chuyển thành lề hai bên.
     */
    val contentMaxWidth: Dp = 840.dp

    /** Bề rộng pill điều hướng dọc (72dp) + lề trái (12dp) + khoảng thở (12dp). */
    val navigationRailInset: Dp = 96.dp
}

/**
 * Bậc cửa sổ hiện tại. Mặc định [WindowLayout.Compact] để preview/test không cần
 * dựng activity thật vẫn render đúng như điện thoại.
 */
val LocalWindowLayout = staticCompositionLocalOf { WindowLayout.Compact }

/**
 * Lề ngang cộng thêm vào padding sẵn có để nội dung không vượt quá
 * [NotePayAdaptive.contentMaxWidth]. Bằng 0dp ở Compact nên điện thoại không đổi gì.
 */
val LocalContentGutter = staticCompositionLocalOf { 0.dp }

/** true khi cửa sổ đủ rộng để thay bottom bar bằng navigation rail dọc. */
val WindowLayout.usesNavigationRail: Boolean
    get() = this != WindowLayout.Compact

/**
 * Đặt ở gốc composition (MainActivity) — nơi duy nhất được phép đọc thông tin cửa sổ.
 * Không đặt trong `NotePayTheme` để theme tiếp tục chỉ cung cấp token hiển thị.
 *
 * Dùng `currentWindowAdaptiveInfo()` (không phải bản V2, vốn là API thử nghiệm) vì
 * app gộp Large/ExtraLarge vào [WindowLayout.Expanded] nên không cần tách thêm bậc.
 */
@Suppress("DEPRECATION")
@Composable
fun ProvideWindowLayout(content: @Composable () -> Unit) {
    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val layout = when {
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) ->
            WindowLayout.Expanded

        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
            WindowLayout.Medium

        else -> WindowLayout.Compact
    }

    // WindowSizeClass không phơi chiều rộng thô nên lấy từ configuration để tính đúng
    // phần lề cần bù; chỉ dùng cho màn hình không phải Compact.
    val windowWidth = LocalConfiguration.current.screenWidthDp.dp
    // Rail chiếm một dải bên trái màn hình, nên phải trừ dải đó khỏi bề rộng khả dụng —
    // nếu không nội dung sẽ bị đẩy lệch phải thay vì căn giữa phần còn lại.
    val availableWidth = if (layout.usesNavigationRail) {
        windowWidth - NotePayAdaptive.navigationRailInset
    } else {
        windowWidth
    }
    val gutter = ((availableWidth - NotePayAdaptive.contentMaxWidth) / 2).coerceAtLeast(0.dp)

    CompositionLocalProvider(
        LocalWindowLayout provides layout,
        LocalContentGutter provides gutter,
        content = content,
    )
}
