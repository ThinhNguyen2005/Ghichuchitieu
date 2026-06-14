package com.notepay.ui.feature.addtransaction

import com.notepay.domain.model.Money

private const val MAX_MAJOR_UNITS = Long.MAX_VALUE / 100

data class AmountParseResult(
    val input: String,
    val amount: Money?,
    val error: FieldError?,
)

object AmountParser {
    fun parse(text: String): AmountParseResult {
        val digits = text.filter(Char::isDigit).trimStart('0')
        if (digits.isBlank()) {
            return AmountParseResult(input = "", amount = null, error = FieldError.AMOUNT_EMPTY)
        }

        val majorUnits = digits.toLongOrNull()
            ?: return AmountParseResult(input = digits, amount = null, error = FieldError.AMOUNT_INVALID)

        if (majorUnits <= 0 || majorUnits > MAX_MAJOR_UNITS) {
            return AmountParseResult(input = digits, amount = null, error = FieldError.AMOUNT_INVALID)
        }

        return AmountParseResult(input = majorUnits.toString(), amount = Money(majorUnits * 100), error = null)
    }
}
