package com.notepay.util

import java.text.Normalizer
import java.util.regex.Pattern

object StringUtils {
    private val DIACRITICAL_MARKS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

    /**
     * Loại bỏ dấu Tiếng Việt sử dụng java.text.Normalizer hiệu năng cao.
     * Tránh việc tạo nhiều chuỗi trung gian qua các hàm replace tuần tự.
     */
    fun removeVietnameseAccents(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        val temp = Normalizer.normalize(text, Normalizer.Form.NFD)
        val withoutCombining = DIACRITICAL_MARKS.matcher(temp).replaceAll("")
        return withoutCombining
            .replace('đ', 'd')
            .replace('Đ', 'D')
    }
}
