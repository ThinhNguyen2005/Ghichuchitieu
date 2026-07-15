package com.notepay.domain.analytics

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.Test

class SpendingForecastEngineTest {
    private val today = LocalDate(2026, 7, 15)

    @Test
    fun `stable daily spending produces stable month projection`() {
        val expenses = dailyHistory(days = 60, amountInCents = 100_000_00)

        val result = SpendingForecastEngine.forecast(
            expenses = expenses,
            today = today,
            daysInMonth = 31,
            budgetLimitInCents = 4_000_000_00,
        )

        assertThat(result.spentSoFarInCents).isEqualTo(1_500_000_00)
        assertThat(result.predictedMonthTotalInCents).isWithin(1_000L).of(3_100_000_00)
        assertThat(result.overBudgetProbability).isEqualTo(0.0)
        assertThat(result.confidence).isEqualTo(ForecastConfidence.HIGH)
    }

    @Test
    fun `forecast reports high probability when every simulation exceeds budget`() {
        val result = SpendingForecastEngine.forecast(
            expenses = dailyHistory(days = 60, amountInCents = 100_000_00),
            today = today,
            daysInMonth = 31,
            budgetLimitInCents = 2_000_000_00,
        )

        assertThat(result.overBudgetProbability).isEqualTo(1.0)
        assertThat(result.lowerBoundInCents).isGreaterThan(2_000_000_00)
    }

    @Test
    fun `same input always returns same bootstrap interval`() {
        val expenses = (0 until 45).map { offset ->
            DailyExpense(
                date = today.minus(DatePeriod(days = offset)),
                amountInCents = if (offset % 7 == 0) 500_000_00 else 80_000_00,
            )
        }

        val first = SpendingForecastEngine.forecast(expenses, today, 31, 4_000_000_00)
        val second = SpendingForecastEngine.forecast(expenses, today, 31, 4_000_000_00)

        assertThat(second).isEqualTo(first)
        assertThat(first.upperBoundInCents).isAtLeast(first.predictedMonthTotalInCents)
        assertThat(first.lowerBoundInCents).isAtMost(first.predictedMonthTotalInCents)
    }

    @Test
    fun `empty history returns low confidence zero forecast`() {
        val result = SpendingForecastEngine.forecast(emptyList(), today, 31, null)

        assertThat(result.predictedMonthTotalInCents).isEqualTo(0L)
        assertThat(result.overBudgetProbability).isNull()
        assertThat(result.confidence).isEqualTo(ForecastConfidence.LOW)
    }

    private fun dailyHistory(days: Int, amountInCents: Long): List<DailyExpense> =
        (0 until days).map { offset ->
            DailyExpense(today.minus(DatePeriod(days = offset)), amountInCents)
        }
}
