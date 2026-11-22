package com.notepay.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wallets",
    indices = [
        Index(value = ["is_active"]),
        Index(value = ["is_archived"]),
        Index(value = ["remote_id"]),
    ],
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
    @ColumnInfo(name = "wallet_type", defaultValue = "CASH") val walletType: String = "CASH",
    @ColumnInfo(name = "account_nature", defaultValue = "ASSET") val accountNature: String = "ASSET",
    @ColumnInfo(name = "currency_code", defaultValue = "VND") val currencyCode: String = "VND",
    @ColumnInfo(name = "credit_limit_cents") val creditLimitCents: Long? = null,
    @ColumnInfo(name = "is_archived", defaultValue = "0") val isArchived: Boolean = false,
    @ColumnInfo(name = "purpose", defaultValue = "PERSONAL") val purpose: String = "PERSONAL",
    @ColumnInfo(name = "target_amount_cents") val targetAmountCents: Long? = null,
    @ColumnInfo(name = "target_date_epoch_day") val targetDateEpochDay: Long? = null,
    @ColumnInfo(name = "include_in_total_balance", defaultValue = "1") val includeInTotalBalance: Boolean = true,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "is_shared", defaultValue = "0") val isShared: Boolean = false,
    @ColumnInfo(name = "sync_status", defaultValue = "LOCAL_ONLY") val syncStatus: String = "LOCAL_ONLY",
    @ColumnInfo(name = "shared_role") val sharedRole: String? = null,
    @ColumnInfo(name = "updated_at_epoch_ms", defaultValue = "0") val updatedAtEpochMs: Long = 0L,
)
