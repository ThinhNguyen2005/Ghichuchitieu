package com.notepay.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.notepay.data.local.entity.TransactionEntity
import com.notepay.data.local.model.WalletStatsProjection
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()


    @Query("SELECT * FROM transactions ORDER BY occurred_at DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE occurred_at BETWEEN :startMillis AND :endMillis
        ORDER BY occurred_at DESC
    """)
    fun observeByRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE wallet_id = :walletId ORDER BY occurred_at DESC")
    fun observeByWallet(walletId: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE wallet_id = :walletId
        AND occurred_at BETWEEN :startMillis AND :endMillis
        ORDER BY occurred_at DESC
    """)
    fun observeByWalletAndRange(walletId: Long, startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Upsert
    suspend fun upsert(entity: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Tìm giao dịch có note chứa chuỗi [noteKeyword] trong khoảng thời gian chỉ định.
     * Dùng cho phát hiện hóa đơn định kỳ (Case 6: Smart Subscription Detection).
     */
    @Query("""
        SELECT * FROM transactions
        WHERE note LIKE '%' || :noteKeyword || '%'
        AND occurred_at BETWEEN :startMillis AND :endMillis
        ORDER BY occurred_at DESC
        LIMIT 5
    """)
    suspend fun findRecentSimilar(
        noteKeyword: String,
        startMillis: Long,
        endMillis: Long
    ): List<TransactionEntity>

    /** Returns only occurred_at timestamps for streak calculation (lightweight). */
    @Query("SELECT occurred_at FROM transactions ORDER BY occurred_at DESC")
    fun observeAllOccurredDates(): Flow<List<Long>>

    /** Transactions since [sinceMillis] excluding internal transfers (for charts). */
    @Query("""
        SELECT * FROM transactions
        WHERE occurred_at >= :sinceMillis
        AND is_internal_transfer = 0
        ORDER BY occurred_at ASC
    """)
    fun observeRecentNonTransfers(sinceMillis: Long): Flow<List<TransactionEntity>>

    /**
     * Tổng hợp thu/chi theo từng ví do SQLite tính (GROUP BY + SUM).
     * Trả về đúng một dòng/ví thay vì nạp toàn bộ giao dịch lên RAM để cộng tay.
     */
    @Query("""
        SELECT wallet_id AS walletId,
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount_cents ELSE 0 END), 0) AS allTimeIncomeCents,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount_cents ELSE 0 END), 0) AS allTimeExpenseCents,
            COALESCE(SUM(CASE WHEN type = 'INCOME' AND occurred_at BETWEEN :monthStartMillis AND :monthEndMillis
                THEN amount_cents ELSE 0 END), 0) AS monthIncomeCents,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' AND occurred_at BETWEEN :monthStartMillis AND :monthEndMillis
                THEN amount_cents ELSE 0 END), 0) AS monthExpenseCents,
            COUNT(*) AS txCount
        FROM transactions
        GROUP BY wallet_id
    """)
    fun observeWalletStats(
        monthStartMillis: Long,
        monthEndMillis: Long,
    ): Flow<List<WalletStatsProjection>>
}
