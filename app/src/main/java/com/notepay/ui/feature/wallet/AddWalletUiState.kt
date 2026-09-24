package com.notepay.ui.feature.wallet

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
    val usedColorKeys: Set<String> = emptySet(),
    val isAutoColorAssigned: Boolean = false,
    val linkedPackageName: String = "",
    val bankBin: String? = null,
    val accountNumber: String = "",
    val accountName: String = "",
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && !isSaving
}
