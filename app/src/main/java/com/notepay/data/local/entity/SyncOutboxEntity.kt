package com.notepay.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["status", "created_at_epoch_ms"]),
    ],
)
data class SyncOutboxEntity(
    @PrimaryKey @ColumnInfo(name = "operation_id") val operationId: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "wallet_remote_id") val walletRemoteId: String,
    @ColumnInfo(name = "action") val action: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "status", defaultValue = "PENDING") val status: String = STATUS_PENDING,
    @ColumnInfo(name = "retry_count", defaultValue = "0") val retryCount: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long,
) {
    companion object {
        const val ENTITY_TYPE_TRANSACTION = "TRANSACTION"
        const val ACTION_UPSERT = "UPSERT"
        const val ACTION_DELETE = "DELETE"
        const val STATUS_PENDING = "PENDING"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_SYNCED = "SYNCED"
    }
}
