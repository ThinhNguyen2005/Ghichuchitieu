package com.notepay.ui.feature.wallet

import com.notepay.domain.model.Money

data class AddWalletUiState(
    val name: String = "",
    val initialBalanceInput: String = "",
    val hasBudgetLimit: Boolean = false,
    val budgetLimitInput: String = "",
    val iconKey: String = "cash",
    val colorKey: String = "primary",
    val linkedPackageName: String = "",
    val bankBin: String? = null,
    val accountNumber: String = "",
    val accountName: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val canSave: Boolean get() = name.isNotBlank() && !isSaving
}
