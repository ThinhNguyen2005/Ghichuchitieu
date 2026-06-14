package com.notepay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.notepay.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions WHERE is_active = 1 ORDER BY next_due_date ASC")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    /** Sắp đến hạn: next_due_date <= limitDateMs */
    @Query("SELECT * FROM subscriptions WHERE is_active = 1 AND next_due_date <= :limitDateMs ORDER BY next_due_date ASC")
    fun observeUpcoming(limitDateMs: Long): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SubscriptionEntity): Long

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SubscriptionEntity?

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE subscriptions SET next_due_date = :newDueDate WHERE id = :id")
    suspend fun updateNextDueDate(id: Long, newDueDate: Long)
}
