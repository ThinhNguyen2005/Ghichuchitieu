package com.notepay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.notepay.data.local.entity.ProcessedNotificationKeyEntity

@Dao
interface ProcessedNotificationKeyDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ProcessedNotificationKeyEntity): Long

    @Query("SELECT * FROM processed_notification_keys WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): ProcessedNotificationKeyEntity?

    @Query("DELETE FROM processed_notification_keys WHERE processedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long): Int
}
