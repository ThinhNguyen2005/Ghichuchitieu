package com.notepay.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.GetMonthlySummaryUseCase
import com.notepay.domain.usecase.ObserveWalletBalanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val walletRepo: WalletRepository,
    private val getMonthlySummary: GetMonthlySummaryUseCase,
    private val observeWalletBalance: ObserveWalletBalanceUseCase,
) : ViewModel() {

    private val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    private val currentYear = today.year
    private val currentMonth = today.monthNumber

    private val _selectedMonth = MutableStateFlow(currentYear to currentMonth)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state = combine(
        _selectedMonth.flatMapLatest { (year, month) -> getMonthlySummary(year, month) },
        walletRepo.observeActive(),
        walletRepo.observeAll()
    ) { summary, activeWallet, wallets ->
        val balance = activeWallet?.let { observeWalletBalance(it.id).first() }
        HomeUiState(
            activeWallet = activeWallet,
            wallets = wallets,
            currentBalance = balance ?: com.notepay.domain.model.Money.ZERO,
            monthlyIncome = summary.totalIncome,
            monthlyExpense = summary.totalExpense,
            recentTransactions = summary.transactions.take(5),
            monthLabel = "Tháng ${summary.month}/${summary.year}",
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun selectWallet(walletId: Long) {
        viewModelScope.launch {
            walletRepo.setActive(walletId)
        }
    }
}
