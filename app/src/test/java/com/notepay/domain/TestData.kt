package com.notepay.domain

import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import kotlin.time.Clock
import kotlin.time.Instant

object TestData {
    val NOW: Instant = Clock.System.now()

    fun wallet(
        id: Long = 1L,
        name: String = "Tiền mặt",
        initialBalance: Money = Money.ZERO,
        budgetLimit: Money? = null,
    ) = Wallet(
        id = id,
        name = name,
        initialBalance = initialBalance,
        iconKey = Wallet.ICON_CASH,
        colorKey = Wallet.COLOR_PRIMARY,
        isActive = true,
        budgetLimit = budgetLimit,
    )

    fun transaction(
        id: Long = 0L,
        amount: Money = Money(50_000_00),
        type: TransactionType = TransactionType.EXPENSE,
        category: Category = Category.FOOD,
        note: String = "Cơm trưa",
        walletId: Long = 1L,
        occurredAt: Instant = NOW,
    ) = Transaction(
        id = id,
        amount = amount,
        type = type,
        category = category,
        note = note,
        occurredAt = occurredAt,
        walletId = walletId,
    )
}
