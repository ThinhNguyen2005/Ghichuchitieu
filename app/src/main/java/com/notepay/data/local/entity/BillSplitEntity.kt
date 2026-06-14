package com.notepay.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bill_splits",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("transaction_id")]
)
data class BillSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "debtor_name") val debtorName: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "is_paid") val isPaid: Boolean = false,
    @ColumnInfo(name = "memo_code") val memoCode: String,
    @ColumnInfo(name = "paid_at") val paidAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
