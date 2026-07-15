package com.notepay.ui.feature.addtransaction

import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType
import android.net.Uri
import kotlinx.datetime.Instant

/**
 * Events cho AddTransaction screen theo pattern UDF.
 *
 * Mỗi event là một data class/sealed object đại diện cho action của user.
 * ViewModel sẽ xử lý event và cập nhật state tương ứng.
 */
sealed interface AddTransactionEvent {

    /** User thay đổi input số tiền (raw text) */
    data class AmountChanged(val text: String) : AddTransactionEvent

    /** User chọn loại giao dịch: Chi tiêu / Thu nhập */
    data class TypeChanged(val type: TransactionType) : AddTransactionEvent

    /** User chọn danh mục */
    data class CategoryChanged(val category: Category) : AddTransactionEvent

    /** User thay đổi ghi chú */
    data class NoteChanged(val note: String) : AddTransactionEvent

    /** User chọn ngày giờ */
    data class DateChanged(val instant: Instant) : AddTransactionEvent

    /** User chọn ví */
    data class WalletChanged(val walletId: Long) : AddTransactionEvent

    /** User selected a screenshot or QR image for on-device OCR. */
    data class ImageSelected(val uri: Uri) : AddTransactionEvent

    /** User nhấn nút Lưu */
    data object Save : AddTransactionEvent

    /** User nhấn nút Hủy / Back */
    data object Cancel : AddTransactionEvent

    /** Reset form sau khi lưu thành công (navigate back) */
    data object Reset : AddTransactionEvent

    /** User thêm danh mục tùy biến mới */
    data class CreateCategory(
        val displayName: String,
        val colorArgb: Long,
        val iconId: String,
        val isIncome: Boolean,
    ) : AddTransactionEvent
}
