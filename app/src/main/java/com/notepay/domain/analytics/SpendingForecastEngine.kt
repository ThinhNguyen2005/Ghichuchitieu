package com.notepay.domain.analytics

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.max

data class DailyExpense(
    val date: LocalDate,
    val amountInCents: Long,
)

enum class ForecastConfidence { LOW, MEDIUM, HIGH }

/**
 * Kết quả số học đã được tính hoàn toàn bằng Kotlin. LLM chỉ được phép diễn giải các số này.
 */
data class SpendingPrediction(
    val spentSoFarInCents: Long,
    val predictedMonthTotalInCents: Long,
    val lowerBoundInCents: Long,
    val upperBoundInCents: Long,
    val dailyRunRateInCents: Long,
    val overBudgetProbability: Double?,
    val trendVsPreviousMonth: Double?,
    val observedDays: Int,
    val confidence: ForecastConfidence,
)

/**
 * Dự báo theo chuỗi thời gian ngắn: EWMA + nhịp chi theo thứ trong tuần + bootstrap phần dư.
 * Thuật toán có seed cố định để cùng một dữ liệu luôn cho cùng một kết quả.
 */
object SpendingForecastEngine {
    private const val HISTORY_DAYS = 90
    private const val RECENT_DAYS = 28
    private const val SIMULATION_COUNT = 400

    fun forecast(
        expenses: List<DailyExpense>,
        today: LocalDate,
        daysInMonth: Int,
        budgetLimitInCents: Long?,
    ): SpendingPrediction {
        val normalized = expenses
            .filter { it.amountInCents >= 0L && it.date <= today }
            .groupBy { it.date }
            .mapValues { (_, rows) -> rows.sumOf { it.amountInCents }.coerceAtLeast(0L) }

        val monthStart = LocalDate(today.year, today.monthNumber, 1)
        val spentSoFar = normalized
            .filterKeys { it >= monthStart && it <= today }
            .values
            .sum()

        val earliest = normalized.keys.minOrNull() ?: today
        val historyFloor = today.minus(DatePeriod(days = HISTORY_DAYS - 1))
        val historyStart = if (earliest > historyFloor) earliest else historyFloor
        val historyDates = dateRange(historyStart, today)
        val observedDays = historyDates.size

        val recentStart = today.minus(DatePeriod(days = RECENT_DAYS - 1))
        val recentDates = historyDates.filter { it >= recentStart }
        val recentValues = recentDates.map { (normalized[it] ?: 0L).toDouble() }
        val ewma = exponentiallyWeightedAverage(recentValues, alpha = 0.25)
        val monthAverage = spentSoFar.toDouble() / today.dayOfMonth.coerceAtLeast(1)

        val historyWeight = ((observedDays - 3) / 25.0).coerceIn(0.0, 0.65)
        val runRate = (ewma * historyWeight + monthAverage * (1.0 - historyWeight))
            .coerceAtLeast(0.0)

        val weekdayFactors = computeWeekdayFactors(historyDates, normalized, observedDays)
        val remainingDates = if (today.dayOfMonth >= daysInMonth) {
            emptyList()
        } else {
            dateRange(today.plus(DatePeriod(days = 1)), LocalDate(today.year, today.monthNumber, daysInMonth))
        }
        val baselines = remainingDates.map { date ->
            runRate * (weekdayFactors[date.dayOfWeek.ordinal] ?: 1.0)
        }
        val pointEstimate = spentSoFar.toDouble() + baselines.sum()

        val residuals = historyDates.map { date ->
            val actual = (normalized[date] ?: 0L).toDouble()
            actual - runRate * (weekdayFactors[date.dayOfWeek.ordinal] ?: 1.0)
        }.ifEmpty { listOf(0.0) }

        val simulations = simulateMonthTotals(
            spentSoFar = spentSoFar.toDouble(),
            baselines = baselines,
            residuals = residuals,
        )
        val lower = percentile(simulations, 0.10).coerceAtMost(pointEstimate)
        val upper = percentile(simulations, 0.90).coerceAtLeast(pointEstimate)
        val overBudgetProbability = budgetLimitInCents
            ?.takeIf { it > 0L }
            ?.let { limit -> simulations.count { it > limit }.toDouble() / simulations.size }

        val previousMonthAverage = previousMonthDailyAverage(normalized, monthStart)
        val trend = previousMonthAverage
            ?.takeIf { it > 0.0 }
            ?.let { (runRate - it) / it }

        return SpendingPrediction(
            spentSoFarInCents = spentSoFar,
            predictedMonthTotalInCents = pointEstimate.toLong().coerceAtLeast(spentSoFar),
            lowerBoundInCents = lower.toLong().coerceAtLeast(spentSoFar),
            upperBoundInCents = upper.toLong().coerceAtLeast(spentSoFar),
            dailyRunRateInCents = runRate.toLong().coerceAtLeast(0L),
            overBudgetProbability = overBudgetProbability,
            trendVsPreviousMonth = trend,
            observedDays = observedDays,
            confidence = when {
                observedDays >= 42 -> ForecastConfidence.HIGH
                observedDays >= 14 -> ForecastConfidence.MEDIUM
                else -> ForecastConfidence.LOW
            },
        )
    }

    private fun exponentiallyWeightedAverage(values: List<Double>, alpha: Double): Double {
        if (values.isEmpty()) return 0.0
        var result = values.first()
        for (index in 1 until values.size) {
            result = alpha * values[index] + (1.0 - alpha) * result
        }
        return result
    }

    private fun computeWeekdayFactors(
        dates: List<LocalDate>,
        values: Map<LocalDate, Long>,
        observedDays: Int,
    ): Map<Int, Double> {
        if (observedDays < 21) return (0..6).associateWith { 1.0 }
        val overall = dates.map { (values[it] ?: 0L).toDouble() }.average()
        if (overall <= 0.0) return (0..6).associateWith { 1.0 }

        val raw = (0..6).associateWith { weekday ->
            val samples = dates
                .filter { it.dayOfWeek.ordinal == weekday }
                .map { (values[it] ?: 0L).toDouble() }
            (samples.average() / overall).coerceIn(0.50, 1.80)
        }
        val normalization = raw.values.average().takeIf { it > 0.0 } ?: 1.0
        return raw.mapValues { (_, value) -> value / normalization }
    }

    private fun simulateMonthTotals(
        spentSoFar: Double,
        baselines: List<Double>,
        residuals: List<Double>,
    ): List<Double> {
        if (baselines.isEmpty()) return List(SIMULATION_COUNT) { spentSoFar }
        var randomState = 0x4E6F74655061794CL
        return List(SIMULATION_COUNT) {
            var total = spentSoFar
            baselines.forEach { baseline ->
                randomState = randomState * 6364136223846793005L + 1442695040888963407L
                val index = ((randomState ushr 1) % residuals.size.toLong()).toInt()
                total += max(0.0, baseline + residuals[index])
            }
            total
        }.sorted()
    }

    private fun percentile(sortedValues: List<Double>, fraction: Double): Double {
        if (sortedValues.isEmpty()) return 0.0
        val index = ((sortedValues.lastIndex) * fraction).toInt().coerceIn(sortedValues.indices)
        return sortedValues[index]
    }

    private fun previousMonthDailyAverage(
        values: Map<LocalDate, Long>,
        currentMonthStart: LocalDate,
    ): Double? {
        val previousMonthEnd = currentMonthStart.minus(DatePeriod(days = 1))
        val previousMonthStart = LocalDate(previousMonthEnd.year, previousMonthEnd.monthNumber, 1)
        val dates = dateRange(previousMonthStart, previousMonthEnd)
        val hasData = values.keys.any { it in previousMonthStart..previousMonthEnd }
        if (!hasData) return null
        return dates.map { (values[it] ?: 0L).toDouble() }.average()
    }

    private fun dateRange(start: LocalDate, endInclusive: LocalDate): List<LocalDate> {
        if (start > endInclusive) return emptyList()
        val dates = mutableListOf<LocalDate>()
        var cursor = start
        while (cursor <= endInclusive) {
            dates += cursor
            cursor = cursor.plus(DatePeriod(days = 1))
        }
        return dates
    }
}
