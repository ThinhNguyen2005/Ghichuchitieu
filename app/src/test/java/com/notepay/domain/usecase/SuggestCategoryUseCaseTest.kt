package com.notepay.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.repository.CategoryLearningStore
import com.notepay.domain.repository.CategoryLearningEntry
import com.notepay.domain.repository.CategoryLearningMatch
import com.notepay.domain.repository.CategoryLearningSnapshot
import com.notepay.domain.repository.CategoryLearningType
import org.junit.Before
import org.junit.Test

class SuggestCategoryUseCaseTest {

    private val learningStore = InMemoryCategoryLearningStore()
    private lateinit var useCase: SuggestCategoryUseCase

    @Before
    fun setUp() {
        learningStore.clear()
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

        val snapshot = learningStore.read(
            type = CategoryLearningType.EXPENSE,
            categoryIds = setOf(Category.FOOD.id),
            tokens = setOf("mua", "cafe"),
        )
        assertThat(snapshot.tokenSamples(Category.FOOD.id, "mua")).isEqualTo(2)
        assertThat(snapshot.tokenSamples(Category.FOOD.id, "cafe")).isEqualTo(2)
    }

    @Test
    fun `learned suggestion queries only the categories and tokens being scored`() {
        learningStore.seed(
            CategoryLearningType.EXPENSE,
            CategoryLearningSnapshot(
                totalSamples = 3,
                vocabularySize = 1000,
                categorySampleCounts = mapOf(Category.FOOD.id to 3),
                categoryWordCounts = mapOf(Category.FOOD.id to 6),
                tokenSampleCounts = mapOf(
                    Category.FOOD.id to mapOf("rare" to 3, "merchant" to 3),
                ),
            ),
        )

        useCase.suggestDetailed("rare merchant", isIncome = false)

        val request = learningStore.readRequests.single()
        assertThat(request.categoryIds).containsExactlyElementsIn(
            Category.getAll().filterNot { it.isIncome }.map { it.id },
        )
        assertThat(request.tokens).containsExactly("rare", "merchant")
    }

    private class InMemoryCategoryLearningStore : CategoryLearningStore {
        private val snapshots = mutableMapOf<CategoryLearningType, CategoryLearningSnapshot>()
        private val exactMatches = mutableMapOf<Pair<CategoryLearningType, String>, CategoryLearningMatch>()
        val readRequests = mutableListOf<ReadRequest>()

        override fun read(
            type: CategoryLearningType,
            categoryIds: Set<String>,
            tokens: Set<String>,
        ): CategoryLearningSnapshot {
            readRequests += ReadRequest(type, categoryIds, tokens)
            return snapshots[type] ?: CategoryLearningSnapshot()
        }

        override fun findExact(type: CategoryLearningType, normalizedNote: String): CategoryLearningMatch? =
            exactMatches[type to normalizedNote]

        override fun learn(entry: CategoryLearningEntry) {
            val current = snapshots[entry.type] ?: CategoryLearningSnapshot()
            val categoryCounts = current.categorySampleCounts.toMutableMap()
            categoryCounts[entry.categoryId] = categoryCounts.getOrDefault(entry.categoryId, 0) + 1
            val wordCounts = current.categoryWordCounts.toMutableMap()
            wordCounts[entry.categoryId] = wordCounts.getOrDefault(entry.categoryId, 0) + entry.tokens.size
            val tokenCounts = current.tokenSampleCounts.mapValues { it.value.toMutableMap() }.toMutableMap()
            val categoryTokens = tokenCounts.getOrPut(entry.categoryId) { mutableMapOf() }
            val vocabulary = current.tokenSampleCounts.values
                .flatMap { it.keys }
                .toMutableSet()
            entry.tokens.forEach { token ->
                categoryTokens[token] = categoryTokens.getOrDefault(token, 0) + 1
                vocabulary += token
            }
            snapshots[entry.type] = current.copy(
                totalSamples = current.totalSamples + 1,
                vocabularySize = vocabulary.size,
                categorySampleCounts = categoryCounts,
                categoryWordCounts = wordCounts,
                tokenSampleCounts = tokenCounts,
            )
            val exactKey = entry.type to entry.normalizedNote
            val previous = exactMatches[exactKey]
            exactMatches[exactKey] = CategoryLearningMatch(
                categoryId = entry.categoryId,
                timesSeen = (previous?.timesSeen ?: 0) + 1,
            )
        }

        fun clear() {
            snapshots.clear()
            exactMatches.clear()
            readRequests.clear()
        }

        fun seed(type: CategoryLearningType, snapshot: CategoryLearningSnapshot) {
            snapshots[type] = snapshot
        }

        data class ReadRequest(
            val type: CategoryLearningType,
            val categoryIds: Set<String>,
            val tokens: Set<String>,
        )
    }
}
