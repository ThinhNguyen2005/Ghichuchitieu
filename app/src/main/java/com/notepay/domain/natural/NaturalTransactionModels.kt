package com.notepay.domain.natural

import androidx.compose.runtime.Immutable
import com.notepay.domain.model.Category
import com.notepay.domain.model.TransactionType
import kotlinx.datetime.LocalDate

@Immutable
data class NaturalTransactionDraft(
    val id: String,
    val amountVnd: Long,
    val type: TransactionType,
    val category: Category,
    val note: String,
    val date: LocalDate,
    val confidence: Float,
)

enum class NaturalParseSource {
    ON_DEVICE,
    AI_FALLBACK,
}

enum class NaturalFallbackNotice {
    NONE,
    AI_UNAVAILABLE,
}

@Immutable
data class NaturalParseResult(
    val drafts: List<NaturalTransactionDraft>,
    val confidence: Float,
    val needsAiFallback: Boolean,
    val source: NaturalParseSource = NaturalParseSource.ON_DEVICE,
    val fallbackNotice: NaturalFallbackNotice = NaturalFallbackNotice.NONE,
)
