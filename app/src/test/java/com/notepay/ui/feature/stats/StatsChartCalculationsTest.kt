package com.notepay.ui.feature.stats

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Money
import org.junit.Test

class StatsChartCalculationsTest {
    @Test fun `average returns zero for empty values`() { assertThat(StatsChartCalculations.average(emptyList())).isEqualTo(0L) }
    @Test fun `average calculates integer mean`() { assertThat(StatsChartCalculations.average(listOf(100L, 200L, 400L))).isEqualTo(233L) }

    @Test fun `axis uses thousand labels for small real amounts`() {
        val axis = StatsChartCalculations.trendAxisScale(listOf(20_000_000L), null)
        assertThat(axis.unitLabel).isEqualTo("Nghìn")
        assertThat(axis.labels).containsExactly(200f, 150f, 100f, 50f, 0f).inOrder()
    }

    @Test fun `forecast expands chart top without changing its unit`() {
        val axis = StatsChartCalculations.trendAxisScale(listOf(5_000_000L), Money(150_000_000L))
        assertThat(axis.unitLabel).isEqualTo("Triệu")
        assertThat(axis.topInCents).isAtLeast(150_000_000L)
    }

    @Test fun `percentage change handles increase decrease and zero baseline`() {
        assertThat(StatsChartCalculations.percentageChange(120L, 100L)).isWithin(.01f).of(20f)
        assertThat(StatsChartCalculations.percentageChange(80L, 100L)).isWithin(.01f).of(-20f)
        assertThat(StatsChartCalculations.percentageChange(20L, 0L)).isNull()
    }

    @Test fun `forecast is only shown for current expense with a value`() {
        assertThat(StatsChartCalculations.shouldShowForecast(StatsMetric.CHI_TIEU, true, Money(1L))).isTrue()
        assertThat(StatsChartCalculations.shouldShowForecast(StatsMetric.THU_NHAP, true, Money(1L))).isFalse()
        assertThat(StatsChartCalculations.shouldShowForecast(StatsMetric.CHI_TIEU, false, Money(1L))).isFalse()
        assertThat(StatsChartCalculations.shouldShowForecast(StatsMetric.CHI_TIEU, true, null)).isFalse()
    }
}