package com.notepay.domain.repository

enum class CategoryLearningType {
    EXPENSE,
    INCOME,
}

data class CategoryLearningEntry(
    val type: CategoryLearningType,
    val categoryId: String,
    val normalizedNote: String,
    val tokens: List<String>,
)

data class CategoryLearningMatch(
    val categoryId: String,
    val timesSeen: Int,
)

data class CategoryLearningSnapshot(
    val totalSamples: Int = 0,
    val vocabularySize: Int = 0,
    val categorySampleCounts: Map<String, Int> = emptyMap(),
    val categoryWordCounts: Map<String, Int> = emptyMap(),
    val tokenSampleCounts: Map<String, Map<String, Int>> = emptyMap(),
) {
    fun categorySamples(categoryId: String): Int = categorySampleCounts[categoryId] ?: 0

    fun categoryWords(categoryId: String): Int = categoryWordCounts[categoryId] ?: 0

    fun tokenSamples(categoryId: String, token: String): Int =
        tokenSampleCounts[categoryId]?.get(token) ?: 0
}

/** Domain-facing persistence boundary for on-device category-learning statistics. */
interface CategoryLearningStore {
    fun read(
        type: CategoryLearningType,
        categoryIds: Set<String>,
        tokens: Set<String>,
    ): CategoryLearningSnapshot

    fun findExact(type: CategoryLearningType, normalizedNote: String): CategoryLearningMatch?

    fun learn(entry: CategoryLearningEntry)
}
