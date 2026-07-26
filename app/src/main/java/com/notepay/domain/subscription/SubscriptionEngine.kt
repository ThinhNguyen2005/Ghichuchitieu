package com.notepay.domain.subscription

import com.notepay.domain.model.Subscription
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

enum class SubscriptionUrgency {
    EXPIRED,
    DUE_SOON,
    UPCOMING
}

/** Pure Domain Engine for Subscription Recurrence & Reminder Calculations */
object SubscriptionEngine {

    /** Calculates next due date after subscription renewal */
    fun calculateNextDueDate(
        currentDueDate: Instant,
        repeatMonths: Int,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Instant {
        val safeMonths = if (repeatMonths <= 0) 1 else repeatMonths
        val localDateTime = currentDueDate.toLocalDateTime(timeZone)
        val newDate = localDateTime.date.plus(safeMonths, DateTimeUnit.MONTH)
        return LocalDateTime(newDate, localDateTime.time).toInstant(timeZone)
    }

    /** Assesses subscription urgency state relative to reference time */
    fun classifyUrgency(
        subscription: Subscription,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): SubscriptionUrgency {
        val dueMs = subscription.nextDueDate.toEpochMilliseconds()
        val nowMs = now.toEpochMilliseconds()
        val remindMs = subscription.remindDaysBefore * 24 * 60 * 60 * 1000L

        return when {
            dueMs < nowMs -> SubscriptionUrgency.EXPIRED
            dueMs - nowMs <= remindMs -> SubscriptionUrgency.DUE_SOON
            else -> SubscriptionUrgency.UPCOMING
        }
    }

    /** Checks if a notification should be sent today */
    fun shouldTriggerReminder(
        subscription: Subscription,
        now: Instant = Clock.System.now()
    ): Boolean {
        val urgency = classifyUrgency(subscription, now)
        return urgency == SubscriptionUrgency.DUE_SOON || urgency == SubscriptionUrgency.EXPIRED
    }
}
