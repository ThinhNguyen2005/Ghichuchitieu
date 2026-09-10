package com.notepay.domain.billsplit

/** Business Pipeline Use Case for Non-Destructive Reconciliation */
interface ReconcileBillSplitUseCase {
    suspend fun markAsPaidWithReconciliation(
        splitId: Long,
        incomeTransactionId: Long?
    ): Result<BillSplitReconciliationResult>

    suspend fun markAsPaidDirect(splitId: Long): Result<BillSplitItem>
}
