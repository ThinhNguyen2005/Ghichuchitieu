package com.notepay.domain.analytics

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Money
import org.junit.Test

class StatsChartCalculatorTest {
    @Test fun `average returns zero for empty values`() { assertThat(StatsChartCalculator.average(emptyList())).isEqualTo(0L) }
    @Test fun `average calculates integer mean`() { assertThat(StatsChartCalculator.average(listOf(100L, 200L, 400L))).isEqualTo(233L) }

    @Test fun `axis uses thousand labels for small real amounts`() {
        val axis = StatsChartCalculator.trendAxisScale(listOf(20_000_000L), null)
        assertThat(axis.unit).isEqualTo(TrendAxisUnit.THOUSANDS)
        assertThat(axis.labels).containsExactly(200f, 150f, 100f, 50f, 0f).inOrder()
    }

    @Test fun `forecast expands chart top without changing its unit`() {
        val axis = StatsChartCalculator.trendAxisScale(listOf(5_000_000L), Money(150_000_000L))
        assertThat(axis.unit).isEqualTo(TrendAxisUnit.MILLIONS)
        assertThat(axis.topInCents).isAtLeast(150_000_000L)
    }

    @Test fun `percentage change handles increase decrease and zero baseline`() {
        assertThat(StatsChartCalculator.percentageChange(120L, 100L)).isWithin(.01f).of(20f)
        assertThat(StatsChartCalculator.percentageChange(80L, 100L)).isWithin(.01f).of(-20f)
        assertThat(StatsChartCalculator.percentageChange(20L, 0L)).isNull()
    }

    @Test fun `forecast is only shown for current expense with a value`() {
        assertThat(StatsChartCalculator.shouldShowForecast(isExpense = true, isSelectedMonthCurrent = true, Money(1L))).isTrue()
        assertThat(StatsChartCalculator.shouldShowForecast(isExpense = false, isSelectedMonthCurrent = true, Money(1L))).isFalse()
        assertThat(StatsChartCalculator.shouldShowForecast(isExpense = true, isSelectedMonthCurrent = false, Money(1L))).isFalse()
        assertThat(StatsChartCalculator.shouldShowForecast(isExpense = true, isSelectedMonthCurrent = true, null)).isFalse()
    }

    @Test fun `forecast display keeps projection when forecast is above actual`() {
        assertThat(
            StatsChartCalculator.forecastDisplayState(
                isExpense = true,
                isSelectedMonthCurrent = true,
                actualAmountInCents = 100L,
                forecast = Money(150L),
            ),
        ).isEqualTo(ForecastDisplayState.PROJECTION)
    }

    @Test fun `forecast display marks forecast reached when forecast equals actual`() {
        assertThat(
            StatsChartCalculator.forecastDisplayState(
                isExpense = true,
                isSelectedMonthCurrent = true,
                actualAmountInCents = 150L,
                forecast = Money(150L),
            ),
        ).isEqualTo(ForecastDisplayState.REACHED)
    }

    @Test fun `forecast display marks forecast exceeded when actual is above forecast`() {
        assertThat(
            StatsChartCalculator.forecastDisplayState(
                isExpense = true,
                isSelectedMonthCurrent = true,
                actualAmountInCents = 200L,
                forecast = Money(150L),
            ),
        ).isEqualTo(ForecastDisplayState.EXCEEDED)
    }

    @Test fun `forecast display hides ineligible forecasts`() {
        assertThat(
            StatsChartCalculator.forecastDisplayState(
                isExpense = false,
                isSelectedMonthCurrent = true,
                actualAmountInCents = 100L,
                forecast = Money(150L),
            ),
        ).isEqualTo(ForecastDisplayState.HIDDEN)
        assertThat(
            StatsChartCalculator.forecastDisplayState(
                isExpense = true,
                isSelectedMonthCurrent = false,
                actualAmountInCents = 100L,
                forecast = Money(150L),
            ),
        ).isEqualTo(ForecastDisplayState.HIDDEN)
        assertThat(
            StatsChartCalculator.forecastDisplayState(
                isExpense = true,
                isSelectedMonthCurrent = true,
                actualAmountInCents = 100L,
                forecast = Money.ZERO,
            ),
        ).isEqualTo(ForecastDisplayState.HIDDEN)
    }

    @Test fun `forecast marker returns bounded fraction for reached and exceeded states`() {
        assertThat(
            StatsChartCalculator.forecastMarkerFraction(
                state = ForecastDisplayState.REACHED,
                forecast = Money(150L),
                axisTopInCents = 300L,
            ),
        ).isWithin(.0001f).of(.5f)
        assertThat(
            StatsChartCalculator.forecastMarkerFraction(
                state = ForecastDisplayState.EXCEEDED,
                forecast = Money(500L),
                axisTopInCents = 300L,
            ),
        ).isWithin(.0001f).of(1f)
    }

    @Test fun `forecast marker is hidden for projection and invalid inputs`() {
        assertThat(
            StatsChartCalculator.forecastMarkerFraction(
                state = ForecastDisplayState.PROJECTION,
                forecast = Money(150L),
                axisTopInCents = 300L,
            ),
        ).isNull()
        assertThat(
            StatsChartCalculator.forecastMarkerFraction(
                state = ForecastDisplayState.EXCEEDED,
                forecast = null,
                axisTopInCents = 300L,
            ),
        ).isNull()
        assertThat(
            StatsChartCalculator.forecastMarkerFraction(
                state = ForecastDisplayState.REACHED,
                forecast = Money(150L),
                axisTopInCents = 0L,
            ),
        ).isNull()
    }
}
