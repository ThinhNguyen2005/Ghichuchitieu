package com.notepay.feature.transaction.form

import android.net.Uri
import com.notepay.domain.model.Category
import com.notepay.domain.model.TransactionType
import com.notepay.domain.natural.NaturalTransactionDraft
import com.notepay.feature.transaction.component.CalcKey
import kotlin.time.Instant

/**
 * Unified events cho TransactionFormScreen (Add + Edit) theo pattern UDF.
 */
sealed interface TransactionFormUiEvent {

    // ── Calculator keypad ─────────────────────────────────────────────────────
    data class CalcKeyPressed(val key: CalcKey) : TransactionFormUiEvent
    data object BackspaceLong : TransactionFormUiEvent

    // ── Form fields ───────────────────────────────────────────────────────────
    data class TypeChanged(val type: TransactionType) : TransactionFormUiEvent
    data class CategorySelected(val category: Category) : TransactionFormUiEvent
    data class NoteChanged(val note: String) : TransactionFormUiEvent
    data class DateChanged(val instant: Instant) : TransactionFormUiEvent
    data class WalletChanged(val walletId: Long) : TransactionFormUiEvent

    /** OCR: user chọn ảnh giao dịch từ thư viện */
    data class ImagePicked(val uri: Uri) : TransactionFormUiEvent

    /** Tạo danh mục tùy biến mới từ CategoryPickerSheet */
    data class CreateCategory(
        val displayName: String,
        val colorArgb: Long,
        val iconId: String,
        val isIncome: Boolean,
    ) : TransactionFormUiEvent

    // ── Sheet / Dialog visibility ─────────────────────────────────────────────
    data class SetDatePickerVisible(val visible: Boolean) : TransactionFormUiEvent
    data class SetWalletPickerVisible(val visible: Boolean) : TransactionFormUiEvent
    data class SetCategoryPickerVisible(val visible: Boolean) : TransactionFormUiEvent
    data class SetNoteSheetVisible(val visible: Boolean) : TransactionFormUiEvent

    // ── Natural language quick entry ──────────────────────────────────────────
    data class NaturalInputChanged(val input: String) : TransactionFormUiEvent
    data object AnalyzeNaturalInputWithAi : TransactionFormUiEvent
    data class UpdateNaturalDraft(val draft: NaturalTransactionDraft) : TransactionFormUiEvent
    data class RemoveNaturalDraft(val draftId: String) : TransactionFormUiEvent
    data object SaveNaturalDrafts : TransactionFormUiEvent

    // ── Actions ───────────────────────────────────────────────────────────────
    data object Save : TransactionFormUiEvent
}

