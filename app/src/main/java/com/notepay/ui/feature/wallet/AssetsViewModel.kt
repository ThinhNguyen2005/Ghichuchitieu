package com.notepay.ui.feature.wallet

import com.notepay.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.model.Category
import com.notepay.domain.money.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.debt.GetDebtSummaryUseCase
import com.notepay.ui.feature.transaction.AmountParser
import com.notepay.ui.feedback.FeedbackType
import com.notepay.ui.feedback.UiFeedback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AssetsViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val getDebtSummaryUseCase: GetDebtSummaryUseCase,
) : ViewModel() {

    private val _chartRange = MutableStateFlow(AssetChartRange.MONTH)

    private val _state = MutableStateFlow(AssetsUiState())
    val state = _state.asStateFlow()

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    init {
        loadAssets()
    }

    fun onChartRangeSelected(range: AssetChartRange) {
        _chartRange.value = range
    }

    private fun loadAssets() {
        val tz = TimeZone.currentSystemDefault()
        val nowInstant = Clock.System.now()
        val now = nowInstant.toLocalDateTime(tz)
        
        val firstDate = LocalDate(now.year, now.monthNumber, 1)
        val nextMonth = if (now.monthNumber == 12) LocalDate(now.year + 1, 1, 1) else LocalDate(now.year, now.monthNumber + 1, 1)
        val monthStartMillis = LocalDateTime(firstDate.year, firstDate.monthNumber, firstDate.dayOfMonth, 0, 0).toInstant(tz).toEpochMilliseconds()
        val monthEndMillis = LocalDateTime(nextMonth.year, nextMonth.monthNumber, nextMonth.dayOfMonth, 0, 0).toInstant(tz).toEpochMilliseconds() - 1

        viewModelScope.launch {
            _chartRange.flatMapLatest { chartRange ->
                val daysAgoSteps = when (chartRange) {
                    AssetChartRange.WEEK -> (6 downTo 0).toList()
                    AssetChartRange.MONTH -> (29 downTo 0).toList()
                    AssetChartRange.HALF_YEAR -> (175 downTo 0 step 7).toList()
                    AssetChartRange.YEAR -> (350 downTo 0 step 14).toList()
                }
                
                val oldestDays = daysAgoSteps.firstOrNull() ?: 0
                val oneDayMs = 86_400_000L
                val sinceMillis = nowInstant.toEpochMilliseconds() - (oldestDays * oneDayMs)
                
                combine(
                    walletRepository.observeAll(),
                    transactionRepository.observeWalletStats(monthStartMillis, monthEndMillis),
                    transactionRepository.observeRecentNonTransfers(sinceMillis),
                    getDebtSummaryUseCase()
                ) { wallets, walletStats, nonTransferTxs, debtSummary ->
                    var totalCents = 0L

                    val items = wallets.map { wallet ->
                        val stats = walletStats.find { it.walletId == wallet.id }
                        val incomeCents = stats?.allTimeIncome?.amountInCents ?: 0L
                        val expenseCents = stats?.allTimeExpense?.amountInCents ?: 0L
                        val monthlyIncomeCents = stats?.currentMonthIncome?.amountInCents ?: 0L
                        val monthlyExpenseCents = stats?.currentMonthExpense?.amountInCents ?: 0L
                        val txCount = stats?.txCount ?: 0
                        
                        val netBalanceCents = wallet.initialBalance.amountInCents + incomeCents - expenseCents
                        totalCents += netBalanceCents

                        WalletAssetItem(
                            wallet = wallet,
                            balance = Money(netBalanceCents),
                            monthlyIncome = Money(monthlyIncomeCents),
                            monthlyExpense = Money(monthlyExpenseCents),
                            transactionCount = txCount,
                        )
                    }

                    // 1. Phân bổ tài sản (Asset Allocation)
                    val positiveWallets = items.filter { it.balance.amountInCents > 0L }
                    val positiveTotalCents = positiveWallets.sumOf { it.balance.amountInCents }
                    val allocationItems = if (positiveTotalCents > 0L) {
                        positiveWallets
                            .sortedByDescending { it.balance.amountInCents }
                            .map { item ->
                                WalletAllocationItem(
                                    walletId = item.wallet.id,
                                    name = item.wallet.name,
                                    colorKey = item.wallet.colorKey,
                                    iconKey = item.wallet.iconKey,
                                    balance = item.balance,
                                    percentage = item.balance.amountInCents.toFloat() / positiveTotalCents.toFloat()
                                )
                            }
                    } else if (items.isNotEmpty()) {
                        items.map { item ->
                            WalletAllocationItem(
                                walletId = item.wallet.id,
                                name = item.wallet.name,
                                colorKey = item.wallet.colorKey,
                                iconKey = item.wallet.iconKey,
                                balance = item.balance,
                                percentage = 1f / items.size
                            )
                        }
                    } else {
                        emptyList()
                    }

                    // 2. Xu hướng tài sản (Asset Trend Points)
                    val trendPoints = daysAgoSteps.map { daysAgo ->
                        val pointMs = nowInstant.toEpochMilliseconds() - (daysAgo * oneDayMs)
                        val txsAfterPoint = nonTransferTxs.filter { it.occurredAt.toEpochMilliseconds() > pointMs }

                        var incomeAfter = 0L
                        var expenseAfter = 0L
                        for (tx in txsAfterPoint) {
                            when (tx.type) {
                                TransactionType.INCOME -> incomeAfter += tx.amount.amountInCents
                                TransactionType.EXPENSE -> expenseAfter += tx.amount.amountInCents
                            }
                        }

                        val balanceAtPoint = totalCents - incomeAfter + expenseAfter
                        val pointInstant = kotlinx.datetime.Instant.fromEpochMilliseconds(pointMs)
                        val pointDate = pointInstant.toLocalDateTime(tz)
                        val label = String.format("%02d/%02d", pointDate.dayOfMonth, pointDate.monthNumber)

                        AssetTrendPoint(
                            timestampMs = pointMs,
                            dateLabel = label,
                            amount = Money(balanceAtPoint)
                        )
                    }

                    val firstPointAmount = trendPoints.firstOrNull()?.amount?.amountInCents ?: totalCents
                    val netWorthChangeCents = totalCents - firstPointAmount
                    val netWorthChangePercentage = if (firstPointAmount != 0L) {
                        (netWorthChangeCents.toFloat() / kotlin.math.abs(firstPointAmount).toFloat()) * 100f
                    } else 0f

                    _state.update { current ->
                        current.copy(
                            totalNetWorth = Money(totalCents),
                            wallets = items,
                            isLoading = false,
                            selectedChartRange = chartRange,
                            trendPoints = trendPoints,
                            allocationItems = allocationItems,
                            netWorthChangeInPeriod = Money(netWorthChangeCents),
                            netWorthChangePercentage = netWorthChangePercentage,
                            debtSummary = debtSummary,
                        )
                    }
                }
            }.collect {}
        }
    }

    fun setActiveWallet(walletId: Long) {
        viewModelScope.launch {
            try {
                walletRepository.setActive(walletId)
                _feedback.tryEmit(UiFeedback(message = stringResource(R.string.ui_ch_n_l_m_v_ch_nh), type = FeedbackType.Success))
            } catch (e: Exception) {
                _feedback.tryEmit(UiFeedback(message = "Không thể đổi ví: ${e.message}", type = FeedbackType.Error))
            }
        }
    }

    fun openTransferSheet(fromWalletId: Long? = null) {
        val currentWallets = _state.value.wallets
        val fromId = fromWalletId ?: currentWallets.firstOrNull()?.wallet?.id
        val toId = currentWallets.firstOrNull { it.wallet.id != fromId }?.wallet?.id
        _state.update {
            it.copy(
                isTransferSheetVisible = true,
                transferFromWalletId = fromId,
                transferToWalletId = toId,
                transferAmountInput = "",
                transferNote = "",
            )
        }
    }

    fun closeTransferSheet() {
        _state.update { it.copy(isTransferSheetVisible = false) }
    }

    fun onTransferFromWalletChanged(walletId: Long) {
        _state.update { current ->
            val toId = if (current.transferToWalletId == walletId) {
                current.wallets.firstOrNull { it.wallet.id != walletId }?.wallet?.id
            } else {
                current.transferToWalletId
            }
            current.copy(transferFromWalletId = walletId, transferToWalletId = toId)
        }
    }

    fun onTransferToWalletChanged(walletId: Long) {
        _state.update { it.copy(transferToWalletId = walletId) }
    }

    fun onTransferAmountChanged(input: String) {
        val clean = input.filter(Char::isDigit)
        _state.update { it.copy(transferAmountInput = clean) }
    }

    fun onTransferNoteChanged(note: String) {
        _state.update { it.copy(transferNote = note) }
    }

    fun executeTransfer() {
        val current = _state.value
        val fromId = current.transferFromWalletId ?: return
        val toId = current.transferToWalletId ?: return
        if (fromId == toId) {
            _feedback.tryEmit(UiFeedback(message = "Ví gửi và ví nhận phải khác nhau", type = FeedbackType.Error))
            return
        }
        val amountCents = current.transferAmountInput.toLongOrNull()?.let { it * 100 } ?: 0L
        if (amountCents <= 0L) {
            _feedback.tryEmit(UiFeedback(message = "Vui lòng nhập số tiền hợp lệ", type = FeedbackType.Error))
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isTransferSubmitting = true) }
            try {
                val fromWallet = current.wallets.firstOrNull { it.wallet.id == fromId }?.wallet
                val toWallet = current.wallets.firstOrNull { it.wallet.id == toId }?.wallet
                val now = Clock.System.now()
                val noteText = current.transferNote.ifBlank { "Chuyển tiền nội bộ" }

                transactionRepository.upsert(
                    Transaction(
                        amount = Money(amountCents),
                        type = TransactionType.EXPENSE,
                        category = Category.OTHER,
                        note = "Chuyển đến ${toWallet?.name ?: "ví khác"}: $noteText",
                        occurredAt = now,
                        walletId = fromId,
                        isInternalTransfer = true,
                    )
                )

                transactionRepository.upsert(
                    Transaction(
                        amount = Money(amountCents),
                        type = TransactionType.INCOME,
                        category = Category.OTHER,
                        note = "Nhận từ ${fromWallet?.name ?: "ví khác"}: $noteText",
                        occurredAt = now,
                        walletId = toId,
                        isInternalTransfer = true,
                    )
                )

                _state.update { it.copy(isTransferSheetVisible = false, isTransferSubmitting = false) }
                _feedback.tryEmit(UiFeedback(message = "Chuyển tiền thành công!", type = FeedbackType.Success))
            } catch (e: Exception) {
                _state.update { it.copy(isTransferSubmitting = false) }
                _feedback.tryEmit(UiFeedback(message = "Lỗi khi chuyển tiền: ${e.message}", type = FeedbackType.Error))
            }
        }
    }
}
