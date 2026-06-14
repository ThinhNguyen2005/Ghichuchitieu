package com.notepay.domain.repository

import com.notepay.domain.model.Subscription
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface SubscriptionRepository {
    fun observeAll(): Flow<List<Subscription>>
    fun observeUpcoming(beforeDate: Instant): Flow<List<Subscription>>
    suspend fun upsert(subscription: Subscription): Long
    suspend fun getById(id: Long): Subscription?
    suspend fun delete(id: Long)
    suspend fun updateNextDueDate(id: Long, newDueDate: Instant)
}
