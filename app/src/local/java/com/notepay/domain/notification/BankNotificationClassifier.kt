package com.notepay.domain.notification

import com.notepay.data.preferences.KnownBankApps
import java.security.MessageDigest
import java.util.Locale

enum class NotificationDecision {
    CONFIRMED,
    PENDING,
    REJECTED,
}

enum class LearnedNotificationDecision {
    NONE,
    APPROVED,
    REJECTED,
}

data class ClassifiedNotification(
    val decision: NotificationDecision,
    val parsed: ParsedNotification? = null,
    val primaryPackageName: String? = null,
    val fingerprint: String? = null,
    val dedupeKey: String? = null,
    val reason: String? = null,
)

object NotificationFingerprint {
    private data class FieldMarker(
        val name: String,
        val pattern: Regex,
    )

    private val fieldMarkers = listOf(
        FieldMarker(
            "amount:số_tiền_gd",
            Regex("(?:^|[|\\n:])\\s*số\\s+tiền\\s*(?:gd|giao\\s+d(?:ịch|ich))", RegexOption.IGNORE_CASE),
        ),
        FieldMarker(
            "amount:giao_dich",
            Regex("(?:^|[|\\n:])\\s*giao\\s+d(?:ịch|ich)", RegexOption.IGNORE_CASE),
        ),
        FieldMarker(
            "amount:gd",
            Regex("(?:^|[|\\n:])\\s*gd\\b", RegexOption.IGNORE_CASE),
        ),
        FieldMarker(
            "amount:ps",
            Regex("(?:^|[|\\n:])\\s*ps\\b", RegexOption.IGNORE_CASE),
        ),
        FieldMarker(
            "account:tktt",
            Regex("(?:^|[|\\n:])\\s*tktt\\b", RegexOption.IGNORE_CASE),
        ),
        FieldMarker(
            "account:tk",
            Regex("(?:^|[|\\n:])\\s*(?:tài\\s+khoản|tai\\s+khoan|tk)\\b", RegexOption.IGNORE_CASE),
        ),
        FieldMarker(
            "date:time",
            Regex("(?:^|[|\\n:])\\s*(?:ngày|ngay|thời\\s+gian|thoi\\s+gian)", RegexOption.IGNORE_CASE),
        ),
        FieldMarker(
            "date:value",
            Regex("\\b\\d{1,4}[/-]\\d{1,2}[/-]\\d{1,4}\\b"),
        ),
        FieldMarker(
            "balance",
            Regex("(?:^|[|\\n:])\\s*(?:số\\s+dư|so\\s+du|sd)\\b", RegexOption.IGNORE_CASE),
        ),
        FieldMarker(
            "fee",
            Regex("(?:^|[|\\n:])\\s*phí\\b|(?:^|[|\\n:])\\s*phi\\b", RegexOption.IGNORE_CASE),
        ),
        FieldMarker(
            "note",
            Regex("(?:^|[|\\n:])\\s*(?:nội\\s+dung(?:\\s+gd)?|noi\\s+dung(?:\\s+gd)?|nd)\\b", RegexOption.IGNORE_CASE),
        ),
        FieldMarker(
            "transaction_id",
            Regex("(?:^|[|\\n:])\\s*(?:mã\\s+gd|ma\\s+gd|so\\s+gd)\\b", RegexOption.IGNORE_CASE),
        ),
    )

    fun create(primaryPackageName: String, body: String): String =
        digest(primaryPackageName + "\n" + structuralKey(body))

    fun createDedupe(primaryPackageName: String, title: String?, body: String): String =
        digest(primaryPackageName + "\n" + title.orEmpty() + "\n" + body)

    private fun structuralKey(value: String): String {
        val occurrences = fieldMarkers
            .flatMap { marker ->
                marker.pattern.findAll(value).map { marker.name to it.range.first }.toList()
            }
            .sortedBy { it.second }
            .map { it.first }
        val direction = when {
            Regex(
                "(?:số\\s+tiền\\s*(?:gd|giao\\s+d(?:ịch|ich))|giao\\s+d(?:ịch|ich)|gd|ps)\\s*:?[ \\t]*\\+",
                RegexOption.IGNORE_CASE,
            )
                .containsMatchIn(value) -> "positive"
            Regex(
                "(?:số\\s+tiền\\s*(?:gd|giao\\s+d(?:ịch|ich))|giao\\s+d(?:ịch|ich)|gd|ps)\\s*:?[ \\t]*-",
                RegexOption.IGNORE_CASE,
            )
                .containsMatchIn(value) -> "negative"
            else -> "unsigned"
        }
        val layout = listOfNotNull(
            if (value.contains('|')) "pipe" else null,
            if (value.contains('\n')) "multiline" else null,
        ).ifEmpty { listOf("single_line") }
        return listOf(
            "fields=${occurrences.joinToString(">")}",
            "direction=$direction",
            "layout=${layout.joinToString("+")}",
            "field_count=${occurrences.size}",
        ).joinToString("|")
    }

    private fun digest(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(Locale.ROOT, it) }
    }
}

object BankNotificationClassifier {
    private val hardRejectPatterns = listOf(
        Regex("\\b(otp|one[- ]time password)\\b", RegexOption.IGNORE_CASE),
        Regex("mã\\s+(xác\\s+thực|otp)|ma\\s+(xac\\s+thuc|otp)", RegexOption.IGNORE_CASE),
        Regex("ưu\\s+đãi|uu\\s+dai|khuyến\\s+mãi|khuyen\\s+mai", RegexOption.IGNORE_CASE),
        Regex("voucher|cơ\\s+hội|co\\s+hoi|lãi\\s+suất|lai\\s+suat", RegexOption.IGNORE_CASE),
        Regex("hoàn\\s+tiền\\s+đến|hoan\\s+tien\\s+den", RegexOption.IGNORE_CASE),
        Regex("mở\\s+tài\\s+khoản|mo\\s+tai\\s+khoan|quà\\s+tặng|qua\\s+tang", RegexOption.IGNORE_CASE),
    )

    fun classify(
        packageName: String,
        title: String?,
        body: String?,
        learnedDecision: LearnedNotificationDecision = LearnedNotificationDecision.NONE,
        learnedFingerprint: String? = null,
    ): ClassifiedNotification {
        val primaryPackageName = KnownBankApps.getPrimaryPackageName(packageName)
        val text = body?.trim().orEmpty()
        val fingerprint = NotificationFingerprint.create(primaryPackageName, text)
        val dedupeKey = NotificationFingerprint.createDedupe(primaryPackageName, title, text)

        if (!KnownBankApps.isRecognized(packageName)) {
            return rejected(primaryPackageName, fingerprint, dedupeKey, "unsupported_source")
        }
        if (text.isBlank()) {
            return rejected(primaryPackageName, fingerprint, dedupeKey, "empty_notification")
        }
        if (hardRejectPatterns.any { it.containsMatchIn(text) || it.containsMatchIn(title.orEmpty()) }) {
            return rejected(primaryPackageName, fingerprint, dedupeKey, "hard_reject_content")
        }

        val parsed = NotificationParser.parse(title, text)
            ?: return rejected(primaryPackageName, fingerprint, dedupeKey, "no_posted_transaction_amount")
        if (parsed.amountSource != NotificationAmountSource.BANK_POSTED_TRANSACTION) {
            return rejected(primaryPackageName, fingerprint, dedupeKey, "not_a_bank_transaction_field")
        }

        if (learnedFingerprint == fingerprint) {
            when (learnedDecision) {
                LearnedNotificationDecision.APPROVED ->
                    return confirmed(primaryPackageName, fingerprint, dedupeKey, parsed)
                LearnedNotificationDecision.REJECTED ->
                    return rejected(primaryPackageName, fingerprint, dedupeKey, "learned_rejection")
                LearnedNotificationDecision.NONE -> Unit
            }
        }

        return if (
            KnownBankApps.isVerified(packageName) &&
            parsed.hasExplicitDirection &&
            parsed.hasTransactionContext
        ) {
            confirmed(primaryPackageName, fingerprint, dedupeKey, parsed)
        } else {
            ClassifiedNotification(
                decision = NotificationDecision.PENDING,
                parsed = parsed,
                primaryPackageName = primaryPackageName,
                fingerprint = fingerprint,
                dedupeKey = dedupeKey,
                reason = if (KnownBankApps.isVerified(packageName)) {
                    "missing_direction_or_context"
                } else {
                    "unverified_bank_requires_confirmation"
                },
            )
        }
    }

    private fun confirmed(
        primaryPackageName: String,
        fingerprint: String,
        dedupeKey: String,
        parsed: ParsedNotification,
    ) = ClassifiedNotification(
        decision = NotificationDecision.CONFIRMED,
        parsed = parsed,
        primaryPackageName = primaryPackageName,
        fingerprint = fingerprint,
        dedupeKey = dedupeKey,
    )

    private fun rejected(
        primaryPackageName: String,
        fingerprint: String,
        dedupeKey: String,
        reason: String,
    ) = ClassifiedNotification(
        decision = NotificationDecision.REJECTED,
        primaryPackageName = primaryPackageName,
        fingerprint = fingerprint,
        dedupeKey = dedupeKey,
        reason = reason,
    )
}
