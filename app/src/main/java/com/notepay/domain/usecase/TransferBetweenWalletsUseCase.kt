package com.notepay.domain.usecase

import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import com.notepay.domain.repository.TransactionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant
import javax.inject.Inject

/**
 * Chuyển tiền giữa hai ví của cùng người dùng.
 *
 * Việc ghi cặp giao dịch nội bộ phải nằm trong cùng một transaction của database: nếu ví nguồn
 * bị trừ mà ví đích không được cộng thì người dùng mất tiền. Repository đảm bảo tính nguyên tử,
 * use case này chịu trách nhiệm kiểm tra ràng buộc và dựng đúng hai bản ghi.
 *
 * [outgoingNote]/[incomingNote] do tầng hiển thị dịch sẵn (có tên ví), domain không phụ thuộc
 * Android resource.
 */
class TransferBetweenWalletsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(
        fromWalletId: Long,
        toWalletId: Long,
        amount: Money,
        outgoingNote: String,
        incomingNote: String,
        occurredAt: Instant = Clock.System.now(),
    ): Result<Unit> = try {
        require(fromWalletId != toWalletId) { "Cannot transfer to the same wallet" }
        require(amount.amountInCents > 0L) { "Transfer amount must be positive" }
        require(fromWalletId > 0L && toWalletId > 0L) { "Wallet IDs must be positive" }

        withContext(ioDispatcher) {
            val groupId = UUID.randomUUID().toString()
            transactionRepository.upsertTransferPair(
                outgoing = Transaction(
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    category = Category.OTHER,
                    note = outgoingNote,
                    occurredAt = occurredAt,
                    walletId = fromWalletId,
                    isInternalTransfer = true,
                    transferGroupId = groupId,
                ),
                incoming = Transaction(
                    amount = amount,
                    type = TransactionType.INCOME,
                    category = Category.OTHER,
                    note = incomingNote,
                    occurredAt = occurredAt,
                    walletId = toWalletId,
                    isInternalTransfer = true,
                    transferGroupId = groupId,
                ),
            )
            Result.success(Unit)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
