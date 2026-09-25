package com.notepay.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notepay.R
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.TransactionRepository
import com.notepay.platform.notification.NotificationHelper
import com.notepay.ui.util.MoneyFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@HiltWorker
class WeeklyDigestWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val transactionRepository: TransactionRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "weekly_financial_digest_work"
    }

    override suspend fun doWork(): Result {
        val enabled = appSettingsDataStore.weeklyDigestEnabled.first()
        if (!enabled) return Result.success()

        val allTransactions = transactionRepository.observeAll().firstOrNull() ?: emptyList()
        val now = Clock.System.now()
        val sevenDaysAgo = now - 7.days
        val fourteenDaysAgo = now - 14.days

        val thisWeekExpenses = allTransactions.filter { tx ->
            tx.type == TransactionType.EXPENSE && tx.occurredAt in sevenDaysAgo..now
        }
        val lastWeekExpenses = allTransactions.filter { tx ->
            tx.type == TransactionType.EXPENSE && tx.occurredAt in fourteenDaysAgo..<sevenDaysAgo
        }

        val thisWeekCents = thisWeekExpenses.sumOf { it.amount.amountInCents }
        val lastWeekCents = lastWeekExpenses.sumOf { it.amount.amountInCents }

        if (thisWeekCents == 0L && thisWeekExpenses.isEmpty()) {
            return Result.success()
        }

        val totalExpenseThisWeek = Money(thisWeekCents)

        val comparisonText = if (lastWeekCents > 0L) {
            val diff = thisWeekCents - lastWeekCents
            val diffPercent = ((kotlin.math.abs(diff).toDouble() / lastWeekCents.toDouble()) * 100).toInt()
            if (diff > 0) {
                context.getString(R.string.notif_weekly_digest_comparison_increase, diffPercent)
            } else if (diff < 0) {
                context.getString(R.string.notif_weekly_digest_comparison_decrease, diffPercent)
            } else {
                context.getString(R.string.notif_weekly_digest_comparison_equal)
            }
        } else {
            ""
        }

        val topCategoryGroup = thisWeekExpenses
            .groupBy { it.category }
            .maxByOrNull { entry -> entry.value.sumOf { it.amount.amountInCents } }

        val topCategoryText = if (topCategoryGroup != null) {
            val catTotal = Money(topCategoryGroup.value.sumOf { it.amount.amountInCents })
            context.getString(
                R.string.notif_weekly_digest_top_category,
                topCategoryGroup.key.displayName,
                MoneyFormatter.formatCompact(catTotal),
            )
        } else {
            ""
        }

        NotificationHelper.sendWeeklyDigest(
            context = context,
            totalSpentFormatted = MoneyFormatter.format(totalExpenseThisWeek),
            comparisonText = comparisonText,
            topCategoryText = topCategoryText,
        )

        return Result.success()
    }
}
