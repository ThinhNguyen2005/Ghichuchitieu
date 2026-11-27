package com.notepay.feature.transaction.form

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.R
import com.notepay.ai.LocalImageScanResult
import com.notepay.ai.LocalTransactionImageScanner
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.navigation.Route
import com.notepay.domain.TestData
import com.notepay.domain.model.Category
import com.notepay.domain.model.SharedWalletRole
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.domain.repository.CategoryRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.CategorySuggestion
import com.notepay.domain.usecase.CategorySuggestionReason
import com.notepay.domain.usecase.SuggestCategoryUseCase
import com.notepay.feature.transaction.component.CalcKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionFormViewModelStepFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        transactionId: Long? = null,
        existingTransaction: Transaction? = null,
        wallets: List<Wallet> = listOf(TestData.wallet(id = 1L), TestData.wallet(id = 2L, name = "Ngân hàng")),
        categoriesFlow: MutableStateFlow<List<Category>> = MutableStateFlow(Category.getAll()),
        categoryRepo: CategoryRepository = mockk(relaxed = true) {
            every { observeCategories() } returns categoriesFlow
        },
        addTransactionUseCase: AddTransactionUseCase = mockk(relaxed = true),
        suggestCategoryUseCase: SuggestCategoryUseCase = mockk(relaxed = true) {
            every { suggestDetailed(any(), any()) } returns null
        },
        imageScanner: LocalTransactionImageScanner = mockk(relaxed = true),
    ): TransactionFormViewModel {
        val walletRepo = mockk<WalletRepository> {
            every { observeAll() } returns flowOf(wallets)
            every { observeActive() } returns flowOf(wallets.firstOrNull())
        }
        val transactionRepo = mockk<TransactionRepository>(relaxed = true)
        if (existingTransaction != null && transactionId != null) {
            coEvery { transactionRepo.getById(transactionId) } returns existingTransaction
        }
        val context = mockk<Context>(relaxed = true) {
            every { getString(R.string.transaction_validation_category) } returns "Choose category first"
            every { getString(R.string.transaction_validation_amount) } returns "Enter positive amount"
            every { getString(R.string.transaction_validation_wallet) } returns "Choose wallet"
            every { getString(R.string.feedback_category_exists) } returns "Category already exists"
            every { getString(R.string.feedback_category_create_failed) } returns "Create category failed"
            every { getString(R.string.feedback_transaction_saved) } returns "Saved"
            every { getString(R.string.image_scan_reading_device) } returns "Scanning..."
        }
        val savedStateHandle = SavedStateHandle(
            buildMap {
                if (transactionId != null) put(Route.EditTransaction.ARG_ID, transactionId)
            },
        )
        return TransactionFormViewModel(
            savedStateHandle = savedStateHandle,
            addTransactionUseCase = addTransactionUseCase,
            transactionRepository = transactionRepo,
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
    fun `add starts at SELECT_CATEGORY EXPENSE while edit starts at DETAILS`() = runTest {
        val addVm = createViewModel()
        assertThat(addVm.state.value.step).isEqualTo(TransactionFormStep.SELECT_CATEGORY)
        assertThat(addVm.state.value.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(addVm.state.value.isCategoryExplicitlySelected).isFalse()
        assertThat(addVm.state.value.canSave).isFalse()

        val editTx = TestData.transaction(id = 9L, category = Category.COFFEE, type = TransactionType.EXPENSE)
        val editVm = createViewModel(transactionId = 9L, existingTransaction = editTx)
        assertThat(editVm.state.value.step).isEqualTo(TransactionFormStep.DETAILS)
        assertThat(editVm.state.value.category).isEqualTo(Category.COFFEE)
        assertThat(editVm.state.value.isCategoryExplicitlySelected).isTrue()
    }

    @Test
    fun `selecting OTHER or re-selecting current category advances to DETAILS`() = runTest {
        val vm = createViewModel()

        vm.onEvent(TransactionFormUiEvent.CategorySelected(Category.OTHER))
        assertThat(vm.state.value.step).isEqualTo(TransactionFormStep.DETAILS)
        assertThat(vm.state.value.category).isEqualTo(Category.OTHER)
        assertThat(vm.state.value.isCategoryExplicitlySelected).isTrue()

        vm.onEvent(TransactionFormUiEvent.OpenCategorySelection)
        assertThat(vm.state.value.step).isEqualTo(TransactionFormStep.SELECT_CATEGORY)
        assertThat(vm.state.value.category).isEqualTo(Category.OTHER)

        vm.onEvent(TransactionFormUiEvent.CategorySelected(Category.OTHER))
        assertThat(vm.state.value.step).isEqualTo(TransactionFormStep.DETAILS)
    }

    @Test
    fun `save on SELECT_CATEGORY is blocked and emits category validation error`() = runTest {
        val addUseCase = mockk<AddTransactionUseCase>(relaxed = true)
        val vm = createViewModel(addTransactionUseCase = addUseCase)

        vm.feedback.test {
            vm.onEvent(TransactionFormUiEvent.Save)
            val err = awaitItem()
            assertThat(err.type).isEqualTo(FeedbackType.Error)
            assertThat(err.message).isEqualTo("Choose category first")
            coVerify(exactly = 0) { addUseCase(any()) }
        }
    }

    @Test
    fun `open category selection and type switch preserve draft expression, note, date, and wallet`() = runTest {
        val suggestUseCase = mockk<SuggestCategoryUseCase>(relaxed = true) {
            every { suggestDetailed(any(), true) } returns CategorySuggestion(
                category = Category.SALARY,
                confidence = 0.9f,
                reason = CategorySuggestionReason.Phrase("lương"),
            )
            every { suggestDetailed(any(), false) } returns null
        }
        val addUseCase = mockk<AddTransactionUseCase>()
        val txSlot = slot<Transaction>()
        coEvery { addUseCase(capture(txSlot)) } returns Result.success(15L)

        val vm = createViewModel(
            addTransactionUseCase = addUseCase,
            suggestCategoryUseCase = suggestUseCase,
        )
        val customDate = Instant.fromEpochMilliseconds(1_730_000_000_000L)

        vm.onEvent(TransactionFormUiEvent.CategorySelected(Category.FOOD))
        enterNumber(vm, "120")
        vm.onEvent(TransactionFormUiEvent.CalcKeyPressed(CalcKey.Operator('+')))
        enterNumber(vm, "30")
        vm.onEvent(TransactionFormUiEvent.NoteChanged("Lương thưởng trưa"))
        vm.onEvent(TransactionFormUiEvent.DateChanged(customDate))
        vm.onEvent(TransactionFormUiEvent.WalletChanged(2L))

        vm.onEvent(TransactionFormUiEvent.OpenCategorySelection)
        val gridState = vm.state.value
        assertThat(gridState.step).isEqualTo(TransactionFormStep.SELECT_CATEGORY)
        assertThat(gridState.category).isEqualTo(Category.FOOD)
        assertThat(gridState.displayExpression).contains("+")
        assertThat(gridState.note).isEqualTo("Lương thưởng trưa")
        assertThat(gridState.occurredAt).isEqualTo(customDate)
        assertThat(gridState.walletId).isEqualTo(2L)

        // Switching to INCOME requires explicit income category selection (suggestion does NOT auto-advance)
        vm.onEvent(TransactionFormUiEvent.TypeChanged(TransactionType.INCOME))
        val afterSwitch = vm.state.value
        assertThat(afterSwitch.step).isEqualTo(TransactionFormStep.SELECT_CATEGORY)
        assertThat(afterSwitch.type).isEqualTo(TransactionType.INCOME)
        assertThat(afterSwitch.isCategoryExplicitlySelected).isFalse()
        assertThat(afterSwitch.canSave).isFalse()
        assertThat(afterSwitch.note).isEqualTo("Lương thưởng trưa")
        assertThat(afterSwitch.occurredAt).isEqualTo(customDate)
        assertThat(afterSwitch.walletId).isEqualTo(2L)
        assertThat(afterSwitch.displayExpression).contains("+")

        vm.onEvent(TransactionFormUiEvent.CategorySelected(Category.BONUS))
        assertThat(vm.state.value.step).isEqualTo(TransactionFormStep.DETAILS)
        assertThat(vm.state.value.canSave).isTrue()

        vm.onEvent(TransactionFormUiEvent.Save)
        coVerify(exactly = 1) { addUseCase(any()) }
        assertThat(txSlot.captured.type).isEqualTo(TransactionType.INCOME)
        assertThat(txSlot.captured.category).isEqualTo(Category.BONUS)
        assertThat(txSlot.captured.amount).isEqualTo(Money.fromMajorUnit(150L))
        assertThat(txSlot.captured.walletId).isEqualTo(2L)
        assertThat(txSlot.captured.occurredAt).isEqualTo(customDate)
    }

    @Test
    fun `ignores double tap on DETAILS and mismatched category type`() = runTest {
        val vm = createViewModel()

        vm.onEvent(TransactionFormUiEvent.CategorySelected(Category.SALARY)) // INCOME cat on EXPENSE mode
        assertThat(vm.state.value.step).isEqualTo(TransactionFormStep.SELECT_CATEGORY)
        assertThat(vm.state.value.isCategoryExplicitlySelected).isFalse()

        vm.onEvent(TransactionFormUiEvent.CategorySelected(Category.FOOD))
        assertThat(vm.state.value.step).isEqualTo(TransactionFormStep.DETAILS)

        vm.onEvent(TransactionFormUiEvent.CategorySelected(Category.COFFEE))
        assertThat(vm.state.value.category).isEqualTo(Category.FOOD)
    }

    @Test
    fun `catalog removing confirmed category returns to SELECT_CATEGORY keeping draft`() = runTest {
        val custom = Category("CUSTOM_X", "Cầu lông", 0xFF4DB6ACL, isIncome = false, isCustom = true)
        val categoriesFlow = MutableStateFlow(Category.getAll() + custom)
        val vm = createViewModel(categoriesFlow = categoriesFlow)

        vm.onEvent(TransactionFormUiEvent.CategorySelected(custom))
        enterNumber(vm, "60000")
        vm.onEvent(TransactionFormUiEvent.NoteChanged("Thuê sân"))
        assertThat(vm.state.value.step).isEqualTo(TransactionFormStep.DETAILS)

        categoriesFlow.value = Category.getAll() // custom removed
        val state = vm.state.value
        assertThat(state.step).isEqualTo(TransactionFormStep.SELECT_CATEGORY)
        assertThat(state.isCategoryExplicitlySelected).isFalse()
        assertThat(state.amount).isEqualTo(Money.fromMajorUnit(60_000L))
        assertThat(state.note).isEqualTo("Thuê sân")
    }

    @Test
    fun `custom category create handles success, failure, and stale delayed completion`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val categoriesFlow = MutableStateFlow(Category.getAll())
        val categoryRepo = mockk<CategoryRepository>(relaxed = true) {
            every { observeCategories() } returns categoriesFlow
            coEvery { addCustomCategory(any()) } coAnswers { gate.await() }
        }
        val vm = createViewModel(categoriesFlow = categoriesFlow, categoryRepo = categoryRepo)

        // Duplicate name emits error and stays in grid
        vm.feedback.test {
            vm.onEvent(TransactionFormUiEvent.CreateCategory("Ăn uống", 0xFFE57373L, "food", false))
            val dupErr = awaitItem()
            assertThat(dupErr.type).isEqualTo(FeedbackType.Error)
            assertThat(vm.state.value.step).isEqualTo(TransactionFormStep.SELECT_CATEGORY)
        }

        // Delayed creation while user switches to INCOME does not overwrite INCOME / grid
        vm.onEvent(TransactionFormUiEvent.CreateCategory("Nhạc cụ", 0xFFE57373L, "other", false))
        vm.onEvent(TransactionFormUiEvent.TypeChanged(TransactionType.INCOME))
        gate.complete(Unit)

        assertThat(vm.state.value.type).isEqualTo(TransactionType.INCOME)
        assertThat(vm.state.value.step).isEqualTo(TransactionFormStep.SELECT_CATEGORY)
        assertThat(vm.state.value.isCategoryExplicitlySelected).isFalse()

        // Succeeding creation on active step/type advances even before catalog re-emits
        coEvery { categoryRepo.addCustomCategory(any()) } returns Unit
        vm.onEvent(TransactionFormUiEvent.CreateCategory("Tiền dạy kèm", 0xFF66BB6AL, "salary", true))
        assertThat(vm.state.value.step).isEqualTo(TransactionFormStep.DETAILS)
        assertThat(vm.state.value.category.displayName).isEqualTo("Tiền dạy kèm")
        assertThat(vm.state.value.isCategoryExplicitlySelected).isTrue()

        // Repository failure keeps user in grid and emits error
        coEvery { categoryRepo.addCustomCategory(any()) } throws IllegalStateException("DB fail")
        vm.onEvent(TransactionFormUiEvent.OpenCategorySelection)
        vm.feedback.test {
            vm.onEvent(TransactionFormUiEvent.CreateCategory("Thưởng thêm", 0xFF66BB6AL, "gift", true))
            val err = awaitItem()
            assertThat(err.type).isEqualTo(FeedbackType.Error)
            assertThat(vm.state.value.step).isEqualTo(TransactionFormStep.SELECT_CATEGORY)
        }
    }

    @Test
    fun `confirmed category stays intact across note suggestions and delayed OCR`() = runTest {
        val scanGate = CompletableDeferred<LocalImageScanResult>()
        val imageScanner = mockk<LocalTransactionImageScanner>()
        coEvery { imageScanner.scan(any()) } coAnswers { scanGate.await() }

        val suggestUseCase = mockk<SuggestCategoryUseCase>(relaxed = true) {
            every { suggestDetailed(any(), false) } returns CategorySuggestion(
                category = Category.FOOD,
                confidence = 0.9f,
                reason = CategorySuggestionReason.Phrase("bún"),
            )
        }
        val vm = createViewModel(suggestCategoryUseCase = suggestUseCase, imageScanner = imageScanner)

        vm.onEvent(TransactionFormUiEvent.CategorySelected(Category.COFFEE))
        vm.onEvent(TransactionFormUiEvent.NoteChanged("bún đậu"))
        assertThat(vm.state.value.category).isEqualTo(Category.COFFEE)
        assertThat(vm.state.value.step).isEqualTo(TransactionFormStep.DETAILS)

        vm.onEvent(TransactionFormUiEvent.ImagePicked(mockk<Uri>(relaxed = true)))
        scanGate.complete(LocalImageScanResult(amountInput = "85000", note = "GrabFood", message = "OK"))

        assertThat(vm.state.value.amount).isEqualTo(Money.fromMajorUnit(85_000L))
        assertThat(vm.state.value.category).isEqualTo(Category.COFFEE)
        assertThat(vm.state.value.step).isEqualTo(TransactionFormStep.DETAILS)
    }

    @Test
    fun `zero writable wallets disables canSave, blocks save, and preserves draft`() = runTest {
        val viewerWallet = TestData.wallet(id = 7L, name = "Ví xem chung").copy(
            isShared = true,
            sharedRole = SharedWalletRole.VIEWER,
        )
        val addUseCase = mockk<AddTransactionUseCase>(relaxed = true)
        val vm = createViewModel(wallets = listOf(viewerWallet), addTransactionUseCase = addUseCase)

        vm.onEvent(TransactionFormUiEvent.CategorySelected(Category.FOOD))
        enterNumber(vm, "35000")
        vm.onEvent(TransactionFormUiEvent.NoteChanged("Trà đá"))

        assertThat(vm.state.value.availableWallets).isEmpty()
        assertThat(vm.state.value.walletId).isNull()
        assertThat(vm.state.value.canSave).isFalse()

        vm.feedback.test {
            vm.onEvent(TransactionFormUiEvent.Save)
            val err = awaitItem()
            assertThat(err.type).isEqualTo(FeedbackType.Error)
            coVerify(exactly = 0) { addUseCase(any()) }
        }
        assertThat(vm.state.value.amount).isEqualTo(Money.fromMajorUnit(35_000L))
        assertThat(vm.state.value.note).isEqualTo("Trà đá")
        assertThat(vm.state.value.step).isEqualTo(TransactionFormStep.DETAILS)
    }
}
