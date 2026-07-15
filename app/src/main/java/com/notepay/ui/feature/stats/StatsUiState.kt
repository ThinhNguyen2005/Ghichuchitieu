package com.notepay.ui.feature.stats

import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.Wallet
import com.notepay.domain.analytics.SpendingPrediction
import com.notepay.domain.analytics.BudgetAdvisorResult
import com.notepay.domain.analytics.AdvisorAvailability
import com.notepay.ai.LocalModelState

enum class TimeFilterType(val label: String) {
    MONTH("Tháng"),
    WEEK("Tuần này"),
    QUARTER("Quý này"),
    YEAR("Năm nay"),
    CUSTOM("Tự chọn")
}

data class BudgetForecast(
    val dailyAverage: Money,
    val projectedSpend: Money,
    val forecastMessage: String,
    val isProjectedToExceed: Boolean = false,

    val previousMonthDailyAverage: Money? = null,
    val trendPercent: Float? = null,
    val trendMessage: String? = null,
    val prediction: SpendingPrediction? = null,
)

enum class LocalAdvisorStatus { NOT_REQUESTED, RUNNING, READY }

data class LocalAdvisorUiState(
    val status: LocalAdvisorStatus = LocalAdvisorStatus.NOT_REQUESTED,
    val result: BudgetAdvisorResult? = null,
    val availability: AdvisorAvailability = AdvisorAvailability.CHECKING,
    val localModel: LocalModelState = LocalModelState(),
)

data class CategoryBreakdownItem(
    val category: Category,
    val amount: Money,
    val percentage: Float,
)

/** A real calendar-month total used by the three-month trend chart. */
data class MonthlyTrendPoint(
    val year: Int,
    val month: Int,
    val expense: Money,
    val income: Money,
)

data class DynamicDailyBudgetData(
    val dailyBudget: Money,
    val spentToday: Money,
    val remainingToday: Money,
    val tomorrowBudget: Money,
    val isExceeded: Boolean,
    val earlyWarning: String? = null
)

data class AiAdviceItem(
    val id: String,
    val type: String, // "warning", "info", "success"
    val title: String,
    val content: String,
    val categoryId: String? = null,
    val feedback: Int = 0 // 0: none, 1: thumb up, -1: thumb down
)

data class DetectedSubscription(
    val name: String,
    val amount: Money,
    val category: Category,
    val repeatMonths: Int = 1,
    val possibleNextDueDate: Long // epoch ms
)

data class StatsUiState(
    val year: Int = 0,
    val month: Int = 0,
    val totalIncome: Money = Money.ZERO,
    val totalExpense: Money = Money.ZERO,
    val balance: Money = Money.ZERO,
    val breakdown: List<CategoryBreakdownItem> = emptyList(),
    val incomeBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val recentMonths: List<MonthlyTrendPoint> = emptyList(),
    val isLoading: Boolean = true,
    val isCurrentMonth: Boolean = false,
    val selectedCategory: Category? = null,
    val transactions: List<Transaction> = emptyList(),
    
    // Thuộc tính mới phục vụ bộ lọc & hạn mức
    val wallets: List<Wallet> = emptyList(),
    val selectedWallet: Wallet? = null,
    val timeFilter: TimeFilterType = TimeFilterType.MONTH,
    val dateRangeLabel: String = "",
    val customStartDateMillis: Long? = null,
    val customEndDateMillis: Long? = null,
    val budgetLimit: Money? = null,
    val budgetSpent: Money = Money.ZERO,
    val budgetPercentage: Float = 0f,
    val spendingForecast: BudgetForecast? = null,

    // AI & Trợ lý thông minh
    val dynamicDailyBudget: DynamicDailyBudgetData? = null,
    val aiAdvices: List<AiAdviceItem> = emptyList(),
    val detectedSubscriptions: List<DetectedSubscription> = emptyList(),
    val localAdvisor: LocalAdvisorUiState = LocalAdvisorUiState(),
)
