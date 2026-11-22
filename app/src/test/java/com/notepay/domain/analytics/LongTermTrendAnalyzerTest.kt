package com.notepay.domain.analytics

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.model.WalletPurpose
import com.notepay.domain.money.Money
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.time.Instant

class LongTermTrendAnalyzerTest {

    private val analyzer = LongTermTrendAnalyzer()
    private val utc = TimeZone.UTC

    private fun txAt(
        id: Long,
        date: LocalDate,
        amountMajor: Long,
        category: Category = Category.FOOD,
        type: TransactionType = TransactionType.EXPENSE,
        walletId: Long = 1L,
        isInternalTransfer: Boolean = false,
    ): Transaction {
        val instant: Instant = LocalDateTime(date, LocalTime(12, 0)).toInstant(utc)
        return Transaction(
            id = id,
            amount = Money.fromMajorUnit(amountMajor),
            type = type,
            category = category,
            note = "tx_$id",
            occurredAt = instant,
            walletId = walletId,
            isInternalTransfer = isInternalTransfer,
        )
    }

    @Test
    fun `detects PRE_TET spike and POST_TET dip across 24 months of history`() {
        val txs = mutableListOf<Transaction>()
        var nextId = 1L

        // Normal months across 2025 & 2026: ~3,000,000 VND / month on Baseline days (e.g. April, May, Sept)
        for (year in listOf(2025, 2026)) {
            for (month in listOf(4, 5, 9)) {
                txs += txAt(nextId++, LocalDate(year, month, 10), 1_500_000L, Category.FOOD)
                txs += txAt(nextId++, LocalDate(year, month, 20), 1_500_000L, Category.TRANSPORT)
            }
        }
        // Baseline month anchor (Dec 2024 start -> 24 months up to Nov 2026)
        txs += txAt(nextId++, LocalDate(2024, 12, 15), 2_000_000L, Category.FOOD)
        txs += txAt(nextId++, LocalDate(2025, 11, 15), 2_000_000L, Category.FOOD)
        txs += txAt(nextId++, LocalDate(2026, 11, 15), 3_000_000L, Category.SHOPPING)

        // Pre-Tet 2026 (Jan 20..Feb 15, Tet = Feb 17): heavy shopping & family spending (9,000,000 VND)
        txs += txAt(nextId++, LocalDate(2026, 1, 25), 5_000_000L, Category.SHOPPING)
        txs += txAt(nextId++, LocalDate(2026, 2, 5), 4_000_000L, Category.FAMILY)

        // Post-Tet 2026 (Feb 18..Mar 15): low spending (600,000 VND)
        txs += txAt(nextId++, LocalDate(2026, 2, 25), 600_000L, Category.FOOD)

        val result = analyzer.analyze(
            transactions = txs,
            wallets = listOf(Wallet.default().copy(id = 1L)),
            selectedWalletId = null,
            anchorDate = LocalDate(2026, 11, 20),
            zone = utc,
        )

        assertThat(result).isInstanceOf(LongTermTrendAnalysis.Ready::class.java)
        val ready = result as LongTermTrendAnalysis.Ready
        assertThat(ready.monthsAvailable).isEqualTo(24)
        assertThat(ready.rolling12Months).hasSize(12)
        assertThat(ready.yoy.currentExpenseCents).isEqualTo(300_000_000L)
        assertThat(ready.yoy.previousExpenseCents).isEqualTo(200_000_000L)
        assertThat(ready.yoy.expenseChangePercent).isWithin(0.1f).of(50f)

        val preTet = ready.seasonalInsights.first { it.season == VietnameseSeason.PRE_TET }
        assertThat(preTet.changePercent).isGreaterThan(50f)
        assertThat(preTet.topCategories).containsExactly(Category.SHOPPING, Category.FAMILY).inOrder()

        val postTet = ready.seasonalInsights.first { it.season == VietnameseSeason.POST_TET }
        assertThat(postTet.changePercent).isLessThan(-15f)
    }

    @Test
    fun `returns InsufficientData with 3-month MoM fallback when history under 12 months`() {
        val txs = listOf(
            txAt(1L, LocalDate(2026, 9, 10), 2_000_000L),
            txAt(2L, LocalDate(2026, 10, 10), 3_000_000L),
        )

        val result = analyzer.analyze(
            transactions = txs,
            wallets = emptyList(),
            selectedWalletId = null,
            anchorDate = LocalDate(2026, 10, 20),
            zone = utc,
        )

        assertThat(result).isInstanceOf(LongTermTrendAnalysis.InsufficientData::class.java)
        val cold = result as LongTermTrendAnalysis.InsufficientData
        assertThat(cold.monthsAvailable).isEqualTo(2)
        assertThat(cold.monthsRequired).isEqualTo(12)
        assertThat(cold.threeMonthFallback).isNotNull()
        assertThat(cold.threeMonthFallback!!.currentMonthExpenseCents).isEqualTo(300_000_000L)
        assertThat(cold.threeMonthFallback!!.previousMonthsAverageCents).isEqualTo(200_000_000L)
        assertThat(cold.threeMonthFallback!!.changePercent).isWithin(0.1f).of(50f)
    }

    @Test
    fun `handles zero prior-year spend safely without NaN or Infinity`() {
        val txs = listOf(
            txAt(1L, LocalDate(2025, 10, 5), 1_000_000L), // earliest = Oct 2025 (>=12M span to Oct 2026, no spend in Nov 2025)
            txAt(2L, LocalDate(2026, 11, 5), 2_500_000L),
        )

        val result = analyzer.analyze(
            transactions = txs,
            wallets = emptyList(),
            selectedWalletId = null,
            anchorDate = LocalDate(2026, 11, 20),
            zone = utc,
        ) as LongTermTrendAnalysis.Ready

        assertThat(result.yoy.previousExpenseCents).isEqualTo(0L)
        assertThat(result.yoy.currentExpenseCents).isEqualTo(250_000_000L)
        assertThat(result.yoy.expenseChangePercent).isNull()
    }

    @Test
    fun `excludes internal transfers and wallets with includeInTotalBalance false`() {
        val personalWallet = Wallet.default().copy(id = 1L, includeInTotalBalance = true)
        val businessWallet = Wallet.default().copy(
            id = 2L,
            name = "Shop",
            purpose = WalletPurpose.BUSINESS,
            includeInTotalBalance = false,
            isActive = false,
        )
        val txs = listOf(
            txAt(1L, LocalDate(2025, 11, 10), 1_000_000L, walletId = 1L),
            txAt(2L, LocalDate(2026, 11, 10), 1_500_000L, walletId = 1L),
            // Internal transfer must be ignored
            txAt(3L, LocalDate(2026, 11, 11), 9_000_000L, walletId = 1L, isInternalTransfer = true),
            // Excluded business wallet must be ignored when viewing All Wallets
            txAt(4L, LocalDate(2026, 11, 12), 20_000_000L, walletId = 2L),
        )

        val allWalletsResult = analyzer.analyze(
            transactions = txs,
            wallets = listOf(personalWallet, businessWallet),
            selectedWalletId = null,
            anchorDate = LocalDate(2026, 11, 20),
            zone = utc,
        ) as LongTermTrendAnalysis.Ready

        assertThat(allWalletsResult.yoy.currentExpenseCents).isEqualTo(150_000_000L)
        assertThat(allWalletsResult.yoy.previousExpenseCents).isEqualTo(100_000_000L)
    }
}
