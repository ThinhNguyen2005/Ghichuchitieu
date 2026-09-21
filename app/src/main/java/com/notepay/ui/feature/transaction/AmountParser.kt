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
    fun parse(text: String): AmountParseResult {
        val digits = text.filter(Char::isDigit).trimStart('0')
        if (digits.isBlank()) {
            return AmountParseResult(input = "", amount = null, error = AmountParseError.EMPTY)
        }

        val majorUnits = digits.toLongOrNull()
            ?: return AmountParseResult(input = digits, amount = null, error = AmountParseError.INVALID)

        if (majorUnits <= 0L || majorUnits > Money.MAX_MAJOR_UNITS) {
            return AmountParseResult(input = digits, amount = null, error = AmountParseError.INVALID)
        }

        return AmountParseResult(
            input = majorUnits.toString(),
            amount = Money.fromMajorUnit(majorUnits),
            error = null,
        )
    }
}
