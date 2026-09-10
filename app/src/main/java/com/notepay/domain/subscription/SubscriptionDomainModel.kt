package com.notepay.domain.subscription

import com.notepay.domain.money.Money
import kotlinx.datetime.*

/** Subscription Status Enum */
enum class SubscriptionStatus {
    ACTIVE,
    DUE_SOON,
    OVERDUE,
    PAUSED,
    CANCELLED
}

/** Pure Subscription Domain Model */
data class SubscriptionDomainModel(
    val id: Long,
    val name: String,
    val amount: Money,
    val repeatMonths: Int,
    val remindDaysBefore: Int,
    val nextDueDate: Instant,
    val categoryId: Long?,
    val isActive: Boolean = true
) {
    fun daysRemaining(
        now: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Long {
        val today = now.toLocalDateTime(timeZone).date
        val dueDate = nextDueDate.toLocalDateTime(timeZone).date
        return (dueDate - today).days.toLong()
    }

    fun isOverdue(
        now: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Boolean = daysRemaining(now, timeZone) < 0 && isActive

    fun isDueSoon(
        now: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Boolean {
        val days = daysRemaining(now, timeZone)
        return days in 0..remindDaysBefore.toLong() && isActive
    }

    fun status(
        now: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): SubscriptionStatus = when {
        !isActive -> SubscriptionStatus.PAUSED
        isOverdue(now, timeZone) -> SubscriptionStatus.OVERDUE
        isDueSoon(now, timeZone) -> SubscriptionStatus.DUE_SOON
        else -> SubscriptionStatus.ACTIVE
    }

    /** Calculates next due date based on repeatMonths using local calendar math */
    fun calculateNextDueDate(
        fromInstant: Instant = nextDueDate,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Instant {
        val localDateTime = fromInstant.toLocalDateTime(timeZone)
        val nextDate = localDateTime.date.plus(DatePeriod(months = repeatMonths))
        return LocalDateTime(nextDate, localDateTime.time).toInstant(timeZone)
    }
}
