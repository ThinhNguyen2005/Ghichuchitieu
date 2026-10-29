package com.notepay.feature.transaction.component

import androidx.annotation.StringRes
import com.notepay.R

/**
 * Khoảng thời gian lọc. Nhãn hiển thị lấy từ tài nguyên chuỗi để đổi ngôn ngữ được, không
 * giữ chuỗi tiếng Việt cứng trong enum.
 */
enum class DateRangePreset(@param:StringRes val labelRes: Int) {
    THIS_MONTH(R.string.date_range_this_month),
    LAST_MONTH(R.string.date_range_last_month),
    LAST_30_DAYS(R.string.date_range_last_30_days),
    LAST_3_MONTHS(R.string.date_range_last_3_months),
    LAST_6_MONTHS(R.string.date_range_last_6_months),
    THIS_YEAR(R.string.date_range_this_year),
    LAST_YEAR(R.string.date_range_last_year),
    ALL_TIME(R.string.date_range_all_time),
    CUSTOM(R.string.date_range_custom)
}
