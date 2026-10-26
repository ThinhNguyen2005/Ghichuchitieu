package com.notepay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.notepay.data.local.entity.ProcessedNotificationKeyEntity

/**
 * Stub DAO cho tính năng khử trùng lặp thông báo ngân hàng (Auto Capture) — CHƯA kích hoạt.
 *
 * Trạng thái hiện tại: `ProcessedNotificationKeyEntity` chưa được khai báo trong mảng
 * `entities` của `NotePayDatabase` (đang là version 6), vì vậy bảng
 * `processed_notification_keys` không tồn tại trong CSDL lúc ứng dụng chạy. Mọi câu
 * `@Query` bên dưới sẽ thất bại với "no such table" nếu gọi thẳng.
 *
 * Trình tự bắt buộc trước khi sử dụng:
 * 1. Thêm entity vào `@Database(entities = [...])` và nâng `version = 7`.
 * 2. Viết `MIGRATION_6_7` tạo bảng (theo schema JSON do KSP xuất ra).
 * 3. Khai báo `abstract fun processedNotificationKeyDao()` trong `NotePayDatabase`
 *    và đăng ký migration trong `DatabaseModule`.
 * 4. Bổ sung ca kiểm thử migration tương ứng trong `DatabaseMigrationTest`.
 */
@Deprecated(
    "Stub cho tính năng Notification — Chưa đăng ký vào NotePayDatabase. Phải thêm entity vào @Database và viết MIGRATION_6_7 trước khi inject/sử dụng.",
    level = DeprecationLevel.WARNING,
)
@Dao
interface ProcessedNotificationKeyDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ProcessedNotificationKeyEntity): Long

    @Query("SELECT * FROM processed_notification_keys WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): ProcessedNotificationKeyEntity?

    @Query("DELETE FROM processed_notification_keys WHERE processedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long): Int
}
