package com.notepay.domain.money

/** Configuration options for Money Formatting */
data class MoneyFormatConfig(
    val localeLanguage: String = "vi",
    val localeCountry: String = "VN",
    val currencySymbol: String = "₫",
    val showSymbol: Boolean = true,
    val compactFormat: Boolean = false,
    val useVietnameseUnits: Boolean = false // e.g., "k"/"Tr" vs "K"/"M"/"B"
)

/** Thread-safe Money Formatter Engine contract */
interface MoneyFormatterEngine {
    fun format(money: Money, config: MoneyFormatConfig = MoneyFormatConfig()): String
    fun formatCompact(money: Money, useVietnameseUnits: Boolean = false): String
    fun formatInput(digitsOnly: String): String
}
