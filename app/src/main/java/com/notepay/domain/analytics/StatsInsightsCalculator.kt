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

    fun computeAdvices(input: StatsAdviceInput): List<StatsAdviceSignal> = buildList {
        addFoodShareSignal(input)
        addUpcomingSubscriptionSignals(input)
        addSpendingSpikeSignal(input)
        addTrendChangeSignal(input)
        addSavingSignal(input)
    }

    private fun MutableList<StatsAdviceSignal>.addFoodShareSignal(input: StatsAdviceInput) {
        val foodShare = input.categoryShares.firstOrNull { it.categoryId == Category.FOOD.id }
        if (foodShare != null && foodShare.share > 0.35f && !isDismissed(input.feedbacks, "advice_food")) {
            add(
                StatsAdviceSignal.FoodShare(
                    feedback = input.feedbacks["advice_food"] ?: 0,
                    percentage = foodShare.share,
                ),
            )
        }
    }

    private fun MutableList<StatsAdviceSignal>.addUpcomingSubscriptionSignals(input: StatsAdviceInput) {
        val balanceInCents = input.incomeInCents - input.expenseInCents
        input.subscriptions
            .filter { subscription ->
                subscription.isActive && (subscription.nextDueDate - input.now).inWholeDays in 0..3
            }
            .forEach { subscription ->
                val id = "advice_bill_balance_${subscription.id}"
                if (balanceInCents < subscription.amount.amountInCents && !isDismissed(input.feedbacks, id)) {
                    add(
                        StatsAdviceSignal.UpcomingSubscription(
                            id = id,
                            feedback = input.feedbacks[id] ?: 0,
                            name = subscription.name,
                            amountInCents = subscription.amount.amountInCents,
                        ),
                    )
                }
            }
    }

    private fun MutableList<StatsAdviceSignal>.addSpendingSpikeSignal(input: StatsAdviceInput) {
        if (input.remainingDays < 3 || input.currentDay <= 1 || isDismissed(input.feedbacks, "advice_spike")) {
            return
        }
        val dailyAverage = input.currentMonthExpenseInCents / input.currentDay
        if (dailyAverage > 0L && input.spentTodayInCents > dailyAverage * 2L) {
            add(
                StatsAdviceSignal.SpendingSpike(
                    feedback = input.feedbacks["advice_spike"] ?: 0,
                    ratioPercent = input.spentTodayInCents * 100 / dailyAverage,
                    spentTodayInCents = input.spentTodayInCents,
                    dailyAverageInCents = dailyAverage,
                ),
            )
        }
    }

    private fun MutableList<StatsAdviceSignal>.addTrendChangeSignal(input: StatsAdviceInput) {
        val previousAverage = input.previousMonthDailyAverageInCents ?: return
        val limit = input.limitInCents ?: return
        val canEvaluate = previousAverage > 0L && limit > 0L && input.currentDay > 3
        if (!canEvaluate || isDismissed(input.feedbacks, "advice_trend")) return

        val currentAverage = input.currentMonthExpenseInCents / input.currentDay
        val changePercent = ((currentAverage - previousAverage).toFloat() / previousAverage * 100)
        if (changePercent > 20f || changePercent < -20f) {
            add(
                StatsAdviceSignal.TrendChange(
                    feedback = input.feedbacks["advice_trend"] ?: 0,
                    changePercent = changePercent,
                    isIncrease = changePercent > 20f,
                ),
            )
        }
    }

    private fun MutableList<StatsAdviceSignal>.addSavingSignal(input: StatsAdviceInput) {
        val limit = input.limitInCents ?: return
        val canEvaluate = limit > 0L && input.remainingDays >= 5 && input.currentDay > 1
        if (!canEvaluate || isDismissed(input.feedbacks, "advice_saving")) return

        val initialDailyBudget = limit / input.daysInMonth.coerceAtLeast(1)
        val currentDailyAverage =
            (input.currentMonthExpenseInCents - input.spentTodayInCents) / (input.currentDay - 1)
        if (currentDailyAverage in 1 until (initialDailyBudget * 85 / 100)) {
            add(
                StatsAdviceSignal.Saving(
                    feedback = input.feedbacks["advice_saving"] ?: 0,
                    currentDailyAverageInCents = currentDailyAverage,
                    percentageOfInitialBudget = currentDailyAverage * 100 / initialDailyBudget,
                ),
            )
        }
    }

    private fun isDismissed(feedbacks: Map<String, Int>, key: String): Boolean = (feedbacks[key] ?: 0) != 0
}
