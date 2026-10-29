package com.notepay.ui.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.model.Category
import com.notepay.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryManagementUiState(
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
)

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val uiState: StateFlow<CategoryManagementUiState> = categoryRepository.observeCategories()
        .map { all ->
            CategoryManagementUiState(
                expenseCategories = all.filter { !it.isIncome },
                incomeCategories = all.filter { it.isIncome },
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
            categoryRepository.addCustomCategory(newCategory)
        }
    }

    fun updateCategory(category: Category, newName: String, newColor: Long, newIconId: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        val updated = category.copy(
            displayName = trimmed,
            colorArgb = newColor,
            iconId = newIconId,
        )
        viewModelScope.launch {
            categoryRepository.updateCustomCategory(updated)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryRepository.deleteCustomCategory(categoryId)
        }
    }
}
