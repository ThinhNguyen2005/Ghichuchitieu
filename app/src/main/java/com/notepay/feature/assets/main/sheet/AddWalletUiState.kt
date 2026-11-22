package com.notepay.feature.assets.main.sheet

import androidx.compose.runtime.Immutable
import com.notepay.domain.model.AccountNature
import com.notepay.domain.model.VietQrBank
import com.notepay.domain.model.WalletPurpose
import com.notepay.domain.model.WalletType
import com.notepay.domain.money.Money

enum class BudgetPeriod {
    DAILY, WEEKLY, MONTHLY
}

@Immutable
data class AddWalletUiState(
    val name: String = "",
    val initialBalanceInput: String = "",
    val currentBalance: Money? = null,
    val hasTransactions: Boolean = false,
    val transactionCount: Int = 0,
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
    val banks: List<VietQrBank> = emptyList(),
    val accountNature: AccountNature = AccountNature.ASSET,
    val walletType: WalletType = WalletType.CASH,
    val currencyCode: String = "VND",
    val creditLimitInput: String = "",
    val isArchived: Boolean = false,
    val purpose: WalletPurpose = WalletPurpose.PERSONAL,
    val targetAmountInput: String = "",
    val targetDateEpochDay: Long? = null,
    val includeInTotalBalance: Boolean = true,
) {
    val hasBankInfo: Boolean
        get() = !bankBin.isNullOrBlank() || accountNumber.isNotBlank() || accountName.isNotBlank()
    val isBankInfoComplete: Boolean
        get() = !bankBin.isNullOrBlank() && accountNumber.isNotBlank() && accountName.isNotBlank()
    val isBankInfoValid: Boolean
        get() = !hasBankInfo || isBankInfoComplete
    val canSave: Boolean
        get() = name.isNotBlank() && isBankInfoValid && !isSaving

    val isCurrencyLocked: Boolean
        get() = isEditMode && transactionCount > 0

    val canDeleteDirectly: Boolean
        get() = isEditMode && transactionCount == 0

    val canArchive: Boolean
        get() = isEditMode && transactionCount > 0
}
