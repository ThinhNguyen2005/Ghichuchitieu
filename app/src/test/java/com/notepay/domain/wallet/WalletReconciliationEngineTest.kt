package com.notepay.domain.wallet

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import kotlin.time.Clock
import org.junit.Test

class WalletReconciliationEngineTest {

    @Test
    fun `calculateSummary computes correct net balance`() {
        val now = Clock.System.now()
        val wallet = Wallet(
            id = 1L,
            name = "Ví Vietcombank",
            initialBalance = Money(1_000_000_00L), // 1.000.000đ
            colorKey = "BLUE",
            iconKey = "WALLET",
            isActive = true
        )

        val tx1 = Transaction(
            id = 1L,
            walletId = 1L,
            amount = Money(500_000_00L),
            type = TransactionType.INCOME,
            note = "Lương",
            category = Category.SALARY,
            occurredAt = now,
            createdAt = now
        )

        val tx2 = Transaction(
            id = 2L,
            walletId = 1L,
            amount = Money(200_000_00L),
            type = TransactionType.EXPENSE,
            note = "Ăn tối",
            category = Category.FOOD,
            occurredAt = now,
            createdAt = now
        )

        val summary = WalletReconciliationEngine.calculateSummary(wallet, listOf(tx1, tx2))

        assertThat(summary.initialBalance.amountInCents).isEqualTo(1_000_000_00L)
        assertThat(summary.totalIncome.amountInCents).isEqualTo(500_000_00L)
        assertThat(summary.totalExpense.amountInCents).isEqualTo(200_000_00L)
        assertThat(summary.netBalance.amountInCents).isEqualTo(1_300_000_00L) // 1.3M
    }

    @Test
    fun `reconcile detects balance discrepancy`() {
        val summary = WalletBalanceSummary(
            walletId = 1L,
            walletName = "Ví Vietcombank",
            initialBalance = Money(1_000_000_00L),
            totalIncome = Money(500_000_00L),
            totalExpense = Money(200_000_00L),
            netBalance = Money(1_300_000_00L)
        )

        val actualBankBalance = Money(1_250_000_00L) // 1.25M
        val recon = WalletReconciliationEngine.reconcile(summary, actualBankBalance)

        assertThat(recon.isBalanced).isFalse()
        assertThat(recon.discrepancy.amountInCents).isEqualTo(-50_000_00L) // Thâm hụt 50k
    }
}
