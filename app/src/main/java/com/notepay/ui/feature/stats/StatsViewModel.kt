package com.notepay.ui.feature.stats

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.ai.OnDeviceBudgetAdvisor
import com.notepay.domain.analytics.AdvisorAvailability
import com.notepay.domain.analytics.AdvisorCategorySummary
import com.notepay.domain.analytics.BudgetAdvisorInput
import com.notepay.domain.analytics.CategoryExpenseShare
import com.notepay.domain.analytics.DailyExpense
import com.notepay.domain.analytics.SpendingForecastEngine
import com.notepay.domain.analytics.StatsAdviceInput
import com.notepay.domain.analytics.StatsInsightsCalculator
import com.notepay.domain.analytics.StatsPeriod
import com.notepay.domain.analytics.StatsRange
import com.notepay.domain.analytics.StatsSummaryCalculator
import com.notepay.domain.analytics.customPeriod
import com.notepay.domain.analytics.periodFor
import com.notepay.domain.analytics.shift
import com.notepay.domain.model.Category
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import com.notepay.domain.repository.SubscriptionRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.ui.formatter.PresentationDateFormatter
import com.notepay.ui.util.localizedName
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val walletRepo: WalletRepository,
    private val subscriptionRepo: SubscriptionRepository,
    @param:ApplicationContext private val context: Context,
    private val budgetAdvisor: OnDeviceBudgetAdvisor,
) : ViewModel() {

    private val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    private val _currentPeriod = MutableStateFlow(
        periodFor(LocalDate(now.year, now.month, 1), StatsRange.MONTH)
    )
    val currentPeriod: StateFlow<StatsPeriod> = _currentPeriod.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _selectedWalletId = MutableStateFlow<Long?>(null)
    val selectedWalletId: StateFlow<Long?> = _selectedWalletId.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private data class FilterState(
        val period: StatsPeriod,
        val selectedWalletId: Long?,
    )

    private val filterState = combine(
        _currentPeriod,
        _selectedWalletId
    ) { period, walletId ->
        FilterState(period, walletId)
    }.distinctUntilChanged()

    private val advicePrefs = context.getSharedPreferences("notepay_ai_feedback", Context.MODE_PRIVATE)
    private val _adviceFeedbacks = MutableStateFlow<Map<String, Int>>(
        advicePrefs.all.mapValues { it.value as? Int ?: 0 }
    )
    private val _localAdvisor = MutableStateFlow(LocalAdvisorUiState())
    @Volatile private var latestAdvisorInput: BudgetAdvisorInput? = null

    init {
        viewModelScope.launch {
            refreshAdvisorAvailability()
        }
    }

    fun sendAdviceFeedback(adviceId: String, score: Int) {
        advicePrefs.edit { putInt(adviceId, score) }
        _adviceFeedbacks.update { current ->
            current.toMutableMap().apply { this[adviceId] = score }
        }
    }

    private val baseState: StateFlow<StatsUiState> = combine(
        transactionRepo.observeAll(),
        walletRepo.observeAll(),
        subscriptionRepo.observeAll(),
        _adviceFeedbacks,
        filterState
    ) { allTransactions, wallets, subscriptions, feedbacks, filters ->
        val selectedWalletId = filters.selectedWalletId
        val period = filters.period
        
        val zone = TimeZone.currentSystemDefault()
        
        // 1. Tạo date range label hiển thị trên UI từ period
        val dateRangeLabel = when (period.range) {
            StatsRange.MONTH -> {
                val anchor = period.start ?: now.date
                context.getString(R.string.stats_date_month_format, anchor.month.number, anchor.year)
            }
            StatsRange.WEEK -> {
                val s = period.start ?: now.date
                val e = period.endExclusive?.minus(DatePeriod(days = 1)) ?: now.date
                context.getString(R.string.stats_date_week_format, s.dayOfMonth, s.month.number, e.dayOfMonth, e.month.number)
            }
            StatsRange.YEAR -> {
                val anchor = period.start ?: now.date
                context.getString(R.string.stats_date_year_format, anchor.year)
            }
            StatsRange.ALL -> context.getString(R.string.stats_date_all_time)
            StatsRange.CUSTOM -> {
                val s = period.start ?: now.date
                val e = period.endExclusive?.minus(DatePeriod(days = 1)) ?: now.date
                "${s.day}/${s.month.number}/${s.year} - ${e.day}/${e.month.number}/${e.year}"
            }
        }

        // 2. Lọc danh sách giao dịch theo period
        val filteredTxs = allTransactions.filter { tx ->
            val txDate = tx.occurredAt.toLocalDateTime(zone).date
            val matchesTime = period.contains(txDate)
            val matchesWallet = selectedWalletId == null || tx.walletId == selectedWalletId
            matchesTime && matchesWallet
        }

        // 2.5 Tính toán so sánh với kỳ trước
        val firstTxDate = allTransactions.minByOrNull { it.occurredAt }?.occurredAt?.toLocalDateTime(zone)?.date
        val prevRange = com.notepay.domain.analytics.comparablePreviousRange(period, now.date)
        val hasData = com.notepay.domain.analytics.hasComparableData(prevRange, firstTxDate)
        
        var previousExpense: Money? = null
        var previousIncome: Money? = null
        if (hasData && prevRange != null) {
            var pExp = 0L
            var pInc = 0L
            allTransactions.forEach { tx ->
                val txDate = tx.occurredAt.toLocalDateTime(zone).date
                if (txDate in prevRange) {
                    if (selectedWalletId == null || tx.walletId == selectedWalletId) {
                        if (tx.type == com.notepay.domain.model.TransactionType.EXPENSE) pExp += tx.amount.amountInCents
                        else if (tx.type == com.notepay.domain.model.TransactionType.INCOME) pInc += tx.amount.amountInCents
                    }
                }
            }
            previousExpense = Money(pExp)
            previousIncome = Money(pInc)
        }

        // 4. Tính toán thu nhập, chi tiêu, breakdown
        val summary = StatsSummaryCalculator.summarize(filteredTxs)
        val income = Money(summary.totalIncomeInCents)
        val expense = Money(summary.totalExpenseInCents)
        val breakdown = summary.expenseBreakdown.map { item ->
            CategoryBreakdownItem(
                category = item.category,
                amount = Money(item.amountInCents),
                percentage = item.share,
            )
        }
        val incomeBreakdown = summary.incomeBreakdown.map { item ->
            CategoryBreakdownItem(
                category = item.category,
                amount = Money(item.amountInCents),
                percentage = item.share,
            )
        }

        // 5. Xác định ví được chọn
        val selectedWallet = wallets.find { it.id == selectedWalletId }

        // 6. Tính toán hạn mức (Budget Progress)
        val currentMonthStartEnd = getMonthRange(now.year, now.month.number)
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
        val isViewingCurrentMonth = period.range == com.notepay.domain.analytics.StatsRange.MONTH && period.contains(now.date)
        
        val prediction = if (isViewingCurrentMonth) {
            val dailyExpenses = allTransactions.asSequence()
                .filter {
                    it.type == TransactionType.EXPENSE &&
                        (selectedWalletId == null || it.walletId == selectedWalletId)
                }
                .groupBy { it.occurredAt.toLocalDateTime(zone).date }
                .map { (date, rows) ->
                    DailyExpense(date, rows.sumOf { it.amount.amountInCents })
                }
            SpendingForecastEngine.forecast(
                expenses = dailyExpenses,
                today = now.date,
                daysInMonth = getDaysInMonth(now.year, now.month.number),
                budgetLimitInCents = limit?.amountInCents,
            )
        } else null

        val forecast = prediction?.let { value ->
            val probabilityText = value.overBudgetProbability?.let {
                context.getString(R.string.stats_forecast_probability_suffix, (it * 100).toInt())
            }.orEmpty()
            StatsInsightsUiMapper.mapPrediction(value, limit?.amountInCents, probabilityText)
        }

        // 8. Tính toán Dynamic Daily Budget
        val daysInMonth = getDaysInMonth(now.year, now.month.number)
        val currentDay = now.day.coerceIn(1, daysInMonth)

        val todayStart = LocalDateTime(now.year, now.month.number, currentDay, 0, 0)
            .toInstant(zone).toEpochMilliseconds()
        val todayEnd = LocalDateTime(now.year, now.month.number, currentDay, 23, 59, 59, 999000000)
            .toInstant(zone).toEpochMilliseconds()

        val spentToday = currentMonthTxs.filter {
            it.type == TransactionType.EXPENSE &&
            (selectedWalletId == null || it.walletId == selectedWalletId) &&
            it.occurredAt.toEpochMilliseconds() in todayStart..todayEnd
        }.fold(Money.ZERO) { acc, t -> acc + t.amount }

        val remainingDays = daysInMonth - currentDay + 1
        val spentExceptToday = (walletExpenseInCurrentMonth.amountInCents - spentToday.amountInCents).coerceAtLeast(0L)
        
        val dynamicDailyBudget = if (limit != null && limit.amountInCents > 0 && isViewingCurrentMonth) {
            StatsInsightsUiMapper.mapDynamicDailyBudget(
                StatsInsightsCalculator.computeDynamicDailyBudget(
                    limitInCents = limit.amountInCents,
                    spentExceptTodayInCents = spentExceptToday,
                    spentTodayInCents = spentToday.amountInCents,
                    remainingDays = remainingDays,
                    daysInMonth = daysInMonth,
                    currentDay = currentDay,
                    currentMonthExpenseInCents = walletExpenseInCurrentMonth.amountInCents,
                ),
            )
        } else {
            null
        }

        // 9. Smart Subscription Detection
        val detectedSubscriptions = mutableListOf<DetectedSubscription>()
        if (isViewingCurrentMonth) {
            val expenses = allTransactions.filter { it.type == TransactionType.EXPENSE }
            val groupedExpenses = expenses.groupBy { 
                it.category.id to (it.amount.amountInCents / 500000L) // Group within 5,000 VND range
            }

            for ((_, txList) in groupedExpenses) {
                if (txList.size >= 2) {
                    val sortedTx = txList.sortedBy { it.occurredAt }
                    for (i in 0 until sortedTx.size - 1) {
                        val tx1 = sortedTx[i]
                        val tx2 = sortedTx[i + 1]
                        val daysBetween = (tx2.occurredAt - tx1.occurredAt).inWholeDays
                        if (daysBetween in 27..33) {
                            val possibleName = cleanSubscriptionName(tx2.note.ifBlank { tx2.category.localizedName(context) })
                            
                            val alreadyRegistered = subscriptions.any { sub ->
                                sub.isActive && (
                                    sub.name.contains(possibleName, ignoreCase = true) || 
                                    possibleName.contains(sub.name, ignoreCase = true)
                                )
                            }
                            
                            if (!alreadyRegistered) {
                                val nextDueDateEpoch = tx2.occurredAt.plus(DatePeriod(months = 1), zone)
                                detectedSubscriptions.add(
                                    DetectedSubscription(
                                        name = possibleName,
                                        amount = tx2.amount,
                                        category = tx2.category,
                                        repeatMonths = 1,
                                        possibleNextDueDate = nextDueDateEpoch.toEpochMilliseconds()
                                    )
                                )
                                break
                            }
                        }
                    }
                }
            }
        }

        // 10. Rule Engine Lời khuyên tài chính thông minh
        val aiAdvices = if (isViewingCurrentMonth) {
            StatsInsightsUiMapper.mapAdviceSignals(
                StatsInsightsCalculator.computeAdvices(
                    StatsAdviceInput(
                        categoryShares = breakdown.map {
                            CategoryExpenseShare(
                                categoryId = it.category.id,
                                amountInCents = it.amount.amountInCents,
                                share = it.percentage,
                            )
                        },
                        expenseInCents = expense.amountInCents,
                        incomeInCents = income.amountInCents,
                        subscriptions = subscriptions,
                        currentMonthExpenseInCents = walletExpenseInCurrentMonth.amountInCents,
                        spentTodayInCents = spentToday.amountInCents,
                        currentDay = currentDay,
                        daysInMonth = daysInMonth,
                        remainingDays = remainingDays,
                        limitInCents = limit?.amountInCents,
                        previousMonthDailyAverageInCents = null,
                        feedbacks = feedbacks,
                        now = Clock.System.now(),
                    ),
                ),
            )
        } else {
            emptyList()
        }

        val advisorInput = prediction?.let { value ->
            BudgetAdvisorInput(
                prediction = value,
                budgetLimitInCents = limit?.amountInCents,
                incomeThisMonthInCents = income.amountInCents,
                categories = breakdown.map {
                    AdvisorCategorySummary(
                        name = it.category.localizedName(context),
                        amountInCents = it.amount.amountInCents,
                        share = it.percentage.toDouble(),
                    )
                },
            )
        }
        if (latestAdvisorInput != advisorInput) {
            latestAdvisorInput = advisorInput
            _localAdvisor.update { current ->
                LocalAdvisorUiState(
                    availability = current.availability,
                )
            }
        }

        // Keep the trend chart deterministic and tied to the same wallet filter as this screen.
        val anchor = period.start ?: now.date
        val anchorYear = anchor.year
        val anchorMonth = anchor.month.number
        val recentMonths = (2 downTo 0).map { offset ->
            val absoluteMonth = anchorYear * 12 + (anchorMonth - 1) - offset
            val trendYear = absoluteMonth / 12
            val trendMonth = absoluteMonth % 12 + 1
            val monthTransactions = allTransactions.asSequence().filter { transaction ->
                val localDateTime = transaction.occurredAt.toLocalDateTime(zone)
                (selectedWalletId == null || transaction.walletId == selectedWalletId) &&
                    localDateTime.year == trendYear && localDateTime.month.number == trendMonth
            }
            val trendExpense = monthTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .fold(Money.ZERO) { total, transaction -> total + transaction.amount }
            val trendIncome = allTransactions.asSequence()
                .filter { transaction ->
                    val localDateTime = transaction.occurredAt.toLocalDateTime(zone)
                    (selectedWalletId == null || transaction.walletId == selectedWalletId) &&
                        localDateTime.year == trendYear &&
                        localDateTime.month.number == trendMonth &&
                        transaction.type == TransactionType.INCOME
                }
                .fold(Money.ZERO) { total, transaction -> total + transaction.amount }
            MonthlyTrendPoint(trendYear, trendMonth, trendExpense, trendIncome)
        }

        StatsUiState(
            totalIncome = income,
            totalExpense = expense,
            balance = income - expense,
            previousExpense = previousExpense,
            previousIncome = previousIncome,
            currentPeriod = period,
            breakdown = breakdown,
            incomeBreakdown = incomeBreakdown,
            recentMonths = recentMonths,
            isLoading = false,
            isCurrentMonth = period.range == com.notepay.domain.analytics.StatsRange.MONTH && period.contains(now.date),
            isLatestPeriod = period.contains(now.date),
            selectedCategory = null,
            transactions = filteredTxs,
            hasAnyTransactions = allTransactions.isNotEmpty(),
            wallets = wallets,
            selectedWallet = selectedWallet,
            dateRangeLabel = dateRangeLabel,
            budgetLimit = limit,
            budgetSpent = walletExpenseInCurrentMonth,
            budgetPercentage = budgetPercentage,
            spendingForecast = forecast,
            dynamicDailyBudget = dynamicDailyBudget,
            aiAdvices = aiAdvices,
            detectedSubscriptions = detectedSubscriptions
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(),
    )

    val state: StateFlow<StatsUiState> = combine(
        baseState,
        _localAdvisor,
        _selectedCategory
    ) { base, advisor, selectedCategory ->
        base.copy(
            localAdvisor = advisor,
            selectedCategory = selectedCategory
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = baseState.value.copy(
            localAdvisor = _localAdvisor.value,
            selectedCategory = _selectedCategory.value
        ),
    )

    fun generateLocalAdvice() {
        val input = latestAdvisorInput ?: return
        if (_localAdvisor.value.status == LocalAdvisorStatus.RUNNING) return
        _localAdvisor.value = _localAdvisor.value.copy(
            status = LocalAdvisorStatus.RUNNING,
            result = null,
        )
        viewModelScope.launch {
            try {
                val result = budgetAdvisor.generate(input)
                if (latestAdvisorInput == input) {
                    _localAdvisor.value = LocalAdvisorUiState(
                        status = LocalAdvisorStatus.READY,
                        result = result,
                        availability = when (result.provider) {
                            com.notepay.domain.analytics.AdvisorProvider.GEMINI_NANO ->
                                AdvisorAvailability.GEMINI_NANO
                            com.notepay.domain.analytics.AdvisorProvider.CLOUD_GEMINI ->
                                AdvisorAvailability.CLOUD_GEMINI
                            com.notepay.domain.analytics.AdvisorProvider.STATISTICAL_FALLBACK ->
                                _localAdvisor.value.availability
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                resetLocalAdviceAfterFailure(input)
                throw cancelled
            } catch (_: Throwable) {
                resetLocalAdviceAfterFailure(input)
            }
        }
    }

    private fun resetLocalAdviceAfterFailure(input: BudgetAdvisorInput) {
        if (latestAdvisorInput == input) {
            _localAdvisor.update { current ->
                current.copy(
                    status = LocalAdvisorStatus.NOT_REQUESTED,
                    result = null,
                )
            }
        }
    }

    suspend fun refreshAdvisorAvailability() {
        val availability = budgetAdvisor.availability()
        _localAdvisor.update { current ->
            current.copy(
                availability = availability,
            )
        }
    }

    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
    }
    fun selectWallet(walletId: Long?) {
        _selectedWalletId.value = walletId
    }

    fun previousPeriod() {
        _currentPeriod.update { current ->
            current.shift(-1) ?: current
        }
        _selectedCategory.value = null
    }

    fun nextPeriod() {
        val current = _currentPeriod.value
        val next = current.shift(1) ?: return
        val today = now.date
        if (next.start != null && next.start > today) return
        _currentPeriod.value = next
        _selectedCategory.value = null
    }

    /** Select a historical trend bar without navigating away from the statistics screen. */
    fun selectMonth(year: Int, month: Int) {
        val candidate = MonthYear(year, month)
        val latest = MonthYear(now.year, now.month.number)
        if (candidate.year > latest.year || (candidate.year == latest.year && candidate.month > latest.month)) return
        _currentPeriod.value = periodFor(LocalDate(year, month, 1), StatsRange.MONTH)
        _selectedCategory.value = null
    }

    fun selectRange(range: StatsRange) {
        if (range == StatsRange.CUSTOM) return
        _currentPeriod.value = periodFor(now.date, range)
        _selectedCategory.value = null
    }

    fun selectCustomPeriod(start: LocalDate, endInclusive: LocalDate) {
        _currentPeriod.value = customPeriod(start, endInclusive)
        _selectedCategory.value = null
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    private fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val zone = TimeZone.currentSystemDefault()
        val firstDate = LocalDate(year, month, 1)
        val nextMonth = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
        val first = LocalDateTime(firstDate.year, firstDate.month.number, firstDate.day, 0, 0)
            .toInstant(zone)
        val lastExclusive = LocalDateTime(nextMonth.year, nextMonth.month.number, nextMonth.day, 0, 0)
            .toInstant(zone)
        return first.toEpochMilliseconds() to (lastExclusive.toEpochMilliseconds() - 1)
    }

    private fun formatEpochMillis(millis: Long): String {
        return PresentationDateFormatter.formatDate(Instant.fromEpochMilliseconds(millis))
    }

    private fun getDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }

    private fun cleanSubscriptionName(note: String): String {
        val lower = note.lowercase()
        return when {
            lower.contains("netflix") -> "Netflix"
            lower.contains("spotify") -> "Spotify"
            lower.contains("youtube") -> "YouTube Premium"
            lower.contains("icloud") -> "iCloud"
            lower.contains("google") -> "Google One"
            lower.contains("canva") -> "Canva"
            lower.contains("microsoft") || lower.contains("office365") -> "Microsoft 365"
            else -> note.trim().replaceFirstChar { it.uppercase() }
        }
    }

    private val _addSubForm = MutableStateFlow(StatsAddSubscriptionFormState())
    val addSubForm: StateFlow<StatsAddSubscriptionFormState> = _addSubForm.asStateFlow()

    fun showAddSubscription(name: String, amountCents: Long, categoryId: String, nextDueMs: Long) {
        val amountStr = (amountCents / 100).toString()
        _addSubForm.value = StatsAddSubscriptionFormState(
            name = name,
            amountInput = amountStr,
            category = categoryId,
            nextDueEpochMs = nextDueMs,
            isVisible = true
        )
    }

    fun updateSubFormName(name: String) {
        _addSubForm.update { it.copy(name = name) }
    }

    fun updateSubFormAmount(amountInput: String) {
        _addSubForm.update { it.copy(amountInput = amountInput) }
    }

    fun updateSubFormRepeatMonths(months: Int) {
        _addSubForm.update { it.copy(repeatMonths = months) }
    }

    fun updateSubFormRemindDays(days: Int) {
        _addSubForm.update { it.copy(remindDaysBefore = days) }
    }

    fun updateSubFormNote(note: String) {
        _addSubForm.update { it.copy(note = note) }
    }

    fun updateSubFormCategory(categoryId: String) {
        _addSubForm.update { it.copy(category = categoryId) }
    }

    fun updateSubFormNextDueDate(dateMs: Long) {
        _addSubForm.update { it.copy(nextDueEpochMs = dateMs) }
    }

    fun dismissSubForm() {
        _addSubForm.value = StatsAddSubscriptionFormState()
    }

    fun saveSubscription() {
        val form = _addSubForm.value
        if (!form.canSave) return
        
        viewModelScope.launch {
            val cleanAmountInput = form.amountInput.replace(Regex("[^0-9]"), "")
            val cents = (cleanAmountInput.toLongOrNull() ?: 0L) * 100
            val newSub = com.notepay.domain.model.Subscription(
                id = 0L,
                name = form.name,
                amount = Money(cents),
                category = form.category,
                nextDueDate = Instant.fromEpochMilliseconds(form.nextDueEpochMs),
                repeatMonths = form.repeatMonths,
                remindDaysBefore = form.remindDaysBefore,
                note = form.note,
                isActive = true
            )
            subscriptionRepo.upsert(newSub)
            dismissSubForm()
        }
    }

    private data class MonthYear(val year: Int, val month: Int)
}

data class StatsAddSubscriptionFormState(
    val name: String = "",
    val amountInput: String = "",
    val repeatMonths: Int = 1,
    val remindDaysBefore: Int = 3,
    val note: String = "",
    val category: String = "subscription",
    val nextDueEpochMs: Long = Clock.System.now().toEpochMilliseconds(),
    val isVisible: Boolean = false
) {
    val canSave: Boolean get() = name.isNotBlank() && amountInput.isNotEmpty()
}
