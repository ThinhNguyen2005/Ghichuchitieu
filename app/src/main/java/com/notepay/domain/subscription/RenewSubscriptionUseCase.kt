package com.notepay.domain.subscription

import kotlinx.datetime.TimeZone

/** Pure Domain Use Case for Renewing / Advancing Subscriptions */
interface RenewSubscriptionUseCase {
    suspend fun renewSubscription(
        subscriptionId: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Result<SubscriptionDomainModel>
}
