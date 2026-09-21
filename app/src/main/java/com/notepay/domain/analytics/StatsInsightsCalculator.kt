package com.notepay.domain.analytics

import com.notepay.domain.model.Category
import com.notepay.domain.model.Subscription
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import kotlin.math.max
import kotlin.time.Instant

data class StatsForecastResult(
    val dailyAverageInCents: Long,
    val projectedSpendInCents: Long,
    val isProjectedToExceed: Boolean,
    val previousMonthDailyAverageInCents: Long?,
    val trendPercent: Float?,
)

enum class StatsEarlyWarning {
    FIRST_DAYS,
    FIRST_WEEK,
}

data class DynamicDailyBudgetResult(
    val dailyBudgetInCents: Long,
    val spentTodayInCents: Long,
    val remainingTodayInCents: Long,
    val tomorrowBudgetInCents: Long,
    val isExceeded: Boolean,
    val earlyWarning: StatsEarlyWarning?,
    val initialDailyBudgetInCents: Long = 0L,
)

data class CategoryExpenseShare(
    val categoryId: String,
    val amountInCents: Long,
    val share: Float,
)

enum class StatsAdviceType {
    WARNING,
    SUCCESS,
}

sealed interface StatsAdviceSignal {
    val id: String
    val type: StatsAdviceType
    val feedback: Int

    data class FoodShare(
        override val feedback: Int,
        val percentage: Float,
    ) : StatsAdviceSignal {
        override val id: String = "advice_food"
        override val type: StatsAdviceType = StatsAdviceType.WARNING
    }

    data class UpcomingSubscription(
        override val id: String,
        override val feedback: Int,
        val name: String,
        val amountInCents: Long,
    ) : StatsAdviceSignal {
        override val type: StatsAdviceType = StatsAdviceType.WARNING
    }

    data class SpendingSpike(
        override val feedback: Int,
        val ratioPercent: Long,
        val spentTodayInCents: Long,
        val dailyAverageInCents: Long,
    ) : StatsAdviceSignal {
        override val id: String = "advice_spike"
        override val type: StatsAdviceType = StatsAdviceType.WARNING
    }

    data class TrendChange(
        override val feedback: Int,
        val changePercent: Float,
        val isIncrease: Boolean,
    ) : StatsAdviceSignal {
        override val id: String = "advice_trend"
        override val type: StatsAdviceType = if (isIncrease) {
            StatsAdviceType.WARNING
        } else {
            StatsAdviceType.SUCCESS
        }
    }

    data class Saving(
        override val feedback: Int,
        val currentDailyAverageInCents: Long,
        val percentageOfInitialBudget: Long,
    ) : StatsAdviceSignal {
        override val id: String = "advice_saving"
        override val type: StatsAdviceType = StatsAdviceType.SUCCESS
    }
}

data class StatsAdviceInput(
    val categoryShares: List<CategoryExpenseShare>,
    val expenseInCents: Long,
    val incomeInCents: Long,
    val subscriptions: List<Subscription>,
    val currentMonthExpenseInCents: Long,
    val spentTodayInCents: Long,
    val currentDay: Int,
    val daysInMonth: Int,
    val remainingDays: Int,
    val limitInCents: Long?,
    val previousMonthDailyAverageInCents: Long?,
    val feedbacks: Map<String, Int>,
    val now: Instant,
)

object StatsInsightsCalculator {
    fun computeForecast(
        currentMonthExpenseInCents: Long,
        currentDay: Int,
        daysInMonth: Int,
        limitInCents: Long?,
        previousMonthDailyAverageInCents: Long? = null,
    ): StatsForecastResult {
        val dailyAverage = if (currentDay > 0) {
            (currentMonthExpenseInCents.toDouble() * 2 / (currentDay + 1)).toLong()
        } else {
            0L
        }
        val projectedSpend = (dailyAverage.toDouble() * daysInMonth.coerceAtLeast(0)).toLong()
        val trendPercent = previousMonthDailyAverageInCents
            ?.takeIf { it > 0L }
            ?.let { previous ->
                ((dailyAverage - previous).toDouble() / previous * 100).toFloat()
            }

        return StatsForecastResult(
            dailyAverageInCents = dailyAverage.coerceAtLeast(0L),
            projectedSpendInCents = projectedSpend.coerceAtLeast(0L),
            isProjectedToExceed = limitInCents != null && projectedSpend > limitInCents,
            previousMonthDailyAverageInCents = previousMonthDailyAverageInCents,
            trendPercent = trendPercent,
        )
    }

    fun computePreviousMonthDailyAverage(
        allTransactions: List<Transaction>,
        previousMonthStartMillis: Long,
        previousMonthEndMillis: Long,
        previousDaysInMonth: Int,
    ): Long {
        if (previousDaysInMonth <= 0) return 0L
        val total = allTransactions
            .asSequence()
            .filter { transaction ->
                transaction.type == TransactionType.EXPENSE &&
                    transaction.occurredAt.toEpochMilliseconds() in previousMonthStartMillis..previousMonthEndMillis
            }
            .sumOf { it.amount.amountInCents }
        return total / previousDaysInMonth
    }

    fun computeDynamicDailyBudget(
        limitInCents: Long,
        spentExceptTodayInCents: Long,
        spentTodayInCents: Long,
        remainingDays: Int,
        daysInMonth: Int,
        currentDay: Int,
        currentMonthExpenseInCents: Long,
    ): DynamicDailyBudgetResult {
        val remainingBudget = max(0L, limitInCents - spentExceptTodayInCents)
        val safeRemainingDays = remainingDays.coerceAtLeast(0)
        val dailyBudget = if (safeRemainingDays > 0) remainingBudget / safeRemainingDays else 0L
        val remainingToday = max(0L, dailyBudget - spentTodayInCents)
        val tomorrowBudget = if (safeRemainingDays > 1) {
            max(0L, limitInCents - currentMonthExpenseInCents) / (safeRemainingDays - 1)
        } else {
            0L
        }

        val initialDailyBudget = limitInCents / daysInMonth.coerceAtLeast(1)
        val earlyWarning = when {
            initialDailyBudget > 0L && currentDay <= 3 && spentTodayInCents > initialDailyBudget * 2L ->
                StatsEarlyWarning.FIRST_DAYS
            initialDailyBudget > 0L && currentDay <= 7 && spentTodayInCents > initialDailyBudget * 15L / 10L ->
                StatsEarlyWarning.FIRST_WEEK
            else -> null
        }

        return DynamicDailyBudgetResult(
            dailyBudgetInCents = dailyBudget,
            spentTodayInCents = spentTodayInCents,
            remainingTodayInCents = remainingToday,
            tomorrowBudgetInCents = tomorrowBudget,
            isExceeded = spentTodayInCents > dailyBudget,
            earlyWarning = earlyWarning,
            initialDailyBudgetInCents = initialDailyBudget,
        )
    }

    fun computeAdvices(input: StatsAdviceInput): List<StatsAdviceSignal> {
        val signals = mutableListOf<StatsAdviceSignal>()
        val feedbacks = input.feedbacks
        val foodShare = input.categoryShares.firstOrNull { it.categoryId == Category.FOOD.id }
        if (foodShare != null && foodShare.share > 0.35f && (feedbacks["advice_food"] ?: 0) == 0) {
            signals += StatsAdviceSignal.FoodShare(
                feedback = feedbacks["advice_food"] ?: 0,
                percentage = foodShare.share,
            )
        }

        val upcomingSubscriptions = input.subscriptions.filter { subscription ->
            subscription.isActive && (subscription.nextDueDate - input.now).inWholeDays in 0..3
        }
        val balanceInCents = input.incomeInCents - input.expenseInCents
        upcomingSubscriptions.forEach { subscription ->
            val id = "advice_bill_balance_${subscription.id}"
            if (balanceInCents < subscription.amount.amountInCents && (feedbacks[id] ?: 0) == 0) {
                signals += StatsAdviceSignal.UpcomingSubscription(
                    id = id,
                    feedback = feedbacks[id] ?: 0,
                    name = subscription.name,
                    amountInCents = subscription.amount.amountInCents,
                )
            }
        }

        if (input.remainingDays >= 3 && input.currentDay > 1 && (feedbacks["advice_spike"] ?: 0) == 0) {
            val dailyAverage = input.currentMonthExpenseInCents / input.currentDay
            if (dailyAverage > 0L && input.spentTodayInCents > dailyAverage * 2L) {
                signals += StatsAdviceSignal.SpendingSpike(
                    feedback = feedbacks["advice_spike"] ?: 0,
                    ratioPercent = input.spentTodayInCents * 100 / dailyAverage,
                    spentTodayInCents = input.spentTodayInCents,
                    dailyAverageInCents = dailyAverage,
                )
            }
        }

        val previousAverage = input.previousMonthDailyAverageInCents
        if (previousAverage != null && previousAverage > 0L && input.currentDay > 3
            && (feedbacks["advice_trend"] ?: 0) == 0 && input.limitInCents != null && input.limitInCents > 0L
        ) {
            val currentAverage = input.currentMonthExpenseInCents / input.currentDay
            val changePercent = ((currentAverage - previousAverage).toFloat() / previousAverage * 100)
            if (changePercent > 20f || changePercent < -20f) {
                signals += StatsAdviceSignal.TrendChange(
                    feedback = feedbacks["advice_trend"] ?: 0,
                    changePercent = changePercent,
                    isIncrease = changePercent > 20f,
                )
            }
        }

        val limit = input.limitInCents
        if (limit != null && limit > 0L && input.remainingDays >= 5 && input.currentDay > 1
            && (feedbacks["advice_saving"] ?: 0) == 0
        ) {
            val initialDailyBudget = limit / input.daysInMonth.coerceAtLeast(1)
            val currentDailyAverage =
                (input.currentMonthExpenseInCents - input.spentTodayInCents) / (input.currentDay - 1)
            if (currentDailyAverage in 1 until (initialDailyBudget * 85 / 100)) {
                signals += StatsAdviceSignal.Saving(
                    feedback = feedbacks["advice_saving"] ?: 0,
                    currentDailyAverageInCents = currentDailyAverage,
                    percentageOfInitialBudget = currentDailyAverage * 100 / initialDailyBudget,
                )
            }
        }
        return signals
    }
}
