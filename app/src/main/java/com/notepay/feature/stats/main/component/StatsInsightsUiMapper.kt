package com.notepay.feature.stats.main.component

import com.notepay.R
import com.notepay.domain.analytics.DynamicDailyBudgetResult
import com.notepay.domain.analytics.SpendingPrediction
import com.notepay.domain.analytics.StatsAdviceSignal
import com.notepay.domain.analytics.StatsEarlyWarning
import com.notepay.domain.model.Category
import com.notepay.domain.money.Money
import com.notepay.feature.stats.main.AiAdviceItem
import com.notepay.feature.stats.main.BudgetForecast
import com.notepay.feature.stats.main.DynamicDailyBudgetData
import com.notepay.feature.stats.main.StatsUiText
import com.notepay.core.designsystem.formatter.MoneyFormatter
import java.util.Locale
import kotlin.math.abs

object StatsInsightsUiMapper {
    /**
     * [overBudgetProbabilityPercent] là phần trăm (0-100) đã nhân sẵn để tài nguyên hiển thị đúng
     * ngôn ngữ; `null` khi mô hình không đủ dữ liệu để ước lượng.
     */
    fun mapPrediction(
        prediction: SpendingPrediction,
        limitInCents: Long?,
        overBudgetProbabilityPercent: Int? = null,
    ): BudgetForecast {
        val messageParts = buildList {
            add(
                StatsUiText.Resource(
                    R.string.stats_forecast_message,
                    listOf(
                        MoneyFormatter.format(Money(prediction.dailyRunRateInCents)),
                        MoneyFormatter.format(Money(prediction.predictedMonthTotalInCents)),
                    ),
                ),
            )
            overBudgetProbabilityPercent?.let { percent ->
                add(
                    StatsUiText.Resource(
                        R.string.stats_forecast_probability_suffix,
                        listOf(percent),
                    ),
                )
            }
        }
        return BudgetForecast(
            dailyAverage = Money(prediction.dailyRunRateInCents),
            projectedSpend = Money(prediction.predictedMonthTotalInCents),
            forecastMessage = StatsUiText.Composite(messageParts),
            isProjectedToExceed = limitInCents != null && prediction.predictedMonthTotalInCents > limitInCents,
            trendPercent = prediction.trendVsPreviousMonth?.times(100)?.toFloat(),
            prediction = prediction,
        )
    }

    fun mapDynamicDailyBudget(result: DynamicDailyBudgetResult): DynamicDailyBudgetData {
        val earlyWarning = when (result.earlyWarning) {
            StatsEarlyWarning.FIRST_DAYS -> StatsUiText.Resource(
                R.string.stats_insight_early_warning_first_days,
                listOf(MoneyFormatter.format(Money(result.spentTodayInCents))),
            )
            StatsEarlyWarning.FIRST_WEEK -> {
                val initialBudget = result.initialDailyBudgetInCents
                if (initialBudget <= 0L) {
                    null
                } else {
                    StatsUiText.Resource(
                        R.string.stats_insight_early_warning_first_week,
                        listOf(
                            result.spentTodayInCents * 100 / initialBudget,
                            MoneyFormatter.format(Money(initialBudget * 8 / 10)),
                        ),
                    )
                }
            }
            null -> null
        }
        return DynamicDailyBudgetData(
            dailyBudget = Money(result.dailyBudgetInCents),
            spentToday = Money(result.spentTodayInCents),
            remainingToday = Money(result.remainingTodayInCents),
            tomorrowBudget = Money(result.tomorrowBudgetInCents),
            isExceeded = result.isExceeded,
            earlyWarning = earlyWarning,
        )
    }

    fun mapAdviceSignals(signals: List<StatsAdviceSignal>): List<AiAdviceItem> = signals.map { signal ->
        when (signal) {
            is StatsAdviceSignal.FoodShare -> AiAdviceItem(
                id = signal.id,
                type = signal.type,
                title = StatsUiText.Resource(R.string.stats_advice_food_title),
                content = StatsUiText.Resource(
                    R.string.stats_advice_food_content,
                    listOf("%.1f%%".format(Locale.ROOT, signal.percentage * 100)),
                ),
                categoryId = Category.FOOD.id,
                feedback = signal.feedback,
            )
            is StatsAdviceSignal.UpcomingSubscription -> AiAdviceItem(
                id = signal.id,
                type = signal.type,
                title = StatsUiText.Resource(R.string.stats_advice_bill_title),
                content = StatsUiText.Resource(
                    R.string.stats_advice_bill_content,
                    listOf(signal.name, MoneyFormatter.format(Money(signal.amountInCents))),
                ),
                feedback = signal.feedback,
            )
            is StatsAdviceSignal.SpendingSpike -> AiAdviceItem(
                id = signal.id,
                type = signal.type,
                title = StatsUiText.Resource(R.string.stats_engine_advice_spike_title),
                content = StatsUiText.Resource(
                    R.string.stats_engine_advice_spike_content,
                    listOf(
                        MoneyFormatter.format(Money(signal.spentTodayInCents)),
                        signal.ratioPercent,
                        MoneyFormatter.format(Money(signal.dailyAverageInCents)),
                    ),
                ),
                feedback = signal.feedback,
            )
            is StatsAdviceSignal.TrendChange -> AiAdviceItem(
                id = signal.id,
                type = signal.type,
                title = StatsUiText.Resource(
                    if (signal.isIncrease) R.string.stats_engine_advice_trend_up_title
                    else R.string.stats_engine_advice_trend_down_title,
                ),
                content = StatsUiText.Resource(
                    if (signal.isIncrease) R.string.stats_engine_advice_trend_up_content
                    else R.string.stats_engine_advice_trend_down_content,
                    listOf("%.0f".format(Locale.ROOT, abs(signal.changePercent))),
                ),
                feedback = signal.feedback,
            )
            is StatsAdviceSignal.Saving -> AiAdviceItem(
                id = signal.id,
                type = signal.type,
                title = StatsUiText.Resource(R.string.stats_engine_advice_saving_title),
                content = StatsUiText.Resource(
                    R.string.stats_engine_advice_saving_content,
                    listOf(signal.percentageOfInitialBudget),
                ),
                feedback = signal.feedback,
            )
        }
    }
}
