package com.notepay.data.mapper

import com.notepay.data.local.entity.WalletEntity
import com.notepay.domain.model.AccountNature
import com.notepay.domain.model.SharedWalletRole
import com.notepay.domain.model.SyncStatus
import com.notepay.domain.model.Wallet
import com.notepay.domain.model.WalletPurpose
import com.notepay.domain.model.WalletType
import com.notepay.domain.money.Money
import kotlin.time.Instant
import javax.inject.Inject

class WalletMapper @Inject constructor() {

    fun toDomain(entity: WalletEntity): Wallet {
        val type = WalletType.fromString(entity.walletType, entity.iconKey)
        val nature = if (entity.accountNature.equals("LIABILITY", ignoreCase = true)) {
            AccountNature.LIABILITY
        } else if (entity.accountNature.equals("ASSET", ignoreCase = true)) {
            AccountNature.ASSET
        } else {
            type.nature
        }
        return Wallet(
            id = entity.id,
            name = entity.name,
            initialBalance = Money(entity.initialBalanceCents),
            iconKey = entity.iconKey,
            colorKey = entity.colorKey,
            isActive = entity.isActive,
            budgetLimit = entity.budgetLimitCents?.let { Money(it) },
            linkedPackageName = entity.linkedPackageName,
            bankBin = entity.bankBin,
            accountNumber = entity.accountNumber,
            accountName = entity.accountName,
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            type = type,
            nature = nature,
            currencyCode = entity.currencyCode.ifBlank { "VND" },
            creditLimit = entity.creditLimitCents?.let { Money(it) },
            isArchived = entity.isArchived,
            purpose = WalletPurpose.fromString(entity.purpose),
            targetAmount = entity.targetAmountCents?.let { Money(it) },
            targetDateEpochDay = entity.targetDateEpochDay,
            includeInTotalBalance = entity.includeInTotalBalance,
            remoteId = entity.remoteId,
            isShared = entity.isShared,
            syncStatus = SyncStatus.fromString(entity.syncStatus),
            sharedRole = SharedWalletRole.fromString(entity.sharedRole),
            updatedAtEpochMs = entity.updatedAtEpochMs,
        )
    }

    fun toEntity(wallet: Wallet): WalletEntity = WalletEntity(
        id = wallet.id,
        name = wallet.name,
        initialBalanceCents = wallet.initialBalance.amountInCents,
        iconKey = wallet.iconKey,
        colorKey = wallet.colorKey,
        isActive = wallet.isActive,
        budgetLimitCents = wallet.budgetLimit?.amountInCents,
        linkedPackageName = wallet.linkedPackageName,
        bankBin = wallet.bankBin,
        accountNumber = wallet.accountNumber,
        accountName = wallet.accountName,
        createdAt = wallet.createdAt.toEpochMilliseconds(),
        walletType = wallet.type.name,
        accountNature = wallet.nature.name,
        currencyCode = wallet.currencyCode,
        creditLimitCents = wallet.creditLimit?.amountInCents,
        isArchived = wallet.isArchived,
        purpose = wallet.purpose.name,
        targetAmountCents = wallet.targetAmount?.amountInCents,
        targetDateEpochDay = wallet.targetDateEpochDay,
        includeInTotalBalance = wallet.includeInTotalBalance,
        remoteId = wallet.remoteId,
        isShared = wallet.isShared,
        syncStatus = wallet.syncStatus.name,
        sharedRole = wallet.sharedRole?.name,
        updatedAtEpochMs = wallet.updatedAtEpochMs,
    )
}
