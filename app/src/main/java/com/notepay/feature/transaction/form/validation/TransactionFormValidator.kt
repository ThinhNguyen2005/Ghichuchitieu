package com.notepay.feature.transaction.form.validation

import com.notepay.domain.model.Transaction
import com.notepay.domain.money.Money
import com.notepay.feature.transaction.form.calculator.AmountParseError
import com.notepay.feature.transaction.form.calculator.AmountParseResult
import com.notepay.feature.transaction.form.calculator.AmountParser

/** Lỗi xác thực của form Thêm/Sửa giao dịch. */
enum class FieldError {
    AMOUNT_EMPTY,
    AMOUNT_INVALID,
    NOTE_TOO_LONG,
    WALLET_MISSING,
}

/**
 * Quy tắc xác thực dùng chung cho màn Thêm và Sửa giao dịch.
 *
 * Trước đây mỗi ViewModel tự tính lại tập lỗi `AMOUNT_*` / `NOTE_TOO_LONG`, nên hai màn có thể
 * lệch nhau mỗi khi quy tắc thay đổi. Mọi ràng buộc về số tiền và độ dài ghi chú giờ nằm ở đây.
 */
object TransactionFormValidator {

    /** Kiểm tra toàn bộ form trước khi lưu. */
    fun validate(amount: Money?, walletId: Long?, note: String): Set<FieldError> = buildSet {
        if (amount == null || amount.amountInCents <= 0L) add(FieldError.AMOUNT_EMPTY)
        if (walletId == null) add(FieldError.WALLET_MISSING)
        if (note.length > Transaction.MAX_NOTE_LENGTH) add(FieldError.NOTE_TOO_LONG)
    }

    /** Lỗi ô số tiền khi giá trị đến từ ô nhập liệu văn bản. */
    fun amountErrors(current: Set<FieldError>, result: AmountParseResult): Set<FieldError> =
        withoutAmountErrors(current).let { base ->
            when (result.error) {
                AmountParseError.EMPTY -> base + FieldError.AMOUNT_EMPTY
                AmountParseError.INVALID -> base + FieldError.AMOUNT_INVALID
                null -> base
            }
        }

    /** Lỗi ô số tiền khi giá trị đến từ bàn phím máy tính tự chế. */
    fun amountErrors(current: Set<FieldError>, amountInCents: Long): Set<FieldError> =
        withoutAmountErrors(current).let { base ->
            if (amountInCents <= 0L) base + FieldError.AMOUNT_EMPTY else base
        }

    /** Lỗi ô ghi chú. */
    fun noteErrors(current: Set<FieldError>, noteLength: Int): Set<FieldError> =
        (current - FieldError.NOTE_TOO_LONG).let { base ->
            if (noteLength > Transaction.MAX_NOTE_LENGTH) base + FieldError.NOTE_TOO_LONG else base
        }

    /** Số tiền dương (đơn vị cent) đọc từ chuỗi người dùng nhập; null nếu không hợp lệ. */
    fun positiveAmountInCents(input: String): Long? =
        AmountParser.parse(input).amount?.amountInCents?.takeIf { it > 0L }

    private fun withoutAmountErrors(errors: Set<FieldError>): Set<FieldError> =
        errors - FieldError.AMOUNT_EMPTY - FieldError.AMOUNT_INVALID
}
