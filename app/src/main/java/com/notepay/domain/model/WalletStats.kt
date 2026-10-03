package com.notepay.domain.model

import com.notepay.domain.money.Money

data class WalletStats(
    val walletId: Long,
    val allTimeIncome: Money,
    val allTimeExpense: Money,
    val currentMonthIncome: Money,
    val currentMonthExpense: Money,
    val txCount: Int
)
