package com.notepay.domain.usecase

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Before
import org.junit.Test

class SuggestCategoryUseCaseTest {

    private val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
    private val context = mockk<Context>()
    private lateinit var useCase: SuggestCategoryUseCase

    private val fakePrefsMap = mutableMapOf<String, Any>()

    @Before
    fun setUp() {
        fakePrefsMap.clear()
        every { context.getSharedPreferences("notepay_category_habits", Context.MODE_PRIVATE) } returns sharedPrefs

        // Mock shared preferences all and getInt
        every { sharedPrefs.all } answers { fakePrefsMap }
        
        val keySlot = slot<String>()
        every { sharedPrefs.getInt(capture(keySlot), any()) } answers {
            fakePrefsMap[keySlot.captured] as? Int ?: 0
        }

        // Mock editor
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { sharedPrefs.edit() } returns editor
        
        val putKey = slot<String>()
        val putValue = slot<Int>()
        every { editor.putInt(capture(putKey), capture(putValue)) } answers {
            fakePrefsMap[putKey.captured] = putValue.captured
            editor
        }
        every { editor.apply() } returns Unit

        useCase = SuggestCategoryUseCase(context)
    }

    @Test
    fun `suggest returns correct category from static rules`() {
        // Expense cases
        assertThat(useCase.suggest("đi xe grab", isIncome = false)).isEqualTo(Category.TRANSPORT)
        assertThat(useCase.suggest("Ăn trưa cơm văn phòng", isIncome = false)).isEqualTo(Category.FOOD)
        assertThat(useCase.suggest("Mua quần áo shopee", isIncome = false)).isEqualTo(Category.SHOPPING)
        assertThat(useCase.suggest("Thanh toán tiền điện tháng này", isIncome = false)).isEqualTo(Category.BILL)
        assertThat(useCase.suggest("Đi xem phim ở CGV", isIncome = false)).isEqualTo(Category.ENTERTAINMENT)
        assertThat(useCase.suggest("Mua thuốc cảm cúm", isIncome = false)).isEqualTo(Category.HEALTH)
        assertThat(useCase.suggest("Mua sách học tiếng Anh", isIncome = false)).isEqualTo(Category.EDUCATION)

        // Income cases
        assertThat(useCase.suggest("Nhận lương tháng 6", isIncome = true)).isEqualTo(Category.SALARY)
        assertThat(useCase.suggest("Được tặng quà sinh nhật", isIncome = true)).isEqualTo(Category.GIFT)
    }

    @Test
    fun `learn updates habits and suggest returns learned category`() {
        // Initial suggestion should fallback to static rule or default (OTHER/FOOD/etc.)
        assertThat(useCase.suggest("nạp tiền game vtc", isIncome = false)).isEqualTo(Category.DEFAULT_EXPENSE)

        // Learn the habit
        useCase.learn("nạp tiền game vtc", Category.ENTERTAINMENT.id)

        // Now suggestion should return ENTERTAINMENT
        assertThat(useCase.suggest("nạp tiền game vtc", isIncome = false)).isEqualTo(Category.ENTERTAINMENT)
    }

    @Test
    fun `learn stores token frequency correctly`() {
        // Learn once
        useCase.learn("mua cafe", Category.FOOD.id)
        
        // Learn twice
        useCase.learn("mua cafe", Category.FOOD.id)

        // We expect "mua" and "cafe" to have a count of 2 for FOOD
        assertThat(fakePrefsMap["habit_mua_${Category.FOOD.id}"]).isEqualTo(2)
        assertThat(fakePrefsMap["habit_cafe_${Category.FOOD.id}"]).isEqualTo(2)
    }
}
