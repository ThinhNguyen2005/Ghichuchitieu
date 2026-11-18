package com.notepay.feature.assets.main

import androidx.compose.runtime.Immutable
import com.notepay.domain.money.Money
import com.notepay.domain.model.Wallet
import com.notepay.domain.usecase.debt.DebtSummary

/**
 * Một phân đoạn của thanh phân bổ tài sản trên thẻ Ink.
 *
 * [percentage] nằm trong khoảng 0.0f - 1.0f và chỉ tính trên các ví có số dư dương.
 */
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
    val hasError: Boolean = false,
    val allocationItems: List<WalletAllocationItem> = emptyList(),
    /**
     * Biến động số dư trong cửa sổ so sánh, tính theo phần trăm.
     *
     * null nghĩa là **không có cơ sở tính** (số dư đầu kỳ bằng 0), khác hẳn 0f là
     * "đã tính được và không thay đổi".
     */
    val netWorthChangePercentage: Float? = null,
    val debtSummary: DebtSummary? = null,
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
    /**
     * Lỗi từ tầng lưu trữ, hiển thị phía trên nút xác nhận.
     *
     * Lỗi nhập liệu không nằm ở đây: chúng được suy ra từ [validateTransfer] ngay trên UI để
     * không bao giờ lệch với danh sách ví hiện tại.
     */
    val submitError: TransferError? = null,
)
