package com.notepay.ui.formatter

import kotlinx.datetime.LocalDate

object TransactionDateLabelFormatter {
    fun format(
        target: LocalDate,
        today: LocalDate,
        todayPrefix: String,
        yesterdayPrefix: String,
        monthSuffixFormat: String,
    ): String {
        val prefix = when (today.toEpochDays() - target.toEpochDays()) {
            0L -> todayPrefix
            1L -> yesterdayPrefix
            else -> ""
        }
        return "$prefix${target.day} ${monthSuffixFormat.format(target.month.ordinal + 1)}"
    }
}
