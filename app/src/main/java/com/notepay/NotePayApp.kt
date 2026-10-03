package com.notepay

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.notepay.worker.ReminderScheduler
import com.notepay.worker.SubscriptionReminderWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NotePayApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        SubscriptionReminderWorker.schedule(this)
        ReminderScheduler.scheduleDailyReminder(this)
        com.notepay.worker.NotificationWatchdog.schedule(this)
    }
}

