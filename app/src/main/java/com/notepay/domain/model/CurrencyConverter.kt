package com.notepay.domain.model

import com.notepay.R
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.domain.money.Money
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val nameRes: Int,
    /** Tỷ giá quy đổi ước tính (1 đơn vị ngoại tệ = bao nhiêu VND). Dùng khi offline chưa có cache. */
    val approximateRateToVnd: Double,
)

object CurrencyConverter {

    val zeroDecimalCurrencies: Set<String> = setOf("VND", "JPY", "KRW")
    private val singleDotThousandsRegex = Regex("^\\d{1,3}\\.\\d{3}$")

    val supportedCurrencies: List<CurrencyInfo> = listOf(
        CurrencyInfo("VND", "₫", R.string.currency_vnd, 1.0),
        CurrencyInfo("USD", "$", R.string.currency_usd, 25_500.0),
        CurrencyInfo("EUR", "€", R.string.currency_eur, 27_500.0),
        CurrencyInfo("JPY", "¥", R.string.currency_jpy, 165.0),
        CurrencyInfo("GBP", "£", R.string.currency_gbp, 32_500.0),
        CurrencyInfo("KRW", "₩", R.string.currency_krw, 18.5),
        CurrencyInfo("SGD", "S$", R.string.currency_sgd, 19_200.0),
        CurrencyInfo("THB", "฿", R.string.currency_thb, 740.0),
    )

    fun getCurrencyInfo(code: String): CurrencyInfo =
        supportedCurrencies.find { it.code.equals(code, ignoreCase = true) }
            ?: CurrencyInfo(code.uppercase(), code.uppercase(), R.string.currency_vnd, 1.0)

    /** Trả về tỷ giá `1 VND = ? [currencyCode]`, ưu tiên tỷ giá API và fallback về ước tính cục bộ. */
    fun ratePerVnd(
        currencyCode: String,
        rates: Map<String, BigDecimal>? = null,
    ): BigDecimal {
        val codeUpper = currencyCode.uppercase(Locale.ROOT)
        if (codeUpper == "VND") return BigDecimal.ONE
        val liveRate = rates?.get(codeUpper)?.takeIf { it > BigDecimal.ZERO }
        if (liveRate != null) return liveRate
        val fallbackVndPerUnit = supportedCurrencies
            .find { it.code.equals(codeUpper, ignoreCase = true) }
            ?.approximateRateToVnd
            ?: 25_500.0
        return BigDecimal.ONE.divide(BigDecimal.valueOf(fallbackVndPerUnit), MathContext.DECIMAL128)
    }

    /** Quy đổi [amount] từ [fromCode] sang [toCode] dựa trên bảng tỷ giá nền VND. */
    fun convertBetween(
        amount: BigDecimal,
        fromCode: String,
        toCode: String,
        rates: Map<String, BigDecimal>? = null,
    ): BigDecimal {
        if (fromCode.equals(toCode, ignoreCase = true)) return amount
        val amountInVnd = amount.divide(ratePerVnd(fromCode, rates), MathContext.DECIMAL128)
        return amountInVnd.multiply(ratePerVnd(toCode, rates))
    }

    /** Giá trị quy đổi của 1 đơn vị [currencyCode] theo VND. */
    fun vndPerUnit(
        currencyCode: String,
        rates: Map<String, BigDecimal>? = null,
    ): BigDecimal = BigDecimal.ONE.divide(ratePerVnd(currencyCode, rates), MathContext.DECIMAL128)

    /** Phân tích chuỗi số tiền nhập tự do (hỗ trợ cả `1.000.000`, `1,000,000`, `500.000`, `12,50`). */
    fun parseFlexibleAmount(
        raw: String,
        allowDotAsThousands: Boolean,
    ): BigDecimal? {
        val trimmed = raw.replace("\\s".toRegex(), "")
        if (trimmed.isEmpty()) return null

        val dotCount = trimmed.count { it == '.' }
        val commaCount = trimmed.count { it == ',' }
        val normalized = when {
            dotCount > 1 -> trimmed.replace(".", "").replace(',', '.')
            commaCount > 1 -> trimmed.replace(",", "")
            dotCount == 1 && commaCount == 1 -> {
                if (trimmed.indexOf('.') > trimmed.indexOf(',')) {
                    trimmed.replace(",", "")
                } else {
                    trimmed.replace(".", "").replace(',', '.')
                }
            }
            allowDotAsThousands && singleDotThousandsRegex.matches(trimmed) -> trimmed.replace(".", "")
            else -> trimmed.replace(',', '.')
        }
        return normalized.toBigDecimalOrNull()
    }

    /**
     * Quy đổi số tiền [amountCents] từ [fromCurrency] sang VND (đơn vị cents).
     *
     * [rates] là snapshot từ API mở er-api: baseCode = VND (tức 1 VND = rates[code] ngoại tệ).
     * => amountInVnd = amountInCurrency / rate.
     */
    fun convertToVndCents(
        amountCents: Long,
        fromCurrency: String,
        rates: Map<String, BigDecimal>? = null,
    ): Long {
        if (fromCurrency.equals("VND", ignoreCase = true) || amountCents == 0L) {
            return amountCents
        }
        val codeUpper = fromCurrency.uppercase()
        val rateFromApi = rates?.get(codeUpper)

        val vndEquivalent: Double = if (rateFromApi != null && rateFromApi > BigDecimal.ZERO) {
            // 1 VND = rate [code] => 1 [code] = 1 / rate VND
            amountCents.toDouble() / rateFromApi.toDouble()
        } else {
            val fallback = supportedCurrencies.find { it.code.equals(codeUpper, ignoreCase = true) }
                ?.approximateRateToVnd ?: 25_500.0
            amountCents.toDouble() * fallback
        }
        return vndEquivalent.roundToLong()
    }

    /**
     * Định dạng số tiền ngoại tệ theo chuẩn quốc tế.
     * Ví dụ: USD -> "$205" hoặc "$205.50"
     *        VND -> "5.283.055 ₫"
     */
    fun formatForeignAmount(amountCents: Long, currencyCode: String): String {
        val codeUpper = currencyCode.uppercase()
        if (codeUpper == "VND") {
            return MoneyFormatter.format(Money(amountCents))
        }

        val info = getCurrencyInfo(codeUpper)
        val majorUnits = BigDecimal(amountCents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }

        val pattern = if (amountCents % 100L == 0L) "#,##0" else "#,##0.00"
        val formattedNumber = DecimalFormat(pattern, symbols).format(majorUnits)

        return when (codeUpper) {
            "USD" -> "$$formattedNumber"
            "EUR" -> "€$formattedNumber"
            "GBP" -> "£$formattedNumber"
            "JPY" -> "¥$formattedNumber"
            "KRW" -> "₩$formattedNumber"
            "SGD" -> "S$$formattedNumber"
            "THB" -> "$formattedNumber ฿"
            else -> "$formattedNumber $codeUpper"
        }
    }
}
