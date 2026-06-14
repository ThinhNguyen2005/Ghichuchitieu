package com.notepay.data.repository

import com.notepay.data.local.dao.BillSplitDao
import com.notepay.data.mapper.BillSplitMapper
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.BillSplit
import com.notepay.domain.repository.BillSplitRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillSplitRepositoryImpl @Inject constructor(
    private val dao: BillSplitDao,
    private val mapper: BillSplitMapper,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : BillSplitRepository {

    override fun observeAll(): Flow<List<BillSplit>> =
        dao.observeAll().map { list -> list.map(mapper::toDomain) }.flowOn(dispatcher)

    override fun observeByTransaction(transactionId: Long): Flow<List<BillSplit>> =
        dao.observeByTransaction(transactionId).map { list -> list.map(mapper::toDomain) }.flowOn(dispatcher)

    override fun observeUnpaid(): Flow<List<BillSplit>> =
        dao.observeUnpaid().map { list -> list.map(mapper::toDomain) }.flowOn(dispatcher)

    override fun observePaid(): Flow<List<BillSplit>> =
        dao.observePaid().map { list -> list.map(mapper::toDomain) }.flowOn(dispatcher)

    override suspend fun getByMemoCode(memoCode: String): BillSplit? =
        dao.getByMemoCode(memoCode)?.let(mapper::toDomain)

    override suspend fun getById(id: Long): BillSplit? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun upsert(billSplit: BillSplit): Long =
        dao.upsert(mapper.toEntity(billSplit))

    override suspend fun upsertAll(billSplits: List<BillSplit>) =
        dao.upsertAll(billSplits.map(mapper::toEntity))

    override suspend fun delete(id: Long) =
        dao.delete(id)

    override suspend fun markAsPaid(id: Long, paidAt: Instant) =
        dao.markAsPaid(id, paidAt.toEpochMilliseconds())
}
