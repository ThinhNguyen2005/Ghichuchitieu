package com.notepay.ui.formatter

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalDate
import org.junit.Test

class TransactionDateLabelFormatterTest {
    @Test
    fun `formats today yesterday and an earlier date`() {
        val today = LocalDate(2026, 9, 20)

        assertThat(format(LocalDate(2026, 9, 20), today)).isEqualTo("Hôm nay, 20 tháng 9")
        assertThat(format(LocalDate(2026, 9, 19), today)).isEqualTo("Hôm qua, 19 tháng 9")
        assertThat(format(LocalDate(2026, 8, 31), today)).isEqualTo("31 tháng 8")
    }

    private fun format(target: LocalDate, today: LocalDate): String =
        TransactionDateLabelFormatter.format(
            target = target,
            today = today,
            todayPrefix = "Hôm nay, ",
            yesterdayPrefix = "Hôm qua, ",
            monthSuffixFormat = "tháng %d",
        )
}
