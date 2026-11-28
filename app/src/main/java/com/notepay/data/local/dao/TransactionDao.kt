package com.notepay.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.notepay.data.local.entity.TransactionEntity
import com.notepay.data.local.model.WalletStatsProjection
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions")
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE deleted_at_epoch_ms IS NULL")
    suspend fun getAll(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()


    @Query("SELECT * FROM transactions WHERE deleted_at_epoch_ms IS NULL ORDER BY occurred_at DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE occurred_at BETWEEN :startMillis AND :endMillis
        AND deleted_at_epoch_ms IS NULL
        ORDER BY occurred_at DESC
    """)
    fun observeByRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE wallet_id = :walletId AND deleted_at_epoch_ms IS NULL ORDER BY occurred_at DESC")
    fun observeByWallet(walletId: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE wallet_id = :walletId
        AND occurred_at BETWEEN :startMillis AND :endMillis
        AND deleted_at_epoch_ms IS NULL
        ORDER BY occurred_at DESC
    """)
    fun observeByWalletAndRange(walletId: Long, startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE remote_id = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE operation_id = :operationId LIMIT 1")
    suspend fun getByOperationId(operationId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transfer_group_id = :groupId")
    suspend fun getByTransferGroupId(groupId: String): List<TransactionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE wallet_id = :walletId AND deleted_at_epoch_ms IS NULL AND (is_internal_transfer = 1 OR transfer_group_id IS NOT NULL))")
    suspend fun hasInternalTransfer(walletId: Long): Boolean

    @Query("SELECT * FROM transactions WHERE id = :id AND deleted_at_epoch_ms IS NULL LIMIT 1")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Upsert
    suspend fun upsert(entity: TransactionEntity): Long

    @Query(
        """
        UPDATE transactions SET sync_status = 'FAILED'
        WHERE id = :id AND operation_id = :operationId AND updated_at_epoch_ms = :updatedAt
            AND deleted_at_epoch_ms IS NULL AND sync_status = 'PENDING'
        """,
    )
    suspend fun markSyncFailed(id: Long, operationId: String, updatedAt: Long)

    @Query(
        """
        UPDATE transactions
        SET remote_id = COALESCE(:remoteId, remote_id),
            sync_status = 'SYNCED',
            created_by_user_id = COALESCE(:createdByUserId, created_by_user_id),
            created_by_name = COALESCE(:createdByName, created_by_name),
            updated_at_epoch_ms = MAX(updated_at_epoch_ms, :remoteUpdatedAt)
        WHERE id = :id AND operation_id = :operationId AND updated_at_epoch_ms = :expectedUpdatedAt
            AND deleted_at_epoch_ms IS :expectedDeletedAt
            AND sync_status IN ('PENDING', 'FAILED')
        """,
    )
    suspend fun markSyncSynced(
        id: Long,
        operationId: String,
        expectedUpdatedAt: Long,
        expectedDeletedAt: Long?,
        remoteId: String?,
        createdByUserId: String?,
        createdByName: String?,
        remoteUpdatedAt: Long,
    )

    @Query(
        """
        UPDATE transactions
        SET deleted_at_epoch_ms = :deletedAtEpochMs,
            updated_at_epoch_ms = :updatedAtEpochMs,
            sync_status = :syncStatus
        WHERE id = :id
        """,
    )
    suspend fun markSoftDeleted(
        id: Long,
        deletedAtEpochMs: Long,
        updatedAtEpochMs: Long,
        syncStatus: String,
    )

    @Query("DELETE FROM transactions WHERE id = :id AND is_internal_transfer = 0 AND transfer_group_id IS NULL")
    suspend fun delete(id: Long)

    @Query("DELETE FROM transactions WHERE transfer_group_id = :groupId")
    suspend fun deleteTransferGroup(groupId: String)

    /**
     * Tìm giao dịch có note chứa chuỗi [noteKeyword] trong khoảng thời gian chỉ định.
     * Dùng cho phát hiện hóa đơn định kỳ (Case 6: Smart Subscription Detection).
     */
    @Query("""
        SELECT * FROM transactions
        WHERE note LIKE '%' || :noteKeyword || '%'
        AND occurred_at BETWEEN :startMillis AND :endMillis
        AND deleted_at_epoch_ms IS NULL
        ORDER BY occurred_at DESC
        LIMIT 5
    """)
    suspend fun findRecentSimilar(
        noteKeyword: String,
        startMillis: Long,
        endMillis: Long
    ): List<TransactionEntity>

    /** Returns only occurred_at timestamps for streak calculation (lightweight). */
    @Query("SELECT occurred_at FROM transactions WHERE deleted_at_epoch_ms IS NULL ORDER BY occurred_at DESC")
    fun observeAllOccurredDates(): Flow<List<Long>>

    /** Transactions since [sinceMillis] excluding internal transfers (for charts). */
    @Query("""
        SELECT * FROM transactions
        WHERE occurred_at >= :sinceMillis
        AND is_internal_transfer = 0
        AND deleted_at_epoch_ms IS NULL
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
        WHERE deleted_at_epoch_ms IS NULL
        GROUP BY wallet_id
    """)
    fun observeWalletStats(
        monthStartMillis: Long,
        monthEndMillis: Long,
    ): Flow<List<WalletStatsProjection>>
}
