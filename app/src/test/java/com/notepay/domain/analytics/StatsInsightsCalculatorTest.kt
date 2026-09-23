package com.notepay.domain.analytics

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.Subscription
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.time.Instant

class StatsInsightsCalculatorTest {

    @Test
    fun `forecast with zero current day has zero projection and no trend`() {
        val result = StatsInsightsCalculator.computeForecast(
            currentMonthExpenseInCents = 10_000L,
            currentDay = 0,
            daysInMonth = 30,
            limitInCents = null,
            previousMonthDailyAverageInCents = 1_000L,
        )

        assertThat(result.dailyAverageInCents).isEqualTo(0L)
        assertThat(result.projectedSpendInCents).isEqualTo(0L)
        assertThat(result.trendPercent).isEqualTo(-100f)
    }

    @Test
    fun `dynamic budget clamps negative remaining budget and handles no remaining days`() {
        val result = StatsInsightsCalculator.computeDynamicDailyBudget(
            limitInCents = 1_000L,
            spentExceptTodayInCents = 2_000L,
            spentTodayInCents = 500L,
            remainingDays = 0,
            daysInMonth = 30,
            currentDay = 31,
            currentMonthExpenseInCents = 2_500L,
        )

        assertThat(result.dailyBudgetInCents).isEqualTo(0L)
        assertThat(result.remainingTodayInCents).isEqualTo(0L)
        assertThat(result.tomorrowBudgetInCents).isEqualTo(0L)
        assertThat(result.isExceeded).isTrue()
    }

    @Test
    fun `advice uses strict food threshold and suppresses acknowledged feedback`() {
        val input = StatsAdviceInput(
            categoryShares = listOf(CategoryExpenseShare(Category.FOOD.id, 3_500L, 0.35f)),
            expenseInCents = 10_000L,
            incomeInCents = 20_000L,
            subscriptions = emptyList(),
            currentMonthExpenseInCents = 10_000L,
            spentTodayInCents = 100L,
            currentDay = 10,
            daysInMonth = 30,
            remainingDays = 21,
            limitInCents = null,
            previousMonthDailyAverageInCents = null,
            feedbacks = emptyMap(),
            now = Instant.parse("2026-09-20T00:00:00Z"),
        )

        assertThat(StatsInsightsCalculator.computeAdvices(input)).isEmpty()
        assertThat(
            StatsInsightsCalculator.computeAdvices(
                input.copy(categoryShares = listOf(CategoryExpenseShare(Category.FOOD.id, 3_600L, 0.36f)))
            )
        ).hasSize(1)
        assertThat(
            StatsInsightsCalculator.computeAdvices(
                input.copy(
                    categoryShares = listOf(CategoryExpenseShare(Category.FOOD.id, 3_600L, 0.36f)),
                    feedbacks = mapOf("advice_food" to 1),
                )
            )
        ).isEmpty()
    }
}
