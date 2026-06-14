package com.notepay.data.mapper

import com.notepay.data.local.entity.WalletEntity
import com.notepay.domain.model.Money
import com.notepay.domain.model.Wallet
import kotlinx.datetime.Instant
import javax.inject.Inject

class WalletMapper @Inject constructor() {

    fun toDomain(entity: WalletEntity): Wallet = Wallet(
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
    )

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
    )
}
