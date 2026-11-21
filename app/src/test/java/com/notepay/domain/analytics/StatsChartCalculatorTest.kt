package com.notepay.domain.analytics

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.TestData
import com.notepay.domain.model.Category
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.time.Instant

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

    @Test
    fun `buildPeriodBars returns 7 zero-filled weekday bars and marks today`() {
        val today = LocalDate(2026, 11, 18) // Wednesday
        val period = periodFor(today, StatsRange.WEEK)
        val transactions = listOf(
            TestData.transaction(
                amount = Money(90_000_00L),
                type = TransactionType.EXPENSE,
                category = Category.FOOD,
                occurredAt = Instant.parse("2026-11-16T08:00:00Z"), // Mon
            ),
            TestData.transaction(
                amount = Money(30_000_00L),
                type = TransactionType.EXPENSE,
                category = Category.TRANSPORT,
                occurredAt = Instant.parse("2026-11-18T12:00:00Z"), // Wed
            ),
            TestData.transaction(
                amount = Money(500_000_00L),
                type = TransactionType.INCOME,
                category = Category.SALARY,
                occurredAt = Instant.parse("2026-11-18T12:00:00Z"),
            ),
        )

        val bars = StatsChartCalculator.buildPeriodBars(
            transactions = transactions,
            type = TransactionType.EXPENSE,
            period = period,
            selectedCategory = null,
            today = today,
            zone = TimeZone.UTC,
        )

        assertThat(bars).hasSize(7)
        assertThat(bars.map { it.bucketKind }.distinct()).containsExactly(PeriodBarBucketKind.WEEKDAY)
        assertThat(bars.map { it.indexValue }).containsExactly(1, 2, 3, 4, 5, 6, 7).inOrder()
        assertThat(bars.map { it.amountInCents }).containsExactly(
            90_000_00L, 0L, 30_000_00L, 0L, 0L, 0L, 0L,
        ).inOrder()
        assertThat(bars.map { it.isCurrentBucket }).containsExactly(
            false, false, true, false, false, false, false,
        ).inOrder()
    }

    @Test
    fun `buildPeriodBars handles month lengths including leap feb and isolates category and type`() {
        val leapFeb = periodFor(LocalDate(2024, 2, 1), StatsRange.MONTH)
        val regularFeb = periodFor(LocalDate(2025, 2, 1), StatsRange.MONTH)

        val txLeap = listOf(
            TestData.transaction(
                amount = Money(120_000_00L),
                type = TransactionType.EXPENSE,
                category = Category.FOOD,
                occurredAt = Instant.parse("2024-02-29T10:00:00Z"),
            ),
            TestData.transaction(
                amount = Money(80_000_00L),
                type = TransactionType.EXPENSE,
                category = Category.COFFEE,
                occurredAt = Instant.parse("2024-02-29T11:00:00Z"),
            ),
            TestData.transaction(
                amount = Money(1_000_000_00L),
                type = TransactionType.INCOME,
                category = Category.FOOD,
                occurredAt = Instant.parse("2024-02-15T10:00:00Z"),
            ),
        )

        val allExpenseBars = StatsChartCalculator.buildPeriodBars(
            transactions = txLeap,
            type = TransactionType.EXPENSE,
            period = leapFeb,
            selectedCategory = null,
            today = LocalDate(2024, 2, 29),
            zone = TimeZone.UTC,
        )
        assertThat(allExpenseBars).hasSize(29)
        assertThat(allExpenseBars[28].amountInCents).isEqualTo(200_000_00L)
        assertThat(allExpenseBars[28].isCurrentBucket).isTrue()

        val foodOnlyBars = StatsChartCalculator.buildPeriodBars(
            transactions = txLeap,
            type = TransactionType.EXPENSE,
            period = leapFeb,
            selectedCategory = Category.FOOD,
            today = LocalDate(2024, 2, 29),
            zone = TimeZone.UTC,
        )
        assertThat(foodOnlyBars[28].amountInCents).isEqualTo(120_000_00L)
        assertThat(foodOnlyBars[14].amountInCents).isEqualTo(0L)

        val regFebBars = StatsChartCalculator.buildPeriodBars(
            transactions = emptyList(),
            type = TransactionType.EXPENSE,
            period = regularFeb,
            selectedCategory = null,
            today = LocalDate(2025, 2, 10),
            zone = TimeZone.UTC,
        )
        assertThat(regFebBars).hasSize(28)
    }

    @Test
    fun `buildPeriodBars handles year custom and all-time spans`() {
        val yearPeriod = periodFor(LocalDate(2026, 1, 1), StatsRange.YEAR)
        val yearTx = listOf(
            TestData.transaction(
                amount = Money(100_000_00L),
                type = TransactionType.EXPENSE,
                occurredAt = Instant.parse("2026-01-05T10:00:00Z"),
            ),
            TestData.transaction(
                amount = Money(250_000_00L),
                type = TransactionType.EXPENSE,
                occurredAt = Instant.parse("2026-11-18T10:00:00Z"),
            ),
        )
        val yearBars = StatsChartCalculator.buildPeriodBars(
            transactions = yearTx,
            type = TransactionType.EXPENSE,
            period = yearPeriod,
            selectedCategory = null,
            today = LocalDate(2026, 11, 18),
            zone = TimeZone.UTC,
        )
        assertThat(yearBars).hasSize(12)
        assertThat(yearBars[0].amountInCents).isEqualTo(100_000_00L)
        assertThat(yearBars[10].amountInCents).isEqualTo(250_000_00L)
        assertThat(yearBars[10].isCurrentBucket).isTrue()

        val shortCustom = customPeriod(LocalDate(2026, 11, 1), LocalDate(2026, 11, 5))
        val customDaily = StatsChartCalculator.buildPeriodBars(
            transactions = listOf(
                TestData.transaction(
                    amount = Money(40_000_00L),
                    type = TransactionType.EXPENSE,
                    occurredAt = Instant.parse("2026-11-03T10:00:00Z"),
                ),
            ),
            type = TransactionType.EXPENSE,
            period = shortCustom,
            selectedCategory = null,
            today = LocalDate(2026, 11, 5),
            zone = TimeZone.UTC,
        )
        assertThat(customDaily).hasSize(5)
        assertThat(customDaily.map { it.bucketKind }.distinct()).containsExactly(PeriodBarBucketKind.DATE_SHORT)
        assertThat(customDaily[2].amountInCents).isEqualTo(40_000_00L)

        val multiYearTx = listOf(
            TestData.transaction(
                amount = Money(100_000_00L),
                type = TransactionType.EXPENSE,
                occurredAt = Instant.parse("2024-05-01T10:00:00Z"),
            ),
            TestData.transaction(
                amount = Money(200_000_00L),
                type = TransactionType.EXPENSE,
                occurredAt = Instant.parse("2026-11-01T10:00:00Z"),
            ),
        )
        val allYearly = StatsChartCalculator.buildPeriodBars(
            transactions = multiYearTx,
            type = TransactionType.EXPENSE,
            period = periodFor(LocalDate(2026, 11, 18), StatsRange.ALL),
            selectedCategory = null,
            today = LocalDate(2026, 11, 18),
            zone = TimeZone.UTC,
        )
        assertThat(allYearly.map { it.bucketKind }.distinct()).containsExactly(PeriodBarBucketKind.YEAR)
        assertThat(allYearly.map { it.indexValue }).containsExactly(2024, 2025, 2026).inOrder()
        assertThat(allYearly.map { it.amountInCents }).containsExactly(100_000_00L, 0L, 200_000_00L).inOrder()
    }
}
