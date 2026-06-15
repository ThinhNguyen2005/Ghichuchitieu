package com.notepay.ui.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.model.Money
import com.notepay.domain.model.Subscription
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.SubscriptionRepository
import com.notepay.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import javax.inject.Inject

data class SubscriptionUiState(
    val allSubscriptions: List<Subscription> = emptyList(),
    val upcomingSubscriptions: List<Subscription> = emptyList(),
    val calendarTransactions: Map<LocalDate, List<Transaction>> = emptyMap(),
    val calendarSubscriptions: Map<LocalDate, List<Subscription>> = emptyMap(),
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val error: String? = null,
)

// Dialog state tách riêng để tránh recompose toàn bộ
data class AddSubscriptionDialogState(
    val name: String = "",
    val amountInput: String = "",
    val repeatMonths: Int = 1,
    val remindDaysBefore: Int = 3,
    val note: String = "",
    val category: String = "subscription",
    val dueDateLabel: String = "",
    val nextDueEpochMs: Long = Clock.System.now().plus(30.days).toEpochMilliseconds(),
    val prefillName: String? = null,
    val prefillAmountCents: Long? = null,
) {
    val canSave: Boolean get() = name.isNotBlank() && amountInput.isNotEmpty()
}

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _dialogState = MutableStateFlow(AddSubscriptionDialogState())
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()
    val dialogState = _dialogState.asStateFlow()

    // Giới hạn 7 ngày tới cho tab "Sắp đến hạn"
    private val upcomingLimit = Clock.System.now().plus(7.days)

    val state = combine(
        subscriptionRepository.observeAll(),
        subscriptionRepository.observeUpcoming(upcomingLimit),
        transactionRepository.observeAll(),
    ) { all, upcoming, transactions ->
        val tz = TimeZone.currentSystemDefault()
        val txByDate = transactions.groupBy { it.occurredAt.toLocalDateTime(tz).date }
        val subByDate = all
            .filter { it.isActive }
            .groupBy { it.nextDueDate.toLocalDateTime(tz).date }
        // 8 giao dịch expense gần nhất để gợi ý prefill nhắc nhở
        val recent = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sortedByDescending { it.occurredAt }
            .take(8)
        SubscriptionUiState(
            allSubscriptions = all,
            upcomingSubscriptions = upcoming,
            calendarTransactions = txByDate,
            calendarSubscriptions = subByDate,
            recentTransactions = recent,
            isLoading = false,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SubscriptionUiState(),
    )

    fun showAddDialog() {
        _dialogState.update {
            AddSubscriptionDialogState() // reset form mỗi lần mở
        }
    }

    fun hideAddDialog() = _dialogState.update { AddSubscriptionDialogState() }

    fun onNameChanged(name: String) = _dialogState.update { it.copy(name = name) }
    fun onAmountChanged(input: String) = _dialogState.update { it.copy(amountInput = input.filter(Char::isDigit)) }
    fun onRepeatMonthsChanged(months: Int) = _dialogState.update { it.copy(repeatMonths = months) }
    fun onRemindDaysChanged(days: Int) = _dialogState.update { it.copy(remindDaysBefore = days) }
    fun onNoteChanged(note: String) = _dialogState.update { it.copy(note = note.take(200)) }
    fun onNextDueDateChanged(epochMs: Long) = _dialogState.update { it.copy(nextDueEpochMs = epochMs) }
    fun onCategoryChanged(category: String) = _dialogState.update { it.copy(category = category) }

    /**
     * Prefill form từ 1 giao dịch: tên = ghi chú (hoặc category), số tiền = amount.
     * Dùng khi user chọn "Tạo nhắc nhở" từ TransactionDetailScreen.
     */
    fun setPrefill(name: String, amountCents: Long, category: String) {
        _dialogState.update {
            AddSubscriptionDialogState(
                name = name,
                amountInput = (amountCents / 100).toString(),
                repeatMonths = 1,
                remindDaysBefore = 3,
                note = "",
                category = category,
            )
        }
    }

    fun saveSubscription() {
        val dialog = _dialogState.value
        if (!dialog.canSave) return

        viewModelScope.launch {
            try {
                val cents = (dialog.amountInput.toLongOrNull() ?: 0L) * 100
                val subscription = Subscription(
                    name = dialog.name.trim(),
                    amount = Money(cents),
                    category = dialog.category,
                    nextDueDate = kotlinx.datetime.Instant.fromEpochMilliseconds(dialog.nextDueEpochMs),
                    repeatMonths = dialog.repeatMonths,
                    remindDaysBefore = dialog.remindDaysBefore,
                    note = dialog.note.trim(),
                )
                subscriptionRepository.upsert(subscription)
                hideAddDialog()
            } catch (e: Exception) {
                // Ignore validation errors silently — UI đã guard bằng canSave
            }
        }
    }

    fun deleteSubscription(id: Long) {
        viewModelScope.launch { subscriptionRepository.delete(id) }
    }

    // Tab selection handling
    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }
}
