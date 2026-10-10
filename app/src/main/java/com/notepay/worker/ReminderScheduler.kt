package com.notepay.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Tiện ích lên lịch và hủy lịch chạy thông báo nhắc nhở hàng ngày qua WorkManager.
 */
object ReminderScheduler {

    /**
     * Lên lịch chạy Periodic Worker mỗi ngày vào giờ [targetHour]:[targetMinute].
     */
    fun scheduleDailyReminder(
        context: Context,
        targetHour: Int = 20,
        targetMinute: Int = 30
    ) {
        try {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val initialDelayMillis = target.timeInMillis - now.timeInMillis

            val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DailyReminderWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        } catch (e: Exception) {
            if (com.notepay.BuildConfig.DEBUG) {
                android.util.Log.w("ReminderScheduler", "Failed to schedule daily reminder: ${e.message}")
            }
        }
    }

    /**
     * Hủy lịch chạy thông báo nhắc nhở hàng ngày.
     */
    fun cancelDailyReminder(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(DailyReminderWorker.WORK_NAME)
        } catch (e: Exception) {
            if (com.notepay.BuildConfig.DEBUG) {
                android.util.Log.w("ReminderScheduler", "Failed to cancel daily reminder: ${e.message}")
            }
        }
    }

    /**
     * Lên lịch chạy Periodic Worker tổng kết tài chính tuần vào tối Chủ Nhật lúc [targetHour]:[targetMinute].
     */
    fun scheduleWeeklyDigest(
        context: Context,
        targetHour: Int = 20,
        targetMinute: Int = 0,
    ) {
        try {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            val initialDelayMillis = target.timeInMillis - now.timeInMillis

            val workRequest = PeriodicWorkRequestBuilder<WeeklyDigestWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WeeklyDigestWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest,
            )
        } catch (e: Exception) {
            if (com.notepay.BuildConfig.DEBUG) {
                android.util.Log.w("ReminderScheduler", "Failed to schedule weekly digest: ${e.message}")
            }
        }
    }

    /**
     * Hủy lịch chạy thông báo tổng kết tài chính tuần.
     */
    fun cancelWeeklyDigest(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WeeklyDigestWorker.WORK_NAME)
        } catch (e: Exception) {
            if (com.notepay.BuildConfig.DEBUG) {
                android.util.Log.w("ReminderScheduler", "Failed to cancel weekly digest: ${e.message}")
            }
        }
    }
}
