package com.notepay.domain.usecase

import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Transaction
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.model.TransactionType
import com.notepay.platform.notification.BudgetAlertNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Thêm (hoặc cập nhật) một giao dịch.
 *
 * Quy tắc nghiệp vụ:
 *  - Ví phải tồn tại trong DB
 *  - Validation đã được enforce ở Transaction.init
 *  - Mọi thao tác DB chạy trên IoDispatcher
 *
 * Trả về Result<Long>:
 *  - success: row id của transaction vừa lưu
 *  - failure: lỗi nghiệp vụ (không tìm thấy ví, …)
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val walletRepo: WalletRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val budgetAlertNotifier: BudgetAlertNotifier? = null,
) {
    suspend operator fun invoke(transaction: Transaction): Result<Long> = try {
        Result.success(withContext(dispatcher) {
            val wallet = walletRepo.getById(transaction.walletId)
                ?: error("Wallet ${transaction.walletId} not found")
            val id = transactionRepo.upsert(transaction)
            if (transaction.type == TransactionType.EXPENSE) {
                budgetAlertNotifier?.checkAndNotify(transaction.walletId)
            }
            id
        })
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
