package com.notepay.domain.usecase

import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

/**
 * Một khoản chi có dấu hiệu định kỳ được phát hiện từ lịch sử giao dịch.
 * [name] đã được chuẩn hóa nhãn thương hiệu nên UI chỉ việc hiển thị.
 */
data class SubscriptionCandidate(
    val name: String,
    val amountInCents: Long,
    val category: Category,
    val repeatMonths: Int,
    val nextDueEpochMs: Long,
)

/**
 * Phát hiện gói dịch vụ định kỳ: hai khoản chi cùng danh mục, lệch nhau 27-33 ngày và
 * chênh nhau không quá một bước nhảy giá trị (5.000 ₫) được xem là một khoản thu định kỳ.
 *
 * Logic từng nằm trong lambda `combine()` của StatsViewModel. [displayNameOf] được truyền vào
 * từ tầng hiển thị để domain không phải phụ thuộc Android resource, đồng thời giữ đúng hành vi
 * so trùng tên với danh mục cục bộ.
 */
object DetectSubscriptionUseCase {

    /** Bước nhảy giá trị khi gom nhóm (5.000 ₫ = 500.000 cent). */
    private const val AMOUNT_BUCKET_CENTS = 500_000L

    /** Khoảng cách ngày được coi là "lặp lại hàng tháng". */
    private const val MIN_CYCLE_DAYS = 27L
    private const val MAX_CYCLE_DAYS = 33L

    operator fun invoke(
        transactions: List<Transaction>,
        activeSubscriptionNames: List<String>,
        displayNameOf: (Category) -> String,
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): List<SubscriptionCandidate> {
        val candidates = mutableListOf<SubscriptionCandidate>()

        val groupedExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category.id to it.amount.amountInCents / AMOUNT_BUCKET_CENTS }

        for ((_, txList) in groupedExpenses) {
            if (txList.size < 2) continue
            val sorted = txList.sortedBy { it.occurredAt }
            for (i in 0 until sorted.size - 1) {
                val first = sorted[i]
                val second = sorted[i + 1]
                val daysBetween = (second.occurredAt - first.occurredAt).inWholeDays
                if (daysBetween !in MIN_CYCLE_DAYS..MAX_CYCLE_DAYS) continue

                val possibleName = cleanName(
                    second.note.ifBlank { displayNameOf(second.category) },
                )
                val alreadyRegistered = activeSubscriptionNames.any { name ->
                    name.contains(possibleName, ignoreCase = true) ||
                        possibleName.contains(name, ignoreCase = true)
                }
                if (alreadyRegistered) continue

                candidates += SubscriptionCandidate(
                    name = possibleName,
                    amountInCents = second.amount.amountInCents,
                    category = second.category,
                    repeatMonths = 1,
                    nextDueEpochMs = second.occurredAt.plus(DatePeriod(months = 1), zone).toEpochMilliseconds(),
                )
                // Một khoản chi chỉ đề xuất một lần, tránh trùng lặp trong cùng nhóm.
                break
            }
        }

        return candidates
    }

    /**
     * Chuẩn hóa nhãn hiển thị của khoản chi định kỳ. Thương hiệu là danh từ riêng nên giữ
     * nguyên dạng, không đưa vào tài nguyên đa ngôn ngữ.
     */
    private fun cleanName(note: String): String {
        val lower = note.lowercase()
        return when {
            lower.contains("netflix") -> "Netflix"
            lower.contains("spotify") -> "Spotify"
            lower.contains("youtube") -> "YouTube Premium"
            lower.contains("icloud") -> "iCloud"
            lower.contains("google") -> "Google One"
            lower.contains("canva") -> "Canva"
            lower.contains("microsoft") || lower.contains("office365") -> "Microsoft 365"
            else -> note.trim().replaceFirstChar { it.uppercase() }
        }
    }
}
