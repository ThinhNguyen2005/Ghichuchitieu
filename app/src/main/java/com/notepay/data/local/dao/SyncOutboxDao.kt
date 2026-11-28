package com.notepay.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.notepay.data.local.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOutboxDao {
    @Upsert
    suspend fun upsert(item: SyncOutboxEntity)

    @Query(
        """
        SELECT * FROM sync_outbox
        WHERE status IN ('PENDING', 'FAILED') AND retry_count < :maxRetries
        ORDER BY created_at_epoch_ms ASC
        """,
    )
    suspend fun getRetryable(maxRetries: Int): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox WHERE operation_id = :operationId LIMIT 1")
    suspend fun getByOperationId(operationId: String): SyncOutboxEntity?

    @Query("SELECT * FROM sync_outbox ORDER BY created_at_epoch_ms ASC")
    suspend fun getAll(): List<SyncOutboxEntity>

    @Query(
        """
        UPDATE sync_outbox
        SET status = :status, retry_count = :retryCount, last_error = :lastError, updated_at_epoch_ms = :updatedAt
        WHERE operation_id = :operationId
            AND updated_at_epoch_ms = :expectedUpdatedAt
            AND payload_json = :expectedPayloadJson
            AND status = :expectedStatus
        """,
    )
    suspend fun updateStatus(
        operationId: String,
        expectedUpdatedAt: Long,
        expectedPayloadJson: String,
        expectedStatus: String,
        status: String,
        retryCount: Int,
        lastError: String?,
        updatedAt: Long,
    ): Int

    @Query("DELETE FROM sync_outbox WHERE status = 'SYNCED'")
    suspend fun purgeSynced()

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status IN ('PENDING', 'FAILED')")
    fun observePendingCount(): Flow<Int>
}
