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
        val categories = Category.getAll().filter { it.isIncome == isIncome }
        val totalLearnedCount = prefs.getInt("total_learned_count", 0)

        // If no training data has been learned yet, fallback to static rules
        val suggestedCategory = if (totalLearnedCount > 0) {
            val vocabSize = prefs.getStringSet("vocabulary", emptySet())?.size?.coerceAtLeast(1) ?: 1
            val numClasses = categories.size
            
            categories.maxByOrNull { category ->
                val categoryCount = prefs.getInt("category_learned_count_${category.id}", 0)
                // P(C) with Laplace smoothing
                val priorProb = (categoryCount + 1.0) / (totalLearnedCount + numClasses)
                var logScore = Math.log(priorProb)

                val totalWordsInCat = prefs.getInt("category_total_words_${category.id}", 0)

                for (token in tokens) {
                    val wordCountInCat = prefs.getInt("habit_${token}_${category.id}", 0)
                    // P(W|C) with Laplace smoothing
                    val wordProb = (wordCountInCat + 1.0) / (totalWordsInCat + vocabSize)
                    logScore += Math.log(wordProb)
                }
                logScore
            }
        } else {
            null
        }

        if (suggestedCategory != null && prefs.getInt("category_learned_count_${suggestedCategory.id}", 0) > 0) {
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
        if (tokens.isEmpty()) return

        val editor = prefs.edit()

        // 1. Update overall learning counts
        val currentTotalLearned = prefs.getInt("total_learned_count", 0)
        editor.putInt("total_learned_count", currentTotalLearned + 1)

        val currentCatLearned = prefs.getInt("category_learned_count_$categoryId", 0)
        editor.putInt("category_learned_count_$categoryId", currentCatLearned + 1)

        // 2. Update total words in this category
        val currentCatTotalWords = prefs.getInt("category_total_words_$categoryId", 0)
        editor.putInt("category_total_words_$categoryId", currentCatTotalWords + tokens.size)

        // 3. Update vocabulary and token frequencies
        val vocab = prefs.getStringSet("vocabulary", emptySet())?.toMutableSet() ?: mutableSetOf()
        var vocabChanged = false

        for (token in tokens) {
            val key = "habit_${token}_$categoryId"
            val currentCount = prefs.getInt(key, 0)
            editor.putInt(key, currentCount + 1)

            if (vocab.add(token)) {
                vocabChanged = true
            }
        }

        if (vocabChanged) {
            editor.putStringSet("vocabulary", vocab)
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
