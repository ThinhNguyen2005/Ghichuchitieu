package com.notepay.ui.feature.wallet

import com.notepay.domain.model.Money

enum class BudgetPeriod {
    DAILY, WEEKLY, MONTHLY
}

data class AddWalletUiState(
    val name: String = "",
    val initialBalanceInput: String = "",
    val hasBudgetLimit: Boolean = false,
    val budgetLimitInput: String = "",
    val budgetPeriod: BudgetPeriod = BudgetPeriod.MONTHLY,
    val iconKey: String = "cash",
    val colorKey: String = "primary",
    val linkedPackageName: String = "",
    val bankBin: String? = null,
    val accountNumber: String = "",
    val accountName: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && !isSaving
}
