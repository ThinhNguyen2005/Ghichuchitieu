package com.notepay.ui.util

import com.google.common.truth.Truth.assertThat
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.domain.money.Money
import org.junit.Test
import java.math.BigDecimal

class MoneyFormatterTest {
    @Test
    fun formatCompactVietnamese_preservesCalendarUnits() {
        assertThat(MoneyFormatter.formatCompactVietnamese(Money(65_000L))).isEqualTo("650")
        assertThat(MoneyFormatter.formatCompactVietnamese(Money(650_000L))).isEqualTo("6k")
        assertThat(MoneyFormatter.formatCompactVietnamese(Money(100_000_000L))).isEqualTo("1Tr")
        assertThat(MoneyFormatter.formatCompactVietnamese(Money(125_000_000L))).isEqualTo("1.3Tr")
    }

    @Test
    fun formatCustom_formatsWithDifferentSymbolsAndPositions() {
        val money = Money(1_250_000_00L) // 1.250.000

        // Default VND after with dot
        val vndAfter = MoneyFormatter.formatCustom(money, currencySymbol = "₫", symbolPosition = "after", thousandSeparator = "dot")
        assertThat(vndAfter).isEqualTo("1.250.000 ₫")

        // USD before with comma
        val usdBefore = MoneyFormatter.formatCustom(money, currencySymbol = "$", symbolPosition = "before", thousandSeparator = "comma")
        assertThat(usdBefore).isEqualTo("$1,250,000")

        // EUR after with dot
        val eurAfter = MoneyFormatter.formatCustom(money, currencySymbol = "€", symbolPosition = "after", thousandSeparator = "dot")
        assertThat(eurAfter).isEqualTo("1.250.000 €")
    }

    @Test
    fun format_usesUnadornedDongSymbol() {
        MoneyFormatter.resetToDefault()
        assertThat(MoneyFormatter.format(Money(500_000L))).isEqualTo("5.000 đ")
    }

    @Test
    fun updateConfig_changesActiveFormatAndResetRestoresDefault() {
        try {
            MoneyFormatter.updateConfig(
                currencyCode = "USD",
                symbolPosition = "before",
                thousandSeparator = "comma",
            )
            assertThat(MoneyFormatter.format(Money(1_250_000_00L))).isEqualTo("$1,250,000")
            assertThat(MoneyFormatter.format(Money(-500_000_00L))).isEqualTo("-$500,000")
        } finally {
            MoneyFormatter.resetToDefault()
        }
        assertThat(MoneyFormatter.format(Money(1_250_000_00L))).isEqualTo("1.250.000 đ")
    }

    @Test
    fun updateConfig_withExchangeRate_convertsVndToTargetCurrency() {
        try {
            MoneyFormatter.updateConfig(
                currencyCode = "USD",
                symbolPosition = "before",
                thousandSeparator = "comma",
                ratePerVnd = BigDecimal("0.00004"),
            )
            assertThat(MoneyFormatter.format(Money(1_250_000_00L))).isEqualTo("$50.00")
            assertThat(MoneyFormatter.format(Money(-450_000_00L))).isEqualTo("-$18.00")
            assertThat(MoneyFormatter.format(Money.ZERO)).isEqualTo("$0")

            MoneyFormatter.updateConfig(
                currencyCode = "JPY",
                symbolPosition = "before",
                thousandSeparator = "comma",
                ratePerVnd = BigDecimal("0.0062"),
            )
            assertThat(MoneyFormatter.format(Money(1_000_000_00L))).isEqualTo("¥6,200")
        } finally {
            MoneyFormatter.resetToDefault()
        }
    }
}
