package com.notepay.ui.feature.home

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Money
import com.notepay.domain.model.Wallet
import org.junit.Test

class HomeUiStateTest {

    @Test
    fun `isBudgetExceeded returns false when active wallet has no budget limit`() {
        val wallet = Wallet(
            id = 1L,
            name = "Ví test",
            initialBalance = Money.ZERO,
            iconKey = "cash",
            colorKey = "primary",
            budgetLimit = null
        )
        val state = HomeUiState(
            activeWallet = wallet,
            monthlyExpense = Money(500L * 100)
        )
        assertThat(state.isBudgetExceeded).isFalse()
    }

    @Test
    fun `isBudgetExceeded returns false when active wallet is null`() {
        val state = HomeUiState(
            activeWallet = null,
            monthlyExpense = Money(500L * 100)
        )
        assertThat(state.isBudgetExceeded).isFalse()
    }

    @Test
    fun `isBudgetExceeded returns true when monthly expense exceeds budget limit`() {
        val wallet = Wallet(
            id = 1L,
            name = "Ví test",
            initialBalance = Money.ZERO,
            iconKey = "cash",
            colorKey = "primary",
            budgetLimit = Money(1000L * 100)
        )
        val state = HomeUiState(
            activeWallet = wallet,
            monthlyExpense = Money(1001L * 100)
        )
        assertThat(state.isBudgetExceeded).isTrue()
    }

    @Test
    fun `isBudgetExceeded returns false when monthly expense is equal to budget limit`() {
        val wallet = Wallet(
            id = 1L,
            name = "Ví test",
            initialBalance = Money.ZERO,
            iconKey = "cash",
            colorKey = "primary",
            budgetLimit = Money(1000L * 100)
        )
        val state = HomeUiState(
            activeWallet = wallet,
            monthlyExpense = Money(1000L * 100)
        )
        assertThat(state.isBudgetExceeded).isFalse()
    }

    @Test
    fun `isBudgetExceeded returns false when monthly expense is below budget limit`() {
        val wallet = Wallet(
            id = 1L,
            name = "Ví test",
            initialBalance = Money.ZERO,
            iconKey = "cash",
            colorKey = "primary",
            budgetLimit = Money(1000L * 100)
        )
        val state = HomeUiState(
            activeWallet = wallet,
            monthlyExpense = Money(999L * 100)
        )
        assertThat(state.isBudgetExceeded).isFalse()
    }
}
