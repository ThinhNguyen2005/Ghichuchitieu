package com.notepay.data.repository

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
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
    @param:ApplicationContext private val context: Context
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
            val iconId = prefs.getString("custom_category_${id}_icon", "category_other") ?: "category_other"
            Category(
                id = id,
                displayName = name.trim(),
                colorArgb = color,
                isIncome = isIncome,
                isCustom = true,
                iconId = iconId,
            )
        }.filter { it.displayName.isNotBlank() }
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
        
        prefs.edit {
            putStringSet("custom_category_ids", ids)
            putString("custom_category_${category.id}_name", category.displayName)
            putLong("custom_category_${category.id}_color", category.colorArgb)
            putBoolean("custom_category_${category.id}_is_income", category.isIncome)
            putString("custom_category_${category.id}_icon", category.iconId)
        }
            
        loadAndRegister()
    }

    override suspend fun updateCustomCategory(category: Category) {
        addCustomCategory(category)
    }

    override suspend fun deleteCustomCategory(categoryId: String) {
        val ids = prefs.getStringSet("custom_category_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        ids.remove(categoryId)
        prefs.edit {
            putStringSet("custom_category_ids", ids)
            remove("custom_category_${categoryId}_name")
            remove("custom_category_${categoryId}_color")
            remove("custom_category_${categoryId}_is_income")
            remove("custom_category_${categoryId}_icon")
        }
        loadAndRegister()
    }

    @SuppressLint("UseKtx") // KTX edit(commit = true) cannot preserve commit()'s Boolean failure check.
    suspend fun replaceCustomCategories(categories: List<Category>) {
        val previousIds = prefs.getStringSet("custom_category_ids", emptySet()).orEmpty()
        val editor = prefs.edit()
        previousIds.forEach { id ->
            editor.remove("custom_category_${id}_name")
            editor.remove("custom_category_${id}_color")
            editor.remove("custom_category_${id}_is_income")
            editor.remove("custom_category_${id}_icon")
        }
        editor.putStringSet("custom_category_ids", categories.mapTo(mutableSetOf()) { it.id })
        categories.forEach { category ->
            editor.putString("custom_category_${category.id}_name", category.displayName)
            editor.putLong("custom_category_${category.id}_color", category.colorArgb)
            editor.putBoolean("custom_category_${category.id}_is_income", category.isIncome)
            editor.putString("custom_category_${category.id}_icon", category.iconId)
        }
        check(editor.commit()) { "Không thể lưu danh mục tùy chỉnh." }
        loadAndRegister()
    }
}
