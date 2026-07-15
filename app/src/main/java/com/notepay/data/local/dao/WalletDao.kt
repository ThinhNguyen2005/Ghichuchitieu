package com.notepay.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.notepay.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets")
    suspend fun getAll(): List<WalletEntity>

    @Query("DELETE FROM wallets")
    suspend fun deleteAll()


    @Query("SELECT * FROM wallets ORDER BY created_at ASC")
    fun observeAll(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE is_active = 1 LIMIT 1")
    fun observeActive(): Flow<WalletEntity?>

    @Query("SELECT * FROM wallets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WalletEntity?

    @Upsert
    suspend fun upsert(entity: WalletEntity): Long

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE wallets SET is_active = 0")
    suspend fun clearActive()

    @Query("UPDATE wallets SET is_active = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Transaction
    suspend fun setActiveExclusive(id: Long) {
        clearActive()
        setActive(id)
    }
}
