package com.notepay.domain.billsplit

/** Structured Memo Code for Payment Identification */
data class MemoCode(
    val transactionId: Long,
    val debtorSlug: String
) {
    fun toFormattedString(): String = "NP${transactionId} ${debtorSlug.uppercase()}"

    companion object {
        private val MEMO_REGEX = Regex("""NP(\d+)\s+([A-Z0-9]+)""", RegexOption.IGNORE_CASE)

        fun parse(text: String): MemoCode? {
            val match = MEMO_REGEX.find(text.trim()) ?: return null
            val txId = match.groupValues[1].toLongOrNull() ?: return null
            val slug = match.groupValues[2].uppercase()
            return MemoCode(txId, slug)
        }
    }
}
