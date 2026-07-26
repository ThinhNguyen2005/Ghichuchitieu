package com.notepay.domain.notification

import com.notepay.domain.ingestion.ParsedTransactionResult
import com.notepay.domain.ingestion.RawTransactionPayload
import com.notepay.domain.ingestion.TransactionAnalyzer
import com.notepay.domain.ingestion.TransactionInputSource
import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType

data class ParsedNotification(
    val amount: Money,
    val type: TransactionType,
    val note: String,
    val sourcePackage: String = "",
)

object NotificationParser {
    fun parse(title: String?, body: String?): ParsedNotification? {
        if (body.isNullOrBlank()) return null
        val payload = RawTransactionPayload(
            rawText = body,
            source = TransactionInputSource.NOTIFICATION,
            title = title
        )
        return when (val result = TransactionAnalyzer.analyze(payload)) {
            is ParsedTransactionResult.Success -> ParsedNotification(
                amount = result.amount,
                type = result.type,
                note = result.note
            )
            is ParsedTransactionResult.Unrecognized -> null
        }
    }
}
