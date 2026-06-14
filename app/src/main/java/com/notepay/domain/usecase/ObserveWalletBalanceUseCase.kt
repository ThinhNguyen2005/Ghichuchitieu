package com.notepay.domain.usecase

import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Tính số dư hiện tại của một ví:
 *   initialBalance + sum(income.amount) - sum(expense.amount)
 *
 * Trả về Flow<Money> để UI observe real-time khi có transaction mới.
 */
class ObserveWalletBalanceUseCase @Inject constructor(
    private val walletRepo: WalletRepository,
    private val transactionRepo: TransactionRepository,
) {
    operator fun invoke(walletId: Long): Flow<Money> = combine(
        walletRepo.observeAll(),
        transactionRepo.observeByWallet(walletId),
    ) { wallets, txs ->
        val wallet = wallets.firstOrNull { it.id == walletId }
            ?: return@combine Money.ZERO
        val income = txs.filter { it.type == TransactionType.INCOME }
            .fold(Money.ZERO) { acc, t -> acc + t.amount }
        val expense = txs.filter { it.type == TransactionType.EXPENSE }
            .fold(Money.ZERO) { acc, t -> acc + t.amount }
        wallet.initialBalance + income - expense
    }
}
