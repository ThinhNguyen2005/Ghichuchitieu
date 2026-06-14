package com.notepay

import android.app.Application
import com.notepay.worker.SubscriptionReminderWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NotePayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SubscriptionReminderWorker.schedule(this)
    }
}
