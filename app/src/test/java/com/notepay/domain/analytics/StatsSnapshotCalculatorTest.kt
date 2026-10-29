package com.notepay.domain.analytics

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.TestData
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.time.Instant

class StatsSnapshotCalculatorTest {

    private val today = LocalDate(2026, 3, 15)
    private val marchPeriod = periodFor(LocalDate(2026, 3, 1), StatsRange.MONTH)

    private val wallet1 = TestData.wallet(id = 1L, budgetLimit = Money(1_000_000L))
    private val wallet2 = TestData.wallet(id = 2L, name = "Ngân hàng", budgetLimit = Money(500_000L))

    private val transactions = listOf(
        expense("2026-02-10T03:00:00Z", 70_000L, walletId = 1L),
        expense("2026-03-10T03:00:00Z", 100_000L, walletId = 1L),
        expense("2026-03-15T02:00:00Z", 30_000L, walletId = 1L),
        expense("2026-03-12T03:00:00Z", 50_000L, walletId = 2L),
    )

    private fun expense(instant: String, cents: Long, walletId: Long) = TestData.transaction(
        amount = Money(cents),
        type = TransactionType.EXPENSE,
        category = Category.FOOD,
        walletId = walletId,
        occurredAt = Instant.parse(instant),
    )

    private fun snapshot(
        selectedWalletId: Long? = null,
        trendWindowMonths: Int = 3,
        period: StatsPeriod = marchPeriod,
        rows: List<Transaction> = transactions,
    ) = StatsSnapshotCalculator.snapshot(
        StatsSnapshotInput(
            transactions = rows,
            wallets = listOf(wallet1, wallet2),
            period = period,
            selectedWalletId = selectedWalletId,
            trendWindowMonths = trendWindowMonths,
            today = today,
            zone = TimeZone.UTC,
        ),
    )

    @Test
    fun `snapshot keeps only transactions of the selected wallet and period`() {
        val result = snapshot(selectedWalletId = 1L)

        assertThat(result.filteredTransactions).hasSize(2)
        assertThat(result.currentMonthExpenseInCents).isEqualTo(130_000L)
        assertThat(result.spentTodayInCents).isEqualTo(30_000L)
    }

    @Test
    fun `snapshot sums every wallet when no wallet is selected`() {
        val result = snapshot(selectedWalletId = null)

        assertThat(result.filteredTransactions).hasSize(3)
        assertThat(result.currentMonthExpenseInCents).isEqualTo(180_000L)
        assertThat(result.spentTodayInCents).isEqualTo(30_000L)
    }

    @Test
    fun `snapshot compares with the previous period only when history covers it`() {
        val withHistory = snapshot(selectedWalletId = 1L)
        assertThat(withHistory.previousExpenseInCents).isEqualTo(70_000L)
        assertThat(withHistory.previousIncomeInCents).isEqualTo(0L)

        val onlyMarch = listOf(expense("2026-03-02T03:00:00Z", 10_000L, walletId = 1L))
        val withoutHistory = snapshot(selectedWalletId = 1L, rows = onlyMarch)
        assertThat(withoutHistory.previousExpenseInCents).isNull()
        assertThat(withoutHistory.previousIncomeInCents).isNull()
    }

    @Test
    fun `snapshot uses the wallet budget in single-wallet mode and the sum otherwise`() {
        val singleWallet = snapshot(selectedWalletId = 1L)
        assertThat(singleWallet.budgetLimitInCents).isEqualTo(1_000_000L)
        assertThat(singleWallet.budgetUsedRatio).isWithin(1e-4f).of(0.13f)

        val allWallets = snapshot(selectedWalletId = null)
        assertThat(allWallets.budgetLimitInCents).isEqualTo(1_500_000L)
        assertThat(allWallets.budgetUsedRatio).isWithin(1e-4f).of(0.12f)
    }

    @Test
    fun `snapshot builds one trend point per requested month`() {
        val result = snapshot(selectedWalletId = 1L, trendWindowMonths = 3)

        assertThat(result.trendMonths.map { it.year to it.month })
            .containsExactly(2026 to 1, 2026 to 2, 2026 to 3).inOrder()
        assertThat(result.trendMonths.map { it.expenseInCents })
            .containsExactly(0L, 70_000L, 130_000L).inOrder()
    }

    @Test
    fun `snapshot marks the current month only when the period contains today`() {
        assertThat(snapshot(period = marchPeriod).isViewingCurrentMonth).isTrue()

        val february = periodFor(LocalDate(2026, 2, 1), StatsRange.MONTH)
        assertThat(snapshot(period = february).isViewingCurrentMonth).isFalse()
    }

    @Test
    fun `monthLength counts leap and regular february correctly`() {
        assertThat(StatsSnapshotCalculator.monthLength(2026, 2)).isEqualTo(28)
        assertThat(StatsSnapshotCalculator.monthLength(2028, 2)).isEqualTo(29)
        assertThat(StatsSnapshotCalculator.monthLength(2026, 3)).isEqualTo(31)
    }

    @Test
    fun `monthRangeMillis covers exactly the calendar month`() {
        val (start, endInclusive) = StatsSnapshotCalculator.monthRangeMillis(2026, 2, TimeZone.UTC)

        assertThat(endInclusive - start + 1).isEqualTo(28L * 24 * 60 * 60 * 1000)
    }
}
