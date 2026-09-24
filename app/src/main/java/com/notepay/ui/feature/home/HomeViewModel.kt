package com.notepay.ui.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.ai.CloudGeminiAdvisor
import com.notepay.ai.GeminiNanoBudgetAdvisor
import com.notepay.ai.LegacyAiModelCleaner
import com.notepay.data.preferences.AiSettingsDataStore
import com.notepay.R
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.data.preferences.BudgetSettings
import com.notepay.data.preferences.BudgetSettingsStore
import com.notepay.domain.model.Money
import com.notepay.domain.repository.SubscriptionRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.GetMonthlySummaryUseCase
import com.notepay.domain.usecase.ObserveWalletBalanceUseCase
import com.notepay.platform.OsCompatHelper
import com.notepay.domain.util.StreakTrackerHelper
import com.notepay.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val walletRepo: WalletRepository,
    private val transactionRepo: TransactionRepository,
    private val getMonthlySummary: GetMonthlySummaryUseCase,
    private val observeWalletBalance: ObserveWalletBalanceUseCase,
    private val budgetSettingsStore: BudgetSettingsStore,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val aiSettingsDataStore: AiSettingsDataStore,
    private val subscriptionRepository: SubscriptionRepository,
    private val geminiNanoAdvisor: GeminiNanoBudgetAdvisor,
    private val cloudGeminiAdvisor: CloudGeminiAdvisor,
    private val legacyModelCleaner: LegacyAiModelCleaner,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    init {
        viewModelScope.launch {
            legacyModelCleaner.cleanLegacyModels()
        }
    }

    private val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    private val currentYear = today.year
    private val currentMonth = today.month.number

    private val _selectedMonth = MutableStateFlow(currentYear to currentMonth)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val state = combine(
        _selectedMonth,
        walletRepo.observeActive(),
        _isSmartInsightsDismissed,
    ) { monthPair, activeWallet, isDismissed ->
        Triple(monthPair, activeWallet, isDismissed)
    }.flatMapLatest { (monthPair, activeWallet, isDismissed) ->
        val (year, month) = monthPair
        val bgFlow = if (activeWallet != null) {
            appSettingsDataStore.observeWalletBackground(activeWallet.id)
        } else {
            flowOf(null)
        }
        combine(
            getMonthlySummary(year, month, activeWallet?.id),
            walletRepo.observeAll(),
            subscriptionRepository.observeAll(),
            transactionRepo.observeAll(),
            bgFlow,
        ) { summary, wallets, subscriptions, allTransactions, bgUri ->
            val balance = activeWallet?.let { observeWalletBalance(it.id).first() }
            
            // Tính số lời nhắc sắp đến hạn
            val now = Clock.System.now()
            val dueCount = subscriptions.count { sub ->
                sub.isActive && (sub.nextDueDate - now).inWholeDays <= sub.remindDaysBefore.toLong()
            }

            // Tính chuỗi ngày ghi chép liên tiếp (Streak 🔥)
            val streak = StreakTrackerHelper.calculateStreak(
                transactionInstants = allTransactions.map { it.createdAt },
                today = today.date
            )

            val activeWalletExpense = summary.totalExpense
        
        val budgetLimit = activeWallet?.budgetLimit
        val projection = if (activeWallet != null && budgetLimit != null && budgetLimit.amountInCents > 0) {
            val spentCents = activeWalletExpense.amountInCents
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
                spentThisWallet = activeWalletExpense
            )
        } else {
            null
        }

            HomeUiState(
                activeWallet = activeWallet,
                wallets = wallets,
                currentBalance = balance ?: Money.ZERO,
                monthlyIncome = summary.totalIncome,
                monthlyExpense = summary.totalExpense,
                recentTransactions = summary.transactions.take(5),
                monthLabel = context.getString(R.string.home_month_label_format, summary.month, summary.year),
                isLoading = false,
                budgetProjection = projection,
                dueRemindersCount = dueCount,
                walletBackgroundUri = bgUri,
                streakDays = streak,
                isSmartInsightsDismissed = isDismissed,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun selectWallet(walletId: Long) {
        viewModelScope.launch {
            walletRepo.setActive(walletId)
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

    val geminiApiKey = aiSettingsDataStore.geminiApiKey.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val cloudAiEnabled = aiSettingsDataStore.cloudAiEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    val smartReceiptAiEnabled = aiSettingsDataStore.smartReceiptAiEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    val isGeminiNanoAvailable = flow {
        emit(geminiNanoAdvisor.isGeminiNanoAvailable())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    val liquidGlassEnabled = appSettingsDataStore.liquidGlassEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OsCompatHelper.supportsLiquidGlass(),
    )

    val dailyReminderEnabled = appSettingsDataStore.dailyReminderEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    fun setLiquidGlassEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setLiquidGlassEnabled(enabled)
        }
    }

    fun setDailyReminderEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setDailyReminderEnabled(enabled)
            if (enabled) {
                ReminderScheduler.scheduleDailyReminder(context)
            } else {
                ReminderScheduler.cancelDailyReminder(context)
            }
        }
    }

    @Suppress("unused")
    fun setMonthlyBudget(amountCents: Long) {
        viewModelScope.launch {
            budgetSettingsStore.setMonthlyBudget(amountCents)
        }
    }

    fun setGeminiApiKey(key: String?) {
        viewModelScope.launch {
            aiSettingsDataStore.setGeminiApiKey(key)
        }
    }

    fun setCloudAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            aiSettingsDataStore.setCloudAiEnabled(enabled)
        }
    }

    fun setSmartReceiptAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            aiSettingsDataStore.setSmartReceiptAiEnabled(enabled)
        }
    }

    fun testGeminiApiKey(key: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = cloudGeminiAdvisor.testConnection(key)
            onResult(result)
        }
    }
}
