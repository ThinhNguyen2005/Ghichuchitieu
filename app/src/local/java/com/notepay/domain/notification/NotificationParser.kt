package com.notepay.domain.notification

import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType

enum class NotificationAmountSource {
    BANK_POSTED_TRANSACTION,
    WALLET_PAYMENT,
    UNKNOWN,
}

data class ParsedNotification(
    val amount: Money,
    val type: TransactionType,
    val note: String,
    /** Package name của app g\u1eedi th\u00f4ng b\u00e1o (ng\u00e2n h\u00e0ng/v\u00ed). D\u00f9ng \u0111\u1ec3 nh\u1eadn bi\u1ebft ngu\u1ed3n chuy\u1ec3n kho\u1ea3n n\u1ed9i b\u1ed9. */
    val sourcePackage: String = "",
    val amountSource: NotificationAmountSource = NotificationAmountSource.UNKNOWN,
    val hasExplicitDirection: Boolean = false,
    val hasTransactionContext: Boolean = false,
)

object NotificationParser {

    /** Số tiền lớn nhất (đơn vị VND) còn nhân được với 100 mà không tràn Long. */
    private const val MAX_MAJOR_UNITS = Long.MAX_VALUE / 100L

    // Chỉ nhận số tiền sau trường giao dịch. Không nhận số sau SD/Số dư/Phí.
    private val BANK_TRANSACTION_REGEX = Regex(
        """(?:^|[\n|:])\s*(?:Số\s+tiền\s*(?:GD|giao\s+dịch)|Giao\s+dịch|GD|PS)\s*:?\s*([+-])?\s*([0-9.,\s]+)\s*(?:VND|đ)""",
        RegexOption.IGNORE_CASE
    )

    // Regex Momo thanh toán / chuyển tiền / nhận tiền
    private val MOMO_PAY_REGEX = Regex(
        """(?:thanh toán|chuyển)\s*([0-9.,\s]+)\s*(?:đ|VND)""",
        RegexOption.IGNORE_CASE
    )
    private val MOMO_RECEIVE_REGEX = Regex(
        """nhận\s*([0-9.,\s]+)\s*(?:đ|VND)""",
        RegexOption.IGNORE_CASE
    )

    // Regex trích xuất nội dung giao dịch (nằm sau ND: hoặc lời nhắn)
    private val NOTE_REGEX = Regex(
        """(?:ND|nội dung(?:\s+GD)?|lời nhắn|cho|từ)\s*:\s*(.*)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(title: String?, body: String?): ParsedNotification? {
        if (body.isNullOrBlank()) return null

        val normalizedBody = body.trim()

        // 1. Phân tích thông báo ngân hàng dạng chung (GD +100.000 VND hoặc GD -50.000 đ)
        val bankMatch = BANK_TRANSACTION_REGEX.find(normalizedBody)
        if (bankMatch != null) {
            val sign = bankMatch.groupValues[1].takeIf { it.isNotBlank() }
            val amountStr = bankMatch.groupValues[2]
            val amount = parseAmount(amountStr) ?: return null
            val type = sign?.let {
                if (it == "+") TransactionType.INCOME else TransactionType.EXPENSE
            } ?: inferType(normalizedBody) ?: return null
            val note = extractNote(normalizedBody) ?: title ?: "Giao dịch ngân hàng"
            return ParsedNotification(
                amount = amount,
                type = type,
                note = note,
                amountSource = NotificationAmountSource.BANK_POSTED_TRANSACTION,
                hasExplicitDirection = sign != null,
                hasTransactionContext = hasTransactionContext(title, normalizedBody),
            )
        }

        // 2. Phân tích thông báo Momo chi tiêu / thanh toán
        val momoPayMatch = MOMO_PAY_REGEX.find(normalizedBody)
        if (momoPayMatch != null) {
            val amountStr = momoPayMatch.groupValues[1]
            val amount = parseAmount(amountStr) ?: return null
            val note = extractNote(normalizedBody) ?: "Momo thanh toán"
            return ParsedNotification(
                amount = amount,
                type = TransactionType.EXPENSE,
                note = note,
                amountSource = NotificationAmountSource.WALLET_PAYMENT,
                hasExplicitDirection = true,
                hasTransactionContext = true,
            )
        }

        // 3. Phân tích thông báo Momo nhận tiền
        val momoReceiveMatch = MOMO_RECEIVE_REGEX.find(normalizedBody)
        if (momoReceiveMatch != null) {
            val amountStr = momoReceiveMatch.groupValues[1]
            val amount = parseAmount(amountStr) ?: return null
            val note = extractNote(normalizedBody) ?: "Momo nhận tiền"
            return ParsedNotification(
                amount = amount,
                type = TransactionType.INCOME,
                note = note,
                amountSource = NotificationAmountSource.WALLET_PAYMENT,
                hasExplicitDirection = true,
                hasTransactionContext = true,
            )
        }

        return null
    }

    private fun parseAmount(text: String): Money? {
        // Loại bỏ khoảng trắng, dấu chấm, dấu phẩy phân cách hàng nghìn
        val cleanText = text.replace(Regex("""[\s.,]"""), "")
        val majorUnits = cleanText.toLongOrNull() ?: return null
        // Chặn trên trước khi nhân 100, nếu không phép nhân sẽ wrap sang số âm. Dùng cùng
        // ngưỡng với đường nhập tay ở AmountParser để hai đường vào không lệch validation.
        if (majorUnits <= 0 || majorUnits > MAX_MAJOR_UNITS) return null
        return Money(majorUnits * 100) // Đổi sang cents
    }

    private fun extractNote(body: String): String? {
        // Tìm nội dung sau ND:, Lời nhắn: v.v.
        val match = NOTE_REGEX.find(body)
        if (match != null) {
            val note = match.groupValues[1].trim()
            // Cắt bớt nếu nội dung quá dài (Transaction.MAX_NOTE_LENGTH = 200)
            return if (note.length > 200) note.substring(0, 197) + "..." else note
        }
        
        // Nếu không có ND: cụ thể, tìm xem Momo thanh toán "cho [Cửa hàng]"
        val forMatch = Regex("""cho\s+([^.]+)(?:\.|\z)""", RegexOption.IGNORE_CASE).find(body)
        if (forMatch != null) {
            return "Thanh toán cho " + forMatch.groupValues[1].trim()
        }

        val fromMatch = Regex("""từ\s+([^.]+)(?:\.|\z)""", RegexOption.IGNORE_CASE).find(body)
        if (fromMatch != null) {
            return "Nhận tiền từ " + fromMatch.groupValues[1].trim()
        }

        return null
    }

    private fun inferType(body: String): TransactionType? {
        val lower = body.lowercase()
        return when {
            listOf("nhận", "nhan", "ghi có", "ghi co", "tiền vào", "tien vao").any(lower::contains) ->
                TransactionType.INCOME
            listOf("chuyển", "chuyen", "thanh toán", "thanh toan", "ghi nợ", "ghi no", "tiền ra", "tien ra")
                .any(lower::contains) -> TransactionType.EXPENSE
            else -> null
        }
    }

    private fun hasTransactionContext(title: String?, body: String): Boolean {
        val source = listOfNotNull(title, body).joinToString(" ")
        return Regex(
            """(?i)(TKTT|TK|tài khoản|tai khoan|ngày|ngay|thời gian|thoi gian|nội dung|noi dung|ND|số dư|so du|SD|mã GD|ma GD|SO GD)""",
        ).containsMatchIn(source)
    }
}

