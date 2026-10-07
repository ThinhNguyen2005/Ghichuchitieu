package com.notepay.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.notepay.R
import com.notepay.domain.model.AuthUser
import java.time.LocalTime

/**
 * CompositionLocal cung cấp thông tin người dùng đã xác thực xuyên suốt cây giao diện.
 * Mặc định là null (chưa đăng nhập / chế độ cục bộ).
 */
val LocalAuthUser = compositionLocalOf<AuthUser?> { null }

/**
 * Các khoảng thời gian trong ngày để hiển thị lời chào phù hợp với ngữ cảnh.
 */
enum class TimeOfDay {
    MORNING,   // 05:00 - 11:59
    AFTERNOON, // 12:00 - 17:59
    EVENING    // 18:00 - 04:59
}

/**
 * Xác định khoảng thời gian trong ngày dựa trên giờ máy (0..23).
 */
fun getTimeOfDay(hour: Int = LocalTime.now().hour): TimeOfDay = when (hour) {
    in 5..11 -> TimeOfDay.MORNING
    in 12..17 -> TimeOfDay.AFTERNOON
    else -> TimeOfDay.EVENING
}

/**
 * Trích xuất tên thân mật hiển thị ngắn gọn trên thanh TopBar.
 * Hỗ trợ nhận diện tên tiếng Việt (lấy tên chính ở cuối) hoặc tên phương Tây (lấy First name).
 */
fun extractFriendlyName(fullName: String?): String? {
    if (fullName.isNullOrBlank()) return null
    val trimmed = fullName.trim()
    val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
    if (parts.size <= 2) return trimmed

    val vietnameseVowels = "àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ"
    val isVietnamese = trimmed.any { c -> vietnameseVowels.contains(c, ignoreCase = true) }

    return if (isVietnamese) parts.last() else parts.first()
}

/**
 * Tạo lời chào cá nhân hóa theo thời gian trên máy của người dùng.
 * Tự động phản ứng khi trạng thái đăng nhập hoặc thông tin người dùng thay đổi.
 */
@Composable
fun rememberUserGreeting(
    user: AuthUser? = LocalAuthUser.current,
    hour: Int = remember { LocalTime.now().hour }
): String {
    val timeOfDay = remember(hour) { getTimeOfDay(hour) }
    val friendlyName = remember(user?.displayName) {
        extractFriendlyName(user?.displayName)
    }

    return if (friendlyName != null) {
        when (timeOfDay) {
            TimeOfDay.MORNING -> stringResource(R.string.greeting_morning_with_name, friendlyName)
            TimeOfDay.AFTERNOON -> stringResource(R.string.greeting_afternoon_with_name, friendlyName)
            TimeOfDay.EVENING -> stringResource(R.string.greeting_evening_with_name, friendlyName)
        }
    } else {
        when (timeOfDay) {
            TimeOfDay.MORNING -> stringResource(R.string.greeting_morning_generic)
            TimeOfDay.AFTERNOON -> stringResource(R.string.greeting_afternoon_generic)
            TimeOfDay.EVENING -> stringResource(R.string.greeting_evening_generic)
        }
    }
}
