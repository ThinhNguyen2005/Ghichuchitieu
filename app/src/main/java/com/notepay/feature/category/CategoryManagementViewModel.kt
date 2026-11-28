package com.notepay.feature.category

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.core.designsystem.feedback.FeedbackDuration
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.domain.model.Category
import com.notepay.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class CategoryManagementUiState(
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    val uiState: StateFlow<CategoryManagementUiState> = categoryRepository.observeCategories()
        .map { all ->
            CategoryManagementUiState(
                expenseCategories = all.filter { !it.isIncome },
                incomeCategories = all.filter { it.isIncome },
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryManagementUiState(),
        )

    fun addCategory(name: String, color: Long, iconId: String, isIncome: Boolean) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val existingList = if (isIncome) uiState.value.incomeCategories else uiState.value.expenseCategories
        if (existingList.any { it.displayName.equals(trimmed, ignoreCase = true) }) {
            _feedback.tryEmit(UiFeedback(context.getString(R.string.feedback_category_exists), type = FeedbackType.Warning))
            return
        }
        val id = "CUSTOM_${System.currentTimeMillis()}"
        val newCategory = Category(
            id = id,
            displayName = trimmed,
            colorArgb = color,
            isIncome = isIncome,
            isCustom = true,
            iconId = iconId,
        )
        viewModelScope.launch {
            try {
                categoryRepository.addCustomCategory(newCategory)
                _feedback.emit(UiFeedback(context.getString(R.string.feedback_category_created), type = FeedbackType.Success))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(R.string.feedback_category_save_failed),
                        actionLabel = context.getString(R.string.action_retry),
                        type = FeedbackType.Error,
                        duration = FeedbackDuration.Long,
                        onAction = { addCategory(trimmed, color, iconId, isIncome) },
                    ),
                )
            }
        }
    }

    fun updateCategory(category: Category, newName: String, newColor: Long, newIconId: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        val existingList = if (category.isIncome) uiState.value.incomeCategories else uiState.value.expenseCategories
        if (existingList.any { it.id != category.id && it.displayName.equals(trimmed, ignoreCase = true) }) {
            _feedback.tryEmit(UiFeedback(context.getString(R.string.feedback_category_exists), type = FeedbackType.Warning))
            return
        }
        val updated = category.copy(
            displayName = trimmed,
            colorArgb = newColor,
            iconId = newIconId,
        )
        viewModelScope.launch {
            try {
                categoryRepository.updateCustomCategory(updated)
                _feedback.emit(UiFeedback(context.getString(R.string.feedback_category_updated), type = FeedbackType.Success))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(R.string.feedback_category_save_failed),
                        actionLabel = context.getString(R.string.action_retry),
                        type = FeedbackType.Error,
                        duration = FeedbackDuration.Long,
                        onAction = { updateCategory(category, trimmed, newColor, newIconId) },
                    ),
                )
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        val deletedCategory = (uiState.value.expenseCategories + uiState.value.incomeCategories)
            .firstOrNull { it.id == categoryId }
        viewModelScope.launch {
            try {
                categoryRepository.deleteCustomCategory(categoryId)
                val canUndo = deletedCategory != null
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(R.string.feedback_category_deleted),
                        actionLabel = if (canUndo) context.getString(R.string.feedback_undo) else null,
                        type = FeedbackType.Success,
                        duration = if (canUndo) FeedbackDuration.Long else FeedbackDuration.Short,
                        onAction = deletedCategory?.let { cat -> { restoreCategory(cat) } },
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(R.string.feedback_category_delete_failed),
                        actionLabel = context.getString(R.string.action_retry),
                        type = FeedbackType.Error,
                        duration = FeedbackDuration.Long,
                        onAction = { deleteCategory(categoryId) },
                    ),
                )
            }
        }
    }

    private fun restoreCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.addCustomCategory(category)
                _feedback.emit(UiFeedback(context.getString(R.string.feedback_category_restored), type = FeedbackType.Success))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(R.string.feedback_category_restore_failed),
                        actionLabel = context.getString(R.string.action_retry),
                        type = FeedbackType.Error,
                        duration = FeedbackDuration.Long,
                        onAction = { restoreCategory(category) },
                    ),
                )
            }
        }
    }
}
