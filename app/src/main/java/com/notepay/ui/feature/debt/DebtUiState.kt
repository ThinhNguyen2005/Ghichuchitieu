package com.notepay.ui.feature.debt

import com.notepay.domain.model.Wallet
import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.model.debt.DebtWithHistory
import com.notepay.domain.usecase.debt.DebtSummary

data class DebtUiState(
    val debts: List<DebtWithHistory> = emptyList(),
    val selectedTab: DebtType? = null, // null = Tất cả, LEND = Cần thu, BORROW = Cần trả
    val onlyUnsettled: Boolean = true,
    val searchQuery: String = "",
    val summary: DebtSummary? = null,
    val wallets: List<Wallet> = emptyList(),
    val isLoading: Boolean = false,
)
