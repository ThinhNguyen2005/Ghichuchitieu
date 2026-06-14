package com.notepay.ui.util

import com.notepay.domain.model.Money
import java.text.NumberFormat
import java.util.Locale

/**
 * Format Money → chuỗi tiền tệ locale VN.
 *
 * Ví dụ: Money(1_000_000_00) → "1.000.000 ₫"
 */
object MoneyFormatter {
    private val VIETNAMESE: Locale = Locale("vi", "VN")
    private val format: NumberFormat = NumberFormat.getCurrencyInstance(VIETNAMESE)

    fun format(money: Money): String = format.format(money.amountInCents / 100.0)

    /** Compact: 1.5M, 250K, 1.2B. Dùng cho chart/dashboard preview. */
    fun formatCompact(money: Money): String {
        val value = money.amountInCents / 100.0
        return when {
            value >= 1_000_000_000 -> "%.1fB".format(value / 1_000_000_000)
            value >= 1_000_000 -> "%.1fM".format(value / 1_000_000)
            value >= 1_000 -> "%.1fK".format(value / 1_000)
            else -> "%.0f".format(value)
        }
    }
}
