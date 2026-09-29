package com.notepay.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notepay.MainActivity
import com.notepay.R
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.util.StreakTrackerHelper
import com.notepay.platform.notification.NotificationHelper
import com.notepay.platform.widget.WidgetConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Worker chạy vào buổi tối mỗi ngày (100% Offline):
 * Kiểm tra xem hôm nay người dùng đã ghi chép giao dịch nào chưa.
 * Nếu chưa, gửi thông báo nhắc nhở thông minh với nội dung xoay vòng đa dạng kèm động lực duy trì chuỗi Streak.
 */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val transactionRepository: TransactionRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val enabled = appSettingsDataStore.dailyReminderEnabled.first()
        if (!enabled) return Result.success()

        val allTransactions = transactionRepository.observeAll().firstOrNull() ?: emptyList()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val hasTransactionToday = allTransactions.any { tx ->
            tx.occurredAt.toLocalDateTime(TimeZone.currentSystemDefault()).date == today
        }

        // Nếu hôm nay chưa ghi chép, gửi thông báo nhắc nhở xoay vòng thông minh
        if (!hasTransactionToday) {
            val streak = StreakTrackerHelper.calculateStreak(
                transactionInstants = allTransactions.map { it.createdAt },
                today = today,
            )

            sendReminderNotification(streak, today.day)
        }

        return Result.success()
    }

    private fun sendReminderNotification(streak: Int, dayOfMonth: Int) {
        if (!NotificationHelper.hasNotificationPermission(context)) return
        NotificationHelper.createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(WidgetConstants.EXTRA_NAVIGATE_TO, "add-transaction")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val (title, content) = when {
            streak >= 30 -> {
                context.getString(R.string.notif_streak_milestone_30_title) to
                    context.getString(R.string.notif_streak_milestone_30_content)
            }
            streak >= 14 -> {
                context.getString(R.string.notif_streak_milestone_14_title) to
                    context.getString(R.string.notif_streak_milestone_14_content)
            }
            streak >= 7 -> {
                context.getString(R.string.notif_streak_milestone_7_title) to
                    context.getString(R.string.notif_streak_milestone_7_content)
            }
            streak >= 3 -> {
                context.getString(R.string.notif_streak_milestone_3_title) to
                    context.getString(R.string.notif_streak_milestone_3_content)
            }
            streak > 0 -> {
                val titleStr = context.getString(R.string.notif_daily_reminder_title_streak, streak)
                val contentStr = when (dayOfMonth % 3) {
                    0 -> context.getString(R.string.notif_streak_rotate_1, streak)
                    1 -> context.getString(R.string.notif_streak_rotate_2, streak)
                    else -> context.getString(R.string.notif_streak_rotate_3, streak)
                }
                titleStr to contentStr
            }
            else -> {
                val (t, c) = when (dayOfMonth % 3) {
                    0 -> R.string.notif_daily_rotate_title_1 to R.string.notif_daily_rotate_content_1
                    1 -> R.string.notif_daily_rotate_title_2 to R.string.notif_daily_rotate_content_2
                    else -> R.string.notif_daily_rotate_title_3 to R.string.notif_daily_rotate_content_3
                }
                context.getString(t) to context.getString(c)
            }
        }

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_DAILY_REMINDERS)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 2030
        const val WORK_NAME = "notepay_daily_reminder_work"
    }
}
