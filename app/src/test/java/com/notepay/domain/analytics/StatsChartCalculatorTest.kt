package com.notepay.domain.analytics

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.money.Money
import org.junit.Test

class StatsChartCalculatorTest {

    @Test
    fun `average and percentageChange handle empty values baselines and deltas`() {
        assertThat(StatsChartCalculator.average(emptyList())).isEqualTo(0L)
        assertThat(StatsChartCalculator.average(listOf(100L, 200L, 400L))).isEqualTo(233L)

        assertThat(StatsChartCalculator.percentageChange(120L, 100L)).isWithin(.01f).of(20f)
        assertThat(StatsChartCalculator.percentageChange(80L, 100L)).isWithin(.01f).of(-20f)
        assertThat(StatsChartCalculator.percentageChange(20L, 0L)).isNull()
    }

    @Test
    fun `trendAxisScale picks thousands or millions unit and expands for forecast`() {
        val smallAxis = StatsChartCalculator.trendAxisScale(listOf(20_000_000L), null)
        assertThat(smallAxis.unit).isEqualTo(TrendAxisUnit.THOUSANDS)
        assertThat(smallAxis.labels).containsExactly(200f, 150f, 100f, 50f, 0f).inOrder()

        val forecastAxis = StatsChartCalculator.trendAxisScale(listOf(5_000_000L), Money(150_000_000L))
        assertThat(forecastAxis.unit).isEqualTo(TrendAxisUnit.MILLIONS)
        assertThat(forecastAxis.topInCents).isAtLeast(150_000_000L)
    }

    @Test
    fun `shouldShowForecast only activates for current expense with positive forecast`() {
        data class Case(val isExpense: Boolean, val currentMonth: Boolean, val forecast: Money?, val expected: Boolean)
        listOf(
            Case(true, true, Money(1L), true),
            Case(false, true, Money(1L), false),
            Case(true, false, Money(1L), false),
            Case(true, true, null, false),
        ).forEach { c ->
            assertThat(StatsChartCalculator.shouldShowForecast(c.isExpense, c.currentMonth, c.forecast))
                .isEqualTo(c.expected)
        }
    }

    @Test
    fun `forecastDisplayState classifies projection passed budget and hidden states`() {
        data class Case(
            val isExpense: Boolean,
            val currentMonth: Boolean,
            val actual: Long,
            val forecast: Money?,
            val budget: Long?,
            val expected: ForecastDisplayState,
        )
        listOf(
            Case(true, true, 100L, Money(150L), null, ForecastDisplayState.PROJECTION),
            Case(true, true, 150L, Money(150L), null, ForecastDisplayState.PASSED),
            Case(true, true, 200L, Money(150L), null, ForecastDisplayState.PASSED),
            Case(true, true, 100L, Money(150L), 150L, ForecastDisplayState.WITHIN_BUDGET),
            Case(true, true, 200L, Money(150L), 300L, ForecastDisplayState.WITHIN_BUDGET),
            Case(true, true, 100L, Money(150L), 100L, ForecastDisplayState.OVER_BUDGET),
            Case(false, true, 100L, Money(150L), null, ForecastDisplayState.HIDDEN),
            Case(true, false, 100L, Money(150L), null, ForecastDisplayState.HIDDEN),
            Case(true, true, 100L, Money.ZERO, null, ForecastDisplayState.HIDDEN),
        ).forEach { c ->
            assertThat(
                StatsChartCalculator.forecastDisplayState(
                    isExpense = c.isExpense,
                    isSelectedMonthCurrent = c.currentMonth,
                    actualAmountInCents = c.actual,
                    forecast = c.forecast,
                    budgetInCents = c.budget,
                ),
            ).isEqualTo(c.expected)
        }
    }

    @Test
    fun `forecastMarkerFraction computes position when actual reaches forecast and hides otherwise`() {
        assertThat(StatsChartCalculator.forecastMarkerFraction(150L, Money(150L), 300L))
            .isWithin(.0001f).of(.5f)
        assertThat(StatsChartCalculator.forecastMarkerFraction(500L, Money(500L), 300L))
            .isWithin(.0001f).of(1f)

        listOf(
            Triple(100L, Money(150L), 300L),
            Triple(200L, null, 300L),
            Triple(200L, Money(150L), 0L),
        ).forEach { (actual, forecast, top) ->
            assertThat(StatsChartCalculator.forecastMarkerFraction(actual, forecast, top)).isNull()
        }
    }
}
