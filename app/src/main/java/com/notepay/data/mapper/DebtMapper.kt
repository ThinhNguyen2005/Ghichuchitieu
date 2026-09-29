package com.notepay.data.mapper

import com.notepay.data.local.entity.DebtEntity
import com.notepay.data.local.entity.DebtPaymentEntity
import com.notepay.data.local.entity.DebtWithPayments
import com.notepay.domain.model.Money
import com.notepay.domain.model.debt.Debt
import com.notepay.domain.model.debt.DebtPayment
import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.model.debt.DebtWithHistory
import kotlin.time.Instant
import javax.inject.Inject

class DebtMapper @Inject constructor() {

    fun toDomainDebt(entity: DebtEntity): Debt = Debt(
        id = entity.id,
        personName = entity.personName,
        phoneNumber = entity.phoneNumber,
        type = DebtType.valueOf(entity.type),
        originalAmount = Money(entity.originalAmountCents),
        walletId = entity.walletId,
        createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
        dueDate = entity.dueDate?.let { Instant.fromEpochMilliseconds(it) },
        note = entity.note,
        isSettled = entity.isSettled,
    )

    fun toEntityDebt(domain: Debt): DebtEntity = DebtEntity(
        id = domain.id,
        personName = domain.personName,
        phoneNumber = domain.phoneNumber,
        type = domain.type.name,
        originalAmountCents = domain.originalAmount.amountInCents,
        walletId = domain.walletId,
        createdAt = domain.createdAt.toEpochMilliseconds(),
        dueDate = domain.dueDate?.toEpochMilliseconds(),
        note = domain.note,
        isSettled = domain.isSettled,
    )

    fun toDomainPayment(entity: DebtPaymentEntity): DebtPayment = DebtPayment(
        id = entity.id,
        debtId = entity.debtId,
        amount = Money(entity.amountCents),
        walletId = entity.walletId,
        paidAt = Instant.fromEpochMilliseconds(entity.paidAt),
        note = entity.note,
        transactionId = entity.transactionId,
    )

    fun toEntityPayment(domain: DebtPayment): DebtPaymentEntity = DebtPaymentEntity(
        id = domain.id,
        debtId = domain.debtId,
        amountCents = domain.amount.amountInCents,
        walletId = domain.walletId,
        paidAt = domain.paidAt.toEpochMilliseconds(),
        note = domain.note,
        transactionId = domain.transactionId,
    )

    fun toDomainWithHistory(entity: DebtWithPayments): DebtWithHistory = DebtWithHistory(
        debt = toDomainDebt(entity.debt),
        payments = entity.payments.map { toDomainPayment(it) },
    )
}
