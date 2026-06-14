package com.notepay.ui.feature.list

import com.google.common.truth.Truth.assertThat
import com.notepay.ui.feature.addtransaction.MainDispatcherRule
import com.notepay.domain.TestData
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.DeleteTransactionUseCase
import com.notepay.domain.usecase.GetTransactionsUseCase
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
class TransactionListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val t1 = TestData.transaction(id = 1L, note = "Cơm trưa", category = Category.FOOD)
    private val t2 = TestData.transaction(id = 2L, note = "Taxi đi làm", category = Category.TRANSPORT)
    private val t3 = TestData.transaction(id = 3L, note = "Mua sách học", category = Category.EDUCATION)

    @Test
    fun `init emits correct list from repository`() = runTest {
        val viewModel = createViewModel(transactions = listOf(t1, t2))
        
        // Bắt đầu collect state flow ở background để kích hoạt combine flow
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.state.collect {}
        }

        assertThat(viewModel.state.value.transactions).containsExactly(t1, t2)
        assertThat(viewModel.state.value.isLoading).isFalse()

        collectJob.cancel()
    }

    @Test
    fun `search query filters by note or category display name`() = runTest {
        val viewModel = createViewModel(transactions = listOf(t1, t2, t3))
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.state.collect {}
        }

        // Gõ tìm kiếm chữ thường "cơm"
        viewModel.onQueryChanged("cơm")
        assertThat(viewModel.state.value.transactions).containsExactly(t1)

        // Tìm theo category display name (e.g. "Di chuyển")
        viewModel.onQueryChanged("di chuyển")
        assertThat(viewModel.state.value.transactions).containsExactly(t2)

        collectJob.cancel()
    }

    @Test
    fun `category filter restricts transactions list`() = runTest {
        val viewModel = createViewModel(transactions = listOf(t1, t2, t3))
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.state.collect {}
        }

        viewModel.onCategorySelected(Category.FOOD)
        assertThat(viewModel.state.value.transactions).containsExactly(t1)

        // Bỏ lọc
        viewModel.onCategorySelected(null)
        assertThat(viewModel.state.value.transactions).containsExactly(t1, t2, t3)

        collectJob.cancel()
    }

    @Test
    fun `delete transaction updates state and calling undo restores it`() = runTest {
        val repo = FakeTransactionRepository(listOf(t1, t2))
        val viewModel = createViewModel(transactionRepository = repo)
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.state.collect {}
        }

        // Xóa t1
        viewModel.delete(t1)

        assertThat(viewModel.state.value.pendingUndoTransaction).isEqualTo(t1)
        assertThat(repo.deletedIds).containsExactly(1L)

        // Click hoàn tác (Undo)
        viewModel.undoDelete()
        assertThat(viewModel.state.value.pendingUndoTransaction).isNull()
        assertThat(repo.savedTransactions).contains(t1.copy(id = 0L))

        collectJob.cancel()
    }

    private fun createViewModel(
        transactions: List<Transaction> = emptyList(),
        transactionRepository: FakeTransactionRepository = FakeTransactionRepository(transactions),
    ): TransactionListViewModel {
        val dispatcher = mainDispatcherRule.testDispatcher
        val getUseCase = GetTransactionsUseCase(transactionRepository, dispatcher)
        val deleteUseCase = DeleteTransactionUseCase(transactionRepository, dispatcher)
        // FakeWalletRepository cho AddTransactionUseCase
        val walletRepo = FakeWalletRepository()
        val addUseCase = AddTransactionUseCase(transactionRepository, walletRepo, dispatcher)

        return TransactionListViewModel(getUseCase, deleteUseCase, addUseCase, dispatcher)
    }
}

private class FakeTransactionRepository(
    initial: List<Transaction> = emptyList(),
) : TransactionRepository {
    val savedTransactions = initial.toMutableList()
    val deletedIds = mutableListOf<Long>()

    override fun observeAll(): Flow<List<Transaction>> = MutableStateFlow(savedTransactions)
    override fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>> = flowOf(savedTransactions)
    override fun observeByWallet(walletId: Long): Flow<List<Transaction>> = flowOf(savedTransactions)
    override suspend fun getById(id: Long): Transaction? = savedTransactions.firstOrNull { it.id == id }
    override suspend fun upsert(transaction: Transaction): Long {
        savedTransactions.add(transaction)
        return transaction.id
    }
    override suspend fun delete(id: Long) {
        deletedIds.add(id)
        savedTransactions.removeAll { it.id == id }
    }
}

private class FakeWalletRepository : com.notepay.domain.repository.WalletRepository {
    override fun observeAll() = flowOf(emptyList<com.notepay.domain.model.Wallet>())
    override fun observeActive() = flowOf(TestData.wallet())
    override suspend fun getById(id: Long) = TestData.wallet(id = id)
    override suspend fun upsert(wallet: com.notepay.domain.model.Wallet) = wallet.id
    override suspend fun delete(id: Long) = Unit
    override suspend fun setActive(id: Long) = Unit
}
