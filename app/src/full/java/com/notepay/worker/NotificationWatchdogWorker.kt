package com.notepay.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notepay.service.NotePayNotificationListenerService

class NotificationWatchdogWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        NotePayNotificationListenerService.heal(applicationContext)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "notification_watchdog_worker"
    }
}
