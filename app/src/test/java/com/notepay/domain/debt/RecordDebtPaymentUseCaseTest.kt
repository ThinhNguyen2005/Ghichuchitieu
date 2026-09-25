package com.notepay.domain.debt

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.debt.Debt
import com.notepay.domain.model.debt.DebtPayment
import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.repository.DebtRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.debt.RecordDebtPaymentUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class RecordDebtPaymentUseCaseTest {

    private val debtRepo = mockk<DebtRepository>()
    private val addTransactionUseCase = mockk<AddTransactionUseCase>()
    private val dispatcher = StandardTestDispatcher()
    private val useCase = RecordDebtPaymentUseCase(debtRepo, addTransactionUseCase, dispatcher)

    @Test
    fun `repay lend debt with wallet sync creates income transaction`() = runTest(dispatcher) {
        val debt = Debt(
            id = 10L,
            personName = "Minh Tri",
            phoneNumber = null,
            type = DebtType.LEND,
            originalAmount = Money(1_000_000_00L),
            walletId = 1L,
            createdAt = Clock.System.now(),
            dueDate = null,
            note = "",
            isSettled = false,
        )

        val payment = DebtPayment(
            id = 0L,
            debtId = 10L,
            amount = Money(400_000_00L),
            walletId = 1L,
            paidAt = Clock.System.now(),
            note = "Tra dot 1",
        )

        val txSlot = slot<Transaction>()
        coEvery { addTransactionUseCase(capture(txSlot)) } returns Result.success(99L)
        coEvery { debtRepo.recordPayment(any()) } returns 201L

        val result = useCase(debt, payment, syncWithWallet = true)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(201L)

        val tx = txSlot.captured
        assertThat(tx.type).isEqualTo(TransactionType.INCOME)
        assertThat(tx.category).isEqualTo(Category.INCOME_OTHER)
        assertThat(tx.amount).isEqualTo(Money(400_000_00L))
        assertThat(tx.note).contains("Minh Tri")

        coVerify(exactly = 1) {
            debtRepo.recordPayment(match { it.transactionId == 99L && it.amount == Money(400_000_00L) })
        }
    }

    @Test
    fun `repay borrow debt with wallet sync creates expense transaction`() = runTest(dispatcher) {
        val debt = Debt(
            id = 20L,
            personName = "Anh Hoang",
            phoneNumber = null,
            type = DebtType.BORROW,
            originalAmount = Money(5_000_000_00L),
            walletId = 2L,
            createdAt = Clock.System.now(),
            dueDate = null,
            note = "",
            isSettled = false,
        )

        val payment = DebtPayment(
            id = 0L,
            debtId = 20L,
            amount = Money(2_500_000_00L),
            walletId = 2L,
            paidAt = Clock.System.now(),
            note = "Tra nua tien",
        )

        val txSlot = slot<Transaction>()
        coEvery { addTransactionUseCase(capture(txSlot)) } returns Result.success(100L)
        coEvery { debtRepo.recordPayment(any()) } returns 202L

        val result = useCase(debt, payment, syncWithWallet = true)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(202L)

        val tx = txSlot.captured
        assertThat(tx.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(tx.category).isEqualTo(Category.BILL)
        assertThat(tx.amount).isEqualTo(Money(2_500_000_00L))
        assertThat(tx.note).contains("Anh Hoang")
    }

    @Test
    fun `payment without wallet sync records payment directly without transaction`() = runTest(dispatcher) {
        val debt = Debt(
            id = 30L,
            personName = "Quang Huy",
            phoneNumber = null,
            type = DebtType.LEND,
            originalAmount = Money(200_000_00L),
            walletId = null,
            createdAt = Clock.System.now(),
            dueDate = null,
            note = "",
            isSettled = false,
        )

        val payment = DebtPayment(
            id = 0L,
            debtId = 30L,
            amount = Money(200_000_00L),
            walletId = null,
            paidAt = Clock.System.now(),
            note = "Tien mat trao tay",
        )

        coEvery { debtRepo.recordPayment(any()) } returns 203L

        val result = useCase(debt, payment, syncWithWallet = false)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(203L)
        coVerify(exactly = 0) { addTransactionUseCase(any()) }
        coVerify(exactly = 1) { debtRepo.recordPayment(payment) }
    }

    @Test
    fun `preserves CancellationException`() = runTest(dispatcher) {
        val debt = Debt(
            id = 40L,
            personName = "Duc",
            phoneNumber = null,
            type = DebtType.LEND,
            originalAmount = Money(50_000_00L),
            walletId = null,
            createdAt = Clock.System.now(),
            dueDate = null,
            note = "",
            isSettled = false,
        )

        val payment = DebtPayment(
            id = 0L,
            debtId = 40L,
            amount = Money(50_000_00L),
            walletId = null,
            paidAt = Clock.System.now(),
            note = "",
        )

        coEvery { debtRepo.recordPayment(any()) } throws CancellationException("cancelled")

        assertFailsWith<CancellationException> {
            useCase(debt, payment, syncWithWallet = false)
        }
    }
}
