package com.notepay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_notification_keys")
data class ProcessedNotificationKeyEntity(
    @PrimaryKey val key: String, // dedupeKey or hash
    val processedAt: Long
)
