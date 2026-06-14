package com.notepay.ui.feature.addtransaction

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.CategoryRepository
import com.notepay.domain.repository.TransactionRepository
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
class EditTransactionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val sampleTx = Transaction(
        id = 15L,
        amount = Money(50_000_00L),
        type = TransactionType.EXPENSE,
        category = Category.DEFAULT_EXPENSE,
        note = "Mua sach",
        occurredAt = Clock.System.now(),
        walletId = 1L,
        isAutoCapture = false
    )

    private val autoCapturedTx = Transaction(
        id = 16L,
        amount = Money(100_000_00L),
        type = TransactionType.EXPENSE,
        category = Category.DEFAULT_EXPENSE,
        note = "BANK TRANSFER RECV",
        occurredAt = Clock.System.now(),
        walletId = 1L,
        isAutoCapture = true
    )

    @Test
    fun `init loads transaction successfully`() = runTest {
        val transactionRepository = EditFakeTransactionRepository(listOf(sampleTx))
        val viewModel = createViewModel(15L, transactionRepository)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.isAutoCapture).isFalse()
        assertThat(state.amountInput).isEqualTo("50000")
        assertThat(state.note).isEqualTo("Mua sach")
        assertThat(state.category).isEqualTo(Category.DEFAULT_EXPENSE)
    }

    @Test
    fun `regular transaction edits allowed and saved`() = runTest {
        val transactionRepository = EditFakeTransactionRepository(listOf(sampleTx))
        val viewModel = createViewModel(15L, transactionRepository)
        testScheduler.advanceUntilIdle()

        viewModel.onAmountChanged("60000")
        viewModel.onNoteChanged("Mua sach moi")
        val customCategory = Category.getAll().first { it.id == "FOOD" }
        viewModel.onCategoryChanged(customCategory)
        viewModel.save()
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.state.value.savedSuccessfully).isTrue()
        val saved = transactionRepository.savedTransactions.find { it.id == 15L }
        assertThat(saved).isNotNull()
        assertThat(saved!!.amount).isEqualTo(Money(60_000_00L))
        assertThat(saved.note).isEqualTo("Mua sach moi")
        assertThat(saved.category).isEqualTo(customCategory)
    }

    @Test
    fun `auto captured transaction amount and note edits blocked but category editable`() = runTest {
        val transactionRepository = EditFakeTransactionRepository(listOf(autoCapturedTx))
        val viewModel = createViewModel(16L, transactionRepository)
        testScheduler.advanceUntilIdle()

        // Try changing amount and note (should be ignored)
        viewModel.onAmountChanged("150000")
        viewModel.onNoteChanged("TRICK NOTE")
        
        // Change category (should be allowed)
        val customCategory = Category.getAll().first { it.id == "FOOD" }
        viewModel.onCategoryChanged(customCategory)
        
        viewModel.save()
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.state.value.savedSuccessfully).isTrue()
        val saved = transactionRepository.savedTransactions.find { it.id == 16L }
        assertThat(saved).isNotNull()
        // Amount and note must remain original
        assertThat(saved!!.amount).isEqualTo(Money(100_000_00L))
        assertThat(saved.note).isEqualTo("BANK TRANSFER RECV")
        assertThat(saved.category).isEqualTo(customCategory)
    }

    @Test
    fun `init error when transaction not found`() = runTest {
        val transactionRepository = EditFakeTransactionRepository(emptyList())
        val viewModel = createViewModel(99L, transactionRepository)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).contains("Không tìm thấy giao dịch")
    }

    private fun createViewModel(
        txId: Long,
        transactionRepository: TransactionRepository
    ): EditTransactionViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("id" to txId))
        val categoryRepository = EditFakeCategoryRepository()
        return EditTransactionViewModel(
            savedStateHandle = savedStateHandle,
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository
        )
    }
}

private class EditFakeCategoryRepository : CategoryRepository {
    override fun observeCategories(): Flow<List<Category>> = flowOf(Category.getAll())
    override suspend fun getCategories(): List<Category> = Category.getAll()
    override suspend fun addCustomCategory(category: Category) = Unit
}

private class EditFakeTransactionRepository(
    initialTransactions: List<Transaction>
) : TransactionRepository {
    val savedTransactions = initialTransactions.toMutableList()

    override fun observeAll(): Flow<List<Transaction>> = flowOf(savedTransactions)
    override fun observeByWallet(walletId: Long): Flow<List<Transaction>> = flowOf(savedTransactions)
    override fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>> = flowOf(savedTransactions)
    override suspend fun getById(id: Long): Transaction? = savedTransactions.find { it.id == id }
    override suspend fun upsert(transaction: Transaction): Long {
        val idx = savedTransactions.indexOfFirst { it.id == transaction.id }
        if (idx != -1) {
            savedTransactions[idx] = transaction
        } else {
            savedTransactions.add(transaction)
        }
        return transaction.id
    }
    override suspend fun delete(id: Long) = Unit
}
