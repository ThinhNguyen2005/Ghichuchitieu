package com.notepay.domain.wallet

import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet

data class WalletBalanceSummary(
    val walletId: Long,
    val walletName: String,
    val initialBalance: Money,
    val totalIncome: Money,
    val totalExpense: Money,
    val netBalance: Money
)

data class WalletReconciliationResult(
    val walletId: Long,
    val expectedBalance: Money,
    val actualBalance: Money,
    val discrepancy: Money,
    val isBalanced: Boolean
)

/** Pure Domain Engine for Wallet Cashflow & Reconciliation */
object WalletReconciliationEngine {

    /** Calculates net wallet balance from initial balance + income - expense */
    fun calculateSummary(
        wallet: Wallet,
        transactions: List<Transaction>
    ): WalletBalanceSummary {
        val walletTransactions = transactions.filter { it.walletId == wallet.id }

        var incomeCents = 0L
        var expenseCents = 0L

        walletTransactions.forEach { tx ->
            when (tx.type) {
                TransactionType.INCOME -> incomeCents += tx.amount.amountInCents
                TransactionType.EXPENSE -> expenseCents += tx.amount.amountInCents
            }
        }

        val initialCents = wallet.initialBalance.amountInCents
        val netCents = initialCents + incomeCents - expenseCents

        return WalletBalanceSummary(
            walletId = wallet.id,
            walletName = wallet.name,
            initialBalance = wallet.initialBalance,
            totalIncome = Money(incomeCents),
            totalExpense = Money(expenseCents),
            netBalance = Money(netCents)
        )
    }

    /** Reconciles expected wallet balance against actual bank statement balance */
    fun reconcile(
        summary: WalletBalanceSummary,
        actualBankBalance: Money
    ): WalletReconciliationResult {
        val discrepancyCents = actualBankBalance.amountInCents - summary.netBalance.amountInCents
        return WalletReconciliationResult(
            walletId = summary.walletId,
            expectedBalance = summary.netBalance,
            actualBalance = actualBankBalance,
            discrepancy = Money(discrepancyCents),
            isBalanced = discrepancyCents == 0L
        )
    }
}
