package com.notepay.domain

import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import kotlin.time.Clock

object TestData {
    val NOW: kotlinx.datetime.Instant = Clock.System.now()

    fun wallet(
        id: Long = 1L,
        name: String = "Tiền mặt",
        initialBalance: Money = Money.ZERO,
    ) = Wallet(
        id = id,
        name = name,
        initialBalance = initialBalance,
        iconKey = Wallet.ICON_CASH,
        colorKey = Wallet.COLOR_PRIMARY,
        isActive = true,
    )

    fun transaction(
        id: Long = 0L,
        amount: Money = Money(50_000_00),
        type: TransactionType = TransactionType.EXPENSE,
        category: Category = Category.FOOD,
        note: String = "Cơm trưa",
        walletId: Long = 1L,
    ) = Transaction(
        id = id,
        amount = amount,
        type = type,
        category = category,
        note = note,
        occurredAt = NOW,
        walletId = walletId,
    )
}
