package com.notepay.domain.subscription

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Money
import com.notepay.domain.model.Subscription
import kotlin.time.Clock
import kotlinx.datetime.Instant
import org.junit.Test

class SubscriptionEngineTest {

    @Test
    fun `calculateNextDueDate rolls over date by specified months`() {
        // 2026-01-15 10:00:00 UTC
        val startInstant = Instant.fromEpochMilliseconds(1768471200000L)
        val nextDue = SubscriptionEngine.calculateNextDueDate(startInstant, 1)

        val startMs = startInstant.toEpochMilliseconds()
        val nextMs = nextDue.toEpochMilliseconds()

        assertThat(nextMs).isGreaterThan(startMs)
    }

    @Test
    fun `classifyUrgency identifies due soon subscription`() {
        val now = Clock.System.now()
        val dueSoonTime = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + 2 * 24 * 60 * 60 * 1000L)

        val sub = Subscription(
            id = 1L,
            name = "Netflix",
            amount = Money(260_000_00L),
            category = "ENTERTAINMENT",
            nextDueDate = dueSoonTime,
            repeatMonths = 1,
            remindDaysBefore = 3
        )

        val urgency = SubscriptionEngine.classifyUrgency(sub, now)
        assertThat(urgency).isEqualTo(SubscriptionUrgency.DUE_SOON)
        assertThat(SubscriptionEngine.shouldTriggerReminder(sub, now)).isTrue()
    }
}
