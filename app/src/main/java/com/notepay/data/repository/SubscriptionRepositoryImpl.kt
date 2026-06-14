package com.notepay.data.repository

import com.notepay.data.local.dao.SubscriptionDao
import com.notepay.data.mapper.SubscriptionMapper
import com.notepay.domain.model.Subscription
import com.notepay.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject

class SubscriptionRepositoryImpl @Inject constructor(
    private val dao: SubscriptionDao,
    private val mapper: SubscriptionMapper,
) : SubscriptionRepository {

    override fun observeAll(): Flow<List<Subscription>> =
        dao.observeAll().map { list -> list.map(mapper::toDomain) }

    override fun observeUpcoming(beforeDate: Instant): Flow<List<Subscription>> =
        dao.observeUpcoming(beforeDate.toEpochMilliseconds()).map { list -> list.map(mapper::toDomain) }

    override suspend fun upsert(subscription: Subscription): Long =
        dao.upsert(mapper.toEntity(subscription))

    override suspend fun getById(id: Long): Subscription? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun updateNextDueDate(id: Long, newDueDate: Instant) =
        dao.updateNextDueDate(id, newDueDate.toEpochMilliseconds())
}
