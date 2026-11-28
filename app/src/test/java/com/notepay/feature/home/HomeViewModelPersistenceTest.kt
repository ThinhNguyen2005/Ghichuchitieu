package com.notepay.feature.home

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.R
import com.notepay.ai.LegacyAiModelCleaner
import com.notepay.core.designsystem.feedback.FeedbackDuration
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.data.datastore.AppSettingsDataStore
import com.notepay.domain.TestData
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.domain.repository.SubscriptionRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.DeleteTransactionUseCase
import com.notepay.domain.usecase.GetMonthlySummaryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelPersistenceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val walletRepo: WalletRepository = mockk(relaxed = true)
    private val transactionRepo: TransactionRepository = mockk(relaxed = true)
    private val deleteTransactionUseCase: DeleteTransactionUseCase = mockk(relaxed = true)
    private val addTransactionUseCase: AddTransactionUseCase = mockk(relaxed = true)
    private val getMonthlySummary: GetMonthlySummaryUseCase = mockk(relaxed = true)
    private val appSettingsDataStore: AppSettingsDataStore = mockk(relaxed = true)
    private val subscriptionRepository: SubscriptionRepository = mockk(relaxed = true)
    private val legacyModelCleaner: LegacyAiModelCleaner = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    private val dismissedFlow = MutableStateFlow(false)
    private val rotationFlow = MutableStateFlow(0)
    private val presetFlow = MutableStateFlow("warm_silk")
    private val customPathFlow = MutableStateFlow<String?>(null)
    private val blurFlow = MutableStateFlow(false)
    private val balanceHiddenFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        val sampleWallet = Wallet(
            id = 1L,
            name = "Tiền mặt",
            initialBalance = Money.fromMajorUnit(1_000_000L),
            iconKey = Wallet.ICON_CASH,
            colorKey = Wallet.COLOR_PRIMARY,
            isActive = true,
        )
        every { walletRepo.observeActive() } returns flowOf(sampleWallet)
        every { walletRepo.observeAll() } returns flowOf(listOf(sampleWallet))
        every { transactionRepo.observeWalletStats(any(), any()) } returns flowOf(emptyList())
        every { transactionRepo.observeAllCreatedDates() } returns flowOf(emptyList())
        every { subscriptionRepository.observeAll() } returns flowOf(emptyList())
        every { getMonthlySummary.invoke(any(), any(), any()) } returns flowOf(
            GetMonthlySummaryUseCase.Summary(
                year = 2026,
                month = 11,
                totalIncome = Money.ZERO,
                totalExpense = Money.ZERO,
                balance = Money.ZERO,
                byCategory = emptyMap(),
                byIncomeCategory = emptyMap(),
                transactions = emptyList(),
            )
        )
        every { context.getString(any(), any(), any()) } returns "11/2026"

        every { appSettingsDataStore.homeSmartTipsDismissed } returns dismissedFlow
        every { appSettingsDataStore.homeSmartTipRotation } returns rotationFlow
        every { appSettingsDataStore.homeBackgroundPreset } returns presetFlow
        every { appSettingsDataStore.homeBackgroundImagePath } returns customPathFlow
        every { appSettingsDataStore.homeBackgroundBlurEnabled } returns blurFlow
        every { appSettingsDataStore.homeBalanceHidden } returns balanceHiddenFlow
        every { appSettingsDataStore.hapticFeedbackEnabled } returns flowOf(true)

        coEvery { appSettingsDataStore.setHomeSmartTipsDismissed(any()) } coAnswers {
            dismissedFlow.value = firstArg()
        }
        coEvery { appSettingsDataStore.incrementHomeSmartTipRotation() } coAnswers {
            rotationFlow.value = rotationFlow.value + 1
        }
        coEvery { appSettingsDataStore.setHomeBackgroundPreset(any()) } coAnswers {
            presetFlow.value = firstArg()
        }
        coEvery { appSettingsDataStore.setHomeBackgroundBlurEnabled(any()) } coAnswers {
            blurFlow.value = firstArg()
        }
        coEvery { appSettingsDataStore.setHomeBalanceHidden(any()) } coAnswers {
            balanceHiddenFlow.value = firstArg()
        }
    }

    private fun createViewModel() = HomeViewModel(
        walletRepo = walletRepo,
        transactionRepo = transactionRepo,
        deleteTransactionUseCase = deleteTransactionUseCase,
        getMonthlySummary = getMonthlySummary,
        appSettingsDataStore = appSettingsDataStore,
        subscriptionRepository = subscriptionRepository,
        legacyModelCleaner = legacyModelCleaner,
        context = context,
        addTransactionUseCase = addTransactionUseCase,
    )

    @Test
    fun `deleteTransaction emits success with Undo and restores on Undo action`() = runTest {
        val tx = TestData.transaction(id = 42L, walletId = 1L, note = "Ca phe")
        coEvery { transactionRepo.getById(42L) } returns tx
        coEvery { deleteTransactionUseCase(42L) } returns Result.success(Unit)
        coEvery { addTransactionUseCase(any()) } returns Result.success(43L)
        every { context.getString(R.string.transaction_deleted) } returns "Deleted"
        every { context.getString(R.string.feedback_undo) } returns "Undo"
        every { context.getString(R.string.feedback_transaction_restored) } returns "Restored"

        val viewModel = createViewModel()

        viewModel.feedback.test {
            viewModel.deleteTransaction(42L)
            val fb = awaitItem()
            assertThat(fb.type).isEqualTo(FeedbackType.Success)
            assertThat(fb.duration).isEqualTo(FeedbackDuration.Long)
            assertThat(fb.actionLabel).isEqualTo("Undo")

            fb.onAction?.invoke()
            val restored = awaitItem()
            assertThat(restored.type).isEqualTo(FeedbackType.Success)
            assertThat(restored.message).isEqualTo("Restored")
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { addTransactionUseCase(match { it.id == 0L && it.note == "Ca phe" }) }
    }

    @Test
    fun `deleteTransaction emits error with Retry on failure`() = runTest {
        coEvery { transactionRepo.getById(42L) } returns null
        coEvery { deleteTransactionUseCase(42L) } returnsMany listOf(
            Result.failure(IllegalStateException("db error")),
            Result.success(Unit),
        )
        every { context.getString(R.string.feedback_transaction_delete_failed) } returns "Delete failed"
        every { context.getString(R.string.action_retry) } returns "Retry"
        every { context.getString(R.string.transaction_deleted) } returns "Deleted"

        val viewModel = createViewModel()

        viewModel.feedback.test {
            viewModel.deleteTransaction(42L)
            val err = awaitItem()
            assertThat(err.type).isEqualTo(FeedbackType.Error)
            assertThat(err.actionLabel).isEqualTo("Retry")

            err.onAction?.invoke()
            val retried = awaitItem()
            assertThat(retried.type).isEqualTo(FeedbackType.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissSmartInsights persists dismissal and reflects DataStore restore`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            var current = awaitItem()
            while (current.isLoading) current = awaitItem()
            assertThat(current.isSmartInsightsDismissed).isFalse()

            viewModel.dismissSmartInsights()
            current = awaitItem()
            assertThat(current.isSmartInsightsDismissed).isTrue()

            dismissedFlow.value = false
            current = awaitItem()
            assertThat(current.isSmartInsightsDismissed).isFalse()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { appSettingsDataStore.setHomeSmartTipsDismissed(true) }
    }

    @Test
    fun `nextSmartTip rotates activeSmartTipIndex and selection persists`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            var current = awaitItem()
            while (current.isLoading) current = awaitItem()
            val firstIndex = current.activeSmartTipIndex

            viewModel.nextSmartTip()
            current = awaitItem()
            assertThat(current.smartTips.size).isAtLeast(8)
            assertThat(current.activeSmartTipIndex).isNotEqualTo(firstIndex)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { appSettingsDataStore.incrementHomeSmartTipRotation() }
    }

    @Test
    fun `toggleBalanceHidden persists and state tracks DataStore background preset`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            var current = awaitItem()
            while (current.isLoading) current = awaitItem()
            assertThat(current.backgroundPreset).isEqualTo(HomeBackgroundPreset.WARM_SILK)
            assertThat(current.isBalanceHidden).isFalse()

            presetFlow.value = HomeBackgroundPreset.OCEAN_MIST.id
            current = awaitItem()
            assertThat(current.backgroundPreset).isEqualTo(HomeBackgroundPreset.OCEAN_MIST)

            viewModel.toggleBalanceHidden()
            current = awaitItem()
            assertThat(current.isBalanceHidden).isTrue()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { appSettingsDataStore.setHomeBalanceHidden(true) }
    }
}
