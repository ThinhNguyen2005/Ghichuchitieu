package com.notepay.core.designsystem.formatter

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalDate
import org.junit.Test

class TransactionDateLabelFormatterTest {

    @Test
    fun `format inserts space after comma prefix when prefix lacks trailing whitespace`() {
        val today = LocalDate(2026, 11, 28)
        val formattedToday = TransactionDateLabelFormatter.format(
            target = today,
            today = today,
            todayPrefix = "Hôm nay,",
            yesterdayPrefix = "Hôm qua,",
            monthSuffixFormat = "Th%d",
        )
        assertThat(formattedToday).isEqualTo("Hôm nay, 28 Th11")

        val formattedOlder = TransactionDateLabelFormatter.format(
            target = LocalDate(2026, 11, 25),
            today = today,
            todayPrefix = "Hôm nay,",
            yesterdayPrefix = "Hôm qua,",
            monthSuffixFormat = "Th%d",
        )
        assertThat(formattedOlder).isEqualTo("25 Th11")
    }
}
