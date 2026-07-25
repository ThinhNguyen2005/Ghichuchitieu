package com.notepay.domain.billsplit

import com.notepay.domain.model.BillSplit
import com.notepay.domain.model.Money
import java.text.Normalizer
import kotlin.time.Clock
import kotlinx.datetime.Instant

/** Pure Domain Engine for Non-Destructive Bill Split Logic & Memo Matching */
object BillSplitEngine {

    /** Removes Vietnamese accents for ASCII-safe bank memo compatibility */
    fun removeDiacritics(input: String): String {
        val nfdNormalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        val regex = Regex("""\p{InCombiningDiacriticalMarks}+""")
        return regex.replace(nfdNormalized, "").replace("Đ", "D").replace("đ", "d")
    }

    /** Formats standardized memo code string */
    fun formatMemoCode(transactionId: Long, debtorName: String): String {
        val asciiName = removeDiacritics(debtorName)
        val sanitized = asciiName.filter { it.isLetterOrDigit() || it.isWhitespace() }.trim()
        val slug = sanitized.uppercase().replace(Regex("""\s+"""), " ")
        return "NP${transactionId} $slug"
    }

    /**
     * Non-destructive debt settlement calculation.
     * Preserves original parent transaction and incoming income transaction intact.
     */
    fun settleSplit(
        split: BillSplit,
        paidAt: Instant = Clock.System.now()
    ): BillSplit {
        return split.copy(
            isPaid = true,
            paidAt = paidAt
        )
    }

    /** Matches unpaid split by memo code in incoming bank notification */
    fun matchByMemoCode(
        notificationText: String,
        unpaidSplits: List<BillSplit>
    ): BillSplit? {
        val parsedMemo = MemoCode.parse(notificationText) ?: return null
        return unpaidSplits.find { split ->
            val asciiMemo = removeDiacritics(split.memoCode).uppercase()
            split.transactionId == parsedMemo.transactionId &&
                    asciiMemo.contains(parsedMemo.debtorSlug, ignoreCase = true)
        }
    }
}
