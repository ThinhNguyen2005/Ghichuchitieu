package com.notepay.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.notepay.BuildConfig
import com.notepay.domain.repository.SharedWalletRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker flushing pending outbox entries to Supabase
 * when network connectivity returns and pulling remote changes.
 */
@HiltWorker
class SharedWalletSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sharedWalletRepository: SharedWalletRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!sharedWalletRepository.isCloudConfigured) {
            return Result.success()
        }
        return try {
            val outcome = sharedWalletRepository.syncAllSharedWallets()
            val summary = outcome.getOrNull()
            when {
                outcome.isFailure && runAttemptCount < MAX_ATTEMPTS -> Result.retry()
                summary != null && summary.failedPushCount > 0 && runAttemptCount < MAX_ATTEMPTS -> Result.retry()
                else -> Result.success()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val ONE_TIME_WORK_NAME = "shared_wallet_sync_one_time"
        private const val PERIODIC_WORK_NAME = "shared_wallet_sync_periodic"
        private const val MAX_ATTEMPTS = 3

        fun enqueueOneTime(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val request = OneTimeWorkRequestBuilder<SharedWalletSyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    ONE_TIME_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
            } catch (error: Exception) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.w(ONE_TIME_WORK_NAME, "WorkManager skipped: ${error.message}")
                }
            }
        }

        fun schedulePeriodic(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val request = PeriodicWorkRequestBuilder<SharedWalletSyncWorker>(
                    15L,
                    TimeUnit.MINUTES,
                )
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1L, TimeUnit.MINUTES)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
            } catch (error: Exception) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.w(PERIODIC_WORK_NAME, "WorkManager skipped: ${error.message}")
                }
            }
        }
    }
}
