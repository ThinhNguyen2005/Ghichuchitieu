package com.notepay.ui.feature.stats

import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.Wallet

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
    val isProjectedToExceed: Boolean = false
)

data class CategoryBreakdownItem(
    val category: Category,
    val amount: Money,
    val percentage: Float,
)

data class StatsUiState(
    val year: Int = 0,
    val month: Int = 0,
    val totalIncome: Money = Money.ZERO,
    val totalExpense: Money = Money.ZERO,
    val balance: Money = Money.ZERO,
    val breakdown: List<CategoryBreakdownItem> = emptyList(),
    val incomeBreakdown: List<CategoryBreakdownItem> = emptyList(),
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
)
