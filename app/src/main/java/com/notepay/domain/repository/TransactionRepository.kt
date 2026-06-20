package com.notepay.domain.repository

import com.notepay.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Interface thuần Kotlin — domain layer không phụ thuộc Room.
 * Implementation nằm ở data layer.
 */
interface TransactionRepository {
    /** Observe tất cả giao dịch, mới nhất trước. */
    fun observeAll(): Flow<List<Transaction>>

    /** Observe giao dịch trong 1 tháng cụ thể (1-indexed). */
    fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>>

    /** Observe tổng hợp theo ví, real-time. */
    fun observeByWallet(walletId: Long): Flow<List<Transaction>>

    suspend fun getById(id: Long): Transaction?

    /** Insert hoặc update. Trả về row id. */
    suspend fun upsert(transaction: Transaction): Long

    suspend fun delete(id: Long)

    /**
     * Tìm giao dịch có note chứa [noteKeyword] trong khoảng [fromMillis]..[toMillis].
     * Dùng để phát hiện hóa đơn định kỳ (Case 6).
     */
    suspend fun findRecentSimilar(noteKeyword: String, fromMillis: Long, toMillis: Long): List<Transaction>
}
