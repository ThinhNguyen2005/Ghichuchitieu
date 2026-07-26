package com.notepay.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.notepay.R
import com.notepay.domain.repository.SubscriptionRepository
import com.notepay.ui.util.MoneyFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import java.util.concurrent.TimeUnit

/**
 * WorkManager job chạy mỗi ngày một lần.
 * Kiểm tra các subscription sắp đến hạn và gửi push notification.
 */
@HiltWorker
class SubscriptionReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val subscriptionRepository: SubscriptionRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        createNotificationChannel()

        val now = Clock.System.now()
        // Lấy tất cả subscription sắp đến hạn trong 7 ngày tới
        val upcomingLimit = now + 7.days
        val upcoming = subscriptionRepository.observeUpcoming(upcomingLimit).firstOrNull() ?: emptyList()

        upcoming.forEach { subscription ->
            val daysUntilDue = (subscription.nextDueDate - now).inWholeDays
            if (daysUntilDue <= subscription.remindDaysBefore) {
                sendNotification(
                    id = subscription.id.toInt(),
                    name = subscription.name,
                    daysLeft = daysUntilDue,
                    amount = MoneyFormatter.format(subscription.amount),
                    dueDate = subscription.nextDueDate
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
                )
            }
        }

        return Result.success()
    }

    private fun sendNotification(
        id: Int,
        name: String,
        daysLeft: Long,
        amount: String,
        dueDate: String,
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = when {
            daysLeft <= 0 -> "⏰ $name đã đến hạn hôm nay!"
            daysLeft == 1L -> "⚠️ $name sắp hết hạn ngày mai"
            else -> "🔔 $name hết hạn sau $daysLeft ngày"
        }
        val body = "Gia hạn $amount vào ngày $dueDate"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(com.notepay.R.drawable.ic_stat_notepay)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_BASE_ID + id, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_subscription_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notif_channel_subscription_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "notepay_subscription_reminder"
        const val NOTIFICATION_BASE_ID = 2000
        const val WORK_NAME = "subscription_reminder_daily"

        /** Lên lịch chạy mỗi ngày một lần. Gọi từ Application.onCreate() */
        fun schedule(context: Context) {
            try {
                val request = PeriodicWorkRequestBuilder<SubscriptionReminderWorker>(
                    1, TimeUnit.DAYS,
                ).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
            } catch (e: IllegalStateException) {
                // WorkManager is not initialized (e.g., in Robolectric tests)
                e.printStackTrace()
            }
        }
    }
}
