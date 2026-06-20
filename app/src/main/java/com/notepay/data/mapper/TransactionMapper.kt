package com.notepay.data.mapper

import com.notepay.data.local.entity.TransactionEntity
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import kotlinx.datetime.Instant
import javax.inject.Inject

class TransactionMapper @Inject constructor() {

    fun toDomain(entity: TransactionEntity): Transaction = Transaction(
        id = entity.id,
        amount = Money(entity.amountCents),
        type = TransactionType.valueOf(entity.type),
        category = Category.safeValueOf(entity.category),
        note = entity.note,
        occurredAt = Instant.fromEpochMilliseconds(entity.occurredAt),
        walletId = entity.walletId,
        isAutoCapture = entity.isAutoCapture,
        isInternalTransfer = entity.isInternalTransfer,
        createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
    )

    fun toEntity(transaction: Transaction): TransactionEntity = TransactionEntity(
        id = transaction.id,
        amountCents = transaction.amount.amountInCents,
        type = transaction.type.name,
        category = transaction.category.name,
        note = transaction.note,
        occurredAt = transaction.occurredAt.toEpochMilliseconds(),
        walletId = transaction.walletId,
        isAutoCapture = transaction.isAutoCapture,
        isInternalTransfer = transaction.isInternalTransfer,
        createdAt = transaction.createdAt.toEpochMilliseconds(),
    )
}
