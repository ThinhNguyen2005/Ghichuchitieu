package com.notepay.data.repository

import android.content.Context
import com.notepay.domain.model.Category
import com.notepay.domain.repository.CategoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CategoryRepository {

    private val prefs = context.getSharedPreferences("notepay_custom_categories", Context.MODE_PRIVATE)
    
    // MutableStateFlow để phát tín hiệu danh sách thay đổi realtime
    private val _categoriesFlow = MutableStateFlow<List<Category>>(emptyList())

    init {
        loadAndRegister()
    }

    private fun loadAndRegister() {
        val ids = prefs.getStringSet("custom_category_ids", emptySet()) ?: emptySet()
        val list = ids.mapNotNull { id ->
            val name = prefs.getString("custom_category_${id}_name", null) ?: return@mapNotNull null
            val color = prefs.getLong("custom_category_${id}_color", 0xFF90A4AEL)
            val isIncome = prefs.getBoolean("custom_category_${id}_is_income", false)
            Category(
                id = id,
                displayName = name,
                colorArgb = color,
                isIncome = isIncome,
                isCustom = true
            )
        }
        Category.registerCustomCategories(list)
        _categoriesFlow.value = Category.getAll()
    }

    override fun observeCategories(): Flow<List<Category>> = _categoriesFlow.asStateFlow()

    override suspend fun getCategories(): List<Category> {
        return Category.getAll()
    }

    override suspend fun addCustomCategory(category: Category) {
        val ids = prefs.getStringSet("custom_category_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        ids.add(category.id)
        
        prefs.edit()
            .putStringSet("custom_category_ids", ids)
            .putString("custom_category_${category.id}_name", category.displayName)
            .putLong("custom_category_${category.id}_color", category.colorArgb)
            .putBoolean("custom_category_${category.id}_is_income", category.isIncome)
            .apply()
            
        loadAndRegister()
    }
}
