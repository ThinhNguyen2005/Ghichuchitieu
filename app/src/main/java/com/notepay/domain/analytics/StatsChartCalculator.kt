package com.notepay.domain.analytics

import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

enum class PeriodBarBucketKind {
    WEEKDAY,
    DAY_OF_MONTH,
    MONTH,
    YEAR,
    DATE_SHORT,
}

data class PeriodBarData(
    val key: String,
    val bucketKind: PeriodBarBucketKind,
    val indexValue: Int,
    val monthValue: Int? = null,
    val amountInCents: Long,
    val isCurrentBucket: Boolean,
)

enum class TrendAxisUnit { THOUSANDS, MILLIONS }

data class TrendAxisScale(
    val topInCents: Long,
    val labels: List<Float>,
    val unit: TrendAxisUnit,
)

/**
 * Trạng thái hiển thị của dự báo chi tiêu cuối tháng.
 *
 * Dự báo không phải ngân sách: chạm hay vượt mốc dự báo là thông tin trung tính.
 * Chỉ [OVER_BUDGET] mới là cảnh báo, và chỉ xuất hiện khi thực sự có ngân sách tháng.
 */
enum class ForecastDisplayState {
    /** Không đủ điều kiện hiển thị dự báo. */
    HIDDEN,

    /** Chưa có ngân sách, thực tế còn dưới mốc dự báo. */
    PROJECTION,

    /** Chưa có ngân sách, thực tế đã chạm hoặc vượt mốc dự báo. */
    PASSED,

    /** Có ngân sách và dự báo cuối tháng vẫn nằm trong ngân sách. */
    WITHIN_BUDGET,

    /** Có ngân sách và dự báo cuối tháng vượt ngân sách. */
    OVER_BUDGET,
}

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
        budgetInCents: Long?,
    ): ForecastDisplayState {
        if (!shouldShowForecast(isExpense, isSelectedMonthCurrent, forecast)) {
            return ForecastDisplayState.HIDDEN
        }
        val forecastInCents = forecast?.amountInCents ?: return ForecastDisplayState.HIDDEN
        return when {
            budgetInCents != null && forecastInCents > budgetInCents -> ForecastDisplayState.OVER_BUDGET
            budgetInCents != null -> ForecastDisplayState.WITHIN_BUDGET
            actualAmountInCents >= forecastInCents -> ForecastDisplayState.PASSED
            else -> ForecastDisplayState.PROJECTION
        }
    }

    /**
     * Vị trí mốc dự báo trên cột, chỉ vẽ khi cột thực tế đã chạm mốc.
     * Khi thực tế còn thấp hơn dự báo thì phần chênh lệch đã được thể hiện bằng vùng mờ.
     */
    fun forecastMarkerFraction(
        actualAmountInCents: Long,
        forecast: Money?,
        axisTopInCents: Long,
    ): Float? {
        val forecastAmountInCents = forecast?.amountInCents ?: return null
        if (axisTopInCents <= 0L || actualAmountInCents < forecastAmountInCents) return null
        return (forecastAmountInCents.toFloat() / axisTopInCents).coerceIn(0f, 1f)
    }

    /**
     * Gom nhóm chi tiêu hoặc thu nhập theo từng cột thời gian trong kỳ đang chọn.
     *
     * - [StatsRange.WEEK]: 7 ngày Thứ 2..Chủ nhật
     * - [StatsRange.MONTH]: toàn bộ các ngày trong tháng
     * - [StatsRange.YEAR]: 12 tháng trong năm
     * - [StatsRange.CUSTOM]: theo ngày nếu <= 31 ngày, ngược lại theo tháng
     * - [StatsRange.ALL] / null: theo tháng nếu <= 12 tháng, ngược lại theo năm
     */
    fun buildPeriodBars(
        transactions: List<Transaction>,
        type: TransactionType,
        period: StatsPeriod?,
        selectedCategory: Category?,
        today: LocalDate,
        zone: TimeZone,
    ): List<PeriodBarData> {
        val datedAmounts = transactions.mapNotNull { tx ->
            if (tx.type != type) return@mapNotNull null
            if (selectedCategory != null && tx.category != selectedCategory) return@mapNotNull null
            val date = tx.occurredAt.toLocalDateTime(zone).date
            if (period != null && !period.contains(date)) return@mapNotNull null
            date to tx.amount.amountInCents
        }
        return when (period?.range) {
            StatsRange.WEEK -> buildWeekBars(datedAmounts, period, today)
            StatsRange.MONTH -> buildMonthBars(datedAmounts, period, today)
            StatsRange.YEAR -> buildYearBars(datedAmounts, period, today)
            StatsRange.CUSTOM -> buildCustomBars(datedAmounts, period, today)
            StatsRange.ALL, null -> buildAllTimeBars(datedAmounts, today)
        }
    }
}

private fun buildWeekBars(
    datedAmounts: List<Pair<LocalDate, Long>>,
    period: StatsPeriod,
    today: LocalDate,
): List<PeriodBarData> {
    val start = period.start ?: today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    val totalsByDate = sumByDate(datedAmounts)
    return (0..6).map { offset ->
        val date = start.plus(offset, DateTimeUnit.DAY)
        PeriodBarData(
            key = date.toString(),
            bucketKind = PeriodBarBucketKind.WEEKDAY,
            indexValue = date.dayOfWeek.isoDayNumber,
            monthValue = date.month.number,
            amountInCents = totalsByDate[date] ?: 0L,
            isCurrentBucket = date == today,
        )
    }
}

private fun buildMonthBars(
    datedAmounts: List<Pair<LocalDate, Long>>,
    period: StatsPeriod,
    today: LocalDate,
): List<PeriodBarData> {
    val start = period.start ?: LocalDate(today.year, today.month.number, 1)
    val daysInMonth = StatsSnapshotCalculator.monthLength(start.year, start.month.number)
    val totalsByDate = sumByDate(datedAmounts)
    return (1..daysInMonth).map { day ->
        val date = LocalDate(start.year, start.month.number, day)
        PeriodBarData(
            key = date.toString(),
            bucketKind = PeriodBarBucketKind.DAY_OF_MONTH,
            indexValue = day,
            monthValue = start.month.number,
            amountInCents = totalsByDate[date] ?: 0L,
            isCurrentBucket = date == today,
        )
    }
}

private fun buildYearBars(
    datedAmounts: List<Pair<LocalDate, Long>>,
    period: StatsPeriod,
    today: LocalDate,
): List<PeriodBarData> {
    val year = period.start?.year ?: today.year
    val totalsByMonth = HashMap<Int, Long>(12)
    datedAmounts.forEach { (date, amount) ->
        if (date.year == year) {
            val month = date.month.number
            totalsByMonth[month] = (totalsByMonth[month] ?: 0L) + amount
        }
    }
    return (1..12).map { month ->
        PeriodBarData(
            key = "$year-$month",
            bucketKind = PeriodBarBucketKind.MONTH,
            indexValue = month,
            monthValue = month,
            amountInCents = totalsByMonth[month] ?: 0L,
            isCurrentBucket = year == today.year && month == today.month.number,
        )
    }
}

private fun buildCustomBars(
    datedAmounts: List<Pair<LocalDate, Long>>,
    period: StatsPeriod,
    today: LocalDate,
): List<PeriodBarData> {
    val start = period.start ?: return emptyList()
    val endExclusive = period.endExclusive ?: return emptyList()
    val totalDays = start.daysUntil(endExclusive)
    if (totalDays <= 0) return emptyList()

    return if (totalDays <= 31) {
        val totalsByDate = sumByDate(datedAmounts)
        (0 until totalDays).map { offset ->
            val date = start.plus(offset, DateTimeUnit.DAY)
            PeriodBarData(
                key = date.toString(),
                bucketKind = PeriodBarBucketKind.DATE_SHORT,
                indexValue = date.day,
                monthValue = date.month.number,
                amountInCents = totalsByDate[date] ?: 0L,
                isCurrentBucket = date == today,
            )
        }
    } else {
        val endInclusive = endExclusive.minus(1, DateTimeUnit.DAY)
        buildMonthlySpanBars(datedAmounts, start, endInclusive, today)
    }
}

private fun buildAllTimeBars(
    datedAmounts: List<Pair<LocalDate, Long>>,
    today: LocalDate,
): List<PeriodBarData> {
    if (datedAmounts.isEmpty()) return emptyList()
    var minDate = datedAmounts.first().first
    var maxDate = datedAmounts.first().first
    datedAmounts.forEach { (date, _) ->
        if (date < minDate) minDate = date
        if (date > maxDate) maxDate = date
    }
    val monthSpan = (maxDate.year - minDate.year) * 12 + (maxDate.month.number - minDate.month.number) + 1
    return if (monthSpan <= 12) {
        buildMonthlySpanBars(datedAmounts, minDate, maxDate, today)
    } else {
        val totalsByYear = HashMap<Int, Long>()
        datedAmounts.forEach { (date, amount) ->
            totalsByYear[date.year] = (totalsByYear[date.year] ?: 0L) + amount
        }
        (minDate.year..maxDate.year).map { year ->
            PeriodBarData(
                key = year.toString(),
                bucketKind = PeriodBarBucketKind.YEAR,
                indexValue = year,
                monthValue = null,
                amountInCents = totalsByYear[year] ?: 0L,
                isCurrentBucket = year == today.year,
            )
        }
    }
}

private fun buildMonthlySpanBars(
    datedAmounts: List<Pair<LocalDate, Long>>,
    startDate: LocalDate,
    endInclusive: LocalDate,
    today: LocalDate,
): List<PeriodBarData> {
    val totalsByYearMonth = HashMap<Pair<Int, Int>, Long>()
    datedAmounts.forEach { (date, amount) ->
        val ym = date.year to date.month.number
        totalsByYearMonth[ym] = (totalsByYearMonth[ym] ?: 0L) + amount
    }
    val result = ArrayList<PeriodBarData>()
    var cursor = LocalDate(startDate.year, startDate.month.number, 1)
    val endMonth = LocalDate(endInclusive.year, endInclusive.month.number, 1)
    while (cursor <= endMonth) {
        val ym = cursor.year to cursor.month.number
        result.add(
            PeriodBarData(
                key = "${cursor.year}-${cursor.month.number}",
                bucketKind = PeriodBarBucketKind.MONTH,
                indexValue = cursor.month.number,
                monthValue = cursor.month.number,
                amountInCents = totalsByYearMonth[ym] ?: 0L,
                isCurrentBucket = cursor.year == today.year && cursor.month.number == today.month.number,
            ),
        )
        cursor = cursor.plus(1, DateTimeUnit.MONTH)
    }
    return result
}

private fun sumByDate(datedAmounts: List<Pair<LocalDate, Long>>): Map<LocalDate, Long> {
    val totals = HashMap<LocalDate, Long>(datedAmounts.size)
    datedAmounts.forEach { (date, amount) ->
        totals[date] = (totals[date] ?: 0L) + amount
    }
    return totals
}
