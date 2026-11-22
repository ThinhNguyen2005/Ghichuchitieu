package com.notepay.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.notepay.R
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.data.datastore.PendingBankNotification
import com.notepay.domain.money.Money

internal fun NotePayNotificationListenerService.showPendingTransactionNotification(
    pending: PendingBankNotification,
    walletName: String,
) {
    val amountFormat = MoneyFormatter.format(Money(pending.amountCents))
    val notificationId = pendingNotificationId(pending.id)
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val confirmIntent = Intent(this, NotificationActionReceiver::class.java).apply {
        action = "com.notepay.ACTION_SAVE_TRANSACTION"
        putExtra("pending_id", pending.id)
        putExtra("notification_id", notificationId)
    }
    val confirmPendingIntent = PendingIntent.getBroadcast(
        this,
        notificationId,
        confirmIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val ignoreIntent = Intent(this, NotificationActionReceiver::class.java).apply {
        action = "com.notepay.ACTION_IGNORE_TRANSACTION"
        putExtra("pending_id", pending.id)
        putExtra("notification_id", notificationId)
    }
    val ignorePendingIntent = PendingIntent.getBroadcast(
        this,
        notificationId + 1,
        ignoreIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(this, NotePayNotificationListenerService.CHANNEL_ID)
        .setContentTitle(getString(R.string.notif_transaction_pending_title))
        .setContentText(getString(R.string.notif_transaction_pending_content, amountFormat, walletName))
        .setStyle(
            NotificationCompat.BigTextStyle().bigText(
                getString(
                    R.string.notif_transaction_pending_big_text,
                    amountFormat,
                    walletName,
                    pending.note,
                ),
            ),
        )
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .addAction(
            android.R.drawable.ic_menu_save,
            getString(R.string.notif_transaction_confirm_action),
            confirmPendingIntent,
        )
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.notif_transaction_ignore_action),
            ignorePendingIntent,
        )
        .build()

    manager.notify(notificationId, notification)
}

private fun pendingNotificationId(id: String): Int =
    NotePayNotificationListenerService.NOTIFICATION_ID + 100 + (id.hashCode() and Int.MAX_VALUE) % 100_000

internal fun NotePayNotificationListenerService.showExpenseNotification(
    walletName: String,
    amountCents: Long,
    note: String,
    categoryName: String,
    categoryEmoji: String,
) {
    val amountFormat = MoneyFormatter.format(Money(amountCents))
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val bigTextStyle = NotificationCompat.BigTextStyle()
        .setBigContentTitle(getString(R.string.notif_expense_big_title))
        .bigText(
            getString(
                R.string.notif_expense_big_text,
                amountFormat,
                walletName,
                categoryEmoji,
                categoryName,
                note,
            ),
        )

    val notification = NotificationCompat.Builder(this, NotePayNotificationListenerService.CHANNEL_ID)
        .setContentTitle(getString(R.string.notif_expense_title))
        .setContentText(getString(R.string.notif_expense_content, amountFormat, walletName, categoryEmoji, categoryName))
        .setStyle(bigTextStyle)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .build()

    manager.notify(NotePayNotificationListenerService.NOTIFICATION_ID, notification)
}

internal fun NotePayNotificationListenerService.showIncomeNotification(
    walletName: String,
    amountCents: Long,
    note: String,
) {
    val amountFormat = MoneyFormatter.format(Money(amountCents))
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val bigTextStyle = NotificationCompat.BigTextStyle()
        .setBigContentTitle(getString(R.string.notif_income_big_title))
        .bigText(
            getString(R.string.notif_income_big_text, amountFormat, walletName, note),
        )

    val notification = NotificationCompat.Builder(this, NotePayNotificationListenerService.CHANNEL_ID)
        .setContentTitle(getString(R.string.notif_income_title))
        .setContentText(getString(R.string.notif_income_content, amountFormat, walletName))
        .setStyle(bigTextStyle)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .build()

    manager.notify(NotePayNotificationListenerService.NOTIFICATION_ID, notification)
}

internal fun NotePayNotificationListenerService.showDebtRepaymentSingleNotification(
    debtorName: String,
    paidAmountCents: Long,
    remainingDebtCents: Long,
    walletName: String,
) {
    val paidFormat = MoneyFormatter.format(Money(paidAmountCents))
    val remainingFormat = MoneyFormatter.format(Money(remainingDebtCents))
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val bigTextStyle = NotificationCompat.BigTextStyle()
        .setBigContentTitle(getString(R.string.notif_debt_single_big_title))
        .bigText(
            getString(
                R.string.notif_debt_single_big_text,
                debtorName,
                paidFormat,
                walletName,
                remainingFormat,
            ),
        )

    val notification = NotificationCompat.Builder(this, NotePayNotificationListenerService.CHANNEL_ID)
        .setContentTitle(getString(R.string.notif_debt_single_title, debtorName))
        .setContentText(getString(R.string.notif_debt_single_content, paidFormat, remainingFormat))
        .setStyle(bigTextStyle)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .build()

    manager.notify(NotePayNotificationListenerService.NOTIFICATION_ID + 2, notification)
}

internal fun NotePayNotificationListenerService.showDebtRepaymentBulkNotification(
    debtorName: String,
    totalAmountCents: Long,
    billCount: Int,
    walletName: String,
) {
    val totalFormat = MoneyFormatter.format(Money(totalAmountCents))
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val bigTextStyle = NotificationCompat.BigTextStyle()
        .setBigContentTitle(getString(R.string.notif_debt_bulk_big_title))
        .bigText(
            resources.getQuantityString(
                R.plurals.notif_debt_bulk_big_text,
                billCount,
                debtorName,
                totalFormat,
                walletName,
                billCount,
            ),
        )

    val notification = NotificationCompat.Builder(this, NotePayNotificationListenerService.CHANNEL_ID)
        .setContentTitle(getString(R.string.notif_debt_bulk_title, debtorName))
        .setContentText(resources.getQuantityString(R.plurals.notif_debt_bulk_content, billCount, totalFormat, billCount))
        .setStyle(bigTextStyle)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .build()

    manager.notify(NotePayNotificationListenerService.NOTIFICATION_ID + 3, notification)
}

internal fun NotePayNotificationListenerService.showBudgetAlertNotification(
    spentCents: Long,
    budgetCents: Long,
    percentUsed: Int,
    isLimitExceeded: Boolean,
) {
    val spentFormat = MoneyFormatter.format(Money(spentCents))
    val budgetFormat = MoneyFormatter.format(Money(budgetCents))
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val title = getString(
        if (isLimitExceeded) R.string.notif_budget_exceeded_title else R.string.notif_budget_warning_title,
    )
    val content = if (isLimitExceeded) {
        getString(R.string.notif_budget_exceeded_content, spentFormat, budgetFormat, percentUsed)
    } else {
        getString(R.string.notif_budget_warning_content, spentFormat, percentUsed, budgetFormat)
    }

    val bigTextStyle = NotificationCompat.BigTextStyle()
        .setBigContentTitle(title)
        .bigText(
            getString(R.string.notif_budget_advice).let { "$content\n$it" },
        )

    val notification = NotificationCompat.Builder(this, "notepay_budget_alert")
        .setContentTitle(title)
        .setContentText(content)
        .setStyle(bigTextStyle)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .build()

    manager.notify(NotePayNotificationListenerService.NOTIFICATION_ID + 4, notification)
}

internal fun NotePayNotificationListenerService.showSubscriptionDetectedNotification(
    name: String,
    amountCents: Long,
) {
    val amountFormat = MoneyFormatter.format(Money(amountCents))
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notifId = (amountCents + System.currentTimeMillis() / 1000).toInt()

    val addIntent = Intent(this, NotificationActionReceiver::class.java).apply {
        action = "com.notepay.ACTION_ADD_SUBSCRIPTION"
        putExtra("name", name)
        putExtra("amount_cents", amountCents)
        putExtra("notification_id", notifId)
    }
    val addPendingIntent = PendingIntent.getBroadcast(
        this,
        notifId,
        addIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val ignoreIntent = Intent(this, NotificationActionReceiver::class.java).apply {
        action = "com.notepay.ACTION_IGNORE_SUBSCRIPTION"
        putExtra("notification_id", notifId)
    }
    val ignorePendingIntent = PendingIntent.getBroadcast(
        this,
        notifId + 1,
        ignoreIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(this, NotePayNotificationListenerService.CHANNEL_ID)
        .setContentTitle(getString(R.string.notif_subscription_detected_title))
        .setContentText(getString(R.string.notif_subscription_detected_content, name, amountFormat))
        .setStyle(
            NotificationCompat.BigTextStyle().bigText(
                getString(R.string.notif_subscription_detected_big_text, name, amountFormat),
            ),
        )
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .addAction(android.R.drawable.ic_menu_add, getString(R.string.notif_subscription_add_action), addPendingIntent)
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.notif_subscription_ignore_action),
            ignorePendingIntent,
        )
        .build()

    manager.notify(notifId, notification)
}

internal fun NotePayNotificationListenerService.showInternalTransferNotification(
    amountCents: Long,
    fromWalletName: String,
    toWalletName: String,
) {
    val amountFormat = MoneyFormatter.format(Money(amountCents))
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val bigTextStyle = NotificationCompat.BigTextStyle()
        .setBigContentTitle(getString(R.string.notif_transfer_big_title))
        .bigText(
            getString(
                R.string.notif_transfer_big_text,
                amountFormat,
                fromWalletName,
                toWalletName,
                getString(R.string.notif_transfer_description),
            ),
        )

    val notification = NotificationCompat.Builder(this, NotePayNotificationListenerService.CHANNEL_ID)
        .setContentTitle(getString(R.string.notif_transfer_title))
        .setContentText(getString(R.string.notif_transfer_content, fromWalletName, toWalletName, amountFormat))
        .setStyle(bigTextStyle)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .build()

    manager.notify(NotePayNotificationListenerService.NOTIFICATION_ID + 5, notification)
}

internal fun NotePayNotificationListenerService.createNotificationChannel() {
    val channel = NotificationChannel(
        NotePayNotificationListenerService.CHANNEL_ID,
        getString(R.string.notif_channel_auto_capture_name),
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = getString(R.string.notif_channel_auto_capture_desc)
    }
    val budgetChannel = NotificationChannel(
        "notepay_budget_alert",
        getString(R.string.notif_channel_budget_name),
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = getString(R.string.notif_channel_budget_desc)
    }
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
    manager.createNotificationChannel(budgetChannel)
}

internal fun NotePayNotificationListenerService.showErrorNotification(
    title: String,
    message: String,
) {
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val notification = NotificationCompat.Builder(this, NotePayNotificationListenerService.CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .build()

    manager.notify(NotePayNotificationListenerService.NOTIFICATION_ID + 1, notification)
}
