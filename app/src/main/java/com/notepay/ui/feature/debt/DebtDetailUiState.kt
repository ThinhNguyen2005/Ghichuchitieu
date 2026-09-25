package com.notepay.ui.feature.debt

import androidx.compose.runtime.Immutable
import com.notepay.domain.model.VietQrBank
import com.notepay.domain.model.Wallet
import com.notepay.domain.model.debt.DebtWithHistory

@Immutable
data class DebtDetailUiState(
    val debtWithHistory: DebtWithHistory? = null,
    val wallets: List<Wallet> = emptyList(),
    val banks: List<VietQrBank> = emptyList(),
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
)
