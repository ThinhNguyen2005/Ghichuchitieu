package com.notepay.ui.formatter

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalDate
import org.junit.Test

class TransactionDateHeaderFormatterTest {
    private val today = LocalDate(2026, 9, 20)

    @Test
    fun format_today_usesTodayTemplate() {
        assertThat(format(LocalDate(2026, 9, 20)))
            .isEqualTo("Today, 20 M9")
    }

    @Test
    fun format_yesterday_usesYesterdayTemplate() {
        assertThat(format(LocalDate(2026, 9, 19)))
            .isEqualTo("Yesterday, 19 M9")
    }

    @Test
    fun format_otherDate_includesLocalizedWeekday() {
        assertThat(format(LocalDate(2026, 9, 18), dayOfWeek = "Friday"))
            .isEqualTo("Friday, 18 M9")
    }

    private fun format(date: LocalDate, dayOfWeek: String = "") =
        TransactionDateHeaderFormatter.format(
            target = date,
            today = today,
            todayFormat = "Today, %1\$d M%2\$d",
            yesterdayFormat = "Yesterday, %1\$d M%2\$d",
            otherFormat = "%1\$s, %2\$d M%3\$d",
            dayOfWeek = dayOfWeek,
        )
}
