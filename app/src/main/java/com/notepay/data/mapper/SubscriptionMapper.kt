package com.notepay.data.mapper

import com.notepay.data.local.entity.SubscriptionEntity
import com.notepay.domain.model.Money
import com.notepay.domain.model.Subscription
import kotlinx.datetime.Instant
import javax.inject.Inject

class SubscriptionMapper @Inject constructor() {

    fun toDomain(entity: SubscriptionEntity): Subscription = Subscription(
        id = entity.id,
        name = entity.name,
        amount = Money(entity.amountCents),
        category = entity.category,
        nextDueDate = Instant.fromEpochMilliseconds(entity.nextDueDate),
        repeatMonths = entity.repeatMonths,
        remindDaysBefore = entity.remindDaysBefore,
        note = entity.note,
        isActive = entity.isActive,
        createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
    )

    fun toEntity(subscription: Subscription): SubscriptionEntity = SubscriptionEntity(
        id = subscription.id,
        name = subscription.name,
        amountCents = subscription.amount.amountInCents,
        category = subscription.category,
        nextDueDate = subscription.nextDueDate.toEpochMilliseconds(),
        repeatMonths = subscription.repeatMonths,
        remindDaysBefore = subscription.remindDaysBefore,
        note = subscription.note,
        isActive = subscription.isActive,
        createdAt = subscription.createdAt.toEpochMilliseconds(),
    )
}
