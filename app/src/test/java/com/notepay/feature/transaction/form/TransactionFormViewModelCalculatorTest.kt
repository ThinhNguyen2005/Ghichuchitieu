package com.notepay.feature.transaction.form

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.R
import com.notepay.ai.LocalTransactionImageScanner
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.navigation.Route
import com.notepay.domain.TestData
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import com.notepay.domain.repository.CategoryRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.SuggestCategoryUseCase
import com.notepay.feature.transaction.component.CalcKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionFormViewModelCalculatorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        transactionId: Long? = null,
        existingTransaction: Transaction? = null,
        addTransactionUseCase: AddTransactionUseCase = mockk(relaxed = true),
        transactionRepository: TransactionRepository = mockk(relaxed = true),
    ): TransactionFormViewModel {
        val wallet = TestData.wallet(id = 1L)
        val walletRepo = mockk<WalletRepository> {
            every { observeAll() } returns flowOf(listOf(wallet))
            every { observeActive() } returns flowOf(wallet)
        }
        val categoryRepo = mockk<CategoryRepository> {
            every { observeCategories() } returns flowOf(Category.getAll())
        }
        val suggestCategoryUseCase = mockk<SuggestCategoryUseCase>(relaxed = true) {
            every { suggestDetailed(any(), any()) } returns null
        }
        val imageScanner = mockk<LocalTransactionImageScanner>(relaxed = true)
        val context = mockk<Context>(relaxed = true) {
            every { getString(R.string.error_division_by_zero) } returns "Cannot divide by zero"
            every { getString(R.string.transaction_validation_amount) } returns "Enter amount > 0"
            every { getString(R.string.error_amount_positive) } returns "Amount must be > 0"
            every { getString(R.string.feedback_transaction_saved) } returns "Saved"
            every { getString(R.string.feedback_transaction_updated) } returns "Updated"
        }
        if (existingTransaction != null && transactionId != null) {
            coEvery { transactionRepository.getById(transactionId) } returns existingTransaction
        }
        val savedStateHandle = SavedStateHandle(
            buildMap {
                if (transactionId != null) put(Route.EditTransaction.ARG_ID, transactionId)
            },
        )
        return TransactionFormViewModel(
            savedStateHandle = savedStateHandle,
            addTransactionUseCase = addTransactionUseCase,
            transactionRepository = transactionRepository,
            walletRepository = walletRepo,
            categoryRepository = categoryRepo,
            suggestCategoryUseCase = suggestCategoryUseCase,
            imageScanner = imageScanner,
            context = context,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private fun enterNumber(vm: TransactionFormViewModel, value: String) {
        value.forEach { ch ->
            vm.onEvent(TransactionFormUiEvent.CalcKeyPressed(CalcKey.Digit(ch.digitToInt())))
        }
    }

    @Test
    fun `save in add mode evaluates pending expression 100 + 50 and saves 150`() = runTest {
        val addUseCase = mockk<AddTransactionUseCase>()
        val txSlot = slot<Transaction>()
        coEvery { addUseCase(capture(txSlot)) } returns Result.success(10L)

        val vm = createViewModel(addTransactionUseCase = addUseCase)
        enterNumber(vm, "100")
        vm.onEvent(TransactionFormUiEvent.CalcKeyPressed(CalcKey.Operator('+')))
        enterNumber(vm, "50")

        assertThat(vm.state.value.amount).isEqualTo(Money.fromMajorUnit(150L))
        assertThat(vm.state.value.canSave).isTrue()

        vm.onEvent(TransactionFormUiEvent.Save)

        coVerify(exactly = 1) { addUseCase(any()) }
        assertThat(txSlot.captured.amount).isEqualTo(Money.fromMajorUnit(150L))
    }

    @Test
    fun `save in add mode cancels trailing operator 100 + and saves 100`() = runTest {
        val addUseCase = mockk<AddTransactionUseCase>()
        val txSlot = slot<Transaction>()
        coEvery { addUseCase(capture(txSlot)) } returns Result.success(11L)

        val vm = createViewModel(addTransactionUseCase = addUseCase)
        enterNumber(vm, "100")
        vm.onEvent(TransactionFormUiEvent.CalcKeyPressed(CalcKey.Operator('+')))

        assertThat(vm.state.value.activePendingOperator).isEqualTo('+')
        assertThat(vm.state.value.canSave).isTrue()

        vm.onEvent(TransactionFormUiEvent.Save)

        coVerify(exactly = 1) { addUseCase(any()) }
        assertThat(txSlot.captured.amount).isEqualTo(Money.fromMajorUnit(100L))
    }

    @Test
    fun `non-positive evaluated expressions 100 - 100 and 50 - 100 block save and emit error`() = runTest {
        val addUseCase = mockk<AddTransactionUseCase>(relaxed = true)
        val vm = createViewModel(addTransactionUseCase = addUseCase)

        vm.feedback.test {
            enterNumber(vm, "100")
            vm.onEvent(TransactionFormUiEvent.CalcKeyPressed(CalcKey.Operator('-')))
            enterNumber(vm, "100")

            assertThat(vm.state.value.canSave).isFalse()
            assertThat(vm.state.value.amount).isNull()

            vm.onEvent(TransactionFormUiEvent.Save)
            val err1 = awaitItem()
            assertThat(err1.type).isEqualTo(FeedbackType.Error)
            coVerify(exactly = 0) { addUseCase(any()) }

            vm.onEvent(TransactionFormUiEvent.BackspaceLong)
            enterNumber(vm, "50")
            vm.onEvent(TransactionFormUiEvent.CalcKeyPressed(CalcKey.Operator('-')))
            enterNumber(vm, "100")
            assertThat(vm.state.value.canSave).isFalse()

            vm.onEvent(TransactionFormUiEvent.Save)
            val err2 = awaitItem()
            assertThat(err2.type).isEqualTo(FeedbackType.Error)
            coVerify(exactly = 0) { addUseCase(any()) }
        }
    }

    @Test
    fun `division by zero blocks save and emits division by zero error`() = runTest {
        val addUseCase = mockk<AddTransactionUseCase>(relaxed = true)
        val vm = createViewModel(addTransactionUseCase = addUseCase)

        vm.feedback.test {
            enterNumber(vm, "100")
            vm.onEvent(TransactionFormUiEvent.CalcKeyPressed(CalcKey.Operator('/')))
            enterNumber(vm, "0")

            assertThat(vm.state.value.canSave).isFalse()
            vm.onEvent(TransactionFormUiEvent.Save)

            val feedback = awaitItem()
            assertThat(feedback.type).isEqualTo(FeedbackType.Error)
            assertThat(feedback.message).isEqualTo("Cannot divide by zero")
            coVerify(exactly = 0) { addUseCase(any()) }
        }
    }

    @Test
    fun `rapid double-tap save invokes addTransactionUseCase only once`() = runTest {
        val gate = CompletableDeferred<Result<Long>>()
        val addUseCase = mockk<AddTransactionUseCase>()
        coEvery { addUseCase(any()) } coAnswers { gate.await() }

        val vm = createViewModel(addTransactionUseCase = addUseCase)
        enterNumber(vm, "25000")

        vm.onEvent(TransactionFormUiEvent.Save)
        assertThat(vm.state.value.isSaving).isTrue()
        vm.onEvent(TransactionFormUiEvent.Save)

        gate.complete(Result.success(1L))
        coVerify(exactly = 1) { addUseCase(any()) }
    }

    @Test
    fun `edit mode evaluates expression 50 + 20 on save and locks auto-capture amount`() = runTest {
        val editableTx = TestData.transaction(
            id = 42L,
            amount = Money.fromMajorUnit(50L),
            type = TransactionType.EXPENSE,
            category = Category.FOOD,
            walletId = 1L,
        )
        val txRepo = mockk<TransactionRepository>(relaxed = true)
        val updatedSlot = slot<Transaction>()
        coEvery { txRepo.upsert(capture(updatedSlot)) } returns 42L

        val vm = createViewModel(
            transactionId = 42L,
            existingTransaction = editableTx,
            transactionRepository = txRepo,
        )
        vm.onEvent(TransactionFormUiEvent.CalcKeyPressed(CalcKey.Operator('+')))
        enterNumber(vm, "20")
        vm.onEvent(TransactionFormUiEvent.Save)

        coVerify(exactly = 1) { txRepo.upsert(any()) }
        assertThat(updatedSlot.captured.amount).isEqualTo(Money.fromMajorUnit(70L))

        val autoTx = editableTx.copy(id = 43L, isAutoCapture = true, amount = Money.fromMajorUnit(99_000L))
        val autoRepo = mockk<TransactionRepository>(relaxed = true)
        val autoSlot = slot<Transaction>()
        coEvery { autoRepo.upsert(capture(autoSlot)) } returns 43L

        val autoVm = createViewModel(
            transactionId = 43L,
            existingTransaction = autoTx,
            transactionRepository = autoRepo,
        )
        autoVm.onEvent(TransactionFormUiEvent.CalcKeyPressed(CalcKey.Digit(1)))
        autoVm.onEvent(TransactionFormUiEvent.Save)

        coVerify(exactly = 1) { autoRepo.upsert(any()) }
        assertThat(autoSlot.captured.amount).isEqualTo(Money.fromMajorUnit(99_000L))
    }
}
