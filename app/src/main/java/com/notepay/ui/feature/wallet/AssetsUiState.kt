package com.notepay.ui.feature.wallet

import com.notepay.domain.model.Money
import com.notepay.domain.model.Wallet

enum class AssetChartRange {
    WEEK,
    MONTH,
    HALF_YEAR,
    YEAR
}

data class AssetTrendPoint(
    val timestampMs: Long,
    val dateLabel: String,
    val amount: Money,
)

data class WalletAllocationItem(
    val walletId: Long,
    val name: String,
    val colorKey: String,
    val iconKey: String,
    val balance: Money,
    val percentage: Float, // 0.0f - 1.0f
)

data class WalletAssetItem(
    val wallet: Wallet,
    val balance: Money,
    val monthlyIncome: Money,
    val monthlyExpense: Money,
    val transactionCount: Int,
)

data class AssetsUiState(
    val totalNetWorth: Money = Money.ZERO,
    val wallets: List<WalletAssetItem> = emptyList(),
    val isLoading: Boolean = true,
    val selectedChartRange: AssetChartRange = AssetChartRange.MONTH,
    val trendPoints: List<AssetTrendPoint> = emptyList(),
    val allocationItems: List<WalletAllocationItem> = emptyList(),
    val netWorthChangeInPeriod: Money = Money.ZERO,
    val netWorthChangePercentage: Float = 0f,
    val isTransferSheetVisible: Boolean = false,
    val transferFromWalletId: Long? = null,
    val transferToWalletId: Long? = null,
    val transferAmountInput: String = "",
    val transferNote: String = "",
    val isTransferSubmitting: Boolean = false,
)
