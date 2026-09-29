package com.notepay.domain.model.debt

import com.notepay.domain.model.Money
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class DebtType {
    LEND,   // Cho vay (Người khác nợ tôi - Khoản cần thu)
    BORROW  // Đi vay (Tôi nợ người khác - Khoản cần trả)
}

data class Debt(
    val id: Long = 0L,
    val personName: String,
    val phoneNumber: String? = null,
    val type: DebtType,
    val originalAmount: Money,
    val walletId: Long? = null,
    val createdAt: Instant,
    val dueDate: Instant? = null,
    val note: String = "",
    val isSettled: Boolean = false,
) {
    init {
        require(personName.isNotBlank()) { "Person name must not be blank" }
        require(originalAmount.amountInCents > 0L) { "Debt amount must be positive" }
    }
}

data class DebtPayment(
    val id: Long = 0L,
    val debtId: Long,
    val amount: Money,
    val walletId: Long? = null,
    val paidAt: Instant,
    val note: String = "",
    val transactionId: Long? = null,
) {
    init {
        require(amount.amountInCents > 0L) { "Repayment amount must be positive" }
    }
}

data class DebtWithHistory(
    val debt: Debt,
    val payments: List<DebtPayment> = emptyList(),
) {
    val totalPaid: Money = Money(payments.sumOf { it.amount.amountInCents })

    val remainingAmount: Money = Money(
        maxOf(0L, debt.originalAmount.amountInCents - totalPaid.amountInCents)
    )

    val isFullyPaid: Boolean = remainingAmount.amountInCents <= 0L || debt.isSettled

    val progressRatio: Float = if (debt.originalAmount.amountInCents > 0L) {
        (totalPaid.amountInCents.toFloat() / debt.originalAmount.amountInCents.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }

    fun isOverdue(now: Instant = Clock.System.now()): Boolean {
        if (isFullyPaid) return false
        val due = debt.dueDate ?: return false
        return now > due
    }

    fun isDueToday(now: Instant = Clock.System.now(), timeZone: TimeZone = TimeZone.currentSystemDefault()): Boolean {
        if (isFullyPaid) return false
        val due = debt.dueDate ?: return false
        val nowDate = now.toLocalDateTime(timeZone).date
        val dueDate = due.toLocalDateTime(timeZone).date
        return nowDate == dueDate
    }
}
