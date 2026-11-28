package com.notepay.feature.debt

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.R
import com.notepay.core.designsystem.feedback.FeedbackDuration
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.navigation.Route
import com.notepay.data.remote.VietQrBankRepository
import com.notepay.domain.model.Debt
import com.notepay.domain.model.DebtType
import com.notepay.domain.model.DebtWithHistory
import com.notepay.domain.money.Money
import com.notepay.domain.repository.DebtRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.debt.DeleteDebtUseCase
import com.notepay.domain.usecase.debt.RecordDebtPaymentUseCase
import com.notepay.feature.debt.detail.DebtDetailUiEvent
import com.notepay.feature.debt.detail.DebtDetailViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebtDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val debtRepository: DebtRepository = mockk(relaxed = true)
    private val recordDebtPaymentUseCase: RecordDebtPaymentUseCase = mockk()
    private val deleteDebtUseCase: DeleteDebtUseCase = mockk()
    private val walletRepository: WalletRepository = mockk(relaxed = true)
    private val vietQrBankRepository: VietQrBankRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    private val sampleDebt = Debt(
        id = 10L,
        personName = "An",
        type = DebtType.LEND,
        originalAmount = Money(1_000_000_00L),
        createdAt = Instant.parse("2026-10-01T00:00:00Z"),
        isSettled = false,
    )
    private val sampleDebtWithHistory = DebtWithHistory(debt = sampleDebt, payments = emptyList())

    @Before
    fun setup() {
        every { debtRepository.observeById(10L) } returns flowOf(sampleDebtWithHistory)
        every { walletRepository.observeAll() } returns flowOf(emptyList())
        coEvery { vietQrBankRepository.getBanks() } returns emptyList()
        every { context.getString(R.string.action_retry) } returns "Retry"
        every { context.getString(R.string.feedback_undo) } returns "Undo"
        every { context.getString(R.string.debt_payment_success) } returns "Payment recorded"
        every { context.getString(R.string.debt_payment_failed) } returns "Payment failed"
        every { context.getString(R.string.debt_marked_settled_success) } returns "Marked settled"
        every { context.getString(R.string.debt_marked_unsettled_success) } returns "Marked unsettled"
        every { context.getString(R.string.debt_mark_settled_failed) } returns "Settled update failed"
        every { context.getString(R.string.debt_payment_delete_success) } returns "Payment deleted"
        every { context.getString(R.string.debt_payment_delete_failed) } returns "Payment delete failed"
        every { context.getString(R.string.debt_delete_failed) } returns "Debt delete failed"
    }

    private fun createViewModel(): DebtDetailViewModel = DebtDetailViewModel(
        debtRepository = debtRepository,
        recordDebtPaymentUseCase = recordDebtPaymentUseCase,
        deleteDebtUseCase = deleteDebtUseCase,
        walletRepository = walletRepository,
        vietQrBankRepository = vietQrBankRepository,
        context = context,
        savedStateHandle = SavedStateHandle(mapOf(Route.DebtDetail.ARG_ID to 10L)),
    )

    @Test
    fun `recordPayment success emits success feedback`() = runTest {
        val viewModel = createViewModel()
        coEvery { recordDebtPaymentUseCase(any(), any(), any(), any()) } returns Result.success(1L)

        val feedbacks = mutableListOf<UiFeedback>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks += it }
        }

        viewModel.recordPayment(
            debt = sampleDebt,
            amount = Money(250_000_00L),
            walletId = null,
            note = "Part 1",
            syncWithWallet = false,
        )
        advanceUntilIdle()

        assertThat(feedbacks).hasSize(1)
        assertThat(feedbacks.single().type).isEqualTo(FeedbackType.Success)
        assertThat(feedbacks.single().message).isEqualTo("Payment recorded")
    }

    @Test
    fun `recordPayment failure emits error with retry`() = runTest {
        val viewModel = createViewModel()
        coEvery { recordDebtPaymentUseCase(any(), any(), any(), any()) } returnsMany listOf(
            Result.failure(RuntimeException("fail")),
            Result.success(2L),
        )

        val feedbacks = mutableListOf<UiFeedback>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks += it }
        }

        viewModel.recordPayment(
            debt = sampleDebt,
            amount = Money(250_000_00L),
            walletId = null,
            note = "Part 1",
            syncWithWallet = false,
        )
        advanceUntilIdle()

        assertThat(feedbacks).hasSize(1)
        val failure = feedbacks.single()
        assertThat(failure.type).isEqualTo(FeedbackType.Error)
        assertThat(failure.duration).isEqualTo(FeedbackDuration.Long)
        assertThat(failure.actionLabel).isEqualTo("Retry")
        assertThat(failure.message).isEqualTo("Payment failed")

        failure.onAction?.invoke()
        advanceUntilIdle()

        coVerify(exactly = 2) { recordDebtPaymentUseCase(any(), any(), any(), any()) }
        assertThat(feedbacks.last().type).isEqualTo(FeedbackType.Success)
    }

    @Test
    fun `toggleSettled success emits undo action which restores previous state`() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val feedbacks = mutableListOf<UiFeedback>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks += it }
        }

        viewModel.toggleSettled()
        advanceUntilIdle()

        coVerify { debtRepository.markSettled(10L, true) }
        assertThat(feedbacks).hasSize(1)
        val feedback = feedbacks.single()
        assertThat(feedback.type).isEqualTo(FeedbackType.Success)
        assertThat(feedback.actionLabel).isEqualTo("Undo")
        assertThat(feedback.message).isEqualTo("Marked settled")

        feedback.onAction?.invoke()
        advanceUntilIdle()

        coVerify { debtRepository.markSettled(10L, false) }
    }

    @Test
    fun `deletePayment success emits payment delete message and failure emits retry`() = runTest {
        val viewModel = createViewModel()
        coEvery { debtRepository.deletePayment(99L) } throws IllegalStateException("io")

        val feedbacks = mutableListOf<UiFeedback>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks += it }
        }

        viewModel.deletePayment(99L)
        advanceUntilIdle()

        assertThat(feedbacks).hasSize(1)
        val errorFeedback = feedbacks.single()
        assertThat(errorFeedback.type).isEqualTo(FeedbackType.Error)
        assertThat(errorFeedback.actionLabel).isEqualTo("Retry")
        assertThat(errorFeedback.message).isEqualTo("Payment delete failed")

        coEvery { debtRepository.deletePayment(99L) } returns Unit
        errorFeedback.onAction?.invoke()
        advanceUntilIdle()

        assertThat(feedbacks).hasSize(2)
        assertThat(feedbacks.last().type).isEqualTo(FeedbackType.Success)
        assertThat(feedbacks.last().message).isEqualTo("Payment deleted")
    }

    @Test
    fun `deleteDebt failure does not mark deleted and emits retry`() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        coEvery { deleteDebtUseCase(10L) } returns Result.failure(IllegalStateException("db"))

        val feedbacks = mutableListOf<UiFeedback>()
        val events = mutableListOf<DebtDetailUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks += it }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }

        viewModel.deleteDebt()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isDeleted).isFalse()
        assertThat(events).isEmpty()
        assertThat(feedbacks).hasSize(1)
        assertThat(feedbacks.single().type).isEqualTo(FeedbackType.Error)
        assertThat(feedbacks.single().actionLabel).isEqualTo("Retry")
    }

    @Test
    fun `deleteDebt success marks deleted and emits DebtDeleted event`() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        coEvery { deleteDebtUseCase(10L) } returns Result.success(Unit)

        val events = mutableListOf<DebtDetailUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }

        viewModel.deleteDebt()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isDeleted).isTrue()
        assertThat(events).containsExactly(DebtDetailUiEvent.DebtDeleted)
    }

    @Test
    fun `recordPayment ignores concurrent duplicate calls while in-flight and resets isSubmitting`() = runTest {
        val viewModel = createViewModel()
        val deferred = CompletableDeferred<Result<Long>>()
        coEvery { recordDebtPaymentUseCase(any(), any(), any(), any()) } coAnswers { deferred.await() }

        val feedbacks = mutableListOf<UiFeedback>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks += it }
        }
        advanceUntilIdle()

        viewModel.recordPayment(sampleDebt, Money(100_000_00L), null, "", false)
        viewModel.recordPayment(sampleDebt, Money(100_000_00L), null, "", false)

        assertThat(viewModel.uiState.value.isSubmitting).isTrue()
        coVerify(exactly = 1) { recordDebtPaymentUseCase(any(), any(), any(), any()) }

        deferred.complete(Result.failure(RuntimeException("fail")))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSubmitting).isFalse()
        assertThat(feedbacks).hasSize(1)
        assertThat(feedbacks.single().type).isEqualTo(FeedbackType.Error)

        coEvery { recordDebtPaymentUseCase(any(), any(), any(), any()) } returns Result.success(9L)
        feedbacks.single().onAction?.invoke()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSubmitting).isFalse()
        assertThat(feedbacks.last().type).isEqualTo(FeedbackType.Success)
    }

    @Test
    fun `deleteDebt ignores concurrent duplicate calls and prevents delete after success`() = runTest {
        val viewModel = createViewModel()
        val deferred = CompletableDeferred<Result<Unit>>()
        coEvery { deleteDebtUseCase(10L) } coAnswers { deferred.await() }

        val feedbacks = mutableListOf<UiFeedback>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks += it }
        }
        advanceUntilIdle()

        viewModel.deleteDebt()
        viewModel.deleteDebt()

        assertThat(viewModel.uiState.value.isSubmitting).isTrue()
        coVerify(exactly = 1) { deleteDebtUseCase(10L) }

        deferred.complete(Result.failure(IllegalStateException("fail")))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSubmitting).isFalse()
        assertThat(viewModel.uiState.value.isDeleted).isFalse()

        coEvery { deleteDebtUseCase(10L) } returns Result.success(Unit)
        feedbacks.single().onAction?.invoke()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isDeleted).isTrue()
        assertThat(viewModel.uiState.value.isSubmitting).isFalse()

        viewModel.deleteDebt()
        advanceUntilIdle()
        coVerify(exactly = 2) { deleteDebtUseCase(10L) }
    }

    @Test
    fun `toggleSettled ignores concurrent calls and restoreSettled failure emits retry`() = runTest {
        val viewModel = createViewModel()
        val markDeferred = CompletableDeferred<Unit>()
        coEvery { debtRepository.markSettled(10L, true) } coAnswers { markDeferred.await() }
        coEvery { debtRepository.markSettled(10L, false) } throws IllegalStateException("undo failed") andThen Unit

        val feedbacks = mutableListOf<UiFeedback>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks += it }
        }
        advanceUntilIdle()

        viewModel.toggleSettled()
        viewModel.toggleSettled()

        assertThat(viewModel.uiState.value.isSubmitting).isTrue()
        coVerify(exactly = 1) { debtRepository.markSettled(10L, true) }

        markDeferred.complete(Unit)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSubmitting).isFalse()
        assertThat(feedbacks).hasSize(1)
        assertThat(feedbacks.single().actionLabel).isEqualTo("Undo")

        feedbacks.single().onAction?.invoke()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSubmitting).isFalse()
        assertThat(feedbacks).hasSize(2)
        val restoreError = feedbacks.last()
        assertThat(restoreError.type).isEqualTo(FeedbackType.Error)
        assertThat(restoreError.actionLabel).isEqualTo("Retry")
        assertThat(restoreError.message).isEqualTo("Settled update failed")

        restoreError.onAction?.invoke()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSubmitting).isFalse()
        coVerify(exactly = 2) { debtRepository.markSettled(10L, false) }
    }
}
