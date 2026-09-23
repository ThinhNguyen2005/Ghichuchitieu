package com.notepay.data.preferences

import android.content.Context
import com.notepay.domain.repository.CategoryLearningEntry
import com.notepay.domain.repository.CategoryLearningMatch
import com.notepay.domain.repository.CategoryLearningSnapshot
import com.notepay.domain.repository.CategoryLearningStore
import com.notepay.domain.repository.CategoryLearningType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryLearningStoreImpl @Inject constructor(
    @ApplicationContext context: Context,
) : CategoryLearningStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val vocabularySizes = mutableMapOf<CategoryLearningType, Int>()

    override fun read(
        type: CategoryLearningType,
        categoryIds: Set<String>,
        tokens: Set<String>,
    ): CategoryLearningSnapshot = synchronized(PREFERENCES_LOCK) {
        val typeKey = type.storageKey

        CategoryLearningSnapshot(
            totalSamples = preferences.getInt("v2_total_$typeKey", 0),
            vocabularySize = vocabularySizes.getOrPut(type) {
                preferences.getStringSet("v2_vocabulary_$typeKey", emptySet()).orEmpty().size
            },
            categorySampleCounts = categoryIds.associateWith { categoryId ->
                preferences.getInt("v2_category_${typeKey}_$categoryId", 0)
            },
            categoryWordCounts = categoryIds.associateWith { categoryId ->
                preferences.getInt("v2_words_${typeKey}_$categoryId", 0)
            },
            tokenSampleCounts = categoryIds.associateWith { categoryId ->
                tokens.associateWith { token ->
                    preferences.getInt("v2_token_${typeKey}_${token}_$categoryId", 0)
                }
            },
        )
    }

    override fun findExact(
        type: CategoryLearningType,
        normalizedNote: String,
    ): CategoryLearningMatch? = synchronized(PREFERENCES_LOCK) {
        val key = exactKey(type, normalizedNote)
        val categoryId = preferences.getString(key, null) ?: return@synchronized null
        CategoryLearningMatch(
            categoryId = categoryId,
            timesSeen = preferences.getInt("${key}_count", 0),
        )
    }

    override fun learn(entry: CategoryLearningEntry) = synchronized(PREFERENCES_LOCK) {
        val typeKey = entry.type.storageKey
        val editor = preferences.edit()
        fun increment(key: String, amount: Int = 1) {
            editor.putInt(key, preferences.getInt(key, 0) + amount)
        }

        increment("v2_total_$typeKey")
        increment("v2_category_${typeKey}_${entry.categoryId}")
        increment("v2_words_${typeKey}_${entry.categoryId}", entry.tokens.size)

        val vocabularyKey = "v2_vocabulary_$typeKey"
        val vocabulary = preferences.getStringSet(vocabularyKey, emptySet()).orEmpty().toMutableSet()
        entry.tokens.forEach { token ->
            increment("v2_token_${typeKey}_${token}_${entry.categoryId}")
            vocabulary += token
        }
        editor.putStringSet(vocabularyKey, vocabulary)

        val exactKey = exactKey(entry.type, entry.normalizedNote)
        editor.putString(exactKey, entry.categoryId)
        increment("${exactKey}_count")
        editor.apply()
        vocabularySizes[entry.type] = vocabulary.size
    }

    private fun exactKey(type: CategoryLearningType, normalizedNote: String): String =
        "v2_exact_${type.storageKey}_${sha256(normalizedNote.replace(Regex("\\d+"), "#"))}"

    private fun sha256(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val PREFERENCES_NAME = "notepay_category_habits"
        val PREFERENCES_LOCK = Any()
        val CategoryLearningType.storageKey: String
            get() = when (this) {
                CategoryLearningType.EXPENSE -> "expense"
                CategoryLearningType.INCOME -> "income"
            }
    }
}
