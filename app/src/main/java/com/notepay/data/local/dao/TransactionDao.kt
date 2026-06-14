package com.notepay.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.notepay.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY occurred_at DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE occurred_at BETWEEN :startMillis AND :endMillis
        ORDER BY occurred_at DESC
    """)
    fun observeByRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE wallet_id = :walletId ORDER BY occurred_at DESC")
    fun observeByWallet(walletId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Upsert
    suspend fun upsert(entity: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)
}
