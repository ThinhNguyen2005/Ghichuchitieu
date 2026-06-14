package com.notepay.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wallets",
    indices = [Index(value = ["is_active"])],
)
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "initial_balance_cents") val initialBalanceCents: Long,
    @ColumnInfo(name = "icon_key") val iconKey: String,
    @ColumnInfo(name = "color_key") val colorKey: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false,
    @ColumnInfo(name = "budget_limit_cents") val budgetLimitCents: Long? = null,
    @ColumnInfo(name = "linked_package_name") val linkedPackageName: String? = null,
    @ColumnInfo(name = "bank_bin") val bankBin: String? = null,
    @ColumnInfo(name = "account_number") val accountNumber: String? = null,
    @ColumnInfo(name = "account_name") val accountName: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
