package com.notepay.ui.feature.stats

import com.google.common.truth.Truth.assertThat
import com.notepay.R
import com.notepay.domain.analytics.DynamicDailyBudgetResult
import com.notepay.domain.analytics.StatsAdviceSignal
import com.notepay.domain.analytics.StatsAdviceType
import com.notepay.domain.analytics.StatsEarlyWarning
import com.notepay.domain.analytics.StatsForecastResult
import org.junit.Test

class StatsInsightsUiMapperTest {

    @Test
    fun `maps forecast values and resource text without Android context`() {
        val result = StatsInsightsUiMapper.mapForecast(
            StatsForecastResult(
                dailyAverageInCents = 125_000L,
                projectedSpendInCents = 3_750_000L,
                isProjectedToExceed = true,
                previousMonthDailyAverageInCents = 100_000L,
                trendPercent = 25f,
            ),
            limitInCents = 3_000_000L,
        )

        assertThat(result.dailyAverage.amountInCents).isEqualTo(125_000L)
        assertThat(result.projectedSpend.amountInCents).isEqualTo(3_750_000L)
        assertThat(result.forecastMessage).isInstanceOf(StatsUiText.Composite::class.java)
        assertThat(result.trendMessage).isEqualTo(
            StatsUiText.Resource(R.string.stats_insight_trend_up, listOf("25")),
        )
    }

    @Test
    fun `maps dynamic budget warning to presentation resource`() {
        val mapped = StatsInsightsUiMapper.mapDynamicDailyBudget(
            DynamicDailyBudgetResult(
                dailyBudgetInCents = 100_000L,
                spentTodayInCents = 250_000L,
                remainingTodayInCents = 0L,
                tomorrowBudgetInCents = 90_000L,
                isExceeded = true,
                earlyWarning = StatsEarlyWarning.FIRST_WEEK,
                initialDailyBudgetInCents = 100_000L,
            ),
        )

        val warning = mapped.earlyWarning as StatsUiText.Resource
        assertThat(warning.resId).isEqualTo(R.string.stats_insight_early_warning_first_week)
        assertThat(warning.args[0]).isEqualTo(250L)
        assertThat(warning.args[1]).isEqualTo("800 ₫")
    }

    @Test
    fun `maps advice signal type and feedback`() {
        val mapped = StatsInsightsUiMapper.mapAdviceSignals(
            listOf(
                StatsAdviceSignal.FoodShare(feedback = -1, percentage = 0.4f),
            ),
        )

        assertThat(mapped).hasSize(1)
        assertThat(mapped.single().id).isEqualTo("advice_food")
        assertThat(mapped.single().type).isEqualTo("warning")
        assertThat(mapped.single().feedback).isEqualTo(-1)
        assertThat(mapped.single().title).isEqualTo(
            StatsUiText.Resource(R.string.stats_advice_food_title),
        )
    }
}
