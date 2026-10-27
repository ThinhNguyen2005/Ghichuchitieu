package com.notepay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity khử trùng lặp thông báo ngân hàng đã xử lý — CHƯA kích hoạt.
 *
 * Thực thể này chưa nằm trong mảng `entities` của `NotePayDatabase` (version 6) nên bảng
 * `processed_notification_keys` chưa được tạo. Xem `ProcessedNotificationKeyDao` để biết
 * trình tự bắt buộc (nâng version + `MIGRATION_6_7`) trước khi inject DAO vào DI graph.
 */
@Entity(tableName = "processed_notification_keys")
data class ProcessedNotificationKeyEntity(
    @PrimaryKey val key: String, // dedupeKey or hash
    val processedAt: Long
)
