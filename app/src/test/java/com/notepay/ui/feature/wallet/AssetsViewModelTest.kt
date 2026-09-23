package com.notepay.ui.feature.wallet

import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.domain.TestData
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class AssetsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val walletCash = TestData.wallet(
        id = 1L,
        name = "Tiền mặt",
        initialBalance = Money(5_000_000_00),
    ).copy(
        isActive = true,
        colorKey = "emerald"
    )

    private val walletBank = TestData.wallet(
        id = 2L,
        name = "Ngân hàng",
        initialBalance = Money(15_000_000_00),
    ).copy(
        isActive = false,
        colorKey = "indigo"
    )

    private val txSalary = TestData.transaction(
        id = 101L,
        walletId = 2L,
        amount = Money(10_000_000_00),
        type = TransactionType.INCOME,
    ).copy(occurredAt = Clock.System.now())

    private val txFood = TestData.transaction(
        id = 102L,
        walletId = 1L,
        amount = Money(1_000_000_00),
        type = TransactionType.EXPENSE,
    ).copy(occurredAt = Clock.System.now())

    @Test
    fun `loadAssets calculates total net worth and asset allocation correctly`() = runTest {
        val walletRepo = FakeWalletRepository(listOf(walletCash, walletBank))
        val txRepo = FakeTransactionRepository(listOf(txSalary, txFood))

        val viewModel = AssetsViewModel(walletRepo, txRepo)
        val state = viewModel.state.value

        assertThat(state.isLoading).isFalse()
        // Cash: 5M - 1M = 4M. Bank: 15M + 10M = 25M. Total Net Worth = 29M
        assertThat(state.totalNetWorth).isEqualTo(Money(29_000_000_00))
        assertThat(state.wallets).hasSize(2)

        // Phân bổ tài sản:
        assertThat(state.allocationItems).hasSize(2)
        val bankAlloc = state.allocationItems.find { it.walletId == 2L }
        val cashAlloc = state.allocationItems.find { it.walletId == 1L }

        assertThat(bankAlloc).isNotNull()
        assertThat(cashAlloc).isNotNull()
        // Bank (25M / 29M ~ 86%), Cash (4M / 29M ~ 14%)
        assertThat(bankAlloc!!.percentage).isGreaterThan(cashAlloc!!.percentage)
        assertThat(bankAlloc.percentage + cashAlloc.percentage).isWithin(0.01f).of(1.0f)
    }

    @Test
    fun `onChartRangeSelected updates chart range and trend points`() = runTest {
        val walletRepo = FakeWalletRepository(listOf(walletCash))
        val txRepo = FakeTransactionRepository(listOf(txFood))

        val viewModel = AssetsViewModel(walletRepo, txRepo)
        assertThat(viewModel.state.value.selectedChartRange).isEqualTo(AssetChartRange.MONTH)
        assertThat(viewModel.state.value.trendPoints).hasSize(30)

        viewModel.onChartRangeSelected(AssetChartRange.WEEK)
        assertThat(viewModel.state.value.selectedChartRange).isEqualTo(AssetChartRange.WEEK)
        assertThat(viewModel.state.value.trendPoints).hasSize(7)
    }

    @Test
    fun `setActiveWallet sets active in repository`() = runTest {
        val walletRepo = FakeWalletRepository(listOf(walletCash, walletBank))
        val txRepo = FakeTransactionRepository(emptyList())

        val viewModel = AssetsViewModel(walletRepo, txRepo)
        viewModel.setActiveWallet(2L)

        assertThat(walletRepo.activeWalletId).isEqualTo(2L)
    }

    private class FakeWalletRepository(
        private val initialWallets: List<Wallet> = emptyList(),
    ) : WalletRepository {
        var activeWalletId: Long? = initialWallets.firstOrNull { it.isActive }?.id
        private val walletsFlow = MutableStateFlow(initialWallets)

        override fun observeAll(): Flow<List<Wallet>> = walletsFlow

        override suspend fun getById(id: Long): Wallet? = walletsFlow.value.find { it.id == id }

        override fun observeActive(): Flow<Wallet?> = flowOf(walletsFlow.value.find { it.id == activeWalletId })

        override suspend fun upsert(wallet: Wallet): Long = wallet.id

        override suspend fun delete(id: Long) {}

        override suspend fun setActive(id: Long) {
            activeWalletId = id
        }
    }

    private class FakeTransactionRepository(
        private val transactions: List<Transaction> = emptyList(),
    ) : TransactionRepository {
        private val txFlow = MutableStateFlow(transactions)

        override fun observeAll(): Flow<List<Transaction>> = txFlow

        override fun observeById(id: Long): Flow<Transaction?> = flowOf(transactions.find { it.id == id })

        override fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>> = txFlow

        override fun observeByWalletAndMonth(walletId: Long, year: Int, month: Int): Flow<List<Transaction>> =
            flowOf(transactions.filter { it.walletId == walletId })

        override fun observeByWallet(walletId: Long): Flow<List<Transaction>> =
            flowOf(transactions.filter { it.walletId == walletId })

        override suspend fun getById(id: Long): Transaction? = transactions.find { it.id == id }

        override suspend fun upsert(transaction: Transaction): Long = transaction.id

        override suspend fun delete(id: Long) {}

        override suspend fun findRecentSimilar(noteKeyword: String, fromMillis: Long, toMillis: Long): List<Transaction> = emptyList()
    }
}
