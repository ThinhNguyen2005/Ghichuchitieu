package com.notepay.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bảng ghi nhận thanh toán từng phần cho khoản nợ
 */
@Entity(
    tableName = "debt_payments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debt_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("debt_id"),
        Index("transaction_id"),
        Index("paid_at"),
    ]
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "debt_id") val debtId: Long,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "wallet_id") val walletId: Long? = null,
    @ColumnInfo(name = "paid_at") val paidAt: Long,
    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "transaction_id") val transactionId: Long? = null,
)
