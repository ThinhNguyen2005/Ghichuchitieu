package com.notepay.ui.feature.stats

import com.google.common.truth.Truth.assertThat
import com.notepay.ui.feature.addtransaction.MainDispatcherRule
import com.notepay.domain.TestData
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import android.content.Context
import com.notepay.domain.repository.SubscriptionRepository
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val tIncome = TestData.transaction(
        id = 1L,
        amount = Money(10_000_000_00), // 10,000,000 đ
        type = TransactionType.INCOME,
        category = Category.SALARY
    )
    private val tFood = TestData.transaction(
        id = 2L,
        amount = Money(1_000_000_00), // 1,000,000 đ
        type = TransactionType.EXPENSE,
        category = Category.FOOD
    )
    private val tShop = TestData.transaction(
        id = 3L,
        amount = Money(3_000_000_00), // 3,000,000 đ
        type = TransactionType.EXPENSE,
        category = Category.SHOPPING
    )

    @Test
    fun `init emits correct summary and breakdown sorted by amount descending`() = runTest {
        val repo = FakeTransactionRepository(listOf(tIncome, tFood, tShop))
        val viewModel = createViewModel(repo)

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.state.collect {}
        }

        val state = viewModel.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.totalIncome).isEqualTo(Money(10_000_000_00))
        assertThat(state.totalExpense).isEqualTo(Money(4_000_000_00))
        assertThat(state.balance).isEqualTo(Money(6_000_000_00))

        // Chi tiêu: 4,000,000. Shopping = 3M (75%), Food = 1M (25%)
        // Được sắp xếp giảm dần (Shopping đứng trước Food)
        assertThat(state.breakdown).hasSize(2)
        assertThat(state.breakdown[0].category).isEqualTo(Category.SHOPPING)
        assertThat(state.breakdown[0].percentage).isEqualTo(0.75f)
        assertThat(state.breakdown[1].category).isEqualTo(Category.FOOD)
        assertThat(state.breakdown[1].percentage).isEqualTo(0.25f)

        collectJob.cancel()
    }

    @Test
    fun `navigation changes month and updates state`() = runTest {
        val repo = FakeTransactionRepository(emptyList())
        val viewModel = createViewModel(repo)

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.state.collect {}
        }

        val currentMonth = viewModel.state.value.month
        val currentYear = viewModel.state.value.year

        // Lùi 1 tháng
        viewModel.onPreviousMonth()
        val expectedMonth = if (currentMonth == 1) 12 else currentMonth - 1
        val expectedYear = if (currentMonth == 1) currentYear - 1 else currentYear

        assertThat(viewModel.state.value.month).isEqualTo(expectedMonth)
        assertThat(viewModel.state.value.year).isEqualTo(expectedYear)

        // Tiến 1 tháng (trở lại tháng cũ)
        viewModel.onNextMonth()
        assertThat(viewModel.state.value.month).isEqualTo(currentMonth)
        assertThat(viewModel.state.value.year).isEqualTo(currentYear)

        collectJob.cancel()
    }

    @Test
    fun `wallet filtering and budget progress limit calculation`() = runTest {
        val wallet1 = TestData.wallet(id = 1L, name = "Ví MoMo").copy(budgetLimit = Money(1_000_000_00))
        val wallet2 = TestData.wallet(id = 2L, name = "Ví Tiền mặt").copy(budgetLimit = Money(2_000_000_00))
        
        val tx1 = TestData.transaction(id = 1L, walletId = 1L, amount = Money(200_000_00), type = TransactionType.EXPENSE, category = Category.FOOD)
        val tx2 = TestData.transaction(id = 2L, walletId = 2L, amount = Money(500_000_00), type = TransactionType.EXPENSE, category = Category.SHOPPING)

        val txRepo = FakeTransactionRepository(listOf(tx1, tx2))
        val walletRepo = FakeWalletRepository(listOf(wallet1, wallet2))
        
        val viewModel = createViewModel(txRepo, walletRepo)
        
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.state.collect {}
        }
        
        var state = viewModel.state.value
        assertThat(state.budgetLimit).isEqualTo(Money(3_000_000_00))
        assertThat(state.budgetSpent).isEqualTo(Money(700_000_00))
        assertThat(state.budgetPercentage).isWithin(0.01f).of(700_000_00f / 3_000_000_00f)
        
        viewModel.selectWallet(1L)
        state = viewModel.state.value
        assertThat(state.selectedWallet?.name).isEqualTo("Ví MoMo")
        assertThat(state.budgetLimit).isEqualTo(Money(1_000_000_00))
        assertThat(state.budgetSpent).isEqualTo(Money(200_000_00))
        
        assertThat(state.spendingForecast).isNotNull()
        val forecast = state.spendingForecast!!
        assertThat(forecast.projectedSpend.amountInCents).isGreaterThan(0L)
        
        collectJob.cancel()
    }

    private fun createViewModel(
        repo: TransactionRepository,
        walletRepo: WalletRepository = FakeWalletRepository()
    ): StatsViewModel {
        val fakeContext = mockk<Context>(relaxed = true)
        val fakeSubRepo = FakeSubscriptionRepository()
        return StatsViewModel(repo, walletRepo, fakeSubRepo, fakeContext)
    }
}

private class FakeSubscriptionRepository(
    private val subscriptions: List<com.notepay.domain.model.Subscription> = emptyList()
) : SubscriptionRepository {
    override fun observeAll(): Flow<List<com.notepay.domain.model.Subscription>> = flowOf(subscriptions)
    override fun observeUpcoming(beforeDate: Instant): Flow<List<com.notepay.domain.model.Subscription>> = flowOf(subscriptions)
    override suspend fun upsert(subscription: com.notepay.domain.model.Subscription): Long = 0L
    override suspend fun getById(id: Long): com.notepay.domain.model.Subscription? = subscriptions.find { it.id == id }
    override suspend fun delete(id: Long) = Unit
    override suspend fun updateNextDueDate(id: Long, newDueDate: Instant) = Unit
}

private class FakeWalletRepository(
    private val wallets: List<Wallet> = emptyList(),
) : WalletRepository {
    override fun observeAll(): Flow<List<Wallet>> = flowOf(wallets)
    override fun observeActive(): Flow<Wallet?> = flowOf(wallets.firstOrNull { it.isActive })
    override suspend fun getById(id: Long): Wallet? = wallets.find { it.id == id }
    override suspend fun upsert(wallet: Wallet): Long = 0L
    override suspend fun delete(id: Long) = Unit
    override suspend fun setActive(id: Long) = Unit
}

private class FakeTransactionRepository(
    private val transactions: List<Transaction>,
) : TransactionRepository {
    override fun observeAll(): Flow<List<Transaction>> = flowOf(transactions)
    override fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>> = flowOf(transactions)
    override fun observeByWallet(walletId: Long): Flow<List<Transaction>> = flowOf(transactions)
    override suspend fun getById(id: Long): Transaction? = null
    override suspend fun upsert(transaction: Transaction): Long = 0
    override suspend fun delete(id: Long) = Unit
    override suspend fun findRecentSimilar(
        noteKeyword: String,
        fromMillis: Long,
        toMillis: Long
    ): List<Transaction> {
        return transactions.filter { tx ->
            tx.note.contains(noteKeyword, ignoreCase = true) &&
                    tx.occurredAt.toEpochMilliseconds() in fromMillis..toMillis
        }
    }
}
