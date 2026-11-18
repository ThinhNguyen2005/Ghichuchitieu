package com.notepay.feature.assets.main

import com.notepay.domain.money.Money

/** Lỗi của form chuyển khoản, mỗi giá trị ứng với đúng một chỗ hiển thị trên form. */
enum class TransferError {
    InvalidWalletPair,
    InvalidAmount,
    Failed,
}

/**
 * Kết quả xác thực form. Chỉ nhánh [Valid] mang dữ liệu đã kiểm tra, nên nơi nhận không
 * phải kiểm tra null lần thứ hai rồi mới dám gọi use case.
 */
sealed interface TransferValidation {
    data class Valid(
        val from: WalletAssetItem,
        val to: WalletAssetItem,
        val amount: Money,
    ) : TransferValidation

    data class Invalid(
        val walletError: TransferError? = null,
        val amountError: TransferError? = null,
    ) : TransferValidation
}

/** Giá trị VND nguyên lớn nhất còn biểu diễn được bằng [Money] (đơn vị 1/100 đồng). */
private const val MAX_WHOLE_VND = Long.MAX_VALUE / 100L

/**
 * Chuỗi chữ số (không có dấu phân nhóm) → tiền, hoặc null nếu ngoài miền hợp lệ.
 *
 * Trả null thay vì để phép nhân tràn: `Long` tràn vòng có thể biến một số tiền khổng lồ
 * thành số dương nhỏ và lọt qua kiểm tra `> 0`.
 */
fun parseWholeVnd(raw: String): Money? {
    val wholeVnd = raw.toLongOrNull() ?: return null
    if (wholeVnd <= 0L || wholeVnd > MAX_WHOLE_VND) return null
    return Money(wholeVnd * 100L)
}

/**
 * Nguồn xác thực duy nhất cho form chuyển khoản: UI dùng để bật nút và hiện lỗi từng
 * trường, ViewModel dùng lại ngay trước khi ghi dữ liệu.
 */
fun validateTransfer(
    fromWalletId: Long?,
    toWalletId: Long?,
    amountInput: String,
    wallets: List<WalletAssetItem>,
): TransferValidation {
    val from = wallets.firstOrNull { it.wallet.id == fromWalletId }
    val to = wallets.firstOrNull { it.wallet.id == toWalletId }
    val walletError = if (from == null || to == null || from.wallet.id == to.wallet.id) {
        TransferError.InvalidWalletPair
    } else {
        null
    }
    val amount = parseWholeVnd(amountInput)

    val isValidPair = from != null && to != null && walletError == null
    return if (isValidPair && amount != null) {
        TransferValidation.Valid(from = from, to = to, amount = amount)
    } else {
        TransferValidation.Invalid(
            walletError = walletError,
            amountError = if (amount == null) TransferError.InvalidAmount else null,
        )
    }
}
