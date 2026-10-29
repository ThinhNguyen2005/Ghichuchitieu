package com.notepay.ui.feature.wallet

import androidx.compose.runtime.Immutable
import com.notepay.domain.money.Money
import com.notepay.domain.model.Wallet
import com.notepay.domain.usecase.debt.DebtSummary

enum class AssetChartRange {
    WEEK,
    MONTH,
    HALF_YEAR,
    YEAR
}

@Immutable
data class AssetTrendPoint(
    val timestampMs: Long,
    val dateLabel: String,
    val amount: Money,
)

@Immutable
data class WalletAllocationItem(
    val walletId: Long,
    val name: String,
    val colorKey: String,
    val iconKey: String,
    val balance: Money,
    val percentage: Float, // 0.0f - 1.0f
)

@Immutable
data class WalletAssetItem(
    val wallet: Wallet,
    val balance: Money,
    val monthlyIncome: Money,
    val monthlyExpense: Money,
    val transactionCount: Int,
)

@Immutable
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
    val debtSummary: DebtSummary? = null,
)

/**
 * Trạng thái tức thời của form chuyển khoản nội bộ. Tách khỏi [AssetsUiState] để
 * ViewModel có thể ghép nó với luồng dữ liệu Room mà không phải restart luồng query.
 */
@Immutable
data class TransferFormState(
    val isVisible: Boolean = false,
    val fromWalletId: Long? = null,
    val toWalletId: Long? = null,
    val amountInput: String = "",
    val note: String = "",
    val isSubmitting: Boolean = false,
)
