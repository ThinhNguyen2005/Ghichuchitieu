package com.notepay.ui.feature.stats

import com.notepay.domain.model.Category
import com.notepay.domain.model.Money

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
    // P2-15: true khi (year, month) trùng tháng hiện tại → disable nút "Tháng sau".
    val isCurrentMonth: Boolean = false,
)
