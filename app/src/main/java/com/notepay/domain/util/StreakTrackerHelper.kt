package com.notepay.domain.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.DatePeriod

/**
 * Tiện ích tính toán chuỗi ngày ghi chép liên tiếp (Streak 🔥).
 */
object StreakTrackerHelper {

    /**
     * Tính toán số ngày ghi chép liên tiếp dựa trên danh sách thời gian giao dịch.
     * @param transactionInstants Danh sách Instant của các giao dịch trong hệ thống.
     * @param today Ngày hiện tại.
     * @param timeZone Múi giờ tính toán.
     * @return Số ngày liên tiếp đã ghi chép.
     */
    fun calculateStreak(
        transactionInstants: List<Instant>,
        today: LocalDate,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Int {
        if (transactionInstants.isEmpty()) return 0

        val distinctDates = transactionInstants
            .map { it.toLocalDateTime(timeZone).date }
            .toSet()

        val yesterday = today.minus(DatePeriod(days = 1))

        // Nếu hôm nay có giao dịch, bắt đầu đếm từ hôm nay.
        // Nếu hôm nay chưa ghi nhưng hôm qua có ghi, bắt đầu đếm từ hôm qua (chưa mất chuỗi).
        var currentDate = when {
            distinctDates.contains(today) -> today
            distinctDates.contains(yesterday) -> yesterday
            else -> return 0
        }

        var streak = 0
        while (distinctDates.contains(currentDate)) {
            streak++
            currentDate = currentDate.minus(DatePeriod(days = 1))
        }

        return streak
    }
}
