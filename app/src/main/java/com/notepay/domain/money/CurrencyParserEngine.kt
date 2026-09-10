package com.notepay.domain.money

import java.util.Locale

/** Result of parsing user text into Money */
sealed interface MoneyParseResult {
    data class Success(val money: Money, val sanitizedInput: String) : MoneyParseResult
    data class InvalidInput(val rawInput: String, val reason: String) : MoneyParseResult
}

/** Unified Currency & Input Parsing Engine */
object CurrencyParserEngine {
    private const val MAX_MAJOR_UNITS = Long.MAX_VALUE / 100L

    /**
     * Standardizes parsing raw string input into a valid Money object.
     * Handles:
     * - Thousands separators (., whitespace)
     * - Shorthand multipliers: "50k" -> 50,000; "1.5m" / "1.5tr" -> 1,500,000
     * - Currency symbols (₫, VND, $)
     * - Overflow protection & negative sign handling
     */
    fun parseInputText(input: String): MoneyParseResult {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return MoneyParseResult.InvalidInput(input, "Số tiền không được để trống")
        }

        // Handle sign
        val isNegative = trimmed.startsWith("-")
        val cleanString = trimmed.removePrefix("-").removePrefix("+").trim()

        // Match shorthand units like 50k, 1.5M, 2.5tr, 100.000đ, 50,000 VND
        val multiplier = when {
            cleanString.endsWith("k", ignoreCase = true) -> 1_000.0
            cleanString.endsWith("m", ignoreCase = true) || cleanString.endsWith("tr", ignoreCase = true) -> 1_000_000.0
            cleanString.endsWith("b", ignoreCase = true) || cleanString.endsWith("tỷ", ignoreCase = true) -> 1_000_000_000.0
            else -> 1.0
        }

        // Strip non-numeric characters except decimal dot/comma
        val numericPart = cleanString
            .replace(Regex("""[^\d.,]"""), "")
            .replace(" ", "")

        if (numericPart.isBlank()) {
            return MoneyParseResult.InvalidInput(input, "Không tìm thấy số tiền hợp lệ")
        }

        // Normalize number string based on shorthand multipliers and Vietnamese thousands separators
        val normalizedNumber = if (multiplier > 1.0) {
            // For shorthand like 1.5M or 2.5k, treat comma/dot as decimal separator
            numericPart.replace(",", ".")
        } else {
            // Standard VND numbers: dots (.) and commas (,) in grouping like 100.000 or 50,000 are thousands separators
            val isThousandsGrouped = Regex("""^\d{1,3}([.,]\d{3})+$""").matches(numericPart)
            if (isThousandsGrouped) {
                numericPart.replace(".", "").replace(",", "")
            } else {
                // If ambiguous, strip grouping symbols for integer amounts
                val hasTrailingThreeDigits = Regex("""[.,]\d{3}$""").containsMatchIn(numericPart)
                if (hasTrailingThreeDigits) {
                    numericPart.replace(".", "").replace(",", "")
                } else {
                    numericPart.replace(",", "")
                }
            }
        }

        val parsedDouble = normalizedNumber.toDoubleOrNull()
            ?: return MoneyParseResult.InvalidInput(input, "Định dạng số không hợp lệ")

        val majorUnits = (parsedDouble * multiplier).toLong()
        if (majorUnits <= 0 || majorUnits > MAX_MAJOR_UNITS) {
            return MoneyParseResult.InvalidInput(input, "Số tiền quá lớn hoặc không hợp lệ")
        }

        val finalCents = if (isNegative) -majorUnits * 100L else majorUnits * 100L
        return MoneyParseResult.Success(
            money = Money(finalCents),
            sanitizedInput = majorUnits.toString()
        )
    }

    /** Parses raw OCR or QR payload string into Money */
    fun parseOcrOrQrPayload(rawPayload: String): MoneyParseResult {
        val digitsOnly = rawPayload.filter { it.isDigit() || it == '.' || it == ',' }
        return parseInputText(digitsOnly)
    }
}
