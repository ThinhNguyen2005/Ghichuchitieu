package com.notepay.ui.feature.stats

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.repository.SubscriptionRepository
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.ai.LocalAiModelManager
import com.notepay.ai.OnDeviceBudgetAdvisor
import com.notepay.domain.analytics.AdvisorAvailability
import com.notepay.domain.analytics.AdvisorCategorySummary
import com.notepay.domain.analytics.BudgetAdvisorInput
import com.notepay.domain.analytics.DailyExpense
import com.notepay.domain.analytics.SpendingForecastEngine
import com.notepay.ui.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
import kotlin.math.max

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val walletRepo: WalletRepository,
    private val subscriptionRepo: SubscriptionRepository,
    @ApplicationContext private val context: Context,
    private val budgetAdvisor: OnDeviceBudgetAdvisor,
    private val localModelManager: LocalAiModelManager,
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

    private val advicePrefs = context.getSharedPreferences("notepay_ai_feedback", Context.MODE_PRIVATE)
    private val _adviceFeedbacks = MutableStateFlow<Map<String, Int>>(
        advicePrefs.all.mapValues { it.value as? Int ?: 0 }
    )
    private val _localAdvisor = MutableStateFlow(LocalAdvisorUiState())
    @Volatile private var latestAdvisorInput: BudgetAdvisorInput? = null

    init {
        viewModelScope.launch {
            localModelManager.state.collect { modelState ->
                _localAdvisor.update { current -> current.copy(localModel = modelState) }
            }
        }
        viewModelScope.launch {
            refreshAdvisorAvailability()
        }
    }

    fun sendAdviceFeedback(adviceId: String, score: Int) {
        advicePrefs.edit().putInt(adviceId, score).apply()
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
                daysInMonth = getDaysInMonth(now.year, now.monthNumber),
                budgetLimitInCents = limit?.amountInCents,
            )
        } else null

        val forecast = prediction?.let { value ->
            val dailyAvgStr = MoneyFormatter.format(Money(value.dailyRunRateInCents))
            val projectedSpendStr = MoneyFormatter.format(Money(value.predictedMonthTotalInCents))
            val probabilityText = value.overBudgetProbability?.let {
                " Khả năng vượt định mức khoảng ${(it * 100).toInt()}%."
            }.orEmpty()
            BudgetForecast(
                dailyAverage = Money(value.dailyRunRateInCents),
                projectedSpend = Money(value.predictedMonthTotalInCents),
                forecastMessage = "Nhịp chi gần đây $dailyAvgStr/ngày; dự báo cuối tháng khoảng $projectedSpendStr.$probabilityText",
                isProjectedToExceed = limit != null && value.predictedMonthTotalInCents > limit.amountInCents,
                trendPercent = value.trendVsPreviousMonth?.times(100)?.toFloat(),
                prediction = value,
            )
        }

        // 8. Tính toán Dynamic Daily Budget
        val daysInMonth = getDaysInMonth(now.year, now.monthNumber)
        val currentDay = now.dayOfMonth.coerceIn(1, daysInMonth)

        val todayStart = LocalDateTime(now.year, now.monthNumber, currentDay, 0, 0)
            .toInstant(zone).toEpochMilliseconds()
        val todayEnd = LocalDateTime(now.year, now.monthNumber, currentDay, 23, 59, 59, 999000000)
            .toInstant(zone).toEpochMilliseconds()

        val spentToday = currentMonthTxs.filter {
            it.type == TransactionType.EXPENSE &&
            (selectedWalletId == null || it.walletId == selectedWalletId) &&
            it.occurredAt.toEpochMilliseconds() in todayStart..todayEnd
        }.fold(Money.ZERO) { acc, t -> acc + t.amount }

        val remainingDays = daysInMonth - currentDay + 1
        val spentExceptToday = Money(max(0L, walletExpenseInCurrentMonth.amountInCents - spentToday.amountInCents))
        
        val dynamicDailyBudget = if (limit != null && limit.amountInCents > 0 && isViewingCurrentMonth) {
            val remainingBudget = max(0L, limit.amountInCents - spentExceptToday.amountInCents)
            val dailyBudgetVal = remainingBudget / remainingDays
            val remainingToday = max(0L, dailyBudgetVal - spentToday.amountInCents)
            
            val tomorrowBudget = if (remainingDays > 1) {
                val remainingForTomorrow = max(0L, limit.amountInCents - walletExpenseInCurrentMonth.amountInCents)
                remainingForTomorrow / (remainingDays - 1)
            } else {
                0L
            }
            
            DynamicDailyBudgetData(
                dailyBudget = Money(dailyBudgetVal),
                spentToday = spentToday,
                remainingToday = Money(remainingToday),
                tomorrowBudget = Money(tomorrowBudget),
                isExceeded = spentToday.amountInCents > dailyBudgetVal
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
                            val possibleName = cleanSubscriptionName(tx2.note.ifBlank { tx2.category.displayName })
                            
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
        val aiAdvices = mutableListOf<AiAdviceItem>()
        if (isViewingCurrentMonth) {
            // Quy tắc A: FOOD > 35%
            val foodBreakdown = breakdown.find { it.category == Category.FOOD }
            if (foodBreakdown != null && foodBreakdown.percentage > 0.35f && (feedbacks["advice_food"] ?: 0) == 0) {
                val percentStr = "%.1f%%".format(foodBreakdown.percentage * 100)
                aiAdvices.add(
                    AiAdviceItem(
                        id = "advice_food",
                        type = "warning",
                        title = "Cảnh báo ăn uống",
                        content = "Chi tiêu ăn uống của bạn chiếm $percentStr tổng chi tiêu tháng này. Hãy thử tự nấu ăn tại nhà để tiết kiệm chi phí nhé!",
                        categoryId = Category.FOOD.id,
                        feedback = feedbacks["advice_food"] ?: 0
                    )
                )
            }

            // Quy tắc B: Phí sắp đến hạn & Số dư ví không đủ
            val nowInstant = Clock.System.now()
            val upcomingSubs = subscriptions.filter { sub ->
                sub.isActive && (sub.nextDueDate - nowInstant).inWholeDays in 0..3
            }
            if (upcomingSubs.isNotEmpty()) {
                val balanceVal = income.amountInCents - expense.amountInCents
                for (sub in upcomingSubs) {
                    val feedbackKey = "advice_bill_balance_${sub.id}"
                    if (balanceVal < sub.amount.amountInCents && (feedbacks[feedbackKey] ?: 0) == 0) {
                        aiAdvices.add(
                            AiAdviceItem(
                                id = feedbackKey,
                                type = "warning",
                                title = "Chuẩn bị tiền đóng phí",
                                content = "Hóa đơn '${sub.name}' (${MoneyFormatter.format(sub.amount)}) sẽ đến hạn sau vài ngày nữa. Số dư ví hiện tại không đủ, bạn hãy bổ sung tiền nhé!",
                                feedback = feedbacks[feedbackKey] ?: 0
                            )
                        )
                    }
                }
            }

            // Quy tắc C: Tiêu dùng tiết kiệm (Daily average < 85% safe daily limit ban đầu)
            if (limit != null && limit.amountInCents > 0 && remainingDays >= 5 && (feedbacks["advice_saving"] ?: 0) == 0) {
                val initialDailyBudget = limit.amountInCents / daysInMonth
                val currentDailyAverage = if (currentDay > 1) {
                    (walletExpenseInCurrentMonth.amountInCents - spentToday.amountInCents) / (currentDay - 1)
                } else {
                    spentToday.amountInCents
                }
                
                if (currentDailyAverage < initialDailyBudget * 0.85f && currentDailyAverage > 0) {
                    aiAdvices.add(
                        AiAdviceItem(
                            id = "advice_saving",
                            type = "success",
                            title = "Tiêu dùng thông minh",
                            content = "Thật tuyệt vời! Bạn đang chi tiêu rất tiết kiệm (trung bình chỉ bằng 85% hạn mức ngày an toàn). Hãy tiếp tục duy trì thói quen tốt này nhé!",
                            feedback = feedbacks["advice_saving"] ?: 0
                        )
                    )
                }
            }
        }

        val advisorInput = prediction?.let { value ->
            BudgetAdvisorInput(
                prediction = value,
                budgetLimitInCents = limit?.amountInCents,
                incomeThisMonthInCents = income.amountInCents,
                categories = breakdown.map {
                    AdvisorCategorySummary(
                        name = it.category.displayName,
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
                    localModel = current.localModel,
                )
            }
        }

        // Keep the trend chart deterministic and tied to the same wallet filter as this screen.
        val recentMonths = (2 downTo 0).map { offset ->
            val absoluteMonth = monthYear.year * 12 + (monthYear.month - 1) - offset
            val trendYear = absoluteMonth / 12
            val trendMonth = absoluteMonth % 12 + 1
            val monthTransactions = allTransactions.asSequence().filter { transaction ->
                val localDateTime = transaction.occurredAt.toLocalDateTime(zone)
                (selectedWalletId == null || transaction.walletId == selectedWalletId) &&
                    localDateTime.year == trendYear && localDateTime.monthNumber == trendMonth
            }
            val trendExpense = monthTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .fold(Money.ZERO) { total, transaction -> total + transaction.amount }
            val trendIncome = allTransactions.asSequence()
                .filter { transaction ->
                    val localDateTime = transaction.occurredAt.toLocalDateTime(zone)
                    (selectedWalletId == null || transaction.walletId == selectedWalletId) &&
                        localDateTime.year == trendYear &&
                        localDateTime.monthNumber == trendMonth &&
                        transaction.type == TransactionType.INCOME
                }
                .fold(Money.ZERO) { total, transaction -> total + transaction.amount }
            MonthlyTrendPoint(trendYear, trendMonth, trendExpense, trendIncome)
        }

        StatsUiState(
            year = monthYear.year,
            month = monthYear.month,
            totalIncome = income,
            totalExpense = expense,
            balance = income - expense,
            breakdown = breakdown,
            incomeBreakdown = incomeBreakdown,
            recentMonths = recentMonths,
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
            spendingForecast = forecast,
            dynamicDailyBudget = dynamicDailyBudget,
            aiAdvices = aiAdvices,
            detectedSubscriptions = detectedSubscriptions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(year = now.year, month = now.month.ordinal + 1),
    )

    val state: StateFlow<StatsUiState> = combine(baseState, _localAdvisor) { base, advisor ->
        base.copy(localAdvisor = advisor)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = baseState.value.copy(localAdvisor = _localAdvisor.value),
    )

    fun generateLocalAdvice() {
        val input = latestAdvisorInput ?: return
        if (_localAdvisor.value.status == LocalAdvisorStatus.RUNNING) return
        _localAdvisor.value = _localAdvisor.value.copy(
            status = LocalAdvisorStatus.RUNNING,
            result = null,
        )
        viewModelScope.launch {
            val result = budgetAdvisor.generate(input)
            if (latestAdvisorInput == input) {
                _localAdvisor.value = LocalAdvisorUiState(
                    status = LocalAdvisorStatus.READY,
                    result = result,
                    availability = when (result.provider) {
                        com.notepay.domain.analytics.AdvisorProvider.GEMINI_NANO ->
                            AdvisorAvailability.GEMINI_NANO
                        com.notepay.domain.analytics.AdvisorProvider.LOCAL_LITERT_MODEL ->
                            AdvisorAvailability.LOCAL_MODEL
                        com.notepay.domain.analytics.AdvisorProvider.STATISTICAL_FALLBACK ->
                            _localAdvisor.value.availability
                    },
                    localModel = localModelManager.state.value,
                )
            }
        }
    }

    fun importLocalModel(uri: Uri) {
        viewModelScope.launch {
            localModelManager.importModel(uri)
            refreshAdvisorAvailability()
        }
    }

    fun removeLocalModel() {
        viewModelScope.launch {
            localModelManager.removeModel()
            refreshAdvisorAvailability()
        }
    }

    private suspend fun refreshAdvisorAvailability() {
        val availability = budgetAdvisor.availability()
        _localAdvisor.update { current ->
            current.copy(
                availability = availability,
                localModel = localModelManager.state.value,
            )
        }
    }

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
        val current = currentMonthYear.value
        if (current.year == now.year && current.month == now.monthNumber) return
        currentMonthYear.update { current ->
            if (current.month == 12) {
                MonthYear(current.year + 1, 1)
            } else {
                MonthYear(current.year, current.month + 1)
            }
        }
    }

    /** Select a historical trend bar without navigating away from the statistics screen. */
    fun selectMonth(year: Int, month: Int) {
        val candidate = MonthYear(year, month)
        val latest = MonthYear(now.year, now.monthNumber)
        if (candidate.year > latest.year || (candidate.year == latest.year && candidate.month > latest.month)) return
        currentMonthYear.value = candidate
        _timeFilter.value = TimeFilterType.MONTH
        _customDateRange.value = null
        _selectedCategory.value = null
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
