package com.notepay.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteTransactionUseCaseTest {

    private val transactionRepo = mockk<TransactionRepository>()
    private val dispatcher = StandardTestDispatcher()
    private val useCase = DeleteTransactionUseCase(transactionRepo, dispatcher)

    @Test
    fun `successful delete returns success`() = runTest(dispatcher) {
        coEvery { transactionRepo.delete(1L) } returns Unit

        val result = useCase(1L)

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { transactionRepo.delete(1L) }
    }

    @Test
    fun `repository exception returns failure`() = runTest(dispatcher) {
        coEvery { transactionRepo.delete(1L) } throws RuntimeException("not found")

        val result = useCase(1L)

        assertThat(result.isFailure).isTrue()
    }
}
