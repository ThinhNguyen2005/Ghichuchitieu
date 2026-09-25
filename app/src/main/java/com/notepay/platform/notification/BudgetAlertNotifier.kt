package com.notepay.platform.notification

import android.content.Context
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.data.preferences.BudgetSettingsStore
import com.notepay.domain.model.Money
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.GetMonthlySummaryUseCase
import com.notepay.ui.util.MoneyFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetAlertNotifier @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
    private val budgetSettingsStore: BudgetSettingsStore,
    private val walletRepository: WalletRepository,
    private val getMonthlySummaryUseCase: GetMonthlySummaryUseCase,
    @param:ApplicationContext private val context: Context,
) {

    suspend fun checkAndNotify(walletId: Long? = null) {
        val enabled = appSettingsDataStore.budgetAlertsEnabled.firstOrNull() ?: false
        if (!enabled) return

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val year = now.year
        val month = now.month.number
        val monthKey = "%d-%02d".format(year, month)

        val lastNotifiedMonth = appSettingsDataStore.lastNotifiedBudgetMonth.firstOrNull() ?: ""
        val lastNotifiedThreshold = if (lastNotifiedMonth == monthKey) {
            appSettingsDataStore.lastNotifiedBudgetThreshold.firstOrNull() ?: 0
        } else {
            0
        }

        val activeWallet = if (walletId != null) {
            walletRepository.getById(walletId)
        } else {
            walletRepository.observeActive().firstOrNull() ?: walletRepository.observeAll().firstOrNull()?.firstOrNull()
        }

        val walletBudget = activeWallet?.budgetLimit
        val globalBudgetCents = budgetSettingsStore.settings.firstOrNull()?.monthlyBudgetCents ?: 0L
        val globalBudget = if (globalBudgetCents > 0L) Money(globalBudgetCents) else null

        val effectiveBudget = if (walletBudget != null && walletBudget.amountInCents > 0L) {
            walletBudget
        } else {
            globalBudget
        } ?: return

        val summary = getMonthlySummaryUseCase(year, month, activeWallet?.id).firstOrNull()
        val totalExpense = summary?.totalExpense ?: Money.ZERO

        val spentCents = totalExpense.amountInCents
        val limitCents = effectiveBudget.amountInCents
        val percent = ((spentCents.toDouble() / limitCents.toDouble()) * 100).toInt()

        if (percent >= 100 && lastNotifiedThreshold < 100) {
            NotificationHelper.sendBudgetAlert(
                context = context,
                isOverspent = true,
                spentFormatted = MoneyFormatter.format(totalExpense),
                limitFormatted = MoneyFormatter.format(effectiveBudget),
                percentage = percent,
            )
            appSettingsDataStore.recordBudgetAlertNotification(monthKey, 100)
        } else if (percent >= 80 && lastNotifiedThreshold < 80) {
            NotificationHelper.sendBudgetAlert(
                context = context,
                isOverspent = false,
                spentFormatted = MoneyFormatter.format(totalExpense),
                limitFormatted = MoneyFormatter.format(effectiveBudget),
                percentage = percent,
            )
            appSettingsDataStore.recordBudgetAlertNotification(monthKey, 80)
        }
    }
}
