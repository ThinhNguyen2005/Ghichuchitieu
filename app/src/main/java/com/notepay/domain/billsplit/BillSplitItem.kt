package com.notepay.domain.billsplit

import com.notepay.domain.money.Money
import kotlinx.datetime.Instant

/** Debtor Status */
enum class BillSplitStatus {
    UNPAID,
    PAID_DIRECT,
    PAID_RECONCILED
}

/** Individual Bill Split Record */
data class BillSplitItem(
    val id: Long,
    val parentTransactionId: Long,
    val debtorName: String,
    val amount: Money,
    val memoCode: MemoCode,
    val status: BillSplitStatus,
    val paidAt: Instant? = null,
    val linkedIncomeTransactionId: Long? = null
)

/** Non-destructive Debt Reconciliation Result */
data class BillSplitReconciliationResult(
    val splitItem: BillSplitItem,
    val updatedStatus: BillSplitStatus,
    val parentTransactionUnchanged: Boolean = true,
    val incomeTransactionPreserved: Boolean = true
)
