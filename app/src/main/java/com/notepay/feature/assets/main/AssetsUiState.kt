package com.notepay.feature.assets.main

import androidx.compose.runtime.Immutable
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.domain.usecase.debt.DebtSummary

/**
 * Một phân đoạn của thanh phân bổ tài sản trên thẻ Ink.
 *
 * [percentage] nằm trong khoảng 0.0f - 1.0f và chỉ tính trên các ví có số dư dương (hoặc dư nợ dương).
 */
@Immutable
data class WalletAllocationItem(
    val walletId: Long,
    val name: String,
    val colorKey: String,
    val iconKey: String,
    val balance: Money,
    val convertedBalanceVnd: Money = balance,
    val percentage: Float, // 0.0f - 1.0f
)

@Immutable
data class WalletAssetItem(
    val wallet: Wallet,
    val balance: Money,
    val convertedBalanceVnd: Money = balance,
    val monthlyIncome: Money,
    val monthlyExpense: Money,
    val transactionCount: Int,
    val groupPercentage: Float = 0f,
)

@Immutable
data class AssetsUiState(
    val totalNetWorth: Money = Money.ZERO,
    val totalAssetsVnd: Money = Money.ZERO,
    val totalLiabilitiesVnd: Money = Money.ZERO,
    val assetCount: Int = 0,
    val liabilityCount: Int = 0,
    val wallets: List<WalletAssetItem> = emptyList(),
    val assetWallets: List<WalletAssetItem> = emptyList(),
    val liabilityWallets: List<WalletAssetItem> = emptyList(),
    val archivedWallets: List<WalletAssetItem> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val allocationMode: AllocationMode = AllocationMode.ASSET,
    val allocationItems: List<WalletAllocationItem> = emptyList(),
    val netWorthChangePercentage: Float? = null,
    val debtSummary: DebtSummary? = null,
    val hasForeignCurrency: Boolean = false,
    val ratesLastUpdated: String? = null,
    val adjustBalanceWallet: WalletAssetItem? = null,
)

/**
 * Trạng thái tức thời của form chuyển khoản, do ViewModel phát song song với [AssetsUiState]
 * để gõ số tiền không phải restart luồng query và không phải chép lại từng trường.
 */
@Immutable
data class TransferFormState(
    val isVisible: Boolean = false,
    val fromWalletId: Long? = null,
    val toWalletId: Long? = null,
    val amountInput: String = "",
    val note: String = "",
    val isSubmitting: Boolean = false,
    val submitError: TransferError? = null,
)
