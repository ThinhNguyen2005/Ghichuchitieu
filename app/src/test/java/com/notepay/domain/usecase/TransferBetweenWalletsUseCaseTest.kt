package com.notepay.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import com.notepay.domain.repository.TransactionRepository
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
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TransferBetweenWalletsUseCaseTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val dispatcher = StandardTestDispatcher()
    private val useCase = TransferBetweenWalletsUseCase(transactionRepository, dispatcher)
    private val occurredAt = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    @Test
    fun `writes an internal expense and income pair for the selected wallets`() = runTest(dispatcher) {
        val outgoingSlot = slot<Transaction>()
        val incomingSlot = slot<Transaction>()
        coEvery {
            transactionRepository.upsertTransferPair(capture(outgoingSlot), capture(incomingSlot))
        } returns Unit

        val result = useCase(
            fromWalletId = 1L,
            toWalletId = 2L,
            amount = Money(500_000_00L),
            outgoingNote = "Chuyển đến MoMo",
            incomingNote = "Nhận từ TPBank",
            occurredAt = occurredAt,
        )

        assertThat(result.isSuccess).isTrue()

        val outgoing = outgoingSlot.captured
        assertThat(outgoing.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(outgoing.category).isEqualTo(Category.OTHER)
        assertThat(outgoing.walletId).isEqualTo(1L)
        assertThat(outgoing.amount).isEqualTo(Money(500_000_00L))
        assertThat(outgoing.note).isEqualTo("Chuyển đến MoMo")
        assertThat(outgoing.isInternalTransfer).isTrue()
        assertThat(outgoing.occurredAt).isEqualTo(occurredAt)

        val incoming = incomingSlot.captured
        assertThat(incoming.type).isEqualTo(TransactionType.INCOME)
        assertThat(incoming.category).isEqualTo(Category.OTHER)
        assertThat(incoming.walletId).isEqualTo(2L)
        assertThat(incoming.amount).isEqualTo(Money(500_000_00L))
        assertThat(incoming.note).isEqualTo("Nhận từ TPBank")
        assertThat(incoming.isInternalTransfer).isTrue()
        assertThat(incoming.occurredAt).isEqualTo(occurredAt)
    }

    @Test
    fun `rejects a transfer into the same wallet without writing anything`() = runTest(dispatcher) {
        val result = useCase(
            fromWalletId = 3L,
            toWalletId = 3L,
            amount = Money(100_000_00L),
            outgoingNote = "out",
            incomingNote = "in",
            occurredAt = occurredAt,
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        coVerify(exactly = 0) { transactionRepository.upsertTransferPair(any(), any()) }
    }

    @Test
    fun `rejects a non positive amount without writing anything`() = runTest(dispatcher) {
        val result = useCase(
            fromWalletId = 1L,
            toWalletId = 2L,
            amount = Money(0L),
            outgoingNote = "out",
            incomingNote = "in",
            occurredAt = occurredAt,
        )

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { transactionRepository.upsertTransferPair(any(), any()) }
    }

    @Test
    fun `preserves CancellationException`() = runTest(dispatcher) {
        coEvery {
            transactionRepository.upsertTransferPair(any(), any())
        } throws CancellationException("cancelled")

        assertFailsWith<CancellationException> {
            useCase(
                fromWalletId = 1L,
                toWalletId = 2L,
                amount = Money(100_000_00L),
                outgoingNote = "out",
                incomingNote = "in",
                occurredAt = occurredAt,
            )
        }
    }
}
