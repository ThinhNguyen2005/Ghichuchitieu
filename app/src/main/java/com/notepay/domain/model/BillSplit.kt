package com.notepay.domain.model

import kotlin.time.Clock
import kotlinx.datetime.Instant

data class BillSplit(
    val id: Long = 0L,
    val transactionId: Long,
    val debtorName: String,
    val amount: Money,
    val isPaid: Boolean = false,
    val memoCode: String,
    val paidAt: Instant? = null,
    val createdAt: Instant = Clock.System.now()
) {
    init {
        require(debtorName.isNotBlank()) { "Debtor name must not be blank" }
        require(amount.amountInCents > 0) { "Amount must be positive" }
        require(memoCode.isNotBlank()) { "Memo code must not be blank" }
    }
}
