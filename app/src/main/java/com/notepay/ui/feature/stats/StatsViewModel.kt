package com.notepay.ui.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.usecase.GetMonthlySummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getMonthlySummary: GetMonthlySummaryUseCase,
) : ViewModel() {

    private val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    private val currentMonthYear = MutableStateFlow(MonthYear(now.year, now.monthNumber))

    val state: StateFlow<StatsUiState> = currentMonthYear
        .flatMapLatest { date ->
            getMonthlySummary(date.year, date.month).map { summary ->
                val totalExpenseCents = summary.totalExpense.amountInCents
                val breakdown = summary.byCategory.map { (cat, amount) ->
                    val pct = if (totalExpenseCents > 0) {
                        amount.amountInCents.toFloat() / totalExpenseCents
                    } else {
                        0f
                    }
                    CategoryBreakdownItem(cat, amount, pct)
                }.sortedByDescending { it.amount.amountInCents }

                val totalIncomeCents = summary.totalIncome.amountInCents
                val incomeBreakdown = summary.byIncomeCategory.map { (cat, amount) ->
                    val pct = if (totalIncomeCents > 0) {
                        amount.amountInCents.toFloat() / totalIncomeCents
                    } else {
                        0f
                    }
                    CategoryBreakdownItem(cat, amount, pct)
                }.sortedByDescending { it.amount.amountInCents }

                StatsUiState(
                    year = summary.year,
                    month = summary.month,
                    totalIncome = summary.totalIncome,
                    totalExpense = summary.totalExpense,
                    balance = summary.balance,
                    breakdown = breakdown,
                    incomeBreakdown = incomeBreakdown,
                    isLoading = false,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState(year = now.year, month = now.monthNumber),
        )

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

    private data class MonthYear(val year: Int, val month: Int)
}
