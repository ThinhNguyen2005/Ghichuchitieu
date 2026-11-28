package com.notepay.feature.debt

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.R
import com.notepay.core.designsystem.feedback.FeedbackDuration
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.domain.model.DebtType
import com.notepay.domain.money.Money
import com.notepay.domain.repository.DebtRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.debt.CreateDebtUseCase
import com.notepay.domain.usecase.debt.DebtSummary
import com.notepay.domain.usecase.debt.DeleteDebtUseCase
import com.notepay.domain.usecase.debt.GetDebtSummaryUseCase
import com.notepay.domain.usecase.debt.GetDebtsUseCase
import com.notepay.domain.usecase.debt.RecordDebtPaymentUseCase
import com.notepay.feature.debt.management.DebtUiEvent
import com.notepay.feature.debt.management.DebtViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebtViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getDebtsUseCase: GetDebtsUseCase = mockk(relaxed = true)
    private val getDebtSummaryUseCase: GetDebtSummaryUseCase = mockk(relaxed = true)
    private val createDebtUseCase: CreateDebtUseCase = mockk()
    private val recordDebtPaymentUseCase: RecordDebtPaymentUseCase = mockk()
    private val deleteDebtUseCase: DeleteDebtUseCase = mockk()
    private val debtRepository: DebtRepository = mockk(relaxed = true)
    private val walletRepository: WalletRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setup() {
        every { debtRepository.observeAll() } returns flowOf(emptyList())
        every { walletRepository.observeAll() } returns flowOf(emptyList())
        every { getDebtSummaryUseCase() } returns flowOf(
            DebtSummary(
                totalToCollect = Money.ZERO,
                totalToPay = Money.ZERO,
                netBalance = Money.ZERO,
                activeDebtsCount = 0,
                overdueCount = 0,
                dueTodayCount = 0,
            ),
        )
        every { context.getString(R.string.action_retry) } returns "Retry"
        every { context.getString(R.string.feedback_undo) } returns "Undo"
        every { context.getString(R.string.debt_create_failed) } returns "Create failed"
        every { context.getString(R.string.debt_delete_failed) } returns "Delete failed"
        every { context.getString(R.string.debt_delete_success) } returns "Debt deleted"
        every { context.getString(R.string.debt_mark_settled_failed) } returns "Settled update failed"
        every { context.getString(R.string.debt_marked_settled_success) } returns "Marked settled"
    }

    private fun createViewModel(): DebtViewModel = DebtViewModel(
        getDebtsUseCase = getDebtsUseCase,
        getDebtSummaryUseCase = getDebtSummaryUseCase,
        createDebtUseCase = createDebtUseCase,
        recordDebtPaymentUseCase = recordDebtPaymentUseCase,
        deleteDebtUseCase = deleteDebtUseCase,
        debtRepository = debtRepository,
        walletRepository = walletRepository,
        context = context,
    )

    @Test
    fun `createDebt success emits DebtCreated with created id`() = runTest {
        val viewModel = createViewModel()
        coEvery { createDebtUseCase(any(), any(), any()) } returns Result.success(42L)

        val events = mutableListOf<DebtUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }

        viewModel.createDebt(
            personName = "Minh",
            phoneNumber = null,
            type = DebtType.LEND,
            amount = Money(500_000_00L),
            walletId = null,
            dueDate = null,
            note = "Lunch",
            syncWithWallet = false,
        )
        advanceUntilIdle()

        assertThat(events).containsExactly(DebtUiEvent.DebtCreated(42L))
    }

    @Test
    fun `createDebt failure emits error feedback with retry action that retries creation`() = runTest {
        val viewModel = createViewModel()
        coEvery { createDebtUseCase(any(), any(), any()) } returnsMany listOf(
            Result.failure(IllegalStateException("db error")),
            Result.success(77L),
        )

        val sheetFeedbacks = mutableListOf<UiFeedback>()
        val events = mutableListOf<DebtUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.createSheetFeedback.collect { sheetFeedbacks += it }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }

        viewModel.createDebt(
            personName = "Lan",
            phoneNumber = "0901234567",
            type = DebtType.BORROW,
            amount = Money(200_000_00L),
            walletId = 1L,
            dueDate = null,
            note = "Coffee",
            syncWithWallet = false,
        )
        advanceUntilIdle()

        assertThat(events).isEmpty()
        assertThat(sheetFeedbacks).hasSize(1)
        val feedback = sheetFeedbacks.single()
        assertThat(feedback.type).isEqualTo(FeedbackType.Error)
        assertThat(feedback.duration).isEqualTo(FeedbackDuration.Long)
        assertThat(feedback.actionLabel).isEqualTo("Retry")
        assertThat(feedback.message).isEqualTo("Create failed")

        feedback.onAction?.invoke()
        advanceUntilIdle()

        coVerify(exactly = 2) { createDebtUseCase(any(), any(), any()) }
        assertThat(events).containsExactly(DebtUiEvent.DebtCreated(77L))
    }

    @Test
    fun `deleteDebt failure emits error with retry action`() = runTest {
        val viewModel = createViewModel()
        coEvery { deleteDebtUseCase(9L) } returns Result.failure(RuntimeException("delete failed"))

        val feedbacks = mutableListOf<UiFeedback>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks += it }
        }

        viewModel.deleteDebt(9L)
        advanceUntilIdle()

        assertThat(feedbacks).hasSize(1)
        val feedback = feedbacks.single()
        assertThat(feedback.type).isEqualTo(FeedbackType.Error)
        assertThat(feedback.actionLabel).isEqualTo("Retry")
        assertThat(feedback.message).isEqualTo("Delete failed")

        coEvery { deleteDebtUseCase(9L) } returns Result.success(Unit)
        feedback.onAction?.invoke()
        advanceUntilIdle()

        assertThat(feedbacks).hasSize(2)
        assertThat(feedbacks.last().type).isEqualTo(FeedbackType.Success)
        assertThat(feedbacks.last().message).isEqualTo("Debt deleted")
    }

    @Test
    fun `toggleSettled exception emits error and rethrows CancellationException`() = runTest {
        val viewModel = createViewModel()
        coEvery { debtRepository.markSettled(5L, true) } throws IllegalStateException("disk full")

        val feedbacks = mutableListOf<UiFeedback>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks += it }
        }

        viewModel.toggleSettled(debtId = 5L, currentSettled = false)
        advanceUntilIdle()

        assertThat(feedbacks).hasSize(1)
        assertThat(feedbacks.single().type).isEqualTo(FeedbackType.Error)
        assertThat(feedbacks.single().actionLabel).isEqualTo("Retry")
        assertThat(feedbacks.single().message).isEqualTo("Settled update failed")

        coEvery { debtRepository.markSettled(5L, true) } throws CancellationException("cancelled")
        viewModel.toggleSettled(debtId = 5L, currentSettled = false)
        advanceUntilIdle()

        assertThat(feedbacks).hasSize(1)
    }

    @Test
    fun `createDebt ignores concurrent duplicate calls while in-flight and resets isSubmitting`() = runTest {
        val viewModel = createViewModel()
        val deferred = CompletableDeferred<Result<Long>>()
        coEvery { createDebtUseCase(any(), any(), any()) } coAnswers { deferred.await() }

        val events = mutableListOf<DebtUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        advanceUntilIdle()

        viewModel.createDebt(
            personName = "Minh",
            phoneNumber = null,
            type = DebtType.LEND,
            amount = Money(500_000_00L),
            walletId = null,
            dueDate = null,
            note = "Lunch",
            syncWithWallet = false,
        )
        viewModel.createDebt(
            personName = "Minh",
            phoneNumber = null,
            type = DebtType.LEND,
            amount = Money(500_000_00L),
            walletId = null,
            dueDate = null,
            note = "Lunch",
            syncWithWallet = false,
        )

        assertThat(viewModel.uiState.value.isSubmitting).isTrue()
        coVerify(exactly = 1) { createDebtUseCase(any(), any(), any()) }

        deferred.complete(Result.success(101L))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSubmitting).isFalse()
        assertThat(events).containsExactly(DebtUiEvent.DebtCreated(101L))
    }

    @Test
    fun `createDebt resets isSubmitting on failure so retry succeeds`() = runTest {
        val viewModel = createViewModel()
        val firstAttempt = CompletableDeferred<Result<Long>>()
        coEvery { createDebtUseCase(any(), any(), any()) } coAnswers {
            firstAttempt.await()
        }

        val sheetFeedbacks = mutableListOf<UiFeedback>()
        val events = mutableListOf<DebtUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.createSheetFeedback.collect { sheetFeedbacks += it }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        advanceUntilIdle()

        viewModel.createDebt(
            personName = "Lan",
            phoneNumber = null,
            type = DebtType.BORROW,
            amount = Money(100_000_00L),
            walletId = null,
            dueDate = null,
            note = "",
            syncWithWallet = false,
        )
        assertThat(viewModel.uiState.value.isSubmitting).isTrue()

        firstAttempt.complete(Result.failure(IllegalStateException("db error")))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSubmitting).isFalse()
        assertThat(sheetFeedbacks).hasSize(1)

        coEvery { createDebtUseCase(any(), any(), any()) } returns Result.success(102L)
        sheetFeedbacks.single().onAction?.invoke()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSubmitting).isFalse()
        assertThat(events).containsExactly(DebtUiEvent.DebtCreated(102L))
    }

    @Test
    fun `recordPayment and deleteDebt ignore concurrent calls and reset isSubmitting on failure`() = runTest {
        val viewModel = createViewModel()
        val debt = com.notepay.domain.model.Debt(
            id = 5L,
            personName = "Hoa",
            type = DebtType.LEND,
            originalAmount = Money(300_000_00L),
            createdAt = kotlin.time.Instant.parse("2026-10-01T00:00:00Z"),
        )
        val paymentDeferred = CompletableDeferred<Result<Long>>()
        coEvery { recordDebtPaymentUseCase(any(), any(), any(), any()) } coAnswers { paymentDeferred.await() }

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.recordPayment(debt, Money(100_000_00L), null, "", false)
        viewModel.recordPayment(debt, Money(100_000_00L), null, "", false)

        assertThat(viewModel.uiState.value.isSubmitting).isTrue()
        coVerify(exactly = 1) { recordDebtPaymentUseCase(any(), any(), any(), any()) }

        paymentDeferred.complete(Result.failure(RuntimeException("payment fail")))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isSubmitting).isFalse()

        val deleteDeferred = CompletableDeferred<Result<Unit>>()
        coEvery { deleteDebtUseCase(5L) } coAnswers { deleteDeferred.await() }

        viewModel.deleteDebt(5L)
        viewModel.deleteDebt(5L)

        assertThat(viewModel.uiState.value.isSubmitting).isTrue()
        coVerify(exactly = 1) { deleteDebtUseCase(5L) }

        deleteDeferred.complete(Result.failure(RuntimeException("delete fail")))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isSubmitting).isFalse()
    }

    @Test
    fun `restoreSettled failure emits error with retry action and retries markSettled`() = runTest {
        val viewModel = createViewModel()
        coEvery { debtRepository.markSettled(5L, true) } returns Unit
        coEvery { debtRepository.markSettled(5L, false) } throws IllegalStateException("undo failed") andThen Unit

        val feedbacks = mutableListOf<UiFeedback>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks += it }
        }

        viewModel.toggleSettled(debtId = 5L, currentSettled = false)
        advanceUntilIdle()

        assertThat(feedbacks).hasSize(1)
        val undoFeedback = feedbacks.single()
        assertThat(undoFeedback.actionLabel).isEqualTo("Undo")

        undoFeedback.onAction?.invoke()
        advanceUntilIdle()

        assertThat(feedbacks).hasSize(2)
        val restoreError = feedbacks.last()
        assertThat(restoreError.type).isEqualTo(FeedbackType.Error)
        assertThat(restoreError.actionLabel).isEqualTo("Retry")
        assertThat(restoreError.message).isEqualTo("Settled update failed")

        restoreError.onAction?.invoke()
        advanceUntilIdle()

        coVerify(exactly = 2) { debtRepository.markSettled(5L, false) }
    }
}
