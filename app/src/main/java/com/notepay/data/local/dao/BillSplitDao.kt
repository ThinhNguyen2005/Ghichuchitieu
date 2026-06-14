package com.notepay.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.notepay.data.local.entity.BillSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillSplitDao {

    @Query("SELECT * FROM bill_splits ORDER BY created_at DESC")
    fun observeAll(): Flow<List<BillSplitEntity>>

    @Query("SELECT * FROM bill_splits WHERE transaction_id = :transactionId ORDER BY created_at DESC")
    fun observeByTransaction(transactionId: Long): Flow<List<BillSplitEntity>>

    @Query("SELECT * FROM bill_splits WHERE is_paid = 0 ORDER BY created_at DESC")
    fun observeUnpaid(): Flow<List<BillSplitEntity>>

    @Query("SELECT * FROM bill_splits WHERE is_paid = 1 ORDER BY paid_at DESC")
    fun observePaid(): Flow<List<BillSplitEntity>>

    @Query("SELECT * FROM bill_splits WHERE memo_code = :memoCode LIMIT 1")
    suspend fun getByMemoCode(memoCode: String): BillSplitEntity?

    @Query("SELECT * FROM bill_splits WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BillSplitEntity?

    @Upsert
    suspend fun upsert(entity: BillSplitEntity): Long

    @Upsert
    suspend fun upsertAll(entities: List<BillSplitEntity>)

    @Query("DELETE FROM bill_splits WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE bill_splits SET is_paid = 1, paid_at = :paidAt WHERE id = :id")
    suspend fun markAsPaid(id: Long, paidAt: Long)
}
