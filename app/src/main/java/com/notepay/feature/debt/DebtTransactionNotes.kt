package com.notepay.feature.debt

import android.content.Context
import androidx.annotation.StringRes
import com.notepay.R
import com.notepay.domain.model.DebtType

/**
 * Ghi chú giao dịch ví sinh ra khi người dùng bật đồng bộ ví.
 *
 * Chuỗi hiển thị nằm ở tầng feature thay vì use case: domain không phụ thuộc Android resource,
 * và cùng một khoản nợ có thể hiển thị khác nhau theo ngôn ngữ đang chọn.
 */

/** Ghi chú cho giao dịch tạo khoản nợ: cho vay là chi, đi vay là thu. */
internal fun Context.debtCreationTransactionNote(
    type: DebtType,
    personName: String,
    note: String,
): String = debtTransactionNote(
    personName = personName,
    note = note,
    plainRes = if (type == DebtType.LEND) R.string.debt_tx_note_lend else R.string.debt_tx_note_borrow,
    withNoteRes = if (type == DebtType.LEND) {
        R.string.debt_tx_note_lend_with_note
    } else {
        R.string.debt_tx_note_borrow_with_note
    },
)

/** Ghi chú cho giao dịch thu/trả nợ: thu nợ là thu, trả nợ là chi. */
internal fun Context.debtPaymentTransactionNote(
    type: DebtType,
    personName: String,
    note: String,
): String = debtTransactionNote(
    personName = personName,
    note = note,
    plainRes = if (type == DebtType.LEND) {
        R.string.debt_tx_note_repay_lend
    } else {
        R.string.debt_tx_note_repay_borrow
    },
    withNoteRes = if (type == DebtType.LEND) {
        R.string.debt_tx_note_repay_lend_with_note
    } else {
        R.string.debt_tx_note_repay_borrow_with_note
    },
)

private fun Context.debtTransactionNote(
    personName: String,
    note: String,
    @StringRes plainRes: Int,
    @StringRes withNoteRes: Int,
): String = if (note.isNotBlank()) getString(withNoteRes, personName, note) else getString(plainRes, personName)
