package com.notepay.domain.analytics

import com.notepay.domain.model.Money
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

enum class TrendAxisUnit { THOUSANDS, MILLIONS }

data class TrendAxisScale(
    val topInCents: Long,
    val labels: List<Float>,
    val unit: TrendAxisUnit,
)

enum class ForecastDisplayState { HIDDEN, PROJECTION, REACHED, EXCEEDED }

object StatsChartCalculator {
    fun average(values: List<Long>): Long = if (values.isEmpty()) 0L else values.sum() / values.size

    fun trendAxisScale(actualValues: List<Long>, forecast: Money?): TrendAxisScale {
        val maxValCents = max(
            1L,
            max(actualValues.maxOrNull() ?: 0L, forecast?.amountInCents ?: 0L),
        )
        val isMillions = maxValCents >= 100_000_000L
        val unitDivisor = if (isMillions) 100_000_000f else 100_000f
        val unit = if (isMillions) TrendAxisUnit.MILLIONS else TrendAxisUnit.THOUSANDS
        val valueInUnits = maxValCents.toFloat() / unitDivisor

        val (step, intervals) = when {
            valueInUnits <= 1f -> 0.2f to 5
            valueInUnits <= 2.5f -> 0.5f to 5
            valueInUnits <= 5f -> 1f to 5
            valueInUnits <= 10f -> 2f to 5
            valueInUnits <= 20f -> 5f to 4
            valueInUnits <= 50f -> 10f to 5
            valueInUnits <= 60f -> 10f to 6
            valueInUnits <= 100f -> 20f to 5
            valueInUnits <= 200f -> 50f to 4
            valueInUnits <= 500f -> 100f to 5
            valueInUnits <= 1000f -> 200f to 5
            else -> {
                val rawStep = valueInUnits / 4f
                val exponent = floor(log10(rawStep.toDouble())).toFloat()
                val base = 10.0.pow(exponent.toDouble()).toFloat()
                val stepValue = ceil((rawStep / base).toDouble()).toFloat() * base
                val count = ceil((valueInUnits / stepValue).toDouble()).toInt().coerceIn(3, 6)
                stepValue to count
            }
        }

        val topInUnits = step * intervals
        val topInCents = (topInUnits * unitDivisor).toLong().coerceAtLeast(maxValCents)
        return TrendAxisScale(
            topInCents = topInCents,
            labels = (intervals downTo 0).map { it * step },
            unit = unit,
        )
    }

    fun percentageChange(current: Long, previous: Long): Float? = when {
        previous == 0L -> null
        else -> ((current - previous).toDouble() / previous * 100).toFloat()
    }

    fun shouldShowForecast(
        isExpense: Boolean,
        isSelectedMonthCurrent: Boolean,
        forecast: Money?,
    ): Boolean = isExpense && isSelectedMonthCurrent && (forecast?.amountInCents ?: 0L) > 0L

    fun forecastDisplayState(
        isExpense: Boolean,
        isSelectedMonthCurrent: Boolean,
        actualAmountInCents: Long,
        forecast: Money?,
    ): ForecastDisplayState {
        if (!shouldShowForecast(isExpense, isSelectedMonthCurrent, forecast)) {
            return ForecastDisplayState.HIDDEN
        }
        return when {
            forecast!!.amountInCents > actualAmountInCents -> ForecastDisplayState.PROJECTION
            forecast.amountInCents == actualAmountInCents -> ForecastDisplayState.REACHED
            else -> ForecastDisplayState.EXCEEDED
        }
    }

    fun forecastMarkerFraction(
        state: ForecastDisplayState,
        forecast: Money?,
        axisTopInCents: Long,
    ): Float? {
        if (state != ForecastDisplayState.REACHED && state != ForecastDisplayState.EXCEEDED) return null
        val forecastAmountInCents = forecast?.amountInCents ?: return null
        if (axisTopInCents <= 0L) return null
        return (forecastAmountInCents.toFloat() / axisTopInCents).coerceIn(0f, 1f)
    }
}
