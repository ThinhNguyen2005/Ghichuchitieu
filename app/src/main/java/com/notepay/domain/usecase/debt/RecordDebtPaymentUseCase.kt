package com.notepay.domain.usecase.debt

import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.debt.Debt
import com.notepay.domain.model.debt.DebtPayment
import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.repository.DebtRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import javax.inject.Inject

class RecordDebtPaymentUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(
        debt: Debt,
        payment: DebtPayment,
        syncWithWallet: Boolean = false,
    ): Result<Long> = try {
        withContext(ioDispatcher) {
            var createdTxId: Long? = null

            if (syncWithWallet && payment.walletId != null && payment.walletId > 0L) {
                val isLend = debt.type == DebtType.LEND
                val txType = if (isLend) TransactionType.INCOME else TransactionType.EXPENSE
                val category = if (isLend) Category.INCOME_OTHER else Category.BILL
                val actionPrefix = if (isLend) "Thu nợ từ" else "Trả nợ cho"
                val note = if (payment.note.isNotBlank()) {
                    "$actionPrefix ${debt.personName} (${payment.note})"
                } else {
                    "$actionPrefix ${debt.personName}"
                }

                val transaction = Transaction(
                    id = 0L,
                    amount = payment.amount,
                    type = txType,
                    category = category,
                    note = note,
                    occurredAt = payment.paidAt,
                    walletId = payment.walletId,
                    createdAt = Clock.System.now(),
                )
                val txResult = addTransactionUseCase(transaction)
                createdTxId = txResult.getOrNull()
            }

            val paymentToSave = if (createdTxId != null) {
                payment.copy(transactionId = createdTxId)
            } else {
                payment
            }

            val paymentId = debtRepository.recordPayment(paymentToSave)
            Result.success(paymentId)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
