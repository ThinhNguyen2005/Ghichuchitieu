package com.notepay.ui.feature.home

import com.notepay.domain.money.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.Wallet

data class WalletSummary(
    val wallet: Wallet,
    val balance: Money,
    val monthlyIncome: Money,
    val monthlyExpense: Money,
    val backgroundUri: String? = null
)

data class BudgetProjection(
    val dailyAverage: Money = Money.ZERO,
    val projectedSpend: Money = Money.ZERO,
    val isProjectedToExceed: Boolean = false,
    val exhaustionDateLabel: String? = null,
    val safeDailyLimit: Money = Money.ZERO,
    val spentPercentage: Float = 0f,
    val spentThisWallet: Money = Money.ZERO,
)

data class HomeUiState(
    val activeWallet: Wallet? = null,
    val wallets: List<Wallet> = emptyList(),
    val walletsSummary: List<WalletSummary> = emptyList(),
    val currentBalance: Money = Money.ZERO,
    val monthlyIncome: Money = Money.ZERO,
    val monthlyExpense: Money = Money.ZERO,
    val recentTransactions: List<Transaction> = emptyList(),
    val monthLabel: String = "",
    val isLoading: Boolean = true,
    val budgetProjection: BudgetProjection? = null,
    val dueRemindersCount: Int = 0,
    val walletBackgroundUri: String? = null,
    val streakDays: Int = 0,
    val isSmartInsightsDismissed: Boolean = false,
) {
    val isBudgetExceeded: Boolean
        get() {
            val limit = activeWallet?.budgetLimit ?: return false
            val spent = budgetProjection?.spentThisWallet?.amountInCents ?: monthlyExpense.amountInCents
            return spent > limit.amountInCents
        }
}
