package com.notepay.ui.feature.stats

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Subscription
import com.notepay.ui.feature.addtransaction.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StatsInsightsEngineTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val nowInstant: Instant = Clock.System.now()
    private val zone = TimeZone.currentSystemDefault()

    // ── Forecast Tests ──────────────────────────────────────────────

    @Test
    fun `forecast - linear projection basic`() {
        // 10M spent on day 15 of 30-day month
        val result = StatsInsightsEngine.computeForecast(
            currentMonthExpense = Money(10_000_000_00),
            currentDay = 15,
            daysInMonth = 30,
            limit = null,
            previousMonthDailyAvg = null,
        )
        // Weighted avg: 10M * 2 / 16 = 1.25M
        assertThat(result.dailyAverage.amountInCents).isEqualTo(1_250_000_00)
        // Projected: 1.25M * 30 = 37.5M
        assertThat(result.projectedSpend.amountInCents).isEqualTo(37_500_000_00)
        assertThat(result.isProjectedToExceed).isFalse()
        assertThat(result.trendPercent).isNull()
    }

    @Test
    fun `forecast - exceeds limit`() {
        val result = StatsInsightsEngine.computeForecast(
            currentMonthExpense = Money(8_000_000_00),
            currentDay = 10,
            daysInMonth = 30,
            limit = Money(20_000_000_00),
        )
        // Weighted avg: 8M * 2 / 11 ≈ 14.54M, projected ≈ 436M
        assertThat(result.isProjectedToExceed).isTrue()
        assertThat(result.projectedSpend.amountInCents).isGreaterThan(20_000_000_00)
    }

    @Test
    fun `forecast - trend up when spending more than last month`() {
        val prevAvg = Money(500_000_00) // 500K/day last month
        val result = StatsInsightsEngine.computeForecast(
            currentMonthExpense = Money(7_500_000_00), // 7.5M over 10 days
            currentDay = 10,
            daysInMonth = 30,
            limit = null,
            previousMonthDailyAvg = prevAvg,
        )
        // Weighted avg: 7.5M * 2 / 11 ≈ 1.36M > 500K → trend up
        assertThat(result.trendPercent).isNotNull()
        assertThat(result.trendPercent!!).isGreaterThan(0f)
        assertThat(result.trendMessage).isNotNull()
    }

    @Test
    fun `forecast - trend down when spending less`() {
        val prevAvg = Money(2_000_000_00) // 2M/day last month
        val result = StatsInsightsEngine.computeForecast(
            currentMonthExpense = Money(5_000_000_00), // 5M over 10 days
            currentDay = 10,
            daysInMonth = 30,
            limit = null,
            previousMonthDailyAvg = prevAvg,
        )
        // Weighted avg ≈ 909K < 2M → trend down
        assertThat(result.trendPercent).isNotNull()
        assertThat(result.trendPercent!!).isLessThan(0f)
    }

    @Test
    fun `forecast - no trend when no previous month data`() {
        val result = StatsInsightsEngine.computeForecast(
            currentMonthExpense = Money(5_000_000_00),
            currentDay = 10,
            daysInMonth = 30,
            limit = null,
            previousMonthDailyAvg = null,
        )
        assertThat(result.trendPercent).isNull()
        assertThat(result.trendMessage).isNull()
        assertThat(result.previousMonthDailyAverage).isNull()
    }

    @Test
    fun `forecast - day 1 projection`() {
        val result = StatsInsightsEngine.computeForecast(
            currentMonthExpense = Money(500_000_00),
            currentDay = 1,
            daysInMonth = 30,
            limit = null,
        )
        // Weighted avg: 500K * 2 / 2 = 500K; projected: 500K * 30 = 15M
        assertThat(result.dailyAverage.amountInCents).isEqualTo(500_000_00)
        assertThat(result.projectedSpend.amountInCents).isEqualTo(15_000_000_00)
    }

    // ── Dynamic Daily Budget Tests ──────────────────────────────────

    @Test
    fun `dynamic daily budget - normal case`() {
        val result = StatsInsightsEngine.computeDynamicDailyBudget(
            limit = Money(30_000_000_00),
            spentExceptToday = Money(15_000_000_00),
            spentToday = Money(500_000_00),
            remainingDays = 15,
            daysInMonth = 30,
            currentDay = 16,
            currentMonthExpense = Money(15_500_000_00),
        )
        // remainingBudget = 30M - 15M = 15M; dailyBudget = 15M/15 = 1M
        assertThat(result.dailyBudget.amountInCents).isEqualTo(1_000_000_00)
        assertThat(result.remainingToday.amountInCents).isEqualTo(500_000_00)
        assertThat(result.isExceeded).isFalse()
        assertThat(result.earlyWarning).isNull()
    }

    @Test
    fun `dynamic daily budget - exceeded`() {
        val result = StatsInsightsEngine.computeDynamicDailyBudget(
            limit = Money(30_000_000_00),
            spentExceptToday = Money(25_000_000_00),
            spentToday = Money(2_000_000_00),
            remainingDays = 10,
            daysInMonth = 30,
            currentDay = 21,
            currentMonthExpense = Money(27_000_000_00),
        )
        // remainingBudget = 5M; dailyBudget = 500K
        assertThat(result.dailyBudget.amountInCents).isEqualTo(500_000_00)
        assertThat(result.isExceeded).isTrue()
    }

    @Test
    fun `dynamic daily budget - early warning day 2 overspend`() {
        val result = StatsInsightsEngine.computeDynamicDailyBudget(
            limit = Money(30_000_000_00), // daily = 1M
            spentExceptToday = Money.ZERO,
            spentToday = Money(3_000_000_00), // 3M on day 2 = 3x daily
            remainingDays = 29,
            daysInMonth = 30,
            currentDay = 2,
            currentMonthExpense = Money(3_000_000_00),
        )
        assertThat(result.earlyWarning).isNotNull()
        assertThat(result.earlyWarning).contains("⚠")
    }

    @Test
    fun `dynamic daily budget - early warning week 1 over 150 percent`() {
        val result = StatsInsightsEngine.computeDynamicDailyBudget(
            limit = Money(30_000_000_00), // daily = 1M
            spentExceptToday = Money.ZERO,
            spentToday = Money(2_000_000_00), // 2M on day 5 = 150%+ of daily
            remainingDays = 26,
            daysInMonth = 30,
            currentDay = 5,
            currentMonthExpense = Money(2_000_000_00),
        )
        assertThat(result.earlyWarning).isNotNull()
    }

    @Test
    fun `dynamic daily budget - no early warning normal spending`() {
        val result = StatsInsightsEngine.computeDynamicDailyBudget(
            limit = Money(30_000_000_00),
            spentExceptToday = Money.ZERO,
            spentToday = Money(800_000_00),
            remainingDays = 29,
            daysInMonth = 30,
            currentDay = 1,
            currentMonthExpense = Money(800_000_00),
        )
        assertThat(result.earlyWarning).isNull()
    }

    @Test
    fun `dynamic daily budget - tomorrow budget lower when overspent`() {
        val result = StatsInsightsEngine.computeDynamicDailyBudget(
            limit = Money(30_000_000_00),
            spentExceptToday = Money(20_000_000_00),
            spentToday = Money(5_000_000_00),
            remainingDays = 10,
            daysInMonth = 30,
            currentDay = 21,
            currentMonthExpense = Money(25_000_000_00),
        )
        // remainingForTomorrow = 30M - 25M = 5M; tomorrow = 5M/9 ≈ 555K
        assertThat(result.tomorrowBudget.amountInCents).isLessThan(result.dailyBudget.amountInCents)
    }

    // ── AI Advices Tests ────────────────────────────────────────────

    @Test
    fun `advice - food warning when over 35 percent`() {
        val breakdown = listOf(
            CategoryBreakdownItem(Category.FOOD, Money(4_000_000_00), 0.40f),
            CategoryBreakdownItem(Category.SHOPPING, Money(3_000_000_00), 0.30f),
            CategoryBreakdownItem(Category.TRANSPORT, Money(3_000_000_00), 0.30f),
        )
        val advices = StatsInsightsEngine.computeAdvices(
            breakdown = breakdown,
            expense = Money(10_000_000_00),
            income = Money(15_000_000_00),
            subscriptions = emptyList(),
            currentMonthExpense = Money(10_000_000_00),
            spentToday = Money(1_000_000_00),
            currentDay = 15,
            daysInMonth = 30,
            remainingDays = 16,
            limit = null,
            previousMonthDailyAvg = null,
            feedbacks = emptyMap(),
            nowInstant = nowInstant,
        )
        val foodAdvice = advices.find { it.id == "advice_food" }
        assertThat(foodAdvice).isNotNull()
        assertThat(foodAdvice!!.type).isEqualTo("warning")
    }

    @Test
    fun `advice - no food warning when under 35 percent`() {
        val breakdown = listOf(
            CategoryBreakdownItem(Category.FOOD, Money(2_000_000_00), 0.20f),
            CategoryBreakdownItem(Category.SHOPPING, Money(8_000_000_00), 0.80f),
        )
        val advices = StatsInsightsEngine.computeAdvices(
            breakdown = breakdown,
            expense = Money(10_000_000_00),
            income = Money(15_000_000_00),
            subscriptions = emptyList(),
            currentMonthExpense = Money(10_000_000_00),
            spentToday = Money(500_000_00),
            currentDay = 10,
            daysInMonth = 30,
            remainingDays = 21,
            limit = null,
            previousMonthDailyAvg = null,
            feedbacks = emptyMap(),
            nowInstant = nowInstant,
        )
        assertThat(advices.none { it.id == "advice_food" }).isTrue()
    }

    @Test
    fun `advice - spike detection when today much higher than avg`() {
        val advices = StatsInsightsEngine.computeAdvices(
            breakdown = emptyList(),
            expense = Money(5_000_000_00),
            income = Money(15_000_000_00),
            subscriptions = emptyList(),
            currentMonthExpense = Money(5_000_000_00),
            spentToday = Money(3_000_000_00), // 3M today vs avg 333K
            currentDay = 15,
            daysInMonth = 30,
            remainingDays = 16,
            limit = null,
            previousMonthDailyAvg = null,
            feedbacks = emptyMap(),
            nowInstant = nowInstant,
        )
        val spikeAdvice = advices.find { it.id == "advice_spike" }
        assertThat(spikeAdvice).isNotNull()
        assertThat(spikeAdvice!!.type).isEqualTo("warning")
    }

    @Test
    fun `advice - no spike when spending normal`() {
        val advices = StatsInsightsEngine.computeAdvices(
            breakdown = emptyList(),
            expense = Money(5_000_000_00),
            income = Money(15_000_000_00),
            subscriptions = emptyList(),
            currentMonthExpense = Money(5_000_000_00),
            spentToday = Money(400_000_00),
            currentDay = 15,
            daysInMonth = 30,
            remainingDays = 16,
            limit = null,
            previousMonthDailyAvg = null,
            feedbacks = emptyMap(),
            nowInstant = nowInstant,
        )
        assertThat(advices.none { it.id == "advice_spike" }).isTrue()
    }

    @Test
    fun `advice - trend warning when spending more than last month`() {
        val advices = StatsInsightsEngine.computeAdvices(
            breakdown = emptyList(),
            expense = Money(10_000_000_00),
            income = Money(15_000_000_00),
            subscriptions = emptyList(),
            currentMonthExpense = Money(10_000_000_00),
            spentToday = Money(500_000_00),
            currentDay = 10,
            daysInMonth = 30,
            remainingDays = 21,
            limit = Money(30_000_000_00),
            previousMonthDailyAvg = Money(700_000_00),
            feedbacks = emptyMap(),
            nowInstant = nowInstant,
        )
        // current avg = 10M/10 = 1M vs 700K = +42%
        val trendAdvice = advices.find { it.id == "advice_trend" }
        assertThat(trendAdvice).isNotNull()
        assertThat(trendAdvice!!.type).isEqualTo("warning")
    }

    @Test
    fun `advice - trend success when spending less`() {
        val advices = StatsInsightsEngine.computeAdvices(
            breakdown = emptyList(),
            expense = Money(5_000_000_00),
            income = Money(15_000_000_00),
            subscriptions = emptyList(),
            currentMonthExpense = Money(5_000_000_00),
            spentToday = Money(300_000_00),
            currentDay = 10,
            daysInMonth = 30,
            remainingDays = 21,
            limit = Money(30_000_000_00),
            previousMonthDailyAvg = Money(1_000_000_00),
            feedbacks = emptyMap(),
            nowInstant = nowInstant,
        )
        // current avg = 5M/10 = 500K vs 1M = -50%
        val trendAdvice = advices.find { it.id == "advice_trend" }
        assertThat(trendAdvice).isNotNull()
        assertThat(trendAdvice!!.type).isEqualTo("success")
    }

    @Test
    fun `advice - no trend when early in month`() {
        val advices = StatsInsightsEngine.computeAdvices(
            breakdown = emptyList(),
            expense = Money(5_000_000_00),
            income = Money(15_000_000_00),
            subscriptions = emptyList(),
            currentMonthExpense = Money(5_000_000_00),
            spentToday = Money(300_000_00),
            currentDay = 3, // too early for trend
            daysInMonth = 30,
            remainingDays = 28,
            limit = Money(30_000_000_00),
            previousMonthDailyAvg = Money(1_000_000_00),
            feedbacks = emptyMap(),
            nowInstant = nowInstant,
        )
        assertThat(advices.none { it.id == "advice_trend" }).isTrue()
    }

    @Test
    fun `advice - feedback dismissed not shown`() {
        val breakdown = listOf(
            CategoryBreakdownItem(Category.FOOD, Money(4_000_000_00), 0.40f),
            CategoryBreakdownItem(Category.SHOPPING, Money(6_000_000_00), 0.60f),
        )
        val advices = StatsInsightsEngine.computeAdvices(
            breakdown = breakdown,
            expense = Money(10_000_000_00),
            income = Money(15_000_000_00),
            subscriptions = emptyList(),
            currentMonthExpense = Money(10_000_000_00),
            spentToday = Money(500_000_00),
            currentDay = 15,
            daysInMonth = 30,
            remainingDays = 16,
            limit = null,
            previousMonthDailyAvg = null,
            feedbacks = mapOf("advice_food" to 1),
            nowInstant = nowInstant,
        )
        assertThat(advices.none { it.id == "advice_food" }).isTrue()
    }

    @Test
    fun `advice - saving when spending well under budget`() {
        // initial daily = 30M/30 = 1M; need avg < 80% = 800K
        // (10M - 200K) / 14 ≈ 685K < 800K ✓
        val advices = StatsInsightsEngine.computeAdvices(
            breakdown = emptyList(),
            expense = Money(10_000_000_00),
            income = Money(15_000_000_00),
            subscriptions = emptyList(),
            currentMonthExpense = Money(10_000_000_00),
            spentToday = Money(200_000_00),
            currentDay = 15,
            daysInMonth = 30,
            remainingDays = 16,
            limit = Money(30_000_000_00),
            previousMonthDailyAvg = null,
            feedbacks = emptyMap(),
            nowInstant = nowInstant,
        )
        val savingAdvice = advices.find { it.id == "advice_saving" }
        assertThat(savingAdvice).isNotNull()
        assertThat(savingAdvice!!.type).isEqualTo("success")
    }

    @Test
    fun `advice - no saving when spending close to budget`() {
        // initial daily = 30M/30 = 1M; avg = (10M-500K)/14 ≈ 678K < 800K → still saves
        // Need avg >= 800K: (15M - 500K) / 14 ≈ 1.036M >= 800K
        val advices = StatsInsightsEngine.computeAdvices(
            breakdown = emptyList(),
            expense = Money(15_000_000_00),
            income = Money(20_000_000_00),
            subscriptions = emptyList(),
            currentMonthExpense = Money(15_000_000_00),
            spentToday = Money(500_000_00),
            currentDay = 15,
            daysInMonth = 30,
            remainingDays = 16,
            limit = Money(30_000_000_00),
            previousMonthDailyAvg = null,
            feedbacks = emptyMap(),
            nowInstant = nowInstant,
        )
        assertThat(advices.none { it.id == "advice_saving" }).isTrue()
    }

    @Test
    fun `advice - subscription warning when balance insufficient`() {
        val dueSub = Subscription(
            id = 1L,
            name = "Netflix",
            amount = Money(200_000_00),
            nextDueDate = nowInstant.plus(DatePeriod(days = 2), zone),
            repeatMonths = 1,
            remindDaysBefore = 3,
            isActive = true,
        )
        val advices = StatsInsightsEngine.computeAdvices(
            breakdown = emptyList(),
            expense = Money(10_000_000_00),
            income = Money(10_000_000_00), // balance = 0
            subscriptions = listOf(dueSub),
            currentMonthExpense = Money(10_000_000_00),
            spentToday = Money(500_000_00),
            currentDay = 15,
            daysInMonth = 30,
            remainingDays = 16,
            limit = null,
            previousMonthDailyAvg = null,
            feedbacks = emptyMap(),
            nowInstant = nowInstant,
        )
        val subAdvice = advices.find { it.id == "advice_bill_balance_1" }
        assertThat(subAdvice).isNotNull()
        assertThat(subAdvice!!.type).isEqualTo("warning")
    }

    // ── Previous Month Daily Avg Tests ──────────────────────────────

    @Test
    fun `previous month avg - empty transactions`() {
        val result = StatsInsightsEngine.computePreviousMonthDailyAvg(
            allTransactions = emptyList(),
            previousMonthStartMillis = 0L,
            previousMonthEndMillis = 9999999999999L,
            previousDaysInMonth = 30,
        )
        assertThat(result).isEqualTo(Money.ZERO)
    }
}
