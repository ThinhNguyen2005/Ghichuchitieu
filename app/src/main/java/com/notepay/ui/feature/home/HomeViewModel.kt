package com.notepay.ui.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.ai.LegacyAiModelCleaner
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.data.preferences.BudgetSettings
import com.notepay.data.preferences.BudgetSettingsStore
import com.notepay.domain.money.Money
import com.notepay.domain.repository.SubscriptionRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.DeleteTransactionUseCase
import com.notepay.domain.usecase.GetMonthlySummaryUseCase
import com.notepay.domain.util.StreakTrackerHelper
import com.notepay.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val walletRepo: WalletRepository,
    private val transactionRepo: TransactionRepository,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getMonthlySummary: GetMonthlySummaryUseCase,
    private val budgetSettingsStore: BudgetSettingsStore,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val subscriptionRepository: SubscriptionRepository,
    private val legacyModelCleaner: LegacyAiModelCleaner,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            deleteTransactionUseCase(transactionId)
        }
    }

    init {
        viewModelScope.launch {
            legacyModelCleaner.cleanLegacyModels()
        }
    }

    private val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    private val currentYear = today.year
    private val currentMonth = today.month.number

    private val _selectedMonth = MutableStateFlow(currentYear to currentMonth)
    private val _selectedWalletFilterId = MutableStateFlow<Long?>(null)
    private val _isSmartInsightsDismissed = MutableStateFlow(false)

    fun dismissSmartInsights() {
        _isSmartInsightsDismissed.value = true
    }

    private fun getDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private data class HomeFilterState(
        val monthPair: Pair<Int, Int>,
        val activeWallet: Wallet?,
        val selectedFilterId: Long?,
        val isDismissed: Boolean,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val state = combine(
        _selectedMonth,
        walletRepo.observeActive(),
        _selectedWalletFilterId,
        _isSmartInsightsDismissed,
    ) { monthPair, activeWallet, selectedFilterId, isDismissed ->
        HomeFilterState(monthPair, activeWallet, selectedFilterId, isDismissed)
    }.flatMapLatest { filter ->
        val (year, month) = filter.monthPair
        val activeWallet = filter.activeWallet
        val selectedFilterId = filter.selectedFilterId
        val isDismissed = filter.isDismissed

        val zone = TimeZone.currentSystemDefault()
        val firstDate = LocalDate(year, month, 1)
        val nextMonth = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
        val startMillis = LocalDateTime(firstDate.year, firstDate.month.number, firstDate.day, 0, 0)
            .toInstant(zone).toEpochMilliseconds()
        val endMillis = LocalDateTime(nextMonth.year, nextMonth.month.number, nextMonth.day, 0, 0)
            .toInstant(zone).toEpochMilliseconds() - 1

        val walletsSummaryFlow = combine(
            walletRepo.observeAll(),
            transactionRepo.observeWalletStats(startMillis, endMillis)
        ) { wallets, stats ->
            wallets to stats.associateBy { it.walletId }
        }.flatMapLatest { (wallets, statsMap) ->
            if (wallets.isEmpty()) flowOf(emptyList())
            else {
                val summaryFlows = wallets.map { wallet ->
                    appSettingsDataStore.observeWalletBackground(wallet.id).map { bgUri ->
                        val stat = statsMap[wallet.id]
                        val totalInc = stat?.allTimeIncome ?: Money.ZERO
                        val totalExp = stat?.allTimeExpense ?: Money.ZERO
                        val monthInc = stat?.currentMonthIncome ?: Money.ZERO
                        val monthExp = stat?.currentMonthExpense ?: Money.ZERO
                        WalletSummary(
                            wallet = wallet,
                            balance = wallet.initialBalance + totalInc - totalExp,
                            monthlyIncome = monthInc,
                            monthlyExpense = monthExp,
                            backgroundUri = bgUri
                        )
                    }
                }
                combine(summaryFlows) { it.toList() }
            }
        }

        combine(
            getMonthlySummary(year, month, selectedFilterId),
            walletsSummaryFlow,
            subscriptionRepository.observeAll(),
            transactionRepo.observeAllCreatedDates(),
        ) { summary, walletsSummary, subscriptions, createdDates ->
            val wallets = walletsSummary.map { it.wallet }

            // Phòng thủ: nếu ví đang lọc bị xóa thì tự động fallback về null
            val validFilterId = selectedFilterId?.takeIf { id -> wallets.any { it.id == id } }
            if (selectedFilterId != null && validFilterId == null) {
                _selectedWalletFilterId.value = null
            }

            // Tính số lời nhắc sắp đến hạn
            val now = Clock.System.now()
            val dueCount = subscriptions.count { sub ->
                sub.isActive && (sub.nextDueDate - now).inWholeDays <= sub.remindDaysBefore.toLong()
            }

            // Tính chuỗi ngày ghi chép liên tiếp (Streak 🔥)
            val streak = StreakTrackerHelper.calculateStreak(
                transactionInstants = createdDates,
                today = today.date
            )

            // Chỉ tính budgetProjection khi đang chọn một ví cụ thể có hạn mức
            val targetWallet = validFilterId?.let { id -> wallets.find { it.id == id } }
            val budgetLimit = targetWallet?.budgetLimit
            val projection = if (targetWallet != null && budgetLimit != null && budgetLimit.amountInCents > 0) {
                val spentCents = summary.totalExpense.amountInCents
                val limitCents = budgetLimit.amountInCents
                val currentDay = today.day.coerceIn(1, 31)
                val daysInMonth = getDaysInMonth(today.year, today.month.number)

                val dailyAverageCents = spentCents.toFloat() / currentDay
                val projectedSpendCents = dailyAverageCents * daysInMonth
                val spentPercentage = (spentCents.toFloat() / limitCents).coerceIn(0f, 1f)
                val isProjectedToExceed = projectedSpendCents > limitCents

                val exhaustionDateLabel = if (isProjectedToExceed && dailyAverageCents > 0) {
                    val exhaustionDay = (limitCents / dailyAverageCents).toInt().coerceIn(1, daysInMonth)
                    "%02d/%02d".format(exhaustionDay, today.month.number)
                } else {
                    null
                }

                val remainingBudget = limitCents - spentCents
                val remainingDays = daysInMonth - currentDay
                val safeDailyLimitCents = if (remainingBudget > 0 && remainingDays > 0) {
                    remainingBudget / remainingDays
                } else {
                    0L
                }

                BudgetProjection(
                    dailyAverage = Money(dailyAverageCents.toLong()),
                    projectedSpend = Money(projectedSpendCents.toLong()),
                    isProjectedToExceed = isProjectedToExceed,
                    exhaustionDateLabel = exhaustionDateLabel,
                    safeDailyLimit = Money(safeDailyLimitCents),
                    spentPercentage = spentPercentage,
                    spentThisWallet = summary.totalExpense
                )
            } else {
                null
            }

            val activeSummary = walletsSummary.find { it.wallet.id == (validFilterId ?: activeWallet?.id) }
            HomeUiState(
                selectedWalletId = validFilterId,
                activeWallet = activeWallet,
                wallets = wallets,
                walletsSummary = walletsSummary,
                currentBalance = activeSummary?.balance ?: Money.ZERO,
                monthlyIncome = activeSummary?.monthlyIncome ?: Money.ZERO,
                monthlyExpense = activeSummary?.monthlyExpense ?: Money.ZERO,
                recentTransactions = summary.transactions.take(5),
                monthLabel = context.getString(R.string.home_month_label_format, summary.month, summary.year),
                isLoading = false,
                budgetProjection = projection,
                dueRemindersCount = dueCount,
                walletBackgroundUri = activeSummary?.backgroundUri,
                streakDays = streak,
                isSmartInsightsDismissed = isDismissed,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun selectWallet(walletId: Long?) {
        _selectedWalletFilterId.value = walletId
        if (walletId != null) {
            viewModelScope.launch {
                walletRepo.setActive(walletId)
            }
        }
    }

    fun setWalletBackground(walletId: Long, uri: String?) {
        viewModelScope.launch {
            appSettingsDataStore.setWalletBackground(walletId, uri)
        }
    }

    val settings = budgetSettingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetSettings(),
    )

    val hapticFeedbackEnabled = appSettingsDataStore.hapticFeedbackEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    val dailyReminderEnabled = appSettingsDataStore.dailyReminderEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    val budgetAlertsEnabled = appSettingsDataStore.budgetAlertsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    val weeklyDigestEnabled = appSettingsDataStore.weeklyDigestEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    val reminderHour = appSettingsDataStore.reminderHour.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 20,
    )

    val reminderMinute = appSettingsDataStore.reminderMinute.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 30,
    )

    fun setDailyReminderEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setDailyReminderEnabled(enabled)
            if (enabled) {
                ReminderScheduler.scheduleDailyReminder(
                    context,
                    targetHour = reminderHour.value,
                    targetMinute = reminderMinute.value,
                )
            } else {
                ReminderScheduler.cancelDailyReminder(context)
            }
        }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setBudgetAlertsEnabled(enabled)
        }
    }

    fun setWeeklyDigestEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setWeeklyDigestEnabled(enabled)
            if (enabled) {
                ReminderScheduler.scheduleWeeklyDigest(context)
            } else {
                ReminderScheduler.cancelWeeklyDigest(context)
            }
        }
    }

    fun updateReminderTime(context: Context, hour: Int, minute: Int) {
        viewModelScope.launch {
            appSettingsDataStore.setReminderTime(hour, minute)
            if (dailyReminderEnabled.value) {
                ReminderScheduler.scheduleDailyReminder(context, hour, minute)
            }
        }
    }

    @Suppress("unused")
    fun setMonthlyBudget(amountCents: Long) {
        viewModelScope.launch {
            budgetSettingsStore.setMonthlyBudget(amountCents)
        }
    }

}
