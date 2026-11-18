package com.notepay.feature.stats.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.ai.OnDeviceBudgetAdvisor
import com.notepay.core.designsystem.formatter.PresentationDateFormatter
import com.notepay.core.designsystem.util.localizedName
import com.notepay.data.datastore.AdvisorFeedbackStore
import com.notepay.domain.analytics.AdvisorAvailability
import com.notepay.domain.analytics.AdvisorCategorySummary
import com.notepay.domain.analytics.AdvisorProvider
import com.notepay.domain.analytics.BudgetAdvisorInput
import com.notepay.domain.analytics.CategoryExpenseShare
import com.notepay.domain.analytics.SpendingForecastEngine
import com.notepay.domain.analytics.SpendingPrediction
import com.notepay.domain.analytics.StatsAdviceInput
import com.notepay.domain.analytics.StatsInsightsCalculator
import com.notepay.domain.analytics.StatsPeriod
import com.notepay.domain.analytics.StatsRange
import com.notepay.domain.analytics.StatsSnapshotCalculator
import com.notepay.domain.analytics.StatsSnapshotInput
import com.notepay.domain.analytics.StatsSummaryCalculator
import com.notepay.domain.analytics.customPeriod
import com.notepay.domain.analytics.periodFor
import com.notepay.domain.analytics.shift
import com.notepay.domain.model.Category
import com.notepay.domain.model.Subscription
import com.notepay.domain.money.Money
import com.notepay.domain.repository.SubscriptionRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.DetectSubscriptionUseCase
import com.notepay.feature.stats.main.component.StatsInsightsUiMapper
import com.notepay.feature.transaction.form.calculator.AmountParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Trạng thái màn hình Thống kê.
 *
 * Toàn bộ số liệu tiền tệ được tính bởi [StatsSnapshotCalculator] và các calculator trong
 * `domain/analytics`; ViewModel chỉ ánh xạ chúng sang [StatsUiState] và điều phối sự kiện.
 * `Context` chỉ dùng để tra chuỗi đa ngôn ngữ (tên danh mục cho trợ lý AI), không dùng cho
 * phép tính hay lưu trữ.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val walletRepo: WalletRepository,
    private val subscriptionRepo: SubscriptionRepository,
    private val advisorFeedbackStore: AdvisorFeedbackStore,
    @param:ApplicationContext private val context: Context,
    private val budgetAdvisor: OnDeviceBudgetAdvisor,
) : ViewModel() {

    private val zone = TimeZone.currentSystemDefault()
    private val today: LocalDate = Clock.System.now().toLocalDateTime(zone).date

    private val _currentPeriod = MutableStateFlow(
        periodFor(LocalDate(today.year, today.month.number, 1), StatsRange.MONTH)
    )
    val currentPeriod: StateFlow<StatsPeriod> = _currentPeriod.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _selectedWalletId = MutableStateFlow<Long?>(null)
    val selectedWalletId: StateFlow<Long?> = _selectedWalletId.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _trendWindow = MutableStateFlow(TrendWindow.SIX_MONTHS)

    private data class FilterState(
        val period: StatsPeriod,
        val selectedWalletId: Long?,
        val trendWindow: TrendWindow,
    )

    private val filterState = combine(
        _currentPeriod,
        _selectedWalletId,
        _trendWindow,
    ) { period, walletId, window ->
        FilterState(period, walletId, window)
    }.distinctUntilChanged()

    /**
     * Đầu vào đã tính sẵn của trợ lý AI. Tên danh mục chỉ được tra cứu vào lúc người dùng bấm
     * "Gợi ý", nhờ vậy pipeline tính toán không phụ thuộc ngôn ngữ đang hiển thị.
     */
    private data class AdvisorRequest(
        val prediction: SpendingPrediction,
        val budgetLimitInCents: Long?,
        val incomeInCents: Long,
        val categoryShares: List<CategoryShare>,
    ) {
        data class CategoryShare(
            val category: Category,
            val amountInCents: Long,
            val share: Float,
        )
    }

    private val _localAdvisor = MutableStateFlow(LocalAdvisorUiState())
    @Volatile private var latestAdvisorRequest: AdvisorRequest? = null

    init {
        viewModelScope.launch {
            refreshAdvisorAvailability()
        }
    }

    fun sendAdviceFeedback(adviceId: String, score: Int) {
        viewModelScope.launch {
            advisorFeedbackStore.setFeedback(adviceId, score)
        }
    }

    private val baseState: StateFlow<StatsUiState> = combine(
        transactionRepo.observeAll(),
        walletRepo.observeAll(),
        subscriptionRepo.observeAll(),
        advisorFeedbackStore.feedbacks,
        filterState,
    ) { allTransactions, wallets, subscriptions, feedbacks, filters ->
        val period = filters.period

        // 1. Số liệu gốc: lọc theo kỳ + ví, so sánh kỳ trước, hạn mức, chi tiêu hôm nay.
        val snapshot = StatsSnapshotCalculator.snapshot(
            StatsSnapshotInput(
                transactions = allTransactions,
                wallets = wallets,
                period = period,
                selectedWalletId = filters.selectedWalletId,
                trendWindowMonths = filters.trendWindow.months,
                today = today,
                zone = zone,
            ),
        )
        val limitInCents = snapshot.budgetLimitInCents

        // 2. Tổng thu/chi và cơ cấu danh mục của kỳ đang xem.
        val summary = StatsSummaryCalculator.summarize(snapshot.filteredTransactions)
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

        // 3. Dự báo chi tiêu cuối tháng.
        val prediction = if (snapshot.isViewingCurrentMonth) {
            SpendingForecastEngine.forecast(
                expenses = snapshot.dailyExpenses,
                today = today,
                daysInMonth = snapshot.daysInMonth,
                budgetLimitInCents = limitInCents,
            )
        } else {
            null
        }
        val forecast = prediction?.let { value ->
            StatsInsightsUiMapper.mapPrediction(
                prediction = value,
                limitInCents = limitInCents,
                overBudgetProbabilityPercent = value.overBudgetProbability
                    ?.let { (it * 100).toInt() },
            )
        }

        // 4. Hạn mức chi tiêu linh hoạt cho riêng hôm nay.
        val dynamicDailyBudget = if (limitInCents != null && limitInCents > 0L && snapshot.isViewingCurrentMonth) {
            StatsInsightsUiMapper.mapDynamicDailyBudget(
                StatsInsightsCalculator.computeDynamicDailyBudget(
                    limitInCents = limitInCents,
                    spentExceptTodayInCents =
                        (snapshot.currentMonthExpenseInCents - snapshot.spentTodayInCents).coerceAtLeast(0L),
                    spentTodayInCents = snapshot.spentTodayInCents,
                    remainingDays = snapshot.remainingDays,
                    daysInMonth = snapshot.daysInMonth,
                    currentDay = snapshot.currentDay,
                    currentMonthExpenseInCents = snapshot.currentMonthExpenseInCents,
                ),
            )
        } else {
            null
        }

        // 5. Phát hiện khoản chi định kỳ chưa được đăng ký.
        val detectedSubscriptions = if (snapshot.isViewingCurrentMonth) {
            DetectSubscriptionUseCase(
                transactions = allTransactions,
                activeSubscriptionNames = subscriptions.filter { it.isActive }.map { it.name },
                displayNameOf = { category -> category.localizedName(context) },
                zone = zone,
            ).map { candidate ->
                DetectedSubscription(
                    name = candidate.name,
                    amount = Money(candidate.amountInCents),
                    category = candidate.category,
                    repeatMonths = candidate.repeatMonths,
                    possibleNextDueDate = candidate.nextDueEpochMs,
                )
            }
        } else {
            emptyList()
        }

        // 6. Bộ luật gợi ý tài chính.
        val aiAdvices = if (snapshot.isViewingCurrentMonth) {
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
                        currentMonthExpenseInCents = snapshot.currentMonthExpenseInCents,
                        spentTodayInCents = snapshot.spentTodayInCents,
                        currentDay = snapshot.currentDay,
                        daysInMonth = snapshot.daysInMonth,
                        remainingDays = snapshot.remainingDays,
                        limitInCents = limitInCents,
                        previousMonthDailyAverageInCents = null,
                        feedbacks = feedbacks,
                        now = Clock.System.now(),
                    ),
                ),
            )
        } else {
            emptyList()
        }

        // 7. Ghi lại đầu vào trợ lý AI; đổi dữ liệu thì kết quả cũ không còn giá trị.
        updateAdvisorRequest(
            prediction?.let { value ->
                AdvisorRequest(
                    prediction = value,
                    budgetLimitInCents = limitInCents,
                    incomeInCents = income.amountInCents,
                    categoryShares = breakdown.map {
                        AdvisorRequest.CategoryShare(
                            category = it.category,
                            amountInCents = it.amount.amountInCents,
                            share = it.percentage,
                        )
                    },
                )
            },
        )

        // 8. Biểu đồ xu hướng dùng cùng bộ lọc ví với phần còn lại của màn hình.
        val recentMonths = snapshot.trendMonths.map { month ->
            MonthlyTrendPoint(
                year = month.year,
                month = month.month,
                expense = Money(month.expenseInCents),
                income = Money(month.incomeInCents),
            )
        }

        StatsUiState(
            totalIncome = income,
            totalExpense = expense,
            balance = income - expense,
            previousExpense = snapshot.previousExpenseInCents?.let(::Money),
            previousIncome = snapshot.previousIncomeInCents?.let(::Money),
            currentPeriod = period,
            breakdown = breakdown,
            incomeBreakdown = incomeBreakdown,
            recentMonths = recentMonths,
            trendWindow = filters.trendWindow,
            isLoading = false,
            isCurrentMonth = snapshot.isViewingCurrentMonth,
            isLatestPeriod = period.contains(today),
            selectedCategory = null,
            transactions = snapshot.filteredTransactions,
            hasAnyTransactions = allTransactions.isNotEmpty(),
            wallets = wallets,
            selectedWallet = wallets.find { it.id == filters.selectedWalletId },
            dateRangeLabel = rangeLabel(period),
            budgetLimit = limitInCents?.let(::Money),
            budgetSpent = Money(snapshot.currentMonthExpenseInCents),
            budgetPercentage = snapshot.budgetUsedRatio,
            spendingForecast = forecast,
            dynamicDailyBudget = dynamicDailyBudget,
            aiAdvices = aiAdvices,
            detectedSubscriptions = detectedSubscriptions,
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
        val request = latestAdvisorRequest ?: return
        if (_localAdvisor.value.status == LocalAdvisorStatus.RUNNING) return
        _localAdvisor.value = _localAdvisor.value.copy(
            status = LocalAdvisorStatus.RUNNING,
            result = null,
        )
        viewModelScope.launch {
            try {
                val result = budgetAdvisor.generate(request.toAdvisorInput())
                if (latestAdvisorRequest == request) {
                    _localAdvisor.value = LocalAdvisorUiState(
                        status = LocalAdvisorStatus.READY,
                        result = result,
                        availability = when (result.provider) {
                            AdvisorProvider.GEMINI_NANO ->
                                AdvisorAvailability.GEMINI_NANO
                            AdvisorProvider.CLOUD_GEMINI ->
                                AdvisorAvailability.CLOUD_GEMINI
                            AdvisorProvider.STATISTICAL_FALLBACK ->
                                _localAdvisor.value.availability
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                resetLocalAdviceAfterFailure(request)
                throw cancelled
            } catch (_: Throwable) {
                resetLocalAdviceAfterFailure(request)
            }
        }
    }

    private fun resetLocalAdviceAfterFailure(request: AdvisorRequest) {
        if (latestAdvisorRequest == request) {
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

    private fun updateAdvisorRequest(request: AdvisorRequest?) {
        if (latestAdvisorRequest == request) return
        latestAdvisorRequest = request
        // Dữ liệu đã đổi: bỏ kết quả AI cũ nhưng giữ trạng thái khả dụng của model trên máy.
        _localAdvisor.update { current -> LocalAdvisorUiState(availability = current.availability) }
    }

    private fun AdvisorRequest.toAdvisorInput(): BudgetAdvisorInput = BudgetAdvisorInput(
        prediction = prediction,
        budgetLimitInCents = budgetLimitInCents,
        incomeThisMonthInCents = incomeInCents,
        categories = categoryShares.map { share ->
            AdvisorCategorySummary(
                name = share.category.localizedName(context),
                amountInCents = share.amountInCents,
                share = share.share.toDouble(),
            )
        },
    )

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
        if (next.start != null && next.start > today) return
        _currentPeriod.value = next
        _selectedCategory.value = null
    }

    /** Select a historical trend bar without navigating away from the statistics screen. */
    fun selectMonth(year: Int, month: Int) {
        val candidate = MonthYear(year, month)
        val latest = MonthYear(today.year, today.month.number)
        if (candidate.year > latest.year || (candidate.year == latest.year && candidate.month > latest.month)) return
        _currentPeriod.value = periodFor(LocalDate(year, month, 1), StatsRange.MONTH)
        _selectedCategory.value = null
    }

    fun selectRange(range: StatsRange) {
        if (range == StatsRange.CUSTOM) return
        _currentPeriod.value = periodFor(today, range)
        _selectedCategory.value = null
    }

    /** Đổi số tháng hiển thị trên biểu đồ xu hướng (3T / 6T / 12T). */
    fun selectTrendWindow(window: TrendWindow) {
        _trendWindow.value = window
    }

    fun selectCustomPeriod(start: LocalDate, endInclusive: LocalDate) {
        _currentPeriod.value = customPeriod(start, endInclusive)
        _selectedCategory.value = null
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    /**
     * Nhãn kỳ đang xem dưới dạng tài nguyên, không phải chuỗi đã dịch: đổi ngôn ngữ lúc app đang
     * chạy sẽ cập nhật ngay mà không cần chờ một lần phát state mới.
     */
    private fun rangeLabel(period: StatsPeriod): StatsUiText {
        val anchor = period.start ?: today
        val endInclusive = period.endExclusive?.minus(DatePeriod(days = 1)) ?: today
        return when (period.range) {
            StatsRange.MONTH -> StatsUiText.Resource(
                R.string.stats_date_month_format,
                listOf(anchor.month.number, anchor.year),
            )
            StatsRange.WEEK -> StatsUiText.Resource(
                R.string.stats_date_week_format,
                listOf(anchor.day, anchor.month.number, endInclusive.day, endInclusive.month.number),
            )
            StatsRange.YEAR -> StatsUiText.Resource(
                R.string.stats_date_year_format,
                listOf(anchor.year),
            )
            StatsRange.ALL -> StatsUiText.Resource(R.string.stats_date_all_time)
            StatsRange.CUSTOM -> StatsUiText.Resource(
                R.string.stats_date_range_format,
                listOf(formatDate(anchor), formatDate(endInclusive)),
            )
        }
    }

    private fun formatDate(date: LocalDate): String {
        val instant = LocalDateTime(date.year, date.month.number, date.day, 0, 0).toInstant(zone)
        return PresentationDateFormatter.formatDate(instant, zone)
    }

    private val _addSubForm = MutableStateFlow(StatsAddSubscriptionFormState())
    val addSubForm: StateFlow<StatsAddSubscriptionFormState> = _addSubForm.asStateFlow()

    /** Nhận thẳng model gợi ý: tránh truyền nhầm thứ tự 4 tham số rời (hai tham số Long cạnh nhau). */
    fun showAddSubscription(subscription: DetectedSubscription) {
        _addSubForm.value = StatsAddSubscriptionFormState(
            name = subscription.name,
            amountInput = (subscription.amount.amountInCents / 100).toString(),
            category = subscription.category.id,
            nextDueEpochMs = subscription.possibleNextDueDate,
            isVisible = true,
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
        // Dùng chung bộ phân tích số tiền với form Giao dịch: sai định dạng thì không lưu,
        // tránh tạo Subscription với amount <= 0 (ràng buộc require trong domain model).
        val amount = AmountParser.parse(form.amountInput).amount ?: return

        viewModelScope.launch {
            subscriptionRepo.upsert(
                Subscription(
                    name = form.name.trim(),
                    amount = amount,
                    category = form.category,
                    nextDueDate = Instant.fromEpochMilliseconds(form.nextDueEpochMs),
                    repeatMonths = form.repeatMonths,
                    remindDaysBefore = form.remindDaysBefore,
                    note = form.note,
                ),
            )
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
    val isValidAmount: Boolean get() = AmountParser.parse(amountInput).amount != null
    val canSave: Boolean get() = name.isNotBlank() && isValidAmount
}
