package com.notepay.ui.util

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Money
import org.junit.Test

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
}
