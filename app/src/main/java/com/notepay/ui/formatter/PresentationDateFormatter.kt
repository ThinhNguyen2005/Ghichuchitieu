package com.notepay.ui.formatter

import java.util.Locale
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

/** Shared date/time output for presentation-only surfaces. */
object PresentationDateFormatter {
    fun formatDate(
        instant: Instant?,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        if (instant == null) return ""
        val localDateTime = instant.toLocalDateTime(timeZone)
        return String.format(
            Locale.US,
            "%02d/%02d/%d",
            localDateTime.day,
            localDateTime.month.number,
            localDateTime.year,
        )
    }

    fun formatDayMonth(
        instant: Instant?,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        if (instant == null) return ""
        val localDateTime = instant.toLocalDateTime(timeZone)
        return String.format(
            Locale.US,
            "%02d/%02d",
            localDateTime.day,
            localDateTime.month.number,
        )
    }

    fun formatDayMonthTime(
        instant: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        val localDateTime = instant.toLocalDateTime(timeZone)
        return String.format(
            Locale.US,
            "%02d/%02d %02d:%02d",
            localDateTime.day,
            localDateTime.month.number,
            localDateTime.hour,
            localDateTime.minute,
        )
    }
}
