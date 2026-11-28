package com.notepay.domain.usecase

import com.notepay.core.platform.notification.BudgetAlertNotifier
import com.notepay.data.sync.SharedWalletSyncCoordinator
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Thêm (hoặc cập nhật) một giao dịch.
 *
 * Quy tắc nghiệp vụ:
 *  - Ví phải tồn tại trong DB
 *  - Nếu là Ví chung, kiểm tra phân quyền trước khi ghi và xếp hàng vào SyncOutbox
 *  - Validation đã được enforce ở Transaction.init
 *  - Mọi thao tác DB chạy trên IoDispatcher
 *
 * Trả về Result<Long>:
 *  - success: row id của transaction vừa lưu
 *  - failure: lỗi nghiệp vụ (không tìm thấy ví, thiếu quyền, …)
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val walletRepo: WalletRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val budgetAlertNotifier: BudgetAlertNotifier?,
    private val sharedWalletCoordinator: SharedWalletSyncCoordinator?,
) {
    internal constructor(
        transactionRepo: TransactionRepository,
        walletRepo: WalletRepository,
        dispatcher: CoroutineDispatcher,
        budgetAlertNotifier: BudgetAlertNotifier? = null,
    ) : this(
        transactionRepo = transactionRepo,
        walletRepo = walletRepo,
        dispatcher = dispatcher,
        budgetAlertNotifier = budgetAlertNotifier,
        sharedWalletCoordinator = null,
    )

    suspend operator fun invoke(transaction: Transaction): Result<Long> = try {
        Result.success(
            withContext(dispatcher) {
                val wallet = walletRepo.getById(transaction.walletId)
                    ?: error("Wallet ${transaction.walletId} not found")
                val existing = if (transaction.id != 0L && wallet.isShared) {
                    transactionRepo.getById(transaction.id)
                } else {
                    null
                }
                val prepared = sharedWalletCoordinator?.prepareTransactionForUpsert(
                    wallet = wallet,
                    incoming = transaction,
                    existing = existing,
                ) ?: transaction

                val id = if (wallet.isShared && sharedWalletCoordinator != null) {
                    sharedWalletCoordinator.saveSharedTransaction(wallet) {
                        val insertedId = transactionRepo.upsert(prepared)
                        prepared.id.takeIf { it > 0L } ?: insertedId
                    }
                } else {
                    transactionRepo.upsert(prepared)
                }
                if (prepared.type == TransactionType.EXPENSE) {
                    try {
                        budgetAlertNotifier?.checkAndNotify(prepared.walletId)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Budget alerts run after the transaction commit.
                    }
                }
                id
            },
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
