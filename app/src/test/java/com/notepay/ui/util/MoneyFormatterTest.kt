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
}
