package com.notepay.domain.ingestion

import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType

enum class TransactionInputSource {
    NOTIFICATION,
    OCR_SCREENSHOT,
    PASTED_TEXT,
    MANUAL_INPUT
}

data class RawTransactionPayload(
    val rawText: String,
    val source: TransactionInputSource,
    val title: String? = null,
    val packageName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface ParsedTransactionResult {
    data class Success(
        val amount: Money,
        val type: TransactionType,
        val note: String,
        val suggestedCategoryId: String? = null,
        val source: TransactionInputSource,
        val rawPayload: RawTransactionPayload
    ) : ParsedTransactionResult

    data class Unrecognized(
        val rawPayload: RawTransactionPayload,
        val reason: String
    ) : ParsedTransactionResult
}
