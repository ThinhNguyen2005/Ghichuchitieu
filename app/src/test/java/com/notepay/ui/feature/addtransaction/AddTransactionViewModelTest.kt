package com.notepay.ui.feature.addtransaction

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.TestData
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.Wallet
import com.notepay.domain.repository.CategoryRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.SuggestCategoryUseCase
import com.notepay.ui.feedback.FeedbackType
import com.notepay.ui.feedback.UiFeedback
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    // Khôi phục sử dụng TestData chuẩn để tránh lỗi khởi tạo Wallet sai tham số
    private val wallet = TestData.wallet(id = 1L)

    private val mockSuggestCategoryUseCase = mockk<SuggestCategoryUseCase>(relaxed = true)

    @Before
    fun setUp() {
        // Cấu hình phản hồi mặc định cho mock gợi ý danh mục
        every { mockSuggestCategoryUseCase.suggest(any(), any()) } returns Category.FOOD
        every { mockSuggestCategoryUseCase.suggestDetailed(any(), any()) } returns com.notepay.domain.usecase.CategorySuggestion(
            category = Category.FOOD,
            confidence = 0.9f,
            reason = "Test suggestion"
        )
    }
    @Test
    fun `amount changed clears previous invalid amount error when input becomes valid`() = runTest {
        val viewModel = createViewModel(wallets = listOf(wallet), activeWallet = wallet)

        // ĐÃ SỬA: Nhập chuỗi số siêu lớn gây tràn vùng nhớ để ép sinh lỗi AMOUNT_INVALID chính xác
        viewModel.onEvent(AddTransactionEvent.AmountChanged("99999999999999999999"))
        assertThat(viewModel.state.value.errors).contains(FieldError.AMOUNT_INVALID)

        // Người dùng nhập lại số tiền đúng -> Lỗi cũ phải biến mất tự động
        viewModel.onEvent(AddTransactionEvent.AmountChanged("50000"))
        assertThat(viewModel.state.value.errors).doesNotContain(FieldError.AMOUNT_INVALID)
    }

    @Test
    fun `init selects active wallet`() = runTest {
        val viewModel = createViewModel(wallets = listOf(wallet), activeWallet = wallet)

        assertThat(viewModel.state.value.walletId).isEqualTo(1L)
        assertThat(viewModel.state.value.availableWallets).containsExactly(wallet)
    }

    @Test
    fun `amount changed parses VND major units`() = runTest {
        val viewModel = createViewModel(wallets = listOf(wallet), activeWallet = wallet)

        viewModel.onEvent(AddTransactionEvent.AmountChanged("125.000"))

        assertThat(viewModel.state.value.amountInput).isEqualTo("125000")
        assertThat(viewModel.state.value.amount?.amountInCents).isEqualTo(12_500_000L)
        assertThat(viewModel.state.value.errors).doesNotContain(FieldError.AMOUNT_INVALID)
    }

    @Test
    fun `note changed triggers auto category suggestion successfully`() = runTest {
        val viewModel = createViewModel(wallets = listOf(wallet), activeWallet = wallet)

        // Kịch bản nâng cao 2: Thay đổi ghi chú giao dịch
        viewModel.onEvent(AddTransactionEvent.NoteChanged("Ăn trưa bún bò"))

        // Kiểm tra thông qua verify của MockK để không bị phụ thuộc vào tên biến của State
        verify(exactly = 1) { mockSuggestCategoryUseCase.suggestDetailed("Ăn trưa bún bò", false) }
    }

    @Test
    fun `note over max length blocks save`() = runTest {
        val viewModel = createViewModel(wallets = listOf(wallet), activeWallet = wallet)

        viewModel.onEvent(AddTransactionEvent.NoteChanged("x".repeat(Transaction.MAX_NOTE_LENGTH + 1)))

        assertThat(viewModel.state.value.errors).contains(FieldError.NOTE_TOO_LONG)
        assertThat(viewModel.state.value.canSave).isFalse()
    }

    @Test
    fun `save valid transaction marks success`() = runTest {
        val transactionRepository = FakeTransactionRepository(upsertResult = Result.success(7L))
        val viewModel = createViewModel(
            wallets = listOf(wallet),
            activeWallet = wallet,
            transactionRepository = transactionRepository,
        )

        viewModel.onEvent(AddTransactionEvent.AmountChanged("50000"))
        viewModel.onEvent(AddTransactionEvent.Save)

        assertThat(viewModel.state.value.savedSuccessfully).isTrue()
        assertThat(transactionRepository.savedTransactions).hasSize(1)
    }

    @Test
    fun `save failure exposes save error message`() = runTest {
        val viewModel = createViewModel(
            wallets = listOf(wallet),
            activeWallet = wallet,
            transactionRepository = FakeTransactionRepository(Result.failure(IllegalStateException("db locked"))),
        )

        viewModel.onEvent(AddTransactionEvent.AmountChanged("50000"))
        viewModel.onEvent(AddTransactionEvent.Save)

        assertThat(viewModel.state.value.savedSuccessfully).isFalse()
        assertThat(viewModel.state.value.saveErrorMessage).contains("Không thể lưu giao dịch")
    }

    @Test
    fun `save valid transaction emits success feedback`() = runTest {
        val transactionRepository = FakeTransactionRepository(Result.success(7L))
        val viewModel = createViewModel(
            wallets = listOf(wallet),
            activeWallet = wallet,
            transactionRepository = transactionRepository,
        )
        val feedbacks = mutableListOf<UiFeedback>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks.add(it) }
        }

        viewModel.onEvent(AddTransactionEvent.AmountChanged("50000"))
        viewModel.onEvent(AddTransactionEvent.Save)

        assertThat(feedbacks.any { it.type == FeedbackType.Success }).isTrue()
        assertThat(feedbacks.first { it.type == FeedbackType.Success }.message).isEqualTo("Đã lưu giao dịch")
        job.cancel()
    }

    @Test
    fun `save failure emits error feedback`() = runTest {
        val viewModel = createViewModel(
            wallets = listOf(wallet),
            activeWallet = wallet,
            transactionRepository = FakeTransactionRepository(Result.failure(IllegalStateException("db locked"))),
        )
        val feedbacks = mutableListOf<UiFeedback>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks.add(it) }
        }

        viewModel.onEvent(AddTransactionEvent.AmountChanged("50000"))
        viewModel.onEvent(AddTransactionEvent.Save)

        assertThat(feedbacks.any { it.type == FeedbackType.Error }).isTrue()
        assertThat(feedbacks.first { it.type == FeedbackType.Error }.message).isEqualTo("Không thể lưu giao dịch")
        job.cancel()
    }

    private fun createViewModel(
        wallets: List<Wallet>,
        activeWallet: Wallet?,
        transactionRepository: FakeTransactionRepository = FakeTransactionRepository(Result.success(1L)),
    ): AddTransactionViewModel {
        val dispatcher = mainDispatcherRule.testDispatcher
        val walletRepository = FakeWalletRepository(wallets, activeWallet)
        val categoryRepository = FakeCategoryRepository()
        val useCase = AddTransactionUseCase(transactionRepository, walletRepository, dispatcher)

        // ĐÃ SỬA: Sử dụng cấu trúc gọi hàm an toàn để tự động khớp với định danh ioDispatcher hệ thống
        return AddTransactionViewModel(
            addTransactionUseCase = useCase,
            walletRepository = walletRepository,
            categoryRepository = categoryRepository,
            suggestCategoryUseCase = mockSuggestCategoryUseCase,
            imageScanner = mockk(relaxed = true),
            ioDispatcher = dispatcher
        )
    }
}

private class FakeCategoryRepository : CategoryRepository {
    private val categoriesFlow = MutableStateFlow(Category.getAll())
    override fun observeCategories(): Flow<List<Category>> = categoriesFlow
    override suspend fun getCategories(): List<Category> = Category.getAll()
    override suspend fun addCustomCategory(category: Category) {
        val current = Category.getAll().filter { it.isCustom }.toMutableList()
        current.add(category)
        Category.registerCustomCategories(current)
        categoriesFlow.value = Category.getAll()
    }
}

private class FakeWalletRepository(
    wallets: List<Wallet>,
    private var activeWallet: Wallet?,
) : WalletRepository {
    private val walletFlow = MutableStateFlow(wallets)

    override fun observeAll(): Flow<List<Wallet>> = walletFlow
    override fun observeActive(): Flow<Wallet?> = flowOf(activeWallet)
    override suspend fun getById(id: Long): Wallet? = walletFlow.value.firstOrNull { it.id == id }
    override suspend fun upsert(wallet: Wallet): Long = wallet.id
    override suspend fun delete(id: Long) = Unit
    override suspend fun setActive(id: Long) {
        activeWallet = walletFlow.value.firstOrNull { it.id == id }
    }
}

private class FakeTransactionRepository(
    private val upsertResult: Result<Long>,
) : TransactionRepository {
    val savedTransactions = mutableListOf<Transaction>()

    override fun observeAll(): Flow<List<Transaction>> = flowOf(savedTransactions)
    override fun observeByWallet(walletId: Long): Flow<List<Transaction>> = flowOf(savedTransactions.filter { it.walletId == walletId })
    override fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>> = flowOf(savedTransactions)
    override suspend fun getById(id: Long): Transaction? = savedTransactions.firstOrNull { it.id == id }
    override suspend fun upsert(transaction: Transaction): Long {
        return upsertResult.getOrThrow().also { savedTransactions += transaction }
    }
    override suspend fun delete(id: Long) = Unit

    // ĐÃ SỬA: Hàm tìm kiếm tương tự phục vụ đối soát, tránh hoàn toàn lỗi abstract member compiler
    override suspend fun findRecentSimilar(
        noteKeyword: String,
        fromMillis: Long,
        toMillis: Long
    ): List<Transaction> {
        return savedTransactions.filter { tx ->
            tx.note.contains(noteKeyword, ignoreCase = true) &&
                    tx.occurredAt.toEpochMilliseconds() in fromMillis..toMillis
        }
    }
}
