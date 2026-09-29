package com.notepay.domain.debt

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.debt.Debt
import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.repository.DebtRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.debt.CreateDebtUseCase
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
class CreateDebtUseCaseTest {

    private val debtRepo = mockk<DebtRepository>()
    private val addTransactionUseCase = mockk<AddTransactionUseCase>()
    private val dispatcher = StandardTestDispatcher()
    private val useCase = CreateDebtUseCase(debtRepo, addTransactionUseCase, dispatcher)

    @Test
    fun `create lend debt without wallet sync inserts debt only`() = runTest(dispatcher) {
        val debt = Debt(
            id = 0L,
            personName = "Nguyen Van A",
            phoneNumber = "0901234567",
            type = DebtType.LEND,
            originalAmount = Money(500_000_00L),
            walletId = 1L,
            createdAt = Clock.System.now(),
            dueDate = null,
            note = "Cho muon tien cafe",
            isSettled = false,
        )

        coEvery { debtRepo.upsertDebt(any()) } returns 101L

        val result = useCase(debt, syncWithWallet = false)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(101L)
        coVerify(exactly = 1) { debtRepo.upsertDebt(debt) }
        coVerify(exactly = 0) { addTransactionUseCase(any()) }
    }

    @Test
    fun `create lend debt with wallet sync creates expense transaction`() = runTest(dispatcher) {
        val debt = Debt(
            id = 0L,
            personName = "Tran Thi B",
            phoneNumber = null,
            type = DebtType.LEND,
            originalAmount = Money(1_000_000_00L),
            walletId = 2L,
            createdAt = Clock.System.now(),
            dueDate = null,
            note = "Vay tien mua sach",
            isSettled = false,
        )

        val txSlot = slot<Transaction>()
        coEvery { addTransactionUseCase(capture(txSlot)) } returns Result.success(55L)
        coEvery { debtRepo.upsertDebt(any()) } returns 102L

        val result = useCase(debt, syncWithWallet = true)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(102L)
        coVerify(exactly = 1) { addTransactionUseCase(any()) }

        val createdTx = txSlot.captured
        assertThat(createdTx.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(createdTx.category).isEqualTo(Category.BILL)
        assertThat(createdTx.walletId).isEqualTo(2L)
        assertThat(createdTx.amount).isEqualTo(Money(1_000_000_00L))
        assertThat(createdTx.note).contains("Tran Thi B")
    }

    @Test
    fun `create borrow debt with wallet sync creates income transaction`() = runTest(dispatcher) {
        val debt = Debt(
            id = 0L,
            personName = "Le Van C",
            phoneNumber = null,
            type = DebtType.BORROW,
            originalAmount = Money(2_000_000_00L),
            walletId = 3L,
            createdAt = Clock.System.now(),
            dueDate = null,
            note = "Muon tien sua xe",
            isSettled = false,
        )

        val txSlot = slot<Transaction>()
        coEvery { addTransactionUseCase(capture(txSlot)) } returns Result.success(56L)
        coEvery { debtRepo.upsertDebt(any()) } returns 103L

        val result = useCase(debt, syncWithWallet = true)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(103L)

        val createdTx = txSlot.captured
        assertThat(createdTx.type).isEqualTo(TransactionType.INCOME)
        assertThat(createdTx.category).isEqualTo(Category.INCOME_OTHER)
        assertThat(createdTx.walletId).isEqualTo(3L)
        assertThat(createdTx.amount).isEqualTo(Money(2_000_000_00L))
        assertThat(createdTx.note).contains("Le Van C")
    }

    @Test
    fun `preserves CancellationException when cancelled`() = runTest(dispatcher) {
        val debt = Debt(
            id = 0L,
            personName = "Pham D",
            phoneNumber = null,
            type = DebtType.LEND,
            originalAmount = Money(100_000_00L),
            walletId = null,
            createdAt = Clock.System.now(),
            dueDate = null,
            note = "",
            isSettled = false,
        )

        coEvery { debtRepo.upsertDebt(any()) } throws CancellationException("coroutine cancelled")

        assertFailsWith<CancellationException> {
            useCase(debt, syncWithWallet = false)
        }
    }
}
