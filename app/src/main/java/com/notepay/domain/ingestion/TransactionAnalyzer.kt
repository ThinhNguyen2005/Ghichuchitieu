package com.notepay.domain.ingestion

import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.CurrencyParserEngine
import com.notepay.domain.money.MoneyParseResult

/** Single Source of Truth for analyzing transaction text payloads across all input sources */
object TransactionAnalyzer {

    // Regex ngân hàng chung: GD +100,000 VND hoặc PS: -50,000VND hoặc Biến động số dư: +1.000.000đ
    private val BANK_TRANSACTION_REGEX = Regex(
        """(?:GD|Giao dịch|PS|Biến động số dư)\s*:?\s*([+-])\s*([0-9.,\s]+)\s*(?:VND|đ)""",
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

    // Cú pháp tự do tiếng Việt: "chuyển 50k ăn sáng", "nhận 2tr tiền lương", "rút 500k"
    private val NATURAL_EXPENSE_REGEX = Regex(
        """(?:chuyển|chi|trả|rút|mua|ăn)\s+([0-9.,\s]+[kMmbtýTr]*)\s*(.*)""",
        RegexOption.IGNORE_CASE
    )
    private val NATURAL_INCOME_REGEX = Regex(
        """(?:nhận|thu|lương|nạp)\s+([0-9.,\s]+[kMmbtýTr]*)\s*(.*)""",
        RegexOption.IGNORE_CASE
    )

    // Regex trích xuất nội dung ghi chú sau ND: hoặc lời nhắn:
    private val NOTE_REGEX = Regex(
        """(?:ND|nội dung|lời nhắn|cho|từ)\s*:\s*(.*)""",
        RegexOption.IGNORE_CASE
    )

    fun analyze(payload: RawTransactionPayload): ParsedTransactionResult {
        val text = payload.rawText.trim()
        if (text.isBlank()) {
            return ParsedTransactionResult.Unrecognized(payload, "Nội dung văn bản rỗng")
        }

        // 1. Phân tích thông báo ngân hàng dạng chung (GD +100.000 VND hoặc PS: -50.000đ)
        val bankMatch = BANK_TRANSACTION_REGEX.find(text)
        if (bankMatch != null) {
            val sign = bankMatch.groupValues[1]
            val amountStr = bankMatch.groupValues[2]
            val parseResult = CurrencyParserEngine.parseOcrOrQrPayload(amountStr)
            if (parseResult is MoneyParseResult.Success) {
                val type = if (sign == "+") TransactionType.INCOME else TransactionType.EXPENSE
                val note = extractNote(text) ?: payload.title ?: "Giao dịch ngân hàng"
                val categoryId = suggestCategory(note)
                return ParsedTransactionResult.Success(
                    amount = parseResult.money,
                    type = type,
                    note = note,
                    suggestedCategoryId = categoryId,
                    source = payload.source,
                    rawPayload = payload
                )
            }
        }

        // 2. Phân tích thông báo Momo chi tiêu / thanh toán
        val momoPayMatch = MOMO_PAY_REGEX.find(text)
        if (momoPayMatch != null) {
            val amountStr = momoPayMatch.groupValues[1]
            val parseResult = CurrencyParserEngine.parseOcrOrQrPayload(amountStr)
            if (parseResult is MoneyParseResult.Success) {
                val note = extractNote(text) ?: "Momo thanh toán"
                val categoryId = suggestCategory(note)
                return ParsedTransactionResult.Success(
                    amount = parseResult.money,
                    type = TransactionType.EXPENSE,
                    note = note,
                    suggestedCategoryId = categoryId,
                    source = payload.source,
                    rawPayload = payload
                )
            }
        }

        // 3. Phân tích thông báo Momo nhận tiền
        val momoReceiveMatch = MOMO_RECEIVE_REGEX.find(text)
        if (momoReceiveMatch != null) {
            val amountStr = momoReceiveMatch.groupValues[1]
            val parseResult = CurrencyParserEngine.parseOcrOrQrPayload(amountStr)
            if (parseResult is MoneyParseResult.Success) {
                val note = extractNote(text) ?: "Momo nhận tiền"
                val categoryId = suggestCategory(note)
                return ParsedTransactionResult.Success(
                    amount = parseResult.money,
                    type = TransactionType.INCOME,
                    note = note,
                    suggestedCategoryId = categoryId,
                    source = payload.source,
                    rawPayload = payload
                )
            }
        }

        // 4. Phân tích cú pháp văn bản tự do (tiếng Việt tự nhiên)
        val natExpenseMatch = NATURAL_EXPENSE_REGEX.find(text)
        if (natExpenseMatch != null) {
            val amountStr = natExpenseMatch.groupValues[1]
            val noteStr = natExpenseMatch.groupValues[2].trim().ifBlank { text }
            val parseResult = CurrencyParserEngine.parseInputText(amountStr)
            if (parseResult is MoneyParseResult.Success) {
                val categoryId = suggestCategory(noteStr)
                return ParsedTransactionResult.Success(
                    amount = parseResult.money,
                    type = TransactionType.EXPENSE,
                    note = noteStr,
                    suggestedCategoryId = categoryId,
                    source = payload.source,
                    rawPayload = payload
                )
            }
        }

        val natIncomeMatch = NATURAL_INCOME_REGEX.find(text)
        if (natIncomeMatch != null) {
            val amountStr = natIncomeMatch.groupValues[1]
            val noteStr = natIncomeMatch.groupValues[2].trim().ifBlank { text }
            val parseResult = CurrencyParserEngine.parseInputText(amountStr)
            if (parseResult is MoneyParseResult.Success) {
                val categoryId = suggestCategory(noteStr)
                return ParsedTransactionResult.Success(
                    amount = parseResult.money,
                    type = TransactionType.INCOME,
                    note = noteStr,
                    suggestedCategoryId = categoryId,
                    source = payload.source,
                    rawPayload = payload
                )
            }
        }

        // 5. Thử parse trực tiếp số tiền nếu toàn bộ chuỗi là tiền tệ
        val directParse = CurrencyParserEngine.parseInputText(text)
        if (directParse is MoneyParseResult.Success) {
            return ParsedTransactionResult.Success(
                amount = directParse.money,
                type = TransactionType.EXPENSE,
                note = payload.title ?: "Chi tiêu",
                suggestedCategoryId = null,
                source = payload.source,
                rawPayload = payload
            )
        }

        return ParsedTransactionResult.Unrecognized(payload, "Không thể tự động nhận diện thông tin giao dịch")
    }

    private fun extractNote(body: String): String? {
        val match = NOTE_REGEX.find(body)
        if (match != null) {
            val note = match.groupValues[1].trim()
            return if (note.length > 200) note.substring(0, 197) + "..." else note
        }

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

    private fun suggestCategory(note: String): String? {
        val lower = note.lowercase()
        return when {
            lower.contains("ăn") || lower.contains("an") || lower.contains("cơm") || lower.contains("com") || lower.contains("bún") || lower.contains("phở") -> "FOOD"
            lower.contains("cafe") || lower.contains("cà phê") || lower.contains("trà sữa") -> "COFFEE"
            lower.contains("xăng") || lower.contains("xang") || lower.contains("petrolimex") -> "GAS"
            lower.contains("grab") || lower.contains("be") || lower.contains("xe") -> "TRANSPORT"
            lower.contains("lương") || lower.contains("luong") || lower.contains("thưởng") || lower.contains("thuong") -> "SALARY"
            lower.contains("điện") || lower.contains("dien") -> "ELECTRICITY"
            lower.contains("nước") || lower.contains("nuoc") -> "WATER"
            lower.contains("internet") || lower.contains("wifi") -> "INTERNET"
            else -> null
        }
    }
}
