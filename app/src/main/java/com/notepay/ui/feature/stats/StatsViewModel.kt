package com.notepay.ui.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.ui.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val walletRepo: WalletRepository,
) : ViewModel() {

    private val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    private val currentMonthYear = MutableStateFlow(MonthYear(now.year, now.month.ordinal + 1))
    
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _selectedWalletId = MutableStateFlow<Long?>(null)
    val selectedWalletId: StateFlow<Long?> = _selectedWalletId.asStateFlow()

    private val _timeFilter = MutableStateFlow<TimeFilterType>(TimeFilterType.MONTH)
    val timeFilter: StateFlow<TimeFilterType> = _timeFilter.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private data class FilterState(
        val monthYear: MonthYear,
        val selectedWalletId: Long?,
        val timeFilter: TimeFilterType,
        val customDateRange: Pair<Long, Long>?,
        val selectedCategory: Category?
    )

    private val filterState = combine(
        currentMonthYear,
        _selectedWalletId,
        _timeFilter,
        _customDateRange,
        _selectedCategory
    ) { monthYear, selectedWalletId, timeFilter, customDateRange, selectedCategory ->
        FilterState(monthYear, selectedWalletId, timeFilter, customDateRange, selectedCategory)
    }

    val state: StateFlow<StatsUiState> = combine(
        transactionRepo.observeAll(),
        walletRepo.observeAll(),
        filterState
    ) { allTransactions, wallets, filters ->
        val monthYear = filters.monthYear
        val selectedWalletId = filters.selectedWalletId
        val timeFilter = filters.timeFilter
        val customDateRange = filters.customDateRange
        val selectedCat = filters.selectedCategory
        
        val zone = TimeZone.currentSystemDefault()
        
        // 1. Tính toán khoảng thời gian (start & end millis)
        val (startMillis, endMillis) = when (timeFilter) {
            TimeFilterType.MONTH -> {
                getMonthRange(monthYear.year, monthYear.month)
            }
            TimeFilterType.WEEK -> {
                val todayDate = now.date
                val daysToSubtract = todayDate.dayOfWeek.ordinal // Mon=0, Tue=1... Sun=6
                val monday = todayDate.minus(DatePeriod(days = daysToSubtract))
                val sunday = monday.plus(DatePeriod(days = 6))
                
                val start = LocalDateTime(monday.year, monday.monthNumber, monday.dayOfMonth, 0, 0)
                    .toInstant(zone).toEpochMilliseconds()
                val end = LocalDateTime(sunday.year, sunday.monthNumber, sunday.dayOfMonth, 23, 59, 59, 999000000)
                    .toInstant(zone).toEpochMilliseconds()
                start to end
            }
            TimeFilterType.QUARTER -> {
                val currentMonthNum = now.monthNumber
                val startMonth = ((currentMonthNum - 1) / 3) * 3 + 1
                val endMonth = startMonth + 2
                val startLocalDate = LocalDate(now.year, startMonth, 1)
                val endLocalDate = if (endMonth == 12) {
                    LocalDate(now.year, 12, 31)
                } else {
                    val nextQuarterFirstDate = LocalDate(now.year, endMonth + 1, 1)
                    nextQuarterFirstDate.minus(DatePeriod(days = 1))
                }
                val start = LocalDateTime(startLocalDate.year, startLocalDate.monthNumber, startLocalDate.dayOfMonth, 0, 0)
                    .toInstant(zone).toEpochMilliseconds()
                val end = LocalDateTime(endLocalDate.year, endLocalDate.monthNumber, endLocalDate.dayOfMonth, 23, 59, 59, 999000000)
                    .toInstant(zone).toEpochMilliseconds()
                start to end
            }
            TimeFilterType.YEAR -> {
                val startLocalDate = LocalDate(now.year, 1, 1)
                val endLocalDate = LocalDate(now.year, 12, 31)
                val start = LocalDateTime(startLocalDate.year, startLocalDate.monthNumber, startLocalDate.dayOfMonth, 0, 0)
                    .toInstant(zone).toEpochMilliseconds()
                val end = LocalDateTime(endLocalDate.year, endLocalDate.monthNumber, endLocalDate.dayOfMonth, 23, 59, 59, 999000000)
                    .toInstant(zone).toEpochMilliseconds()
                start to end
            }
            TimeFilterType.CUSTOM -> {
                val range = customDateRange
                if (range != null) {
                    range.first to range.second
                } else {
                    getMonthRange(now.year, now.monthNumber)
                }
            }
        }

        // 2. Tạo date range label hiển thị trên UI
        val dateRangeLabel = when (timeFilter) {
            TimeFilterType.MONTH -> "Tháng %02d/%d".format(monthYear.month, monthYear.year)
            TimeFilterType.WEEK -> {
                val instantStart = Instant.fromEpochMilliseconds(startMillis)
                val instantEnd = Instant.fromEpochMilliseconds(endMillis)
                val dtStart = instantStart.toLocalDateTime(zone)
                val dtEnd = instantEnd.toLocalDateTime(zone)
                "Tuần này (%02d/%02d - %02d/%02d)".format(
                    dtStart.dayOfMonth, dtStart.monthNumber,
                    dtEnd.dayOfMonth, dtEnd.monthNumber
                )
            }
            TimeFilterType.QUARTER -> {
                val q = (now.monthNumber - 1) / 3 + 1
                "Quý $q (%d)".format(now.year)
            }
            TimeFilterType.YEAR -> "Năm %d".format(now.year)
            TimeFilterType.CUSTOM -> {
                "%s - %s".format(formatEpochMillis(startMillis), formatEpochMillis(endMillis))
            }
        }

        // 3. Lọc danh sách giao dịch
        val filteredTxs = allTransactions.filter { tx ->
            val txMillis = tx.occurredAt.toEpochMilliseconds()
            val matchesTime = txMillis in startMillis..endMillis
            val matchesWallet = selectedWalletId == null || tx.walletId == selectedWalletId
            matchesTime && matchesWallet
        }

        // 4. Tính toán thu nhập, chi tiêu, breakdown
        val income = filteredTxs.asSequence()
            .filter { it.type == TransactionType.INCOME }
            .fold(Money.ZERO) { acc, t -> acc + t.amount }
        val expense = filteredTxs.asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .fold(Money.ZERO) { acc, t -> acc + t.amount }

        val totalExpenseCents = expense.amountInCents
        val breakdown = filteredTxs.asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, list) ->
                list.fold(Money.ZERO) { acc, t -> acc + t.amount }
            }
            .map { (cat, amount) ->
                val pct = if (totalExpenseCents > 0) {
                    amount.amountInCents.toFloat() / totalExpenseCents
                } else {
                    0f
                }
                CategoryBreakdownItem(cat, amount, pct)
            }
            .sortedByDescending { it.amount.amountInCents }

        val totalIncomeCents = income.amountInCents
        val incomeBreakdown = filteredTxs.asSequence()
            .filter { it.type == TransactionType.INCOME }
            .groupBy { it.category }
            .mapValues { (_, list) ->
                list.fold(Money.ZERO) { acc, t -> acc + t.amount }
            }
            .map { (cat, amount) ->
                val pct = if (totalIncomeCents > 0) {
                    amount.amountInCents.toFloat() / totalIncomeCents
                } else {
                    0f
                }
                CategoryBreakdownItem(cat, amount, pct)
            }
            .sortedByDescending { it.amount.amountInCents }

        // 5. Xác định ví được chọn
        val selectedWallet = wallets.find { it.id == selectedWalletId }

        // 6. Tính toán hạn mức (Budget Progress)
        val currentMonthStartEnd = getMonthRange(now.year, now.monthNumber)
        val currentMonthTxs = allTransactions.filter {
            val txMillis = it.occurredAt.toEpochMilliseconds()
            txMillis >= currentMonthStartEnd.first && txMillis <= currentMonthStartEnd.second
        }
        val walletExpenseInCurrentMonth = currentMonthTxs.filter {
            it.type == TransactionType.EXPENSE &&
            (selectedWalletId == null || it.walletId == selectedWalletId)
        }.fold(Money.ZERO) { acc, t -> acc + t.amount }

        val limit = if (selectedWalletId != null) {
            selectedWallet?.budgetLimit
        } else {
            val totalLimitCents = wallets.mapNotNull { it.budgetLimit?.amountInCents }.sum()
            if (totalLimitCents > 0) Money(totalLimitCents) else null
        }

        val budgetPercentage = if (limit != null && limit.amountInCents > 0) {
            walletExpenseInCurrentMonth.amountInCents.toFloat() / limit.amountInCents
        } else {
            0f
        }

        // 7. Dự báo chi tiêu cuối tháng (Forecast)
        val isViewingCurrentMonth = timeFilter == TimeFilterType.MONTH &&
                monthYear.year == now.year && monthYear.month == now.monthNumber
        
        val forecast = if (isViewingCurrentMonth) {
            val spentCents = walletExpenseInCurrentMonth.amountInCents
            val currentDay = now.dayOfMonth.coerceIn(1, 31)
            val daysInMonth = getDaysInMonth(now.year, now.monthNumber)
            
            val dailyAverageCents = spentCents.toDouble() / currentDay
            val projectedSpendCents = dailyAverageCents * daysInMonth
            
            val dailyAvgStr = MoneyFormatter.format(Money(dailyAverageCents.toLong()))
            val projectedSpendStr = MoneyFormatter.format(Money(projectedSpendCents.toLong()))
            
            val forecastMessage = "Dự báo chi tiêu: Với tốc độ chi tiêu hiện tại (trung bình $dailyAvgStr/ngày), dự kiến cuối tháng này bạn sẽ chi tiêu khoảng $projectedSpendStr."
            
            val isProjectedToExceed = limit != null && projectedSpendCents > limit.amountInCents
            
            BudgetForecast(
                dailyAverage = Money(dailyAverageCents.toLong()),
                projectedSpend = Money(projectedSpendCents.toLong()),
                forecastMessage = forecastMessage,
                isProjectedToExceed = isProjectedToExceed
            )
        } else {
            null
        }

        StatsUiState(
            year = monthYear.year,
            month = monthYear.month,
            totalIncome = income,
            totalExpense = expense,
            balance = income - expense,
            breakdown = breakdown,
            incomeBreakdown = incomeBreakdown,
            isLoading = false,
            isCurrentMonth = monthYear.year == now.year && monthYear.month == now.month.ordinal + 1,
            selectedCategory = selectedCat,
            transactions = filteredTxs,
            wallets = wallets,
            selectedWallet = selectedWallet,
            timeFilter = timeFilter,
            dateRangeLabel = dateRangeLabel,
            customStartDateMillis = customDateRange?.first,
            customEndDateMillis = customDateRange?.second,
            budgetLimit = limit,
            budgetSpent = walletExpenseInCurrentMonth,
            budgetPercentage = budgetPercentage,
            spendingForecast = forecast
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(year = now.year, month = now.month.ordinal + 1),
    )

    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
    }

    fun selectWallet(walletId: Long?) {
        _selectedWalletId.value = walletId
    }

    fun selectTimeFilter(filter: TimeFilterType) {
        _timeFilter.value = filter
        _selectedCategory.value = null
    }

    fun selectCustomDateRange(startMillis: Long, endMillis: Long) {
        _customDateRange.value = startMillis to endMillis
        _timeFilter.value = TimeFilterType.CUSTOM
        _selectedCategory.value = null
    }

    fun onPreviousMonth() {
        currentMonthYear.update { current ->
            if (current.month == 1) {
                MonthYear(current.year - 1, 12)
            } else {
                MonthYear(current.year, current.month - 1)
            }
        }
    }

    fun onNextMonth() {
        currentMonthYear.update { current ->
            if (current.month == 12) {
                MonthYear(current.year + 1, 1)
            } else {
                MonthYear(current.year, current.month + 1)
            }
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    private fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val zone = TimeZone.currentSystemDefault()
        val firstDate = LocalDate(year, month, 1)
        val nextMonth = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
        val first = LocalDateTime(firstDate.year, firstDate.monthNumber, firstDate.dayOfMonth, 0, 0)
            .toInstant(zone)
        val lastExclusive = LocalDateTime(nextMonth.year, nextMonth.monthNumber, nextMonth.dayOfMonth, 0, 0)
            .toInstant(zone)
        return first.toEpochMilliseconds() to (lastExclusive.toEpochMilliseconds() - 1)
    }

    private fun formatEpochMillis(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "%02d/%02d/%d".format(dt.dayOfMonth, dt.monthNumber, dt.year)
    }

    private fun getDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }

    private data class MonthYear(val year: Int, val month: Int)
}
