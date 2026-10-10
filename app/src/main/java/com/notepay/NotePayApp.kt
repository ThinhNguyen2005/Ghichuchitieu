package com.notepay

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.notepay.worker.NotificationWatchdog
import com.notepay.worker.ReminderScheduler
import com.notepay.worker.SubscriptionReminderWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltAndroidApp
class NotePayApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private val isRunningInRobolectric: Boolean by lazy {
        runCatching {
            Class.forName("org.robolectric.Robolectric")
            true
        }.getOrDefault(false)
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
        if (!isRunningInRobolectric) {
            applicationScope.launch {
                try {
                    delay(2_000L.milliseconds)
                    SubscriptionReminderWorker.schedule(applicationContext)
                    ReminderScheduler.scheduleDailyReminder(applicationContext)
                    NotificationWatchdog.schedule(applicationContext)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (e: Throwable) {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.e("NotePayApp", "Failed to schedule background workers: ${e.message}", e)
                    }
                }
            }
        }
        if (BuildConfig.DEBUG){
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskWrites()
                    .detectDiskReads()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
        }
    }
}

