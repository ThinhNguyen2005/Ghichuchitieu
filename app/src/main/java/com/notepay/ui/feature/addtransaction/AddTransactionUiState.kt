package com.notepay.ui.feature.addtransaction

import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import kotlin.time.Clock
import kotlinx.datetime.Instant

/**
 * UI State cho màn hình Thêm giao dịch.
 *
 * Thiết kế theo pattern UDF (Unidirectional Data Flow):
 * - State là immutable data class
 * - Events là sealed interface
 * - ViewModel nhận event, cập nhật state
 *
 * Validation rules (Phase 1):
 * - amount > 0 (khác 0)
 * - note.length <= 200
 * - walletId > 0 (phải có ví được chọn)
 */
data class AddTransactionUiState(
    /** Raw text input để format realtime (ví dụ: "1,000,000") */
    val amountInput: String = "",
    /** Parsed Money object, null nếu input invalid */
    val amount: Money? = null,
    /** Loại giao dịch: EXPENSE (mặc định) hoặc INCOME */
    val type: TransactionType = TransactionType.EXPENSE,
    /** Danh mục được chọn, default theo type */
    val category: Category = Category.DEFAULT_EXPENSE,
    /** Ghi chú giao dịch */
    val note: String = "",
    /** Thời điểm xảy ra giao dịch */
    val occurredAt: Instant = Clock.System.now(),
    /** ID ví được chọn */
    val walletId: Long? = null,
    /** Danh sách ví để chọn (nếu Phase 2+ mở rộng) */
    val availableWallets: List<Wallet> = emptyList(),
    /** Đang đang lưu giao dịch */
    val isSaving: Boolean = false,
    /** Tập hợp lỗi validation */
    val errors: Set<FieldError> = emptySet(),
    /** Lỗi lưu giao dịch từ tầng domain/data */
    val saveErrorMessage: String? = null,
    /** Đã lưu thành công - dùng để trigger navigation back */
    val savedSuccessfully: Boolean = false,
    /** Danh sách danh mục khả dụng gồm mặc định + tự tạo */
    val availableCategories: List<Category> = emptyList(),
    /** Đánh dấu người dùng đã chủ động chọn danh mục bằng tay */
    val isCategoryExplicitlySelected: Boolean = false,
    /** Danh mục được gợi ý tự động thời gian thực */
    val suggestedCategory: Category? = null,
    /** Lý do ngắn gọn để người dùng biết đề xuất dựa trên dữ liệu nào. */
    val suggestionReason: String? = null,
    /** OCR runs locally; the draft is always shown for user verification before saving. */
    val isImageScanning: Boolean = false,
    val imageScanMessage: String? = null,
) {
    /** Có thể lưu được không: amount > 0, có wallet, không có lỗi */
    val canSave: Boolean
        get() = amount != null && amount.amountInCents > 0 && walletId != null && errors.isEmpty() && !isSaving

    /** Hiển thị số tiền đã format cho UI */
    val displayAmount: String
        get() = if (amountInput.isBlank()) "" else amountInput
}

/** Các loại lỗi validation */
enum class FieldError {
    AMOUNT_EMPTY,
    AMOUNT_INVALID,
    NOTE_TOO_LONG,
    WALLET_MISSING,
}
