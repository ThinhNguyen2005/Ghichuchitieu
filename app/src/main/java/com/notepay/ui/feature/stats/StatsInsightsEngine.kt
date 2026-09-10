package com.notepay.ui.feature.stats

import com.notepay.R
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Subscription
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.ui.util.MoneyFormatter
import kotlin.math.max
import kotlin.time.Instant

/**
 * Pure calculation engine cho Stats insights. Không giữ Context; text trả về dưới dạng
 * resource descriptor để composition layer resolve theo locale.
 */
object StatsInsightsEngine {

    // ── Forecast ──────────────────────────────────────────────────────

    fun computeForecast(
        currentMonthExpense: Money,
        currentDay: Int,
        daysInMonth: Int,
        limit: Money?,
        previousMonthDailyAvg: Money? = null,
    ): BudgetForecast {
        // Weighted moving average: ngày gần nhất nặng hơn (linear weight)
        val weightSum = currentDay * (currentDay + 1L) / 2.0
        // avg = sum(day_i * spend_i) / weightSum
        // With uniform spend assumption: avg ≈ currentExpense / currentDay * (1 + currentDay) / (2 * currentDay)
        // Simplified: linear-weighted avg ≈ currentExpense * 2 / (currentDay + 1)
        val dailyAverageCents = if (currentDay > 0) {
            (currentMonthExpense.amountInCents.toDouble() * 2) / (currentDay + 1)
        } else 0.0
        val projectedSpendCents = dailyAverageCents * daysInMonth

        val dailyAvgStr = MoneyFormatter.format(Money(dailyAverageCents.toLong()))
        val projectedSpendStr = MoneyFormatter.format(Money(projectedSpendCents.toLong()))

        val baseMessage = StatsUiText.Resource(
            R.string.stats_insight_forecast_format,
            listOf(dailyAvgStr, projectedSpendStr),
        )

        val isProjectedToExceed = limit != null && projectedSpendCents > limit.amountInCents
        val exceedMsg: StatsUiText? = if (isProjectedToExceed) {
            StatsUiText.Resource(
                R.string.stats_insight_forecast_exceeded,
                listOf(MoneyFormatter.format(limit!!)),
            )
        } else null

        // Trend so với tháng trước
        val trendPercent: Float? = if (previousMonthDailyAvg != null && previousMonthDailyAvg.amountInCents > 0) {
            val prev = previousMonthDailyAvg.amountInCents.toDouble()
            ((dailyAverageCents - prev) / prev * 100).toFloat()
        } else null

        val trendMessage: StatsUiText? = when {
            trendPercent == null -> null
            trendPercent > 5f -> StatsUiText.Resource(
                R.string.stats_insight_trend_up,
                listOf("%.0f".format(trendPercent)),
            )
            trendPercent < -5f -> StatsUiText.Resource(
                R.string.stats_insight_trend_down,
                listOf("%.0f".format(-trendPercent)),
            )
            else -> StatsUiText.Resource(R.string.stats_insight_trend_stable)
        }

        val prevAvgStr = if (previousMonthDailyAvg != null && previousMonthDailyAvg.amountInCents > 0) {
            MoneyFormatter.format(previousMonthDailyAvg)
        } else null

        val fullMessage = StatsUiText.Composite(
            buildList {
                add(baseMessage)
                if (exceedMsg != null) add(exceedMsg)
                if (prevAvgStr != null) {
                    add(StatsUiText.Resource(R.string.stats_insight_forecast_previous, listOf(prevAvgStr)))
                }
            },
        )

        return BudgetForecast(
            dailyAverage = Money(dailyAverageCents.toLong()),
            projectedSpend = Money(projectedSpendCents.toLong()),
            forecastMessage = fullMessage,
            isProjectedToExceed = isProjectedToExceed,
            previousMonthDailyAverage = previousMonthDailyAvg,
            trendPercent = trendPercent,
            trendMessage = trendMessage,
        )
    }

    /**
     * Tính daily average của tháng trước từ danh sách transactions.
     */
    fun computePreviousMonthDailyAvg(
        allTransactions: List<Transaction>,
        previousMonthStartMillis: Long,
        previousMonthEndMillis: Long,
        previousDaysInMonth: Int,
    ): Money {
        val prevExpenses = allTransactions.filter {
            it.type == TransactionType.EXPENSE &&
                it.occurredAt.toEpochMilliseconds() in previousMonthStartMillis..previousMonthEndMillis
        }
        val totalPrevExpense = prevExpenses.fold(Money.ZERO) { acc, t -> acc + t.amount }
        return if (previousDaysInMonth > 0) {
            Money(totalPrevExpense.amountInCents / previousDaysInMonth)
        } else Money.ZERO
    }

    // ── Dynamic Daily Budget ──────────────────────────────────────────

    fun computeDynamicDailyBudget(
        limit: Money,
        spentExceptToday: Money,
        spentToday: Money,
        remainingDays: Int,
        daysInMonth: Int,
        currentDay: Int,
        currentMonthExpense: Money,
    ): DynamicDailyBudgetData {
        val remainingBudget = max(0L, limit.amountInCents - spentExceptToday.amountInCents)
        val dailyBudgetVal = if (remainingDays > 0) remainingBudget / remainingDays else 0L
        val remainingToday = max(0L, dailyBudgetVal - spentToday.amountInCents)

        val tomorrowBudget = if (remainingDays > 1) {
            val remainingForTomorrow = max(0L, limit.amountInCents - currentMonthExpense.amountInCents)
            remainingForTomorrow / (remainingDays - 1)
        } else {
            0L
        }

        val initialDailyBudget = limit.amountInCents / daysInMonth
        val spentTodayLong = spentToday.amountInCents

        // Early warning: chi nhanh đầu tháng
        val earlyWarning: StatsUiText? = when {
            currentDay <= 3 && spentTodayLong > initialDailyBudget * 2L -> {
                StatsUiText.Resource(
                    R.string.stats_insight_early_warning_first_days,
                    listOf(MoneyFormatter.format(spentToday)),
                )
            }
            currentDay <= 7 && spentTodayLong > initialDailyBudget * 15L / 10 -> {
                val ratio = spentTodayLong * 100 / initialDailyBudget
                StatsUiText.Resource(
                    R.string.stats_insight_early_warning_first_week,
                    listOf(ratio, MoneyFormatter.format(Money(initialDailyBudget * 8 / 10))),
                )
            }
            else -> null
        }

        return DynamicDailyBudgetData(
            dailyBudget = Money(dailyBudgetVal),
            spentToday = spentToday,
            remainingToday = Money(remainingToday),
            tomorrowBudget = Money(tomorrowBudget),
            isExceeded = spentTodayLong > dailyBudgetVal,
            earlyWarning = earlyWarning,
        )
    }

    // ── AI Advices ────────────────────────────────────────────────────

    fun computeAdvices(
        breakdown: List<CategoryBreakdownItem>,
        expense: Money,
        income: Money,
        subscriptions: List<Subscription>,
        currentMonthExpense: Money,
        spentToday: Money,
        currentDay: Int,
        daysInMonth: Int,
        remainingDays: Int,
        limit: Money?,
        previousMonthDailyAvg: Money?,
        feedbacks: Map<String, Int>,
        nowInstant: Instant,
    ): List<AiAdviceItem> {
        val advices = mutableListOf<AiAdviceItem>()

        // Rule A: FOOD > 35% (giữ nguyên — hợp lý)
        val foodBreakdown = breakdown.find { it.category == Category.FOOD }
        if (foodBreakdown != null && foodBreakdown.percentage > 0.35f && (feedbacks["advice_food"] ?: 0) == 0) {
            val percentStr = "%.1f%%".format(foodBreakdown.percentage * 100)
            advices.add(
                AiAdviceItem(
                    id = "advice_food",
                    type = "warning",
                    title = StatsUiText.Resource(R.string.stats_engine_advice_food_title),
                    content = StatsUiText.Resource(R.string.stats_engine_advice_food_content, listOf(percentStr)),
                    categoryId = Category.FOOD.id,
                    feedback = feedbacks["advice_food"] ?: 0,
                )
            )
        }

        // Rule B: Phí sắp đến hạn & Số dư không đủ (giữ nguyên)
        val upcomingSubs = subscriptions.filter { sub ->
            sub.isActive && (sub.nextDueDate - nowInstant).inWholeDays in 0..3
        }
        if (upcomingSubs.isNotEmpty()) {
            val balanceVal = income.amountInCents - expense.amountInCents
            for (sub in upcomingSubs) {
                val feedbackKey = "advice_bill_balance_${sub.id}"
                if (balanceVal < sub.amount.amountInCents && (feedbacks[feedbackKey] ?: 0) == 0) {
                    advices.add(
                        AiAdviceItem(
                            id = feedbackKey,
                            type = "warning",
                            title = StatsUiText.Resource(R.string.stats_engine_advice_bill_title),
                            content = StatsUiText.Resource(
                                R.string.stats_engine_advice_bill_content,
                                listOf(sub.name, MoneyFormatter.format(sub.amount)),
                            ),
                            feedback = feedbacks[feedbackKey] ?: 0,
                        )
                    )
                }
            }
        }

        // Rule C: Chi tiêu hôm nay spike > 2x daily avg
        if (remainingDays >= 3 && currentDay > 1 && (feedbacks["advice_spike"] ?: 0) == 0) {
            val dailyAvg = currentMonthExpense.amountInCents / currentDay
            if (dailyAvg > 0 && spentToday.amountInCents > dailyAvg * 2) {
                val spikeRatio = spentToday.amountInCents * 100 / dailyAvg
                advices.add(
                    AiAdviceItem(
                        id = "advice_spike",
                        type = "warning",
                        title = StatsUiText.Resource(R.string.stats_engine_advice_spike_title),
                        content = StatsUiText.Resource(
                            R.string.stats_engine_advice_spike_content,
                            listOf(spikeRatio, MoneyFormatter.format(spentToday), MoneyFormatter.format(Money(dailyAvg))),
                        ),
                        feedback = feedbacks["advice_spike"] ?: 0,
                    )
                )
            }
        }

        // Rule D: Trend so tháng trước
        if (previousMonthDailyAvg != null && previousMonthDailyAvg.amountInCents > 0 && currentDay > 3
            && (feedbacks["advice_trend"] ?: 0) == 0 && limit != null && limit.amountInCents > 0
        ) {
            val currentDailyAvg = currentMonthExpense.amountInCents / currentDay
            val prevAvg = previousMonthDailyAvg.amountInCents
            val changePercent = ((currentDailyAvg - prevAvg).toFloat() / prevAvg * 100)

            if (changePercent > 20f) {
                advices.add(
                    AiAdviceItem(
                        id = "advice_trend",
                        type = "warning",
                        title = StatsUiText.Resource(R.string.stats_engine_advice_trend_up_title),
                        content = StatsUiText.Resource(
                            R.string.stats_engine_advice_trend_up_content,
                            listOf("%.0f".format(changePercent)),
                        ),
                        feedback = feedbacks["advice_trend"] ?: 0,
                    )
                )
            } else if (changePercent < -20f) {
                advices.add(
                    AiAdviceItem(
                        id = "advice_trend",
                        type = "success",
                        title = StatsUiText.Resource(R.string.stats_engine_advice_trend_down_title),
                        content = StatsUiText.Resource(
                            R.string.stats_engine_advice_trend_down_content,
                            listOf("%.0f".format(-changePercent)),
                        ),
                        feedback = feedbacks["advice_trend"] ?: 0,
                    )
                )
            }
        }

        // Rule E: Đang tiết kiệm — nếu daily avg < 80% hạn mức an toàn
        if (limit != null && limit.amountInCents > 0 && remainingDays >= 5 && currentDay > 1
            && (feedbacks["advice_saving"] ?: 0) == 0
        ) {
            val initialDailyBudget = limit.amountInCents / daysInMonth
            val currentDailyAverage = (currentMonthExpense.amountInCents - spentToday.amountInCents) / (currentDay - 1)
            if (currentDailyAverage in 1 until (initialDailyBudget * 8 / 10)) {
                advices.add(
                    AiAdviceItem(
                        id = "advice_saving",
                        type = "success",
                        title = StatsUiText.Resource(R.string.stats_engine_advice_saving_title),
                        content = StatsUiText.Resource(
                            R.string.stats_engine_advice_saving_content,
                            listOf(currentDailyAverage * 100 / initialDailyBudget),
                        ),
                        feedback = feedbacks["advice_saving"] ?: 0,
                    )
                )
            }
        }

        return advices
    }
}
