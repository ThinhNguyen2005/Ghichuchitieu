package com.notepay.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.BillSplit
import com.notepay.domain.money.Money
import com.notepay.domain.repository.BillSplitRepository
import com.notepay.domain.usecase.billsplit.SettleBillSplitsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Instant

class SettleBillSplitsUseCaseTest {

    private val billSplitRepository = mockk<BillSplitRepository>()
    private val useCase = SettleBillSplitsUseCase(billSplitRepository)
    private val paidAt = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    private fun split(id: Long, isPaid: Boolean = false) = BillSplit(
        id = id,
        transactionId = 7L,
        debtorName = "Lan",
        amount = Money(120_000_00L),
        isPaid = isPaid,
        memoCode = "SPLIT-$id",
        paidAt = null,
        createdAt = Clock.System.now(),
    )

    @Test
    fun `settles only unpaid splits with a single shared timestamp`() = runTest {
        coEvery { billSplitRepository.getById(1L) } returns split(1L)
        coEvery { billSplitRepository.getById(2L) } returns split(2L, isPaid = true)
        coEvery { billSplitRepository.getById(3L) } returns split(3L)
        coEvery { billSplitRepository.markAsPaid(any(), any()) } returns Unit

        val result = useCase(listOf(1L, 2L, 3L), paidAt = paidAt)

        assertThat(result.getOrNull()).isEqualTo(2)
        coVerify(exactly = 1) { billSplitRepository.markAsPaid(1L, paidAt) }
        coVerify(exactly = 1) { billSplitRepository.markAsPaid(3L, paidAt) }
        coVerify(exactly = 0) { billSplitRepository.markAsPaid(2L, any()) }
    }

    @Test
    fun `ignores duplicate and missing split ids`() = runTest {
        coEvery { billSplitRepository.getById(5L) } returns split(5L)
        coEvery { billSplitRepository.getById(6L) } returns null
        coEvery { billSplitRepository.markAsPaid(any(), any()) } returns Unit

        val result = useCase(listOf(5L, 5L, 6L), paidAt = paidAt)

        assertThat(result.getOrNull()).isEqualTo(1)
        coVerify(exactly = 1) { billSplitRepository.markAsPaid(5L, paidAt) }
        coVerify(exactly = 0) { billSplitRepository.markAsPaid(6L, any()) }
    }

    @Test
    fun `reports zero when every split is already paid`() = runTest {
        coEvery { billSplitRepository.getById(9L) } returns split(9L, isPaid = true)

        val result = useCase(listOf(9L), paidAt = paidAt)

        assertThat(result.getOrNull()).isEqualTo(0)
        coVerify(exactly = 0) { billSplitRepository.markAsPaid(any(), any()) }
    }

    @Test
    fun `preserves CancellationException`() = runTest {
        coEvery { billSplitRepository.getById(1L) } returns split(1L)
        coEvery { billSplitRepository.markAsPaid(any(), any()) } throws CancellationException("cancelled")

        assertFailsWith<CancellationException> {
            useCase(listOf(1L), paidAt = paidAt)
        }
    }
}
