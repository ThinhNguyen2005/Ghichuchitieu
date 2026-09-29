package com.notepay.platform.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.notepay.MainActivity
import com.notepay.R
import com.notepay.platform.widget.WidgetConstants

object NotificationHelper {

    const val CHANNEL_BUDGET_ALERTS = "notepay_budget_alerts"
    const val CHANNEL_WEEKLY_DIGEST = "notepay_weekly_digest"
    const val CHANNEL_DAILY_REMINDERS = "daily_reminders_channel"
    const val CHANNEL_SUBSCRIPTION_REMINDERS = "subscription_reminders"

    const val NOTIFICATION_ID_BUDGET = 2001
    const val NOTIFICATION_ID_WEEKLY_DIGEST = 2002
    const val NOTIFICATION_ID_DAILY_REMINDER = 1001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET_ALERTS,
                context.getString(R.string.notif_channel_budget_alerts),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notif_channel_budget_alerts_desc)
                enableVibration(true)
            }

            val weeklyChannel = NotificationChannel(
                CHANNEL_WEEKLY_DIGEST,
                context.getString(R.string.notif_channel_weekly_digest),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notif_channel_weekly_digest_desc)
            }

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY_REMINDERS,
                context.getString(R.string.notif_channel_daily_reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notif_channel_daily_reminders_desc)
            }

            val subChannel = NotificationChannel(
                CHANNEL_SUBSCRIPTION_REMINDERS,
                context.getString(R.string.notif_subscription_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notif_subscription_channel_desc)
            }

            manager.createNotificationChannels(
                listOf(budgetChannel, weeklyChannel, dailyChannel, subChannel),
            )
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun sendBudgetAlert(
        context: Context,
        isOverspent: Boolean,
        spentFormatted: String,
        limitFormatted: String,
        percentage: Int,
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(WidgetConstants.EXTRA_NAVIGATE_TO, "stats")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_BUDGET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (isOverspent) {
            context.getString(R.string.notif_budget_instant_exceeded_title)
        } else {
            context.getString(R.string.notif_budget_instant_warning_title, percentage)
        }

        val content = if (isOverspent) {
            context.getString(R.string.notif_budget_instant_exceeded_content, spentFormatted, limitFormatted)
        } else {
            context.getString(R.string.notif_budget_instant_warning_content, spentFormatted, limitFormatted)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BUDGET, notification)
    }

    fun sendWeeklyDigest(
        context: Context,
        totalSpentFormatted: String,
        comparisonText: String,
        topCategoryText: String,
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(WidgetConstants.EXTRA_NAVIGATE_TO, "stats")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_WEEKLY_DIGEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = context.getString(R.string.notif_weekly_digest_title)
        val content = context.getString(
            R.string.notif_weekly_digest_content,
            totalSpentFormatted,
            comparisonText,
            topCategoryText,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_WEEKLY_DIGEST)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_WEEKLY_DIGEST, notification)
    }
}
