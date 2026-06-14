package com.notepay.domain.notification

import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType

data class ParsedNotification(
    val amount: Money,
    val type: TransactionType,
    val note: String,
)

object NotificationParser {

    // Regex trích xuất ngân hàng chung: GD +100,000 VND hoặc GD -50,000VND hoặc PS: +100,000VND
    private val BANK_TRANSACTION_REGEX = Regex(
        """(?:GD|Giao dịch|PS)\s*:?\s*([+-])\s*([0-9.,\s]+)\s*(?:VND|đ)""",
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
        """(?:ND|nội dung|lời nhắn|cho|từ)\s*:\s*(.*)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(title: String?, body: String?): ParsedNotification? {
        if (body.isNullOrBlank()) return null

        val normalizedBody = body.trim()

        // 1. Phân tích thông báo ngân hàng dạng chung (GD +100.000 VND hoặc GD -50.000 đ)
        val bankMatch = BANK_TRANSACTION_REGEX.find(normalizedBody)
        if (bankMatch != null) {
            val sign = bankMatch.groupValues[1]
            val amountStr = bankMatch.groupValues[2]
            val amount = parseAmount(amountStr) ?: return null
            val type = if (sign == "+") TransactionType.INCOME else TransactionType.EXPENSE
            val note = extractNote(normalizedBody) ?: title ?: "Giao dịch ngân hàng"
            return ParsedNotification(amount, type, note)
        }

        // 2. Phân tích thông báo Momo chi tiêu / thanh toán
        val momoPayMatch = MOMO_PAY_REGEX.find(normalizedBody)
        if (momoPayMatch != null) {
            val amountStr = momoPayMatch.groupValues[1]
            val amount = parseAmount(amountStr) ?: return null
            val note = extractNote(normalizedBody) ?: "Momo thanh toán"
            return ParsedNotification(amount, TransactionType.EXPENSE, note)
        }

        // 3. Phân tích thông báo Momo nhận tiền
        val momoReceiveMatch = MOMO_RECEIVE_REGEX.find(normalizedBody)
        if (momoReceiveMatch != null) {
            val amountStr = momoReceiveMatch.groupValues[1]
            val amount = parseAmount(amountStr) ?: return null
            val note = extractNote(normalizedBody) ?: "Momo nhận tiền"
            return ParsedNotification(amount, TransactionType.INCOME, note)
        }

        return null
    }

    private fun parseAmount(text: String): Money? {
        // Loại bỏ khoảng trắng, dấu chấm, dấu phẩy phân cách hàng nghìn
        val cleanText = text.replace(Regex("""[\s.,]"""), "")
        val majorUnits = cleanText.toLongOrNull() ?: return null
        if (majorUnits <= 0) return null
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
}
