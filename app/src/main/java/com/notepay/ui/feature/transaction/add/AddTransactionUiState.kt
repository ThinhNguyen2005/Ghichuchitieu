package com.notepay.ui.feature.transaction.add

import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.ui.feature.transaction.calculator.CalculatorEngine
import com.notepay.ui.feature.transaction.calculator.CalculatorState
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * UI State cho màn hình Thêm giao dịch - UDF pattern.
 *
 * Validation rules:
 * - amount > 0
 * - note.length <= Transaction.MAX_NOTE_LENGTH
 * - walletId != null
 */
data class AddTransactionUiState(
    val amountInput: String = "",
    val amount: Money? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.DEFAULT_EXPENSE,
    val note: String = "",
    val occurredAt: Instant = Clock.System.now(),
    val walletId: Long? = null,
    val availableWallets: List<Wallet> = emptyList(),
    val isSaving: Boolean = false,
    val errors: Set<FieldError> = emptySet(),
    val availableCategories: List<Category> = emptyList(),
    val isCategoryExplicitlySelected: Boolean = false,
    val suggestedCategory: Category? = null,
    val suggestionReason: String? = null,
    val isImageScanning: Boolean = false,
    val imageScanMessage: String? = null,
    /** Trạng thái engine máy tính (bàn phím tự chế 4x5) */
    val calcState: CalculatorState = CalculatorState(),
) {
    val canSave: Boolean
        get() = amount != null && amount.amountInCents > 0 && walletId != null && errors.isEmpty() && !isSaving

    /** Biểu thức hiển thị trên thanh số tiền */
    val displayExpression: String
        get() = CalculatorEngine.displayExpression(calcState)
}

enum class FieldError {
    AMOUNT_EMPTY,
    AMOUNT_INVALID,
    NOTE_TOO_LONG,
    WALLET_MISSING,
}
