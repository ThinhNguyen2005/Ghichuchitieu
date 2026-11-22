package com.notepay.feature.transaction.detail

import androidx.compose.runtime.Immutable
import com.notepay.domain.model.Transaction

@Immutable
data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val walletName: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val canEdit: Boolean = true,
) {
    val isAutoCapture: Boolean
        get() = transaction?.isAutoCapture == true
}
