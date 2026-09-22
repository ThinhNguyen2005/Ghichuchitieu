package com.notepay.data.preferences

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.notepay.domain.repository.CategoryLearningEntry
import com.notepay.domain.repository.CategoryLearningType
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CategoryLearningStoreImplTest {
    private lateinit var preferences: android.content.SharedPreferences

    @Before
    fun setUp() {
        preferences = RuntimeEnvironment.getApplication()
            .getSharedPreferences("notepay_category_habits", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
    }

    @Test
    fun `reads existing v2 preference data through domain API`() {
        val exactKey = "v2_exact_expense_22e859add8416d388b23196f0efc3e0adfa64ac2aa37ba5f9751f5944b232dd4"
        preferences.edit()
            .putInt("v2_total_expense", 3)
            .putStringSet("v2_vocabulary_expense", setOf("nap", "tien", "vtc"))
            .putInt("v2_category_expense_ENTERTAINMENT", 2)
            .putInt("v2_words_expense_ENTERTAINMENT", 6)
            .putInt("v2_token_expense_tien_ENTERTAINMENT", 2)
            .putString(exactKey, "ENTERTAINMENT")
            .putInt("${exactKey}_count", 2)
            .commit()

        val store = CategoryLearningStoreImpl(RuntimeEnvironment.getApplication())

        val snapshot = store.read(
            type = CategoryLearningType.EXPENSE,
            categoryIds = setOf("ENTERTAINMENT"),
            tokens = setOf("tien"),
        )
        assertThat(snapshot.totalSamples).isEqualTo(3)
        assertThat(snapshot.vocabularySize).isEqualTo(3)
        assertThat(snapshot.categorySamples("ENTERTAINMENT")).isEqualTo(2)
        assertThat(snapshot.categoryWords("ENTERTAINMENT")).isEqualTo(6)
        assertThat(snapshot.tokenSamples("ENTERTAINMENT", "tien")).isEqualTo(2)
        assertThat(store.findExact(CategoryLearningType.EXPENSE, "nap tien vtc"))
            .isEqualTo(com.notepay.domain.repository.CategoryLearningMatch("ENTERTAINMENT", 2))
    }

    @Test
    fun `learn writes the existing v2 preference format`() {
        val store = CategoryLearningStoreImpl(RuntimeEnvironment.getApplication())

        store.learn(
            CategoryLearningEntry(
                type = CategoryLearningType.EXPENSE,
                categoryId = "FOOD",
                normalizedNote = "mua cafe",
                tokens = listOf("mua", "cafe"),
            ),
        )

        assertThat(preferences.getInt("v2_total_expense", 0)).isEqualTo(1)
        assertThat(preferences.getInt("v2_category_expense_FOOD", 0)).isEqualTo(1)
        assertThat(preferences.getInt("v2_words_expense_FOOD", 0)).isEqualTo(2)
        assertThat(preferences.getInt("v2_token_expense_mua_FOOD", 0)).isEqualTo(1)
        assertThat(preferences.getInt("v2_token_expense_cafe_FOOD", 0)).isEqualTo(1)
        assertThat(preferences.getStringSet("v2_vocabulary_expense", emptySet()))
            .containsExactly("mua", "cafe")

        val exactKey = "v2_exact_expense_da32fbbe2de86fb16f76dffa1383b10772a006dee9f0fe54bce1f44d61eea3f8"
        assertThat(preferences.getString(exactKey, null)).isEqualTo("FOOD")
        assertThat(preferences.getInt("${exactKey}_count", 0)).isEqualTo(1)
    }

    @Test
    fun `reads only requested category and token keys despite unrelated legacy preferences`() {
        val unrelated = (0 until 300).associate { "legacy_unrelated_$it" to "value-$it" }
        val editor = preferences.edit()
        unrelated.forEach { (key, value) -> editor.putString(key, value) }
        editor
            .putInt("v2_total_expense", 5)
            .putInt("v2_category_expense_FOOD", 4)
            .putInt("v2_category_expense_UNRELATED", 99)
            .putInt("v2_words_expense_FOOD", 8)
            .putInt("v2_words_expense_UNRELATED", 99)
            .putInt("v2_token_expense_cafe_FOOD", 4)
            .putInt("v2_token_expense_other_FOOD", 99)
            .commit()

        val snapshot = CategoryLearningStoreImpl(RuntimeEnvironment.getApplication()).read(
            type = CategoryLearningType.EXPENSE,
            categoryIds = setOf("FOOD"),
            tokens = setOf("cafe"),
        )

        assertThat(snapshot.categorySampleCounts).containsExactly("FOOD", 4)
        assertThat(snapshot.categoryWordCounts).containsExactly("FOOD", 8)
        assertThat(snapshot.tokenSampleCounts).containsExactly(
            "FOOD",
            mapOf("cafe" to 4),
        )
    }

    @Test
    fun `parallel learns preserve all counts and vocabulary`() {
        val store = CategoryLearningStoreImpl(RuntimeEnvironment.getApplication())
        val workerCount = 8
        val learnsPerWorker = 50
        val start = CountDownLatch(1)
        val done = CountDownLatch(workerCount)
        val failure = AtomicReference<Throwable?>(null)
        val executor = Executors.newFixedThreadPool(workerCount)

        repeat(workerCount) {
            executor.execute {
                try {
                    start.await()
                    repeat(learnsPerWorker) {
                        store.learn(
                            CategoryLearningEntry(
                                type = CategoryLearningType.EXPENSE,
                                categoryId = "FOOD",
                                normalizedNote = "mua cafe",
                                tokens = listOf("mua", "cafe"),
                            ),
                        )
                    }
                } catch (error: Throwable) {
                    failure.compareAndSet(null, error)
                } finally {
                    done.countDown()
                }
            }
        }
        start.countDown()
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()
        assertThat(failure.get()).isNull()

        val expected = workerCount * learnsPerWorker
        val snapshot = store.read(
            type = CategoryLearningType.EXPENSE,
            categoryIds = setOf("FOOD"),
            tokens = setOf("mua", "cafe"),
        )
        assertThat(snapshot.totalSamples).isEqualTo(expected)
        assertThat(snapshot.categorySamples("FOOD")).isEqualTo(expected)
        assertThat(snapshot.categoryWords("FOOD")).isEqualTo(expected * 2)
        assertThat(snapshot.tokenSamples("FOOD", "mua")).isEqualTo(expected)
        assertThat(snapshot.tokenSamples("FOOD", "cafe")).isEqualTo(expected)
        assertThat(snapshot.vocabularySize).isEqualTo(2)
        assertThat(store.findExact(CategoryLearningType.EXPENSE, "mua cafe")?.timesSeen)
            .isEqualTo(expected)
    }
}
