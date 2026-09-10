package com.notepay.domain.wallet

import com.notepay.domain.money.Money
import kotlin.time.Instant

/** Wallet Balance Snapshot & Reconciliation Audit */
data class WalletBalanceSnapshot(
    val walletId: Long,
    val initialBalance: Money,
    val totalIncome: Money,
    val totalExpense: Money,
    val currentBalance: Money,
    val calculatedAt: Instant
)
