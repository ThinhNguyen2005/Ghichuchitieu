package com.notepay.data.local.model

/**
 * Kết quả tổng hợp thu/chi theo ví, được SQLite tính sẵn (GROUP BY + SUM).
 * Không bao giờ nạp toàn bộ bảng transactions lên RAM để cộng tay.
 */
data class WalletStatsProjection(
    val walletId: Long,
    val allTimeIncomeCents: Long,
    val allTimeExpenseCents: Long,
    val monthIncomeCents: Long,
    val monthExpenseCents: Long,
    val txCount: Int,
)
