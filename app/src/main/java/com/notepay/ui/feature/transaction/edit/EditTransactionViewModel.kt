package com.notepay.ui.feature.transaction.edit

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.model.Category
import com.notepay.R
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.CategoryRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.usecase.SuggestCategoryUseCase
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.feedback.FeedbackType
import com.notepay.ui.feature.transaction.CategorySuggestionUiMapper
import com.notepay.ui.feature.transaction.AmountParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import javax.inject.Inject
import java.util.UUID

data class EditTransactionUiState(
    val isLoading: Boolean = true,
    val isAutoCapture: Boolean = false,   // true = read-only (ngân hàng bắt)
    val amountInput: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.DEFAULT_EXPENSE,
    val note: String = "",
    val dateLabel: String = "",
    // P2-14: lưu LocalDate để mở DatePicker chỉnh ngày.
    val date: LocalDate? = null,
    val isSaving: Boolean = false,
    val availableCategories: List<Category> = emptyList(),
    val suggestedCategory: Category? = null,
    val suggestionReason: String? = null,
)

@HiltViewModel
class EditTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val suggestCategoryUseCase: SuggestCategoryUseCase,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val txId: Long = checkNotNull(savedStateHandle["id"])
    private var originalTransaction: Transaction? = null

    private val _state = MutableStateFlow(EditTransactionUiState())
    val state = _state.asStateFlow()

    private val _feedback = MutableSharedFlow<UiFeedback>(replay = 1, extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    init {
        viewModelScope.launch {
            val tx = transactionRepository.getById(txId)
            // Lấy categories ban đầu để khởi tạo UI ngay, đồng thời subscribe luôn
            // để khi user tạo danh mục mới trong lúc edit thì grid cập nhật theo.
            val initialCategories = categoryRepository.observeCategories().firstOrNull() ?: emptyList()

            if (tx == null) {
                _state.update { it.copy(isLoading = false) }
                _feedback.tryEmit(
                    UiFeedback(
                        context.getString(R.string.error_transaction_not_found),
                        type = FeedbackType.Error,
                    ),
                )
                return@launch
            }

            originalTransaction = tx
            val localDateTime = tx.occurredAt.toLocalDateTime(TimeZone.currentSystemDefault())
            val dateStr = localDateTime.date.toString()

            val isIncome = tx.type == TransactionType.INCOME
            val initialSuggestion = suggestCategoryUseCase.suggestDetailed(tx.note, isIncome)

            _state.update {
                it.copy(
                    isLoading = false,
                    isAutoCapture = tx.isAutoCapture,
                    amountInput = tx.amount.toMajorUnitLong().toString(),
                    type = tx.type,
                    category = tx.category,
                    note = tx.note,
                    dateLabel = dateStr,
                    date = localDateTime.date,
                    availableCategories = initialCategories,
                    suggestedCategory = initialSuggestion?.category,
                    suggestionReason = initialSuggestion?.reason?.let { CategorySuggestionUiMapper.toText(context, it) },
                )
            }
        }

        // Observe realtime để thêm/sửa/xóa danh mục phản ánh ngay vào grid chỉnh sửa
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { list ->
                _state.update { it.copy(availableCategories = list) }
            }
        }
    }

    fun onAmountChanged(input: String) {
        if (_state.value.isAutoCapture) return
        val parsed = AmountParser.parse(input)
        _state.update { it.copy(amountInput = parsed.input) }
    }

    fun onNoteChanged(note: String) {
        if (_state.value.isAutoCapture) return
        val cleanNote = note.take(Transaction.MAX_NOTE_LENGTH)
        val isIncome = _state.value.type == TransactionType.INCOME
        val suggestion = suggestCategoryUseCase.suggestDetailed(cleanNote, isIncome)
        _state.update {
            it.copy(
                note = cleanNote,
                suggestedCategory = suggestion?.category,
                suggestionReason = suggestion?.reason?.let { CategorySuggestionUiMapper.toText(context, it) },
            )
        }
    }

    fun onCategoryChanged(category: Category) {
        _state.update { it.copy(category = category) }
    }

    /** P2-14: cập nhật ngày từ DatePicker. */
    fun onDateChanged(date: LocalDate) {
        if (_state.value.isAutoCapture) return
        _state.update { it.copy(date = date, dateLabel = date.toString()) }
    }

    /** Tạo danh mục tùy biến mới từ grid của màn chỉnh sửa, đồng thời chọn luôn. */
    fun createCategory(displayName: String, colorArgb: Long, iconId: String, isIncome: Boolean) {
        val cleanName = displayName.trim().replace(Regex("\\s+"), " ").take(40)
        if (cleanName.isBlank()) return
        if (_state.value.availableCategories.any {
                it.isIncome == isIncome && it.displayName.equals(cleanName, ignoreCase = true)
            }
        ) {
            _feedback.tryEmit(UiFeedback(context.getString(R.string.feedback_category_exists), type = FeedbackType.Error))
            return
        }
        viewModelScope.launch {
            val id = "CUSTOM_${UUID.randomUUID()}"
            val newCategory = Category(
                id = id,
                displayName = cleanName,
                colorArgb = colorArgb,
                isIncome = isIncome,
                isCustom = true,
                iconId = iconId,
            )
            categoryRepository.addCustomCategory(newCategory)
            _state.update { it.copy(category = newCategory) }
        }
    }

    fun save() {
        val current = _state.value
        if (current.isSaving) return
        val tx = originalTransaction ?: return

        val cents = if (current.isAutoCapture) {
            tx.amount.amountInCents
        } else {
            AmountParser.parse(current.amountInput).amount?.amountInCents ?: 0L
        }
        
        if (cents <= 0) {
            val message = context.getString(R.string.error_amount_positive)
            _feedback.tryEmit(UiFeedback(message, type = FeedbackType.Error))
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                // P2-14: nếu user đổi ngày thì cập nhật occurredAt (giữ nguyên giờ cũ).
                val newDate = current.date
                val newOccurredAt: kotlin.time.Instant = if (newDate != null) {
                    val oldLocal = tx.occurredAt.toLocalDateTime(TimeZone.currentSystemDefault())
                    LocalDateTime(
                        date = LocalDate(newDate.year, newDate.month.number, newDate.day),
                        time = LocalTime(oldLocal.hour, oldLocal.minute, oldLocal.second, oldLocal.nanosecond),
                    ).toInstant(TimeZone.currentSystemDefault())
                } else {
                    tx.occurredAt
                }
                val updated = tx.copy(
                    amount = Money(cents),
                    category = current.category,
                    note = if (current.isAutoCapture) tx.note else current.note,
                    occurredAt = newOccurredAt,
                )
                transactionRepository.upsert(updated)
                suggestCategoryUseCase.learn(
                    note = updated.note.trim(),
                    categoryId = updated.category.id,
                    isIncome = updated.type == TransactionType.INCOME,
                )
                _state.update { it.copy(isSaving = false) }
                _feedback.emit(UiFeedback(context.getString(R.string.feedback_transaction_updated), type = FeedbackType.Success))
            } catch (e: Exception) {
                val message = context.getString(R.string.feedback_transaction_update_failed)
                _state.update { it.copy(isSaving = false) }
                _feedback.emit(UiFeedback(message, type = FeedbackType.Error))
            }
        }
    }

}
