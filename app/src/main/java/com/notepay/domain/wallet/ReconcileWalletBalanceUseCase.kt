package com.notepay.domain.wallet

/** Use Case for Real-Time Wallet Balance Reconciliation */
interface ReconcileWalletBalanceUseCase {
    suspend fun reconcileWalletBalance(walletId: Long): Result<WalletBalanceSnapshot>
    suspend fun calculateMonthlySummary(
        walletId: Long,
        yearMonth: String
    ): Result<WalletCashflowSummary>
}
