package com.notepay.ui.feature.transaction.list

enum class DateRangePreset(val labelVi: String, val labelEn: String) {
    THIS_MONTH("Tháng này", "This month"),
    LAST_MONTH("Tháng trước", "Last month"),
    LAST_30_DAYS("30 ngày qua", "Last 30 days"),
    LAST_3_MONTHS("3 tháng qua", "Last 3 months"),
    LAST_6_MONTHS("6 tháng qua", "Last 6 months"),
    THIS_YEAR("Từ đầu năm", "This year"),
    LAST_YEAR("Năm trước", "Last year"),
    ALL_TIME("Toàn bộ thời gian", "All time"),
    CUSTOM("Tùy chỉnh", "Custom")
}
