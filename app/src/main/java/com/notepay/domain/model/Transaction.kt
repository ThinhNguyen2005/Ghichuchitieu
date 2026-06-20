package com.notepay.domain.model

import kotlin.time.Clock
import kotlinx.datetime.Instant

/**
 * Domain model cho một giao dịch thu/chi.
 *
 * Validation tại init:
 *  - amount phải khác 0
 *  - note tối đa 200 ký tự
 *  - walletId phải > 0 (khi đã được lưu DB)
 *
 * id = 0 nghĩa là chưa lưu (sẽ được auto-generate bởi Room).
 */
data class Transaction(
    val id: Long = 0L,
    val amount: Money,
    val type: TransactionType,
    val category: Category,
    val note: String,
    val occurredAt: Instant,
    val walletId: Long,
    val isAutoCapture: Boolean = false,
    /** Đánh dấu giao dịch chuyển khoản nội bộ (vd: TPBank → MoMo).
     *  Khi true, giao dịch được lưu nhưng không tính vào biểu đồ Stats để tránh tính trùng. */
    val isInternalTransfer: Boolean = false,
    val createdAt: Instant = Clock.System.now(),
) {
    init {
        require(amount.amountInCents >= 0L) { "Transaction amount must be positive or zero" }
        require(note.length <= MAX_NOTE_LENGTH) {
            "Note too long: ${note.length} > $MAX_NOTE_LENGTH"
        }
        require(walletId > 0L) { "walletId must be positive, got $walletId" }
    }

    companion object {
        const val MAX_NOTE_LENGTH = 200
    }
}
