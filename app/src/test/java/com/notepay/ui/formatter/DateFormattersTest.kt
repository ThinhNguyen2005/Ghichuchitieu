package com.notepay.ui.formatter

import com.google.common.truth.Truth.assertThat
import com.notepay.core.designsystem.formatter.PresentationDateFormatter
import com.notepay.core.designsystem.formatter.TransactionDateHeaderFormatter
import com.notepay.core.designsystem.formatter.TransactionDateLabelFormatter
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Test

class DateFormattersTest {

    private val timeZone = TimeZone.UTC
    private val today = LocalDate(2026, 9, 20)

    @Test
    fun `PresentationDateFormatter formats dates times and handles null instants`() {
        assertThat(PresentationDateFormatter.formatDate(Instant.parse("2026-09-20T23:04:00Z"), timeZone))
            .isEqualTo("20/09/2026")
        assertThat(PresentationDateFormatter.formatDayMonth(null, timeZone)).isEmpty()
        assertThat(PresentationDateFormatter.formatDayMonthTime(Instant.parse("2026-09-20T03:04:00Z"), timeZone))
            .isEqualTo("20/09 03:04")
    }

    @Test
    fun `TransactionDateHeaderFormatter formats today yesterday and earlier dates`() {
        listOf(
            Triple(LocalDate(2026, 9, 20), "", "Today, 20 M9"),
            Triple(LocalDate(2026, 9, 19), "", "Yesterday, 19 M9"),
            Triple(LocalDate(2026, 9, 18), "Friday", "Friday, 18 M9"),
        ).forEach { (target, weekday, expected) ->
            val actual = TransactionDateHeaderFormatter.format(
                target = target,
                today = today,
                todayFormat = "Today, %1\$d M%2\$d",
                yesterdayFormat = "Yesterday, %1\$d M%2\$d",
                otherFormat = "%1\$s, %2\$d M%3\$d",
                dayOfWeek = weekday,
            )
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `TransactionDateLabelFormatter formats today yesterday and earlier dates`() {
        listOf(
            LocalDate(2026, 9, 20) to "Hôm nay, 20 tháng 9",
            LocalDate(2026, 9, 19) to "Hôm qua, 19 tháng 9",
            LocalDate(2026, 8, 31) to "31 tháng 8",
        ).forEach { (target, expected) ->
            val actual = TransactionDateLabelFormatter.format(
                target = target,
                today = today,
                todayPrefix = "Hôm nay, ",
                yesterdayPrefix = "Hôm qua, ",
                monthSuffixFormat = "tháng %d",
            )
            assertThat(actual).isEqualTo(expected)
        }
    }
}
