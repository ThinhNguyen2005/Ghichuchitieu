package com.notepay.ui.feature.transaction.detail

import com.notepay.domain.model.Transaction

data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val walletName: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val isAutoCapture: Boolean
        get() = transaction?.isAutoCapture == true
}
