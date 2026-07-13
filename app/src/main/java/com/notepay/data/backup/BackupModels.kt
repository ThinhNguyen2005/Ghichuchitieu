package com.notepay.data.backup

import com.notepay.data.local.entity.BillSplitEntity
import com.notepay.data.local.entity.SubscriptionEntity
import com.notepay.data.local.entity.TransactionEntity
import com.notepay.data.local.entity.WalletEntity

/** Cấu trúc JSON backup — versioned, dễ mở rộng */
data class BackupPackage(
    val version: Int = BACKUP_VERSION,
    val exportedAt: String,
    val data: BackupData,
)

data class BackupData(
    val wallets: List<WalletEntity>,
    val transactions: List<TransactionEntity>,
    val billSplits: List<BillSplitEntity>,
    val subscriptions: List<SubscriptionEntity>,
    val customCategories: List<CustomCategoryDto>,
    val preferences: BackupPreferences,
)

data class CustomCategoryDto(
    val id: String,
    val displayName: String,
    val colorArgb: Long,
    val iconId: String,
    val isIncome: Boolean,
)

data class BackupPreferences(
    val themeColor: String,
    val themeCustomColor: String,
    val autoCaptureEnabled: Boolean,
    val enabledPackages: Set<String>,
    val customBankApps: Set<String>,
    val categoryHabits: Map<String, Int>,
)

const val BACKUP_VERSION = 1
