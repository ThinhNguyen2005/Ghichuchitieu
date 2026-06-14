package com.notepay.domain.model

import com.notepay.domain.TestData
import kotlin.time.Clock

object TestTransactionFactory {
    fun expense(
        amount: Money = Money(50_000_00),
        category: Category = Category.FOOD,
        note: String = "Cơm trưa",
        walletId: Long = 1L,
    ) = Transaction(
        id = 0L,
        amount = amount,
        type = TransactionType.EXPENSE,
        category = category,
        note = note,
        occurredAt = Clock.System.now(),
        walletId = walletId,
    )

    fun income(
        amount: Money = Money(5_000_000_00),
        category: Category = Category.SALARY,
        note: String = "Lương tháng",
        walletId: Long = 1L,
    ) = Transaction(
        id = 0L,
        amount = amount,
        type = TransactionType.INCOME,
        category = category,
        note = note,
        occurredAt = Clock.System.now(),
        walletId = walletId,
    )

    fun sample(amount: Money = Money(50_000_00), walletId: Long = 1L): Transaction =
        TestData.transaction(amount = amount, walletId = walletId)
}
