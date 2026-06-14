package com.notepay.data.mapper

import com.notepay.data.local.entity.BillSplitEntity
import com.notepay.domain.model.BillSplit
import com.notepay.domain.model.Money
import kotlinx.datetime.Instant
import javax.inject.Inject

class BillSplitMapper @Inject constructor() {

    fun toDomain(entity: BillSplitEntity): BillSplit = BillSplit(
        id = entity.id,
        transactionId = entity.transactionId,
        debtorName = entity.debtorName,
        amount = Money(entity.amountCents),
        isPaid = entity.isPaid,
        memoCode = entity.memoCode,
        paidAt = entity.paidAt?.let { Instant.fromEpochMilliseconds(it) },
        createdAt = Instant.fromEpochMilliseconds(entity.createdAt)
    )

    fun toEntity(domain: BillSplit): BillSplitEntity = BillSplitEntity(
        id = domain.id,
        transactionId = domain.transactionId,
        debtorName = domain.debtorName,
        amountCents = domain.amount.amountInCents,
        isPaid = domain.isPaid,
        memoCode = domain.memoCode,
        paidAt = domain.paidAt?.toEpochMilliseconds(),
        createdAt = domain.createdAt.toEpochMilliseconds()
    )
}
