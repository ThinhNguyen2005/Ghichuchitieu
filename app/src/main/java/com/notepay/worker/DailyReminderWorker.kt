package com.notepay.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notepay.MainActivity
import com.notepay.R
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.util.StreakTrackerHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Worker chạy lúc 20:30 tối mỗi ngày (100% Offline):
 * Kiểm tra xem hôm nay người dùng đã ghi chép giao dịch nào chưa.
 * Nếu chưa, gửi thông báo nhắc nhở kèm động lực duy trì chuỗi Streak.
 */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        createNotificationChannel()

        val allTransactions = transactionRepository.observeAll().firstOrNull() ?: emptyList()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val hasTransactionToday = allTransactions.any { tx ->
            tx.occurredAt.toLocalDateTime(TimeZone.currentSystemDefault()).date == today
        }

        // Nếu hôm nay chưa ghi chép, gửi thông báo nhắc nhở
        if (!hasTransactionToday) {
            val streak = StreakTrackerHelper.calculateStreak(
                transactionInstants = allTransactions.map { it.createdAt },
                today = today
            )

            sendReminderNotification(streak)
        }

        return Result.success()
    }

    private fun sendReminderNotification(streak: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "add-transaction")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (streak > 0) {
            context.getString(R.string.notif_daily_reminder_title_streak, streak)
        } else {
            context.getString(R.string.notif_daily_reminder_title_default)
        }

        val content = context.getString(R.string.notif_daily_reminder_content)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_daily_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_daily_reminder_channel_desc)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "notepay_daily_reminder"
        const val NOTIFICATION_ID = 2030
        const val WORK_NAME = "notepay_daily_reminder_work"
    }
}
