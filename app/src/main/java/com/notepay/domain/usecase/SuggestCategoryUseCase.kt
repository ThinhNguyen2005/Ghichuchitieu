package com.notepay.domain.usecase

import android.content.Context
import com.notepay.domain.model.Category
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import com.notepay.util.StringUtils

@Singleton
class SuggestCategoryUseCase @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("notepay_category_habits", Context.MODE_PRIVATE)

    private val expenseRules = listOf(
        setOf("grab", "taxi", "xang", "xe buyt", "bus", "gojek", "be", "may bay", "ve xe", "limousine", "di chuyen", "transport") to Category.TRANSPORT,
        setOf("cafe", "ca phe", "tra sua", "an", "com", "pho", "bun", "mi", "banh", "keo", "tiec", "nhau", "nuoc ngot", "food", "an uong") to Category.FOOD,
        setOf("shopee", "lazada", "tiki", "mua sam", "quan ao", "giay", "dep", "son", "my pham", "sieu thi", "cho", "shopping") to Category.SHOPPING,
        setOf("dien", "nuoc", "wifi", "internet", "cap", "bill", "chung cu", "rac", "hoa don") to Category.BILL,
        setOf("phim", "cgv", "netflix", "spotify", "youtube premium", "giai tri", "choi game", "steam", "nap game", "du lich") to Category.ENTERTAINMENT,
        setOf("thuoc", "benh vien", "phong kham", "nha khoa", "bao hiem", "suc khoe", "medical", "health") to Category.HEALTH,
        setOf("hoc", "sach", "vo", "khoa hoc", "hoc phi", "education") to Category.EDUCATION
    )

    private val incomeRules = listOf(
        setOf("luong", "salary", "thu nhap", "bonus", "thuong", "tien luong") to Category.SALARY,
        setOf("qua", "tang", "gift", "li xi", "cho tien") to Category.GIFT
    )

    fun suggest(note: String, isIncome: Boolean): Category {
        val normalizedNote = normalize(note)
        if (normalizedNote.isBlank()) {
            return if (isIncome) Category.DEFAULT_INCOME else Category.DEFAULT_EXPENSE
        }

        val tokens = tokenize(normalizedNote)
        val allHabits = prefs.all
        val scores = mutableMapOf<String, Int>()

        for (token in tokens) {
            for ((key, value) in allHabits) {
                if (key.startsWith("habit_${token}_") && value is Int) {
                    val categoryId = key.substringAfter("habit_${token}_")
                    scores[categoryId] = (scores[categoryId] ?: 0) + value
                }
            }
        }

        val bestLearnedCategory = scores.maxByOrNull { it.value }?.key
        val suggestedCategory = bestLearnedCategory?.let { id ->
            Category.getAll().find { it.id == id && it.isIncome == isIncome }
        }

        if (suggestedCategory != null) {
            return suggestedCategory
        }

        // Fallback to static rules
        val rules = if (isIncome) incomeRules else expenseRules
        for ((keywords, category) in rules) {
            if (keywords.any { containsKeyword(normalizedNote, tokens, it) }) {
                return category
            }
        }

        return if (isIncome) Category.DEFAULT_INCOME else Category.DEFAULT_EXPENSE
    }

    private fun containsKeyword(normalizedNote: String, tokens: List<String>, keyword: String): Boolean {
        if (!keyword.contains(' ')) {
            return tokens.contains(keyword)
        }
        val index = normalizedNote.indexOf(keyword)
        if (index == -1) return false
        val beforeIsWordChar = index > 0 && normalizedNote[index - 1].isLetterOrDigit()
        val afterIsWordChar = index + keyword.length < normalizedNote.length && normalizedNote[index + keyword.length].isLetterOrDigit()
        return !beforeIsWordChar && !afterIsWordChar
    }

    fun learn(note: String, categoryId: String) {
        val normalized = normalize(note)
        if (normalized.isBlank()) return
        val tokens = tokenize(normalized)
        val editor = prefs.edit()
        for (token in tokens) {
            val key = "habit_${token}_$categoryId"
            val currentCount = prefs.getInt(key, 0)
            editor.putInt(key, currentCount + 1)
        }
        editor.apply()
    }

    private fun normalize(text: String): String {
        return StringUtils.removeVietnameseAccents(text).lowercase(Locale.ROOT).trim()
    }

    private fun tokenize(text: String): List<String> {
        return text.split(Regex("[^a-zA-Z0-9]"))
            .map { it.trim() }
            .filter { it.length >= 2 }
    }
}
