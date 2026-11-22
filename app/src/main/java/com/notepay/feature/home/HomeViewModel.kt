package com.notepay.feature.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.ai.LegacyAiModelCleaner
import com.notepay.data.datastore.AppSettingsDataStore
import com.notepay.data.local.HomeBackgroundStorage
import com.notepay.domain.analytics.HomeSmartTipGenerator
import com.notepay.domain.analytics.SmartTipInput
import com.notepay.domain.analytics.SmartTipItem
import com.notepay.domain.analytics.SmartTipWalletSnapshot
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.domain.repository.SharedWalletRepository
import com.notepay.domain.repository.SubscriptionRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.DeleteTransactionUseCase
import com.notepay.domain.usecase.GetMonthlySummaryUseCase
import com.notepay.domain.util.StreakTrackerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Clock

/** Số giao dịch gần đây tối đa hiển thị trên Home; giao diện tự gom theo ngày. */
private const val MaxRecentTransactions = 30

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val walletRepo: WalletRepository,
    private val transactionRepo: TransactionRepository,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getMonthlySummary: GetMonthlySummaryUseCase,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val subscriptionRepository: SubscriptionRepository,
    private val legacyModelCleaner: LegacyAiModelCleaner,
    @param:ApplicationContext private val context: Context,
    private val sharedWalletRepository: SharedWalletRepository? = null,
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

    private fun currentLocalDateTime(): LocalDateTime =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    private val _selectedMonth = MutableStateFlow(
        currentLocalDateTime().let { it.year to it.month.number }
    )
    private val _selectedWalletFilterId = MutableStateFlow<Long?>(null)

    fun dismissSmartInsights() {
        viewModelScope.launch {
            appSettingsDataStore.setHomeSmartTipsDismissed(true)
        }
    }

    fun setSmartInsightsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setHomeSmartTipsDismissed(!enabled)
        }
    }

    fun nextSmartTip() {
        viewModelScope.launch {
            appSettingsDataStore.incrementHomeSmartTipRotation()
        }
    }

    fun toggleBalanceHidden() {
        viewModelScope.launch {
            appSettingsDataStore.setHomeBalanceHidden(!state.value.isBalanceHidden)
        }
    }

    fun selectBackgroundPreset(preset: HomeBackgroundPreset) {
        viewModelScope.launch {
            appSettingsDataStore.setHomeBackgroundPreset(preset.id)
        }
    }

    fun setBackgroundBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setHomeBackgroundBlurEnabled(enabled)
        }
    }

    fun applyCustomBackground(
        uri: Uri,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            val previousPath = state.value.customBackgroundPath
            val saved = HomeBackgroundStorage.saveFromUri(context, uri)
            saved.onSuccess { newPath ->
                appSettingsDataStore.setHomeBackgroundCustomImage(newPath)
                if (previousPath != null && previousPath != newPath) {
                    HomeBackgroundStorage.deleteCustomBackground(previousPath)
                }
                onResult(true)
            }.onFailure {
                onResult(false)
            }
        }
    }

    fun clearCustomBackground() {
        viewModelScope.launch {
            val previousPath = state.value.customBackgroundPath
            appSettingsDataStore.setHomeBackgroundCustomImage(null)
            HomeBackgroundStorage.deleteCustomBackground(previousPath)
        }
    }

    private data class HomeFilterState(
        val monthPair: Pair<Int, Int>,
        val activeWallet: Wallet?,
        val selectedFilterId: Long?,
    )

    private data class HomePrefsState(
        val isSmartTipsDismissed: Boolean,
        val smartTipRotation: Int,
        val backgroundPresetId: String,
        val customBackgroundPath: String?,
        val backgroundBlurEnabled: Boolean,
        val isBalanceHidden: Boolean,
    )

    private data class HomeDataSnapshot(
        val baseState: HomeUiState,
        val smartTipPool: List<SmartTipItem>,
        val epochDay: Long,
    )

    private val homePrefsFlow = combine(
        combine(
            appSettingsDataStore.homeSmartTipsDismissed,
            appSettingsDataStore.homeSmartTipRotation,
            appSettingsDataStore.homeBackgroundPreset,
        ) { dismissed, rotation, presetId ->
            Triple(dismissed, rotation, presetId)
        },
        combine(
            appSettingsDataStore.homeBackgroundImagePath,
            appSettingsDataStore.homeBackgroundBlurEnabled,
            appSettingsDataStore.homeBalanceHidden,
        ) { imagePath, blurEnabled, balanceHidden ->
            Triple(imagePath, blurEnabled, balanceHidden)
        },
    ) { (dismissed, rotation, presetId), (imagePath, blurEnabled, balanceHidden) ->
        HomePrefsState(
            isSmartTipsDismissed = dismissed,
            smartTipRotation = rotation,
            backgroundPresetId = presetId,
            customBackgroundPath = imagePath,
            backgroundBlurEnabled = blurEnabled,
            isBalanceHidden = balanceHidden,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val homeDataFlow = combine(
        _selectedMonth,
        walletRepo.observeActive(),
        _selectedWalletFilterId,
    ) { monthPair, activeWallet, selectedFilterId ->
        HomeFilterState(monthPair, activeWallet, selectedFilterId)
    }.flatMapLatest { filter ->
        val (year, month) = filter.monthPair
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
            val statsMap = stats.associateBy { it.walletId }
            wallets.map { wallet ->
                val stat = statsMap[wallet.id]
                val totalInc = stat?.allTimeIncome ?: Money.ZERO
                val totalExp = stat?.allTimeExpense ?: Money.ZERO
                WalletSummary(
                    wallet = wallet,
                    balance = wallet.initialBalance + totalInc - totalExp,
                    monthlyIncome = stat?.currentMonthIncome ?: Money.ZERO,
                    monthlyExpense = stat?.currentMonthExpense ?: Money.ZERO,
                )
            }
        }

        combine(
            getMonthlySummary(year, month, filter.selectedFilterId),
            walletsSummaryFlow,
            subscriptionRepository.observeAll(),
            transactionRepo.observeAllCreatedDates(),
        ) { summary, walletsSummary, subscriptions, createdDates ->
            buildHomeDataSnapshot(
                filter = filter,
                summary = summary,
                walletsSummary = walletsSummary,
                subscriptions = subscriptions,
                createdDates = createdDates,
            )
        }
    }

    val state = combine(homeDataFlow, homePrefsFlow) { snapshot, prefs ->
        val (_, selectedTipIndex) = HomeSmartTipGenerator.selectTip(
            pool = snapshot.smartTipPool,
            epochDay = snapshot.epochDay,
            rotationOffset = prefs.smartTipRotation,
        )
        val rawPreset = HomeBackgroundPreset.fromId(prefs.backgroundPresetId)
        val resolvedPreset = if (rawPreset == HomeBackgroundPreset.CUSTOM && prefs.customBackgroundPath.isNullOrBlank()) {
            HomeBackgroundPreset.WARM_SILK
        } else {
            rawPreset
        }
        snapshot.baseState.copy(
            isSmartInsightsDismissed = prefs.isSmartTipsDismissed,
            smartTips = snapshot.smartTipPool,
            activeSmartTipIndex = selectedTipIndex,
            backgroundPreset = resolvedPreset,
            customBackgroundPath = prefs.customBackgroundPath,
            backgroundBlurEnabled = prefs.backgroundBlurEnabled,
            isBalanceHidden = prefs.isBalanceHidden,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    private fun buildHomeDataSnapshot(
        filter: HomeFilterState,
        summary: GetMonthlySummaryUseCase.Summary,
        walletsSummary: List<WalletSummary>,
        subscriptions: List<com.notepay.domain.model.Subscription>,
        createdDates: List<kotlin.time.Instant>,
    ): HomeDataSnapshot {
        val today = currentLocalDateTime()
        val wallets = walletsSummary.map { it.wallet }
        val validFilterId = filter.selectedFilterId?.takeIf { id -> wallets.any { it.id == id } }

        val now = Clock.System.now()
        val dueCount = subscriptions.count { sub ->
            sub.isActive && (sub.nextDueDate - now).inWholeDays <= sub.remindDaysBefore.toLong()
        }

        val streak = StreakTrackerHelper.calculateStreak(
            transactionInstants = createdDates,
            today = today.date,
        )

        val targetWallet = validFilterId?.let { id -> wallets.find { it.id == id } }
        val projection = computeBudgetProjection(
            budgetLimit = targetWallet?.budgetLimit,
            totalExpense = summary.totalExpense,
            today = today,
        )

        val activeSummary = walletsSummary.find { it.wallet.id == (validFilterId ?: filter.activeWallet?.id) }
        val epochDay = today.date.toEpochDays()
        val tipPool = HomeSmartTipGenerator.generatePool(
            SmartTipInput(
                transactions = summary.transactions,
                wallets = walletsSummary.map { ws ->
                    SmartTipWalletSnapshot(
                        name = ws.wallet.name,
                        balance = ws.balance,
                        budgetLimit = ws.wallet.budgetLimit,
                    )
                },
                streakDays = streak,
                dueRemindersCount = dueCount,
                epochDay = epochDay,
            )
        )

        val baseState = HomeUiState(
            selectedWalletId = validFilterId,
            activeWallet = filter.activeWallet,
            wallets = wallets,
            walletsSummary = walletsSummary,
            currentBalance = activeSummary?.balance ?: Money.ZERO,
            monthlyIncome = activeSummary?.monthlyIncome ?: Money.ZERO,
            monthlyExpense = activeSummary?.monthlyExpense ?: Money.ZERO,
            recentTransactions = summary.transactions.take(MaxRecentTransactions),
            monthLabel = context.getString(
                R.string.home_month_label_format,
                summary.month,
                summary.year,
            ),
            selectedMonth = summary.month,
            selectedYear = summary.year,
            isLoading = false,
            budgetProjection = projection,
            dueRemindersCount = dueCount,
            streakDays = streak,
            currentUserId = sharedWalletRepository?.currentUserId(),
        )

        return HomeDataSnapshot(
            baseState = baseState,
            smartTipPool = tipPool,
            epochDay = epochDay,
        )
    }

    private fun computeBudgetProjection(
        budgetLimit: Money?,
        totalExpense: Money,
        today: LocalDateTime,
    ): BudgetProjection? {
        if (budgetLimit == null || budgetLimit.amountInCents <= 0L) return null
        val spentCents = totalExpense.amountInCents
        val limitCents = budgetLimit.amountInCents
        val currentDay = today.day.coerceIn(1, 31)
        val todayMonthStart = LocalDate(today.year, today.month.number, 1)
        val daysInMonth = (todayMonthStart.plus(1, DateTimeUnit.MONTH).toEpochDays() -
            todayMonthStart.toEpochDays()).toInt()

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

        return BudgetProjection(
            dailyAverage = Money(dailyAverageCents.toLong()),
            projectedSpend = Money(projectedSpendCents.toLong()),
            isProjectedToExceed = isProjectedToExceed,
            exhaustionDateLabel = exhaustionDateLabel,
            safeDailyLimit = Money(safeDailyLimitCents),
            spentPercentage = spentPercentage,
            spentThisWallet = totalExpense,
        )
    }

    fun selectWallet(walletId: Long?) {
        // Chỉ lọc giao dịch trên trang Home. Việc đặt ví mặc định (active wallet)
        // thuộc về màn Assets để tránh side-effect ghi DB khi người dùng chỉ xem.
        _selectedWalletFilterId.value = walletId
    }

    val hapticFeedbackEnabled = appSettingsDataStore.hapticFeedbackEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )
}
