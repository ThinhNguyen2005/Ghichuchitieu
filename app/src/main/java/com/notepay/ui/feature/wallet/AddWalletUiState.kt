package com.notepay.ui.feature.wallet

import com.notepay.domain.model.VietQrBank
import com.notepay.domain.money.Money

enum class BudgetPeriod {
    DAILY, WEEKLY, MONTHLY
}

data class AddWalletUiState(
    val name: String = "",
    val initialBalanceInput: String = "",
    val currentBalance: Money? = null,
    val hasTransactions: Boolean = false,
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
    val banks: List<VietQrBank> = emptyList()

) {
    val hasBankInfo: Boolean
        get() = !bankBin.isNullOrBlank() || accountNumber.isNotBlank() || accountName.isNotBlank()
    val isBankInfoComplete: Boolean
        get() = !bankBin.isNullOrBlank() && accountNumber.isNotBlank() && accountName.isNotBlank()
    val isBankInfoValid: Boolean
        get() = !hasBankInfo || isBankInfoComplete
    val canSave: Boolean
        get() = name.isNotBlank() && isBankInfoValid && !isSaving

}
