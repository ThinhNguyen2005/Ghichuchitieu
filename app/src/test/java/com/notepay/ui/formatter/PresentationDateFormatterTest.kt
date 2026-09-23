package com.notepay.ui.formatter

import com.google.common.truth.Truth.assertThat
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.Test

class PresentationDateFormatterTest {
    private val timeZone = TimeZone.UTC

    @Test
    fun formatDate_usesDayMonthAndYear() {
        val instant = Instant.parse("2026-09-20T23:04:00Z")

        assertThat(PresentationDateFormatter.formatDate(instant, timeZone))
            .isEqualTo("20/09/2026")
    }

    @Test
    fun formatDayMonth_returnsEmptyForMissingInstant() {
        assertThat(PresentationDateFormatter.formatDayMonth(null, timeZone)).isEmpty()
    }

    @Test
    fun formatDayMonthTime_includesTime() {
        val instant = Instant.parse("2026-09-20T03:04:00Z")

        assertThat(PresentationDateFormatter.formatDayMonthTime(instant, timeZone))
            .isEqualTo("20/09 03:04")
    }
}
