package com.notepay.ui.util

import com.notepay.domain.model.Money
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Thread-safe Money Formatter for Vietnamese Locale & Compact formatting.
 *
 * Ví dụ: Money(1_000_000_00) → "1.000.000 ₫"
 */
object MoneyFormatter {
    private val VIETNAMESE_LOCALE = Locale.Builder().setLanguage("vi").setRegion("VN").build()

    /**
     * Formats Money into standard Vietnamese currency string.
     * Thread-safe per call.
     */
    fun format(money: Money): String {
        val symbols = DecimalFormatSymbols(VIETNAMESE_LOCALE).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,##0", symbols)
        val majorUnits = money.amountInCents / 100L
        return "${formatter.format(majorUnits)} ₫"
    }

    /** Compact: 1.5M, 250K, 1.2B. Dùng cho chart/dashboard preview (hỗ trợ cả số âm). */
    fun formatCompact(money: Money): String {
        val absCents = kotlin.math.abs(money.amountInCents)
        val value = absCents / 100.0
        val sign = if (money.amountInCents < 0) "-" else ""

        val formatted = when {
            value >= 1_000_000_000 -> "%.1fB".format(Locale.US, value / 1_000_000_000)
            value >= 1_000_000 -> "%.1fM".format(Locale.US, value / 1_000_000)
            value >= 1_000 -> "%.1fK".format(Locale.US, value / 1_000)
            else -> "%.0f".format(Locale.US, value)
        }

        return "$sign$formatted"
    }
}
