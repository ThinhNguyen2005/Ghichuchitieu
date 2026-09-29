package com.notepay.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bảng quản lý khoản nợ 2 chiều:
 * - LEND: Tôi cho vay (Người khác nợ tôi - Khoản cần thu)
 * - BORROW: Tôi đi vay (Tôi nợ người khác - Khoản cần trả)
 */
@Entity(
    tableName = "debts",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallet_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("person_name"),
        Index("wallet_id"),
        Index("is_settled"),
        Index("due_date"),
    ]
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "person_name") val personName: String,
    @ColumnInfo(name = "phone_number") val phoneNumber: String? = null,
    @ColumnInfo(name = "type") val type: String, // "LEND" hoặc "BORROW"
    @ColumnInfo(name = "original_amount_cents") val originalAmountCents: Long,
    @ColumnInfo(name = "wallet_id") val walletId: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "due_date") val dueDate: Long? = null,
    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "is_settled") val isSettled: Boolean = false,
)
