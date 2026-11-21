package com.notepay.core.designsystem.formatter

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import com.notepay.domain.model.CurrencyConverter
import com.notepay.domain.money.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

@Immutable
data class MoneyFormatConfig(
    val currencyCode: String = "VND",
    val symbolPosition: String = "after",
    val thousandSeparator: String = "dot",
    val ratePerVnd: BigDecimal = BigDecimal.ONE,
)

/**
 * Thread-safe Money Formatter for Vietnamese Locale & Custom currency formatting.
 *
 * Ví dụ: Money(1_000_000_00) → "1.000.000 đ"
 */
object MoneyFormatter {

    /** Chuỗi che số tiền dùng chung cho mọi màn hình có nút "Ẩn số tiền". */
    const val MASK = "••••••"
    private val CENTS_DIVISOR = BigDecimal(100)

    private var currentConfig by mutableStateOf(MoneyFormatConfig())

    val config: MoneyFormatConfig
        get() = currentConfig

    fun symbolForCode(code: String): String = when (code.uppercase(Locale.ROOT)) {
        "USD" -> "$"
        "EUR" -> "€"
        "JPY" -> "¥"
        "GBP" -> "£"
        "KRW" -> "₩"
        "SGD" -> "S$"
        "THB" -> "฿"
        else -> "đ"
    }

    fun updateConfig(
        currencyCode: String = currentConfig.currencyCode,
        symbolPosition: String = currentConfig.symbolPosition,
        thousandSeparator: String = currentConfig.thousandSeparator,
        ratePerVnd: BigDecimal = currentConfig.ratePerVnd,
    ) {
        val next = MoneyFormatConfig(
            currencyCode = currencyCode,
            symbolPosition = symbolPosition,
            thousandSeparator = thousandSeparator,
            ratePerVnd = ratePerVnd,
        )
        if (next != currentConfig) {
            Snapshot.withMutableSnapshot {
                currentConfig = next
            }
        }
    }

    fun resetToDefault() = updateConfig("VND", "after", "dot", BigDecimal.ONE)

    /**
     * Formats Money using the active user currency, live conversion rate, and separator configuration.
     * Thread-safe per call.
     */
    fun format(money: Money): String {
        val cfg = currentConfig
        val isConverting = !cfg.currencyCode.equals("VND", ignoreCase = true) &&
            cfg.ratePerVnd.compareTo(BigDecimal.ONE) != 0
        val decimalDigits = if (isConverting &&
            cfg.currencyCode.uppercase(Locale.ROOT) !in CurrencyConverter.zeroDecimalCurrencies
        ) {
            2
        } else {
            0
        }
        return formatCustom(
            money = money,
            currencySymbol = symbolForCode(cfg.currencyCode),
            symbolPosition = cfg.symbolPosition,
            thousandSeparator = cfg.thousandSeparator,
            ratePerVnd = cfg.ratePerVnd,
            decimalDigits = decimalDigits,
        )
    }

    /** Format hoặc che số tiền theo cờ [showAmounts] của tuỳ chọn riêng tư trên màn hình. */
    fun format(money: Money, showAmounts: Boolean): String =
        if (showAmounts) format(money) else MASK

    /**
     * Formats Money with custom currency symbol, position, grouping separator, and optional VND rate conversion.
     */
    fun formatCustom(
        money: Money,
        currencySymbol: String = "đ",
        symbolPosition: String = "after",
        thousandSeparator: String = "dot",
        ratePerVnd: BigDecimal = BigDecimal.ONE,
        decimalDigits: Int = 0,
    ): String {
        val groupingChar = if (thousandSeparator == "comma") ',' else '.'
        val decimalChar = if (groupingChar == '.') ',' else '.'
        val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
            groupingSeparator = groupingChar
            decimalSeparator = decimalChar
        }

        val formattedNumber = if (ratePerVnd.compareTo(BigDecimal.ONE) == 0 && decimalDigits == 0) {
            DecimalFormat("#,##0", symbols).format(money.amountInCents / 100L)
        } else {
            val converted = BigDecimal(money.amountInCents)
                .divide(CENTS_DIVISOR, 6, RoundingMode.HALF_UP)
                .multiply(ratePerVnd)
                .setScale(decimalDigits, RoundingMode.HALF_UP)
            if (converted.compareTo(BigDecimal.ZERO) == 0) {
                DecimalFormat("#,##0", symbols).format(0)
            } else {
                val pattern = if (decimalDigits > 0) "#,##0.${"0".repeat(decimalDigits)}" else "#,##0"
                DecimalFormat(pattern, symbols).format(converted)
            }
        }

        return if (symbolPosition == "before") {
            if (formattedNumber.startsWith("-")) {
                "-$currencySymbol${formattedNumber.removePrefix("-")}"
            } else {
                "$currencySymbol$formattedNumber"
            }
        } else {
            "$formattedNumber $currencySymbol"
        }
    }

    /** Compact: 1.5M, 250K, 1.2B. Dùng cho chart/dashboard preview (hỗ trợ cả số âm). */
    fun formatCompact(money: Money): String {
        val absCents = abs(money.amountInCents)
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
