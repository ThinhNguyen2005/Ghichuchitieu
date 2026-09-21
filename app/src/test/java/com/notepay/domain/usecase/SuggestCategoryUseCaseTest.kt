package com.notepay.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.repository.CategoryLearningStore
import com.notepay.domain.repository.CategoryLearningStoreEditor
import org.junit.Before
import org.junit.Test

class SuggestCategoryUseCaseTest {

    private val learningStore = InMemoryCategoryLearningStore()
    private lateinit var useCase: SuggestCategoryUseCase

    private val fakePrefsMap: MutableMap<String, Any>
        get() = learningStore.values

    @Before
    fun setUp() {
        fakePrefsMap.clear()
        useCase = SuggestCategoryUseCase(learningStore)
    }

    @Test
    fun `suggest returns correct category from static rules`() {
        assertThat(useCase.suggest("đi xe grab", isIncome = false)).isEqualTo(Category.TRANSPORT)
        assertThat(useCase.suggest("Ăn cơm tấm", isIncome = false)).isEqualTo(Category.FOOD)
        assertThat(useCase.suggest("Mua hàng shopee", isIncome = false)).isEqualTo(Category.SHOPPING)
        assertThat(useCase.suggest("Thanh toán hóa đơn tháng này", isIncome = false)).isEqualTo(Category.BILL)
        assertThat(useCase.suggest("Đi xem phim ở CGV", isIncome = false)).isEqualTo(Category.ENTERTAINMENT)
        assertThat(useCase.suggest("Mua thuốc cảm cúm", isIncome = false)).isEqualTo(Category.HEALTH)
        assertThat(useCase.suggest("Mua sách học tiếng Anh", isIncome = false)).isEqualTo(Category.EDUCATION)
        assertThat(useCase.suggest("Nhận lương tháng 6", isIncome = true)).isEqualTo(Category.SALARY)
        assertThat(useCase.suggest("Nhận quà tặng", isIncome = true)).isEqualTo(Category.GIFT)
    }

    @Test
    fun `learn updates habits and suggest returns learned category`() {
        assertThat(useCase.suggest("nạp tiền vtc", isIncome = false)).isEqualTo(Category.DEFAULT_EXPENSE)

        useCase.learn("nạp tiền vtc", Category.ENTERTAINMENT.id)

        assertThat(useCase.suggest("nạp tiền vtc", isIncome = false)).isEqualTo(Category.ENTERTAINMENT)
    }

    @Test
    fun `learn stores token frequency correctly`() {
        useCase.learn("mua cafe", Category.FOOD.id)
        useCase.learn("mua cafe", Category.FOOD.id)

        assertThat(fakePrefsMap["v2_token_expense_mua_${Category.FOOD.id}"]).isEqualTo(2)
        assertThat(fakePrefsMap["v2_token_expense_cafe_${Category.FOOD.id}"]).isEqualTo(2)
    }

    private class InMemoryCategoryLearningStore : CategoryLearningStore {
        val values = mutableMapOf<String, Any>()

        override fun getString(key: String): String? = values[key] as? String

        override fun getInt(key: String, defaultValue: Int): Int = values[key] as? Int ?: defaultValue

        override fun getStringSet(key: String): Set<String> = values[key] as? Set<String> ?: emptySet()

        override fun edit(block: CategoryLearningStoreEditor.() -> Unit) {
            Editor(values).apply(block)
        }
    }

    private class Editor(
        private val values: MutableMap<String, Any>,
    ) : CategoryLearningStoreEditor {
        override fun putInt(key: String, value: Int) {
            values[key] = value
        }

        override fun putString(key: String, value: String) {
            values[key] = value
        }

        override fun putStringSet(key: String, value: Set<String>) {
            values[key] = value
        }
    }
}
