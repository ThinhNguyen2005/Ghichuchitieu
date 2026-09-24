package com.notepay.ui.util

import com.notepay.domain.model.Money
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Thread-safe Money Formatter for Vietnamese Locale & Custom currency formatting.
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

    /**
     * Formats Money with custom currency symbol, position and grouping separator.
     */
    fun formatCustom(
        money: Money,
        currencySymbol: String = "₫",
        symbolPosition: String = "after",
        thousandSeparator: String = "dot",
    ): String {
        val groupingChar = if (thousandSeparator == "comma") ',' else '.'
        val decimalChar = if (groupingChar == '.') ',' else '.'
        val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
            groupingSeparator = groupingChar
            decimalSeparator = decimalChar
        }
        val formatter = DecimalFormat("#,##0", symbols)
        val majorUnits = money.amountInCents / 100L
        val formattedNumber = formatter.format(majorUnits)
        return if (symbolPosition == "before") {
            "$currencySymbol$formattedNumber"
        } else {
            "$formattedNumber $currencySymbol"
        }
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

    /** Compact Vietnamese units used by the month calendar (for example, 6500 ₫ → 6k). */
    fun formatCompactVietnamese(money: Money): String {
        val dong = money.amountInCents / 100L
        return when {
            dong < 1_000L -> dong.toString()
            dong < 1_000_000L -> "${dong / 1_000L}k"
            else -> {
                val millions = dong / 1_000_000.0
                if (millions % 1.0 == 0.0) {
                    "${millions.toInt()}Tr"
                } else {
                    String.format(Locale.US, "%.1fTr", millions)
                }
            }
        }
    }
}
