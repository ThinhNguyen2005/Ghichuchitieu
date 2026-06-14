package com.notepay.domain.repository

import com.notepay.domain.model.BillSplit
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface BillSplitRepository {
    fun observeAll(): Flow<List<BillSplit>>
    fun observeByTransaction(transactionId: Long): Flow<List<BillSplit>>
    fun observeUnpaid(): Flow<List<BillSplit>>
    fun observePaid(): Flow<List<BillSplit>>
    suspend fun getByMemoCode(memoCode: String): BillSplit?
    suspend fun getById(id: Long): BillSplit?
    suspend fun upsert(billSplit: BillSplit): Long
    suspend fun upsertAll(billSplits: List<BillSplit>)
    suspend fun delete(id: Long)
    suspend fun markAsPaid(id: Long, paidAt: Instant)
}
