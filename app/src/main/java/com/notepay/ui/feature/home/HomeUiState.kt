package com.notepay.ui.feature.home

import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.Wallet

data class HomeUiState(
    val activeWallet: Wallet? = null,
    val wallets: List<Wallet> = emptyList(),
    val currentBalance: Money = Money.ZERO,
    val monthlyIncome: Money = Money.ZERO,
    val monthlyExpense: Money = Money.ZERO,
    val recentTransactions: List<Transaction> = emptyList(),
    val monthLabel: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val isBudgetExceeded: Boolean
        get() {
            val limit = activeWallet?.budgetLimit ?: return false
            return monthlyExpense.amountInCents > limit.amountInCents
        }
}
