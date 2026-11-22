package com.notepay.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallet_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("wallet_id"),
        Index("occurred_at"),
        Index("remote_id"),
        Index("operation_id"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "note") val note: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_auto_capture", defaultValue = "0") val isAutoCapture: Boolean = false,
    @ColumnInfo(name = "is_internal_transfer", defaultValue = "0") val isInternalTransfer: Boolean = false,
    @ColumnInfo(name = "transfer_group_id") val transferGroupId: String? = null,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "operation_id") val operationId: String? = null,
    @ColumnInfo(name = "sync_status", defaultValue = "LOCAL_ONLY") val syncStatus: String = "LOCAL_ONLY",
    @ColumnInfo(name = "created_by_user_id") val createdByUserId: String? = null,
    @ColumnInfo(name = "created_by_name") val createdByName: String? = null,
    @ColumnInfo(name = "updated_at_epoch_ms", defaultValue = "0") val updatedAtEpochMs: Long = 0L,
    @ColumnInfo(name = "deleted_at_epoch_ms") val deletedAtEpochMs: Long? = null,
)
