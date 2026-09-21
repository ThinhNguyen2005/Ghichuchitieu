package com.notepay.ui.feature.stats

import com.notepay.R
import com.notepay.domain.analytics.DynamicDailyBudgetResult
import com.notepay.domain.analytics.SpendingPrediction
import com.notepay.domain.analytics.StatsAdviceSignal
import com.notepay.domain.analytics.StatsAdviceType
import com.notepay.domain.analytics.StatsEarlyWarning
import com.notepay.domain.analytics.StatsForecastResult
import com.notepay.domain.model.Money
import com.notepay.ui.util.MoneyFormatter
import java.util.Locale

object StatsInsightsUiMapper {
    fun mapForecast(result: StatsForecastResult, limitInCents: Long?): BudgetForecast {
        val parts = buildList {
            add(
                StatsUiText.Resource(
                    R.string.stats_insight_forecast_format,
                    listOf(
                        MoneyFormatter.format(Money(result.dailyAverageInCents)),
                        MoneyFormatter.format(Money(result.projectedSpendInCents)),
                    ),
                ),
            )
            if (result.isProjectedToExceed && limitInCents != null) {
                add(
                    StatsUiText.Resource(
                        R.string.stats_insight_forecast_exceeded,
                        listOf(MoneyFormatter.format(Money(limitInCents))),
                    ),
                )
            }
            result.previousMonthDailyAverageInCents
                ?.takeIf { it > 0L }
                ?.let { previous ->
                    add(
                        StatsUiText.Resource(
                            R.string.stats_insight_forecast_previous,
                            listOf(MoneyFormatter.format(Money(previous))),
                        ),
                    )
                }
        }
        return BudgetForecast(
            dailyAverage = Money(result.dailyAverageInCents),
            projectedSpend = Money(result.projectedSpendInCents),
            forecastMessage = StatsUiText.Composite(parts),
            isProjectedToExceed = result.isProjectedToExceed,
            previousMonthDailyAverage = result.previousMonthDailyAverageInCents?.let(::Money),
            trendPercent = result.trendPercent,
            trendMessage = mapTrend(result.trendPercent),
        )
    }

    fun mapPrediction(
        prediction: SpendingPrediction,
        limitInCents: Long?,
        probabilitySuffix: String = "",
    ): BudgetForecast {
        return BudgetForecast(
            dailyAverage = Money(prediction.dailyRunRateInCents),
            projectedSpend = Money(prediction.predictedMonthTotalInCents),
            forecastMessage = StatsUiText.Resource(
                R.string.stats_forecast_message,
                listOf(
                    MoneyFormatter.format(Money(prediction.dailyRunRateInCents)),
                    MoneyFormatter.format(Money(prediction.predictedMonthTotalInCents)),
                    probabilitySuffix,
                ),
            ),
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
                type = signal.type.asUiType(),
                title = StatsUiText.Resource(R.string.stats_advice_food_title),
                content = StatsUiText.Resource(
                    R.string.stats_advice_food_content,
                    listOf("%.1f%%".format(Locale.ROOT, signal.percentage * 100)),
                ),
                categoryId = com.notepay.domain.model.Category.FOOD.id,
                feedback = signal.feedback,
            )
            is StatsAdviceSignal.UpcomingSubscription -> AiAdviceItem(
                id = signal.id,
                type = signal.type.asUiType(),
                title = StatsUiText.Resource(R.string.stats_advice_bill_title),
                content = StatsUiText.Resource(
                    R.string.stats_advice_bill_content,
                    listOf(signal.name, MoneyFormatter.format(Money(signal.amountInCents))),
                ),
                feedback = signal.feedback,
            )
            is StatsAdviceSignal.SpendingSpike -> AiAdviceItem(
                id = signal.id,
                type = signal.type.asUiType(),
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
                type = signal.type.asUiType(),
                title = StatsUiText.Resource(
                    if (signal.isIncrease) R.string.stats_engine_advice_trend_up_title
                    else R.string.stats_engine_advice_trend_down_title,
                ),
                content = StatsUiText.Resource(
                    if (signal.isIncrease) R.string.stats_engine_advice_trend_up_content
                    else R.string.stats_engine_advice_trend_down_content,
                    listOf("%.0f".format(Locale.ROOT, kotlin.math.abs(signal.changePercent))),
                ),
                feedback = signal.feedback,
            )
            is StatsAdviceSignal.Saving -> AiAdviceItem(
                id = signal.id,
                type = signal.type.asUiType(),
                title = StatsUiText.Resource(R.string.stats_engine_advice_saving_title),
                content = StatsUiText.Resource(
                    R.string.stats_engine_advice_saving_content,
                    listOf(signal.percentageOfInitialBudget),
                ),
                feedback = signal.feedback,
            )
        }
    }

    private fun mapTrend(trendPercent: Float?): StatsUiText? = when {
        trendPercent == null -> null
        trendPercent > 5f -> StatsUiText.Resource(
            R.string.stats_insight_trend_up,
            listOf("%.0f".format(Locale.ROOT, trendPercent)),
        )
        trendPercent < -5f -> StatsUiText.Resource(
            R.string.stats_insight_trend_down,
            listOf("%.0f".format(Locale.ROOT, -trendPercent)),
        )
        else -> StatsUiText.Resource(R.string.stats_insight_trend_stable)
    }

    private fun StatsAdviceType.asUiType(): String = name.lowercase(Locale.ROOT)
}
