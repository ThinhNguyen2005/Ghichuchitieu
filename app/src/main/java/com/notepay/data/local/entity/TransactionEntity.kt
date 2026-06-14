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
)
