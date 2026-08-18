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
    }

    /**
     * Hủy lịch chạy thông báo nhắc nhở hàng ngày.
     */
    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DailyReminderWorker.WORK_NAME)
    }
}
