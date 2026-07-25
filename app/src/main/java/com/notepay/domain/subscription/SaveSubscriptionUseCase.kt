package com.notepay.domain.subscription

import com.notepay.domain.money.Money
import kotlinx.datetime.Instant

/** Unified Subscription Validation & Creation Use Case */
interface SaveSubscriptionUseCase {
    suspend fun saveSubscription(
        id: Long?,
        name: String,
        amount: Money,
        repeatMonths: Int,
        remindDaysBefore: Int,
        startDate: Instant,
        categoryId: Long?
    ): Result<SubscriptionDomainModel>
}
