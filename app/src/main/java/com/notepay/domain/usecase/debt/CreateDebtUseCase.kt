package com.notepay.domain.usecase.debt

import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Debt
import com.notepay.domain.model.DebtType
import com.notepay.domain.repository.DebtRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import javax.inject.Inject

class CreateDebtUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * @param transactionNote ghi chú cho giao dịch ví, do tầng hiển thị dịch sẵn vì domain
     * không giữ chuỗi hiển thị. Chỉ dùng khi [syncWithWallet] bật.
     */
    suspend operator fun invoke(
        debt: Debt,
        syncWithWallet: Boolean = false,
        transactionNote: String = "",
    ): Result<Long> = try {
        withContext(ioDispatcher) {
            val debtId = debtRepository.upsertDebt(debt)

            if (syncWithWallet && debt.walletId != null && debt.walletId > 0L) {
                val isLend = debt.type == DebtType.LEND
                val txType = if (isLend) TransactionType.EXPENSE else TransactionType.INCOME
                val category = if (isLend) Category.BILL else Category.INCOME_OTHER

                val transaction = Transaction(
                    id = 0L,
                    amount = debt.originalAmount,
                    type = txType,
                    category = category,
                    note = transactionNote,
                    occurredAt = debt.createdAt,
                    walletId = debt.walletId,
                    createdAt = Clock.System.now(),
                )
                addTransactionUseCase(transaction)
            }
            Result.success(debtId)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
