package com.notepay.domain.analytics

import androidx.compose.runtime.Immutable
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToLong

@Immutable
data class CategoryYoYDelta(
    val category: Category,
    val currentCents: Long,
    val previousCents: Long,
    val deltaCents: Long,
    val changePercent: Float?,
)

@Immutable
data class YoYPeriodComparison(
    val currentExpenseCents: Long,
    val previousExpenseCents: Long,
    val expenseDeltaCents: Long,
    val expenseChangePercent: Float?,
    val currentIncomeCents: Long,
    val previousIncomeCents: Long,
    val incomeDeltaCents: Long,
    val incomeChangePercent: Float?,
    val topRisingCategories: List<CategoryYoYDelta>,
    val topFallingCategories: List<CategoryYoYDelta>,
)

@Immutable
data class RollingMonthComparisonPoint(
    val year: Int,
    val month: Int,
    val currentYearExpenseCents: Long,
    val priorYearExpenseCents: Long,
    val currentYearIncomeCents: Long,
    val priorYearIncomeCents: Long,
)

@Immutable
data class SeasonalPatternInsight(
    val season: VietnameseSeason,
    val seasonalIndex: Float,
    val changePercent: Float,
    val seasonalMonthlyEquivalentCents: Long,
    val baselineMonthlyAverageCents: Long,
    val topCategories: List<Category>,
)

@Immutable
data class ThreeMonthMoMFallback(
    val currentMonthExpenseCents: Long,
    val previousMonthsAverageCents: Long,
    val deltaCents: Long,
    val changePercent: Float?,
)

sealed interface LongTermTrendAnalysis {
    val nextTetDays: Int?

    @Immutable
    data class InsufficientData(
        val monthsAvailable: Int,
        val monthsRequired: Int = MIN_MONTHS_REQUIRED,
        val threeMonthFallback: ThreeMonthMoMFallback?,
        override val nextTetDays: Int?,
    ) : LongTermTrendAnalysis

    @Immutable
    data class Ready(
        val monthsAvailable: Int,
        val yoy: YoYPeriodComparison,
        val rolling12Months: List<RollingMonthComparisonPoint>,
        val seasonalInsights: List<SeasonalPatternInsight>,
        override val nextTetDays: Int?,
    ) : LongTermTrendAnalysis

    companion object {
        const val MIN_MONTHS_REQUIRED = 12
    }
}

/**
 * Pure-Kotlin long-term Year-over-Year (YoY) and Vietnamese seasonal spending analyzer.
 * Excludes internal transfers and respects wallet / includeInTotalBalance filters.
 */
class LongTermTrendAnalyzer {

    private data class DatedTransaction(
        val tx: Transaction,
        val date: LocalDate,
        val yearMonth: Int,
    )

    fun analyze(
        transactions: List<Transaction>,
        wallets: List<Wallet>,
        selectedWalletId: Long?,
        anchorDate: LocalDate,
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): LongTermTrendAnalysis {
        val eligible = filterEligible(transactions, wallets, selectedWalletId, anchorDate, zone)
        val anchorYm = toYearMonth(anchorDate.year, anchorDate.month.number)
        val earliestDate = eligible.minOfOrNull { it.date }
        val monthsAvailable = if (earliestDate == null) {
            0
        } else {
            (anchorYm - toYearMonth(earliestDate.year, earliestDate.month.number) + 1).coerceAtLeast(1)
        }
        val nextTetDays = VietnameseLunarCalendar.daysUntilNextTet(anchorDate)

        if (monthsAvailable < LongTermTrendAnalysis.MIN_MONTHS_REQUIRED || earliestDate == null) {
            val fallback = computeThreeMonthFallback(eligible, anchorYm, toYearMonth(earliestDate?.year ?: anchorDate.year, earliestDate?.month?.number ?: anchorDate.month.number))
            return LongTermTrendAnalysis.InsufficientData(
                monthsAvailable = monthsAvailable,
                monthsRequired = LongTermTrendAnalysis.MIN_MONTHS_REQUIRED,
                threeMonthFallback = if (earliestDate != null) fallback else null,
                nextTetDays = nextTetDays,
            )
        }

        val yoy = buildYoYComparison(eligible, anchorDate.year, anchorDate.month.number)
        val rolling12Months = buildRolling12Months(eligible, anchorYm)
        val seasonalInsights = detectSeasonalInsights(eligible, earliestDate, anchorDate)

        return LongTermTrendAnalysis.Ready(
            monthsAvailable = monthsAvailable,
            yoy = yoy,
            rolling12Months = rolling12Months,
            seasonalInsights = seasonalInsights,
            nextTetDays = nextTetDays,
        )
    }

    private fun filterEligible(
        transactions: List<Transaction>,
        wallets: List<Wallet>,
        selectedWalletId: Long?,
        anchorDate: LocalDate,
        zone: TimeZone,
    ): List<DatedTransaction> {
        val excludedWalletIds = if (selectedWalletId == null && wallets.isNotEmpty()) {
            wallets.filterNot { it.includeInTotalBalance }.mapTo(hashSetOf()) { it.id }
        } else {
            emptySet()
        }
        val endOfAnchorMonth = LocalDate(anchorDate.year, anchorDate.month.number, 1)
            .plus(DatePeriod(months = 1))
            .minus(DatePeriod(days = 1))

        return transactions.mapNotNull { tx ->
            if (tx.isInternalTransfer) return@mapNotNull null
            val walletMatches = if (selectedWalletId != null) {
                tx.walletId == selectedWalletId
            } else {
                tx.walletId !in excludedWalletIds
            }
            if (!walletMatches) return@mapNotNull null
            val date = tx.occurredAt.toLocalDateTime(zone).date
            if (date > endOfAnchorMonth) return@mapNotNull null
            DatedTransaction(tx = tx, date = date, yearMonth = toYearMonth(date.year, date.month.number))
        }
    }

    private fun computeThreeMonthFallback(
        eligible: List<DatedTransaction>,
        anchorYm: Int,
        earliestYm: Int,
    ): ThreeMonthMoMFallback {
        val byYmExpense = eligible
            .filter { it.tx.type == TransactionType.EXPENSE }
            .groupBy { it.yearMonth }
            .mapValues { (_, items) -> items.sumOf { it.tx.amount.amountInCents } }

        val currentExpense = byYmExpense[anchorYm] ?: 0L
        val priorYms = (1..3).map { anchorYm - it }.filter { it >= earliestYm }
        val prevAvg = if (priorYms.isEmpty()) {
            0L
        } else {
            priorYms.sumOf { byYmExpense[it] ?: 0L } / priorYms.size
        }

        return ThreeMonthMoMFallback(
            currentMonthExpenseCents = currentExpense,
            previousMonthsAverageCents = prevAvg,
            deltaCents = currentExpense - prevAvg,
            changePercent = safePercent(currentExpense, prevAvg),
        )
    }

    private fun buildYoYComparison(
        eligible: List<DatedTransaction>,
        year: Int,
        month: Int,
    ): YoYPeriodComparison {
        val currentYm = toYearMonth(year, month)
        val priorYm = toYearMonth(year - 1, month)
        val currentRows = eligible.filter { it.yearMonth == currentYm }
        val priorRows = eligible.filter { it.yearMonth == priorYm }

        val curExpense = sumType(currentRows, TransactionType.EXPENSE)
        val prevExpense = sumType(priorRows, TransactionType.EXPENSE)
        val curIncome = sumType(currentRows, TransactionType.INCOME)
        val prevIncome = sumType(priorRows, TransactionType.INCOME)

        val curCats = currentRows.filter { it.tx.type == TransactionType.EXPENSE }
            .groupBy { it.tx.category }
            .mapValues { (_, list) -> list.sumOf { it.tx.amount.amountInCents } }
        val prevCats = priorRows.filter { it.tx.type == TransactionType.EXPENSE }
            .groupBy { it.tx.category }
            .mapValues { (_, list) -> list.sumOf { it.tx.amount.amountInCents } }

        val allCats = (curCats.keys + prevCats.keys).distinct()
        val deltas = allCats.map { cat ->
            val cur = curCats[cat] ?: 0L
            val prev = prevCats[cat] ?: 0L
            CategoryYoYDelta(
                category = cat,
                currentCents = cur,
                previousCents = prev,
                deltaCents = cur - prev,
                changePercent = safePercent(cur, prev),
            )
        }

        return YoYPeriodComparison(
            currentExpenseCents = curExpense,
            previousExpenseCents = prevExpense,
            expenseDeltaCents = curExpense - prevExpense,
            expenseChangePercent = safePercent(curExpense, prevExpense),
            currentIncomeCents = curIncome,
            previousIncomeCents = prevIncome,
            incomeDeltaCents = curIncome - prevIncome,
            incomeChangePercent = safePercent(curIncome, prevIncome),
            topRisingCategories = deltas.filter { it.deltaCents > 0L }.sortedByDescending { it.deltaCents }.take(3),
            topFallingCategories = deltas.filter { it.deltaCents < 0L }.sortedBy { it.deltaCents }.take(3),
        )
    }

    private fun buildRolling12Months(
        eligible: List<DatedTransaction>,
        anchorYm: Int,
    ): List<RollingMonthComparisonPoint> {
        val expenseByYm = mutableMapOf<Int, Long>()
        val incomeByYm = mutableMapOf<Int, Long>()
        eligible.forEach { item ->
            val cents = item.tx.amount.amountInCents
            when (item.tx.type) {
                TransactionType.EXPENSE -> expenseByYm[item.yearMonth] = (expenseByYm[item.yearMonth] ?: 0L) + cents
                TransactionType.INCOME -> incomeByYm[item.yearMonth] = (incomeByYm[item.yearMonth] ?: 0L) + cents
            }
        }

        return (11 downTo 0).map { offset ->
            val curYm = anchorYm - offset
            val priorYm = curYm - 12
            val (y, m) = fromYearMonth(curYm)
            RollingMonthComparisonPoint(
                year = y,
                month = m,
                currentYearExpenseCents = expenseByYm[curYm] ?: 0L,
                priorYearExpenseCents = expenseByYm[priorYm] ?: 0L,
                currentYearIncomeCents = incomeByYm[curYm] ?: 0L,
                priorYearIncomeCents = incomeByYm[priorYm] ?: 0L,
            )
        }
    }

    private fun detectSeasonalInsights(
        eligible: List<DatedTransaction>,
        earliestDate: LocalDate,
        anchorDate: LocalDate,
    ): List<SeasonalPatternInsight> {
        val lookbackStart = LocalDate(anchorDate.year - 2, anchorDate.month.number, 1)
        val windowStart = maxOf(earliestDate, lookbackStart)
        if (windowStart > anchorDate) return emptyList()

        val expenseRows = eligible.filter { it.tx.type == TransactionType.EXPENSE && it.date >= windowStart }
        val dailyTotals = expenseRows.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { it.tx.amount.amountInCents } }

        var normalDays = 0
        var normalExpense = 0L
        val seasonDays = LinkedHashMap<VietnameseSeason, Int>()
        val seasonExpense = LinkedHashMap<VietnameseSeason, Long>()

        var cursor = windowStart
        while (cursor <= anchorDate) {
            val daySpend = dailyTotals[cursor] ?: 0L
            val season = VietnameseLunarCalendar.primarySeason(cursor)
            if (season == null) {
                normalDays++
                normalExpense += daySpend
            } else {
                seasonDays[season] = (seasonDays[season] ?: 0) + 1
                seasonExpense[season] = (seasonExpense[season] ?: 0L) + daySpend
            }
            cursor = cursor.plus(DatePeriod(days = 1))
        }

        if (normalDays < MIN_BASELINE_DAYS || normalExpense <= 0L) return emptyList()
        val baselineDailyAvg = normalExpense.toDouble() / normalDays.toDouble()

        val categoryBySeason = expenseRows
            .mapNotNull { item -> VietnameseLunarCalendar.primarySeason(item.date)?.let { it to item.tx } }
            .groupBy({ it.first }, { it.second })

        return VietnameseSeason.entries.mapNotNull { season ->
            val days = seasonDays[season] ?: 0
            val total = seasonExpense[season] ?: 0L
            if (days < MIN_SEASON_DAYS || total <= 0L) return@mapNotNull null

            val seasonDailyAvg = total.toDouble() / days.toDouble()
            val index = (seasonDailyAvg / baselineDailyAvg).toFloat()
            val changePct = ((index - 1.0f) * 100.0f)
            if (!index.isFinite() || abs(changePct) < SIGNIFICANT_SEASON_THRESHOLD_PCT) {
                return@mapNotNull null
            }

            val topCats = categoryBySeason[season].orEmpty()
                .groupBy { it.category }
                .entries
                .sortedByDescending { (_, txs) -> txs.sumOf { it.amount.amountInCents } }
                .take(2)
                .map { it.key }

            SeasonalPatternInsight(
                season = season,
                seasonalIndex = index,
                changePercent = changePct,
                seasonalMonthlyEquivalentCents = (seasonDailyAvg * DAYS_PER_MONTH).roundToLong(),
                baselineMonthlyAverageCents = (baselineDailyAvg * DAYS_PER_MONTH).roundToLong(),
                topCategories = topCats,
            )
        }.sortedByDescending { abs(it.changePercent) }
    }

    private fun sumType(rows: List<DatedTransaction>, type: TransactionType): Long =
        rows.filter { it.tx.type == type }.sumOf { it.tx.amount.amountInCents }

    private fun safePercent(current: Long, previous: Long): Float? {
        if (previous <= 0L) return null
        val pct = ((current - previous).toDouble() / previous.toDouble() * 100.0).toFloat()
        return pct.takeIf { it.isFinite() }
    }

    private fun toYearMonth(year: Int, month: Int): Int = year * 12 + (month - 1)

    private fun fromYearMonth(ym: Int): Pair<Int, Int> = (ym / 12) to (ym % 12 + 1)

    private companion object {
        const val MIN_BASELINE_DAYS = 30
        const val MIN_SEASON_DAYS = 15
        const val SIGNIFICANT_SEASON_THRESHOLD_PCT = 15f
        const val DAYS_PER_MONTH = 30.0
    }
}
