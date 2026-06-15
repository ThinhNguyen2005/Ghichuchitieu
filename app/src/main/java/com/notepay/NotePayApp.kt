package com.notepay

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.notepay.domain.repository.CategoryRepository
import com.notepay.worker.SubscriptionReminderWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NotePayApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var categoryRepository: CategoryRepository // Khởi tạo sớm để đăng ký custom categories

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        SubscriptionReminderWorker.schedule(this)
    }
}

