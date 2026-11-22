package com.notepay.feature.transaction.form

import androidx.compose.runtime.Immutable
import com.notepay.domain.model.Category
import com.notepay.domain.money.Money
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.natural.NaturalParseSource
import com.notepay.domain.natural.NaturalTransactionDraft
import com.notepay.feature.transaction.form.calculator.CalculatorEngine
import com.notepay.feature.transaction.form.calculator.CalculatorState
import com.notepay.feature.transaction.form.validation.FieldError
import kotlin.time.Clock
import kotlin.time.Instant

@Immutable
data class NaturalEntryUiState(
    val rawInput: String = "",
    val drafts: List<NaturalTransactionDraft> = emptyList(),
    val needsAiFallback: Boolean = false,
    val source: NaturalParseSource = NaturalParseSource.ON_DEVICE,
    val isAiLoading: Boolean = false,
    val isAiAvailable: Boolean = false,
    val showAiFallbackNotice: Boolean = false,
    val isSavingBatch: Boolean = false,
)

/**
 * Unified UI State cho TransactionFormScreen (Add + Edit).
 *
 * - isEditMode = false → chế độ thêm mới
 * - isEditMode = true  → chế độ chỉnh sửa, isLoading = true khi đang load dữ liệu cũ
 * - isAutoCapture = true → giao dịch từ ngân hàng, một số field là read-only
 *
 * Tất cả trạng thái Sheet/Dialog được quản lý tại đây (không dùng local remember).
 */
@Immutable
data class TransactionFormUiState(
    // ── Mode ─────────────────────────────────────────────────────────────────
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isAutoCapture: Boolean = false,

    // ── Form fields ───────────────────────────────────────────────────────────
    val amountInput: String = "",
    val amount: Money? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.DEFAULT_EXPENSE,
    val note: String = "",
    val occurredAt: Instant = Clock.System.now(),
    val walletId: Long? = null,
    val availableWallets: List<Wallet> = emptyList(),
    val availableCategories: List<Category> = emptyList(),

    // ── Calculator keypad ─────────────────────────────────────────────────────
    val calcState: CalculatorState = CalculatorState(),

    // ── Category suggestion ───────────────────────────────────────────────────
    val isCategoryExplicitlySelected: Boolean = false,
    val suggestedCategory: Category? = null,
    val suggestionReason: String? = null,

    // ── OCR image scan ────────────────────────────────────────────────────────
    val isImageScanning: Boolean = false,
    val imageScanMessage: String? = null,

    // ── Save state ────────────────────────────────────────────────────────────
    val isSaving: Boolean = false,
    val errors: Set<FieldError> = emptySet(),

    // ── Sheet / Dialog visibility (state-driven, không dùng local remember) ──
    val isDatePickerOpen: Boolean = false,
    val isWalletPickerOpen: Boolean = false,
    val isCategoryPickerOpen: Boolean = false,
    val isNoteSheetOpen: Boolean = false,
    val naturalEntry: NaturalEntryUiState = NaturalEntryUiState(),
) {
    val canSave: Boolean
        get() = amount != null &&
            amount.amountInCents > 0 &&
            walletId != null &&
            errors.isEmpty() &&
            calcState.error == null &&
            !isSaving

    /** Biểu thức hiển thị trên thanh số tiền (Add mode) */
    val displayExpression: String
        get() = CalculatorEngine.displayExpression(calcState)

    val activePendingOperator: Char?
        get() = CalculatorEngine.activePendingOperator(calcState)

    val previewAmountMajorUnits: Long
        get() = (amount?.toMajorUnitLong() ?: 0L).coerceAtLeast(0L)
}

