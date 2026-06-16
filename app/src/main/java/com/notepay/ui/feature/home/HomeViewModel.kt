package com.notepay.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.GetMonthlySummaryUseCase
import com.notepay.domain.usecase.ObserveWalletBalanceUseCase
import com.notepay.data.preferences.NotificationSettingsStore
import com.notepay.domain.model.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import com.notepay.domain.repository.SubscriptionRepository

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val walletRepo: WalletRepository,
    private val getMonthlySummary: GetMonthlySummaryUseCase,
    private val observeWalletBalance: ObserveWalletBalanceUseCase,
    private val notificationSettingsStore: NotificationSettingsStore,
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    private val currentYear = today.year
    private val currentMonth = today.monthNumber

    private val _selectedMonth = MutableStateFlow(currentYear to currentMonth)

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
        _selectedMonth.flatMapLatest { (year, month) -> getMonthlySummary(year, month) },
        walletRepo.observeActive(),
        walletRepo.observeAll(),
        subscriptionRepository.observeAll()
    ) { summary, activeWallet, wallets, subscriptions ->
        val balance = activeWallet?.let { observeWalletBalance(it.id).first() }
        
        // Calculate due/upcoming reminders count
        val now = Clock.System.now()
        val dueCount = subscriptions.count { sub ->
            sub.isActive && (sub.nextDueDate - now).inWholeDays <= sub.remindDaysBefore.toLong()
        }

        val activeWalletExpense = if (activeWallet != null) {
            summary.transactions
                .filter { it.walletId == activeWallet.id && it.type == com.notepay.domain.model.TransactionType.EXPENSE }
                .fold(Money.ZERO) { acc, t -> acc + t.amount }
        } else {
            Money.ZERO
        }
        
        val budgetLimit = activeWallet?.budgetLimit
        val projection = if (activeWallet != null && budgetLimit != null && budgetLimit.amountInCents > 0) {
            val spentCents = activeWalletExpense.amountInCents
            val limitCents = budgetLimit.amountInCents
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val currentDay = today.dayOfMonth.coerceIn(1, 31)
            val daysInMonth = getDaysInMonth(today.year, today.monthNumber)
            
            val dailyAverageCents = spentCents.toFloat() / currentDay
            val projectedSpendCents = dailyAverageCents * daysInMonth
            val spentPercentage = (spentCents.toFloat() / limitCents).coerceIn(0f, 1f)
            val isProjectedToExceed = projectedSpendCents > limitCents
            
            val exhaustionDateLabel = if (isProjectedToExceed && dailyAverageCents > 0) {
                val exhaustionDay = (limitCents / dailyAverageCents).toInt().coerceIn(1, daysInMonth)
                "%02d/%02d".format(exhaustionDay, today.monthNumber)
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
            currentBalance = balance ?: com.notepay.domain.model.Money.ZERO,
            monthlyIncome = summary.totalIncome,
            monthlyExpense = summary.totalExpense,
            recentTransactions = summary.transactions.take(5),
            monthLabel = "Tháng ${summary.month}/${summary.year}",
            isLoading = false,
            budgetProjection = projection,
            dueRemindersCount = dueCount
        )
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

    val settings = notificationSettingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = com.notepay.data.preferences.NotificationSettings(),
    )

    fun setAutoCaptureEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationSettingsStore.setAutoCaptureEnabled(enabled)
        }
    }

    fun setTrackAllBanks(enabled: Boolean) {
        viewModelScope.launch {
            notificationSettingsStore.setTrackAllBanks(enabled)
        }
    }

    fun setPackageEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            notificationSettingsStore.setPackageEnabled(packageName, enabled)
        }
    }

    fun setExcludedPackages(packages: Set<String>) {
        viewModelScope.launch {
            notificationSettingsStore.setExcludedPackages(packages)
        }
    }

    fun addCustomBankApp(packageName: String, label: String) {
        viewModelScope.launch {
            notificationSettingsStore.addCustomBankApp(packageName, label)
        }
    }

    fun removeCustomBankApp(packageName: String) {
        viewModelScope.launch {
            notificationSettingsStore.removeCustomBankApp(packageName)
        }
    }
}
