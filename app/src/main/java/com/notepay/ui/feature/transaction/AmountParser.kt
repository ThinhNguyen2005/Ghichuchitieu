package com.notepay.ui.feature.transaction

import com.notepay.domain.model.Money

enum class AmountParseError {
    EMPTY,
    INVALID,
}

data class AmountParseResult(
    val input: String,
    val amount: Money?,
    val error: AmountParseError?,
)

object AmountParser {
    /**
     * Giới hạn tối đa 12 chữ số: 999.999.999.999 ₫ (999 tỷ đồng).
     * Bao quát toàn bộ giao dịch chi tiêu lẫn mua sắm tài sản lớn (bất động sản, ô tô).
     */
    const val MAX_DIGITS = 12
    const val MAX_AMOUNT_MAJOR_UNITS: Long = 999_999_999_999L

    fun parse(text: String): AmountParseResult {
        val rawDigits = text.filter(Char::isDigit).trimStart('0')
        if (rawDigits.isBlank()) {
            return AmountParseResult(input = "", amount = null, error = AmountParseError.EMPTY)
        }

        val clampedInput = rawDigits.take(MAX_DIGITS)

        if (rawDigits.length > MAX_DIGITS) {
            return AmountParseResult(input = clampedInput, amount = null, error = AmountParseError.INVALID)
        }

        val majorUnits = rawDigits.toLongOrNull()
            ?: return AmountParseResult(input = clampedInput, amount = null, error = AmountParseError.INVALID)

        if (majorUnits <= 0L || majorUnits > MAX_AMOUNT_MAJOR_UNITS) {
            return AmountParseResult(input = clampedInput, amount = null, error = AmountParseError.INVALID)
        }

        return AmountParseResult(
            input = majorUnits.toString(),
            amount = Money.fromMajorUnit(majorUnits),
            error = null,
        )
    }
}
