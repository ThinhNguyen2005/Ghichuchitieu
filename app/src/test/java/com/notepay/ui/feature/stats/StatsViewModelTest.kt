package com.notepay.ui.feature.stats

import com.google.common.truth.Truth.assertThat
import com.notepay.ui.feature.addtransaction.MainDispatcherRule
import com.notepay.domain.TestData
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.usecase.GetMonthlySummaryUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

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

    private fun createViewModel(repo: TransactionRepository): StatsViewModel {
        val getMonthlySummary = GetMonthlySummaryUseCase(repo)
        return StatsViewModel(getMonthlySummary)
    }
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
}
