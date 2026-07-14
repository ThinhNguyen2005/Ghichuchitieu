package com.notepay.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.TestData
import com.notepay.domain.model.Money
import com.notepay.domain.model.TestTransactionFactory
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionUseCaseTest {

    private val transactionRepo = mockk<TransactionRepository>()
    private val walletRepo = mockk<WalletRepository>()
    private val dispatcher = StandardTestDispatcher()
    private val useCase = AddTransactionUseCase(transactionRepo, walletRepo, dispatcher)

    @Test
    fun `invoke with valid transaction returns success with id`() = runTest(dispatcher) {
        val wallet = TestData.wallet(id = 1L)
        coEvery { walletRepo.getById(1L) } returns wallet
        coEvery { transactionRepo.upsert(any()) } returns 42L

        val result = useCase(TestTransactionFactory.expense(walletId = 1L))

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(42L)
        coVerify(exactly = 1) { transactionRepo.upsert(any()) }
    }

    @Test
    fun `invoke with non-existent wallet returns failure and skips upsert`() =
        runTest(dispatcher) {
            coEvery { walletRepo.getById(99L) } returns null

            val result = useCase(TestTransactionFactory.expense(walletId = 99L))

            assertThat(result.isFailure).isTrue()
            coVerify(exactly = 0) { transactionRepo.upsert(any()) }
        }

    @Test
    fun `invoke when repository throws returns failure`() = runTest(dispatcher) {
        val wallet = TestData.wallet()
        coEvery { walletRepo.getById(1L) } returns wallet
        coEvery { transactionRepo.upsert(any()) } throws RuntimeException("db locked")

        val result = useCase(TestTransactionFactory.expense())

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).hasMessageThat().contains("db locked")
    }

    @Test
    fun `invoke does not swallow coroutine cancellation`() = runTest(dispatcher) {
        coEvery { walletRepo.getById(1L) } throws CancellationException("capture disabled")

        assertFailsWith<CancellationException> {
            useCase(TestTransactionFactory.expense())
        }

        coVerify(exactly = 0) { transactionRepo.upsert(any()) }
    }

    @Test
    fun `invoke runs on injected dispatcher`() = runTest(dispatcher) {
        val wallet = TestData.wallet()
        coEvery { walletRepo.getById(1L) } returns wallet
        coEvery { transactionRepo.upsert(any()) } returns 1L

        useCase(TestTransactionFactory.expense())

        coVerify(exactly = 1) { transactionRepo.upsert(any()) }
    }
}
