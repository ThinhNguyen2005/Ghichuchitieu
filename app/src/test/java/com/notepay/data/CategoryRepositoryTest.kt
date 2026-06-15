package com.notepay.data

import com.google.common.truth.Truth.assertThat
import com.notepay.data.repository.CategoryRepositoryImpl
import com.notepay.domain.model.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class CategoryRepositoryTest {

    private lateinit var repository: CategoryRepositoryImpl

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        
        // Reset SharedPreferences cho mỗi test case
        val prefs = context.getSharedPreferences("notepay_custom_categories", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        
        // Reset in-memory custom categories
        Category.registerCustomCategories(emptyList())

        repository = CategoryRepositoryImpl(context)
    }

    @Test
    fun testDefaultCategoriesOnlyAtStart() = runTest {
        val categories = repository.observeCategories().first()
        // Phải có đủ các categories mặc định
        assertThat(categories).contains(Category.FOOD)
        assertThat(categories).contains(Category.SALARY)
        assertThat(categories).contains(Category.OTHER)
        // Chưa có custom category nào
        val customs = categories.filter { it.isCustom }
        assertThat(customs).isEmpty()
    }

    @Test
    fun testAddCustomCategory() = runTest {
        val customCat = Category(
            id = "CUSTOM_TEST",
            displayName = "Từ thiện",
            colorArgb = 0xFFBA68C8L,
            isIncome = false,
            isCustom = true
        )

        repository.addCustomCategory(customCat)

        val categories = repository.observeCategories().first()
        assertThat(categories).contains(customCat)

        val retrieved = Category.safeValueOf("CUSTOM_TEST")
        assertThat(retrieved).isEqualTo(customCat)
        assertThat(retrieved.displayName).isEqualTo("Từ thiện")
        assertThat(retrieved.isCustom).isTrue()
    }

    @Test
    fun testEditCustomCategory() = runTest {
        val customCat1 = Category(
            id = "CUSTOM_TEST",
            displayName = "Từ thiện",
            colorArgb = 0xFFBA68C8L,
            isIncome = false,
            isCustom = true
        )
        repository.addCustomCategory(customCat1)
        
        val customCat2 = Category(
            id = "CUSTOM_TEST",
            displayName = "Ủng hộ",
            colorArgb = 0xFFBA68C8L,
            isIncome = false,
            isCustom = true
        )
        repository.addCustomCategory(customCat2)

        val categories = repository.observeCategories().first()
        val found = categories.find { it.id == "CUSTOM_TEST" }
        assertThat(found?.displayName).isEqualTo("Ủng hộ")

        val retrieved = Category.safeValueOf("CUSTOM_TEST")
        assertThat(retrieved.displayName).isEqualTo("Ủng hộ")
    }
}
