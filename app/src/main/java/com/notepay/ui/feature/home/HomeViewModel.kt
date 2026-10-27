package com.notepay.ui.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.ai.LegacyAiModelCleaner
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
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
import kotlinx.coroutines.flow.flowOf
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
    private val _isSmartInsightsDismissed = MutableStateFlow(false)

    fun dismissSmartInsights() {
        _isSmartInsightsDismissed.value = true
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
            // Mỗi lượt emit dựng thẳng danh sách tóm tắt: một phép biến đổi thuần, không mở
            // thêm N luồng DataStore như trước đây.
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
            getMonthlySummary(year, month, selectedFilterId),
            walletsSummaryFlow,
            subscriptionRepository.observeAll(),
            transactionRepo.observeAllCreatedDates(),
        ) { summary, walletsSummary, subscriptions, createdDates ->
            val today = currentLocalDateTime()
            val wallets = walletsSummary.map { it.wallet }

            // Phòng thủ: nếu ví đang lọc bị xóa thì tự động fallback về null mà không gây side-effect phát lại luồng
            val validFilterId = selectedFilterId?.takeIf { id -> wallets.any { it.id == id } }

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
                // Số ngày của tháng hiện tại = khoảng cách tới ngày 1 của tháng kế tiếp.
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
                recentTransactions = summary.transactions.take(MaxRecentTransactions),
                monthLabel = context.getString(R.string.home_month_label_format, summary.month, summary.year),
                selectedMonth = summary.month,
                selectedYear = summary.year,
                isLoading = false,
                budgetProjection = projection,
                dueRemindersCount = dueCount,
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
