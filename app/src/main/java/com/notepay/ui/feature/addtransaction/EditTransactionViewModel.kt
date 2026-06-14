package com.notepay.ui.feature.addtransaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.CategoryRepository
import com.notepay.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class EditTransactionUiState(
    val isLoading: Boolean = true,
    val isAutoCapture: Boolean = false,   // true = read-only (ngân hàng bắt)
    val amountInput: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.DEFAULT_EXPENSE,
    val note: String = "",
    val dateLabel: String = "",
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null,
    val availableCategories: List<Category> = emptyList(),
)

@HiltViewModel
class EditTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val txId: Long = checkNotNull(savedStateHandle["id"])
    private var originalTransaction: Transaction? = null

    private val _state = MutableStateFlow(EditTransactionUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val tx = transactionRepository.getById(txId)
            // Lấy categories ban đầu để khởi tạo UI ngay, đồng thời subscribe luôn
            // để khi user tạo danh mục mới trong lúc edit thì grid cập nhật theo.
            val initialCategories = categoryRepository.observeCategories().firstOrNull() ?: emptyList()

            if (tx == null) {
                _state.update { it.copy(isLoading = false, error = "Không tìm thấy giao dịch") }
                return@launch
            }

            originalTransaction = tx
            val dateStr = tx.occurredAt
                .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

            _state.update {
                it.copy(
                    isLoading = false,
                    isAutoCapture = tx.isAutoCapture,
                    amountInput = (tx.amount.amountInCents / 100).toString(),
                    type = tx.type,
                    category = tx.category,
                    note = tx.note,
                    dateLabel = dateStr,
                    availableCategories = initialCategories,
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
        val clean = input.filter(Char::isDigit)
        _state.update { it.copy(amountInput = clean) }
    }

    fun onNoteChanged(note: String) {
        if (_state.value.isAutoCapture) return
        _state.update { it.copy(note = note.take(200)) }
    }

    fun onCategoryChanged(category: Category) {
        _state.update { it.copy(category = category) }
    }

    /** Tạo danh mục tùy biến mới từ grid của màn chỉnh sửa, đồng thời chọn luôn. */
    fun createCategory(displayName: String, colorArgb: Long, isIncome: Boolean) {
        if (_state.value.isAutoCapture) return
        viewModelScope.launch {
            val id = "CUSTOM_${System.currentTimeMillis()}"
            val newCategory = Category(
                id = id,
                displayName = displayName.trim(),
                colorArgb = colorArgb,
                isIncome = isIncome,
                isCustom = true,
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
            (current.amountInput.toLongOrNull() ?: 0L) * 100
        }
        
        if (cents <= 0) {
            _state.update { it.copy(error = "Số tiền phải lớn hơn 0") }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val updated = tx.copy(
                    amount = Money(cents),
                    category = current.category,
                    note = if (current.isAutoCapture) tx.note else current.note,
                )
                transactionRepository.upsert(updated)
                _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
