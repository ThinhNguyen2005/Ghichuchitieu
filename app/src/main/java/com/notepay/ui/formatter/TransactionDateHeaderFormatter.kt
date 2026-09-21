package com.notepay.ui.formatter

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/** Formats grouped transaction-list headers while leaving localized strings to the UI layer. */
object TransactionDateHeaderFormatter {
    fun format(
        target: LocalDate,
        today: LocalDate,
        todayFormat: String,
        yesterdayFormat: String,
        otherFormat: String,
        dayOfWeek: String,
    ): String {
        val day = target.day
        val month = target.month.number
        return when (target.toEpochDays() - today.toEpochDays()) {
            0L -> todayFormat.format(day, month)
            -1L -> yesterdayFormat.format(day, month)
            else -> otherFormat.format(dayOfWeek, day, month)
        }
    }
}
