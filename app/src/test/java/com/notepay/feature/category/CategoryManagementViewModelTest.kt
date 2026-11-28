package com.notepay.feature.category

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.R
import com.notepay.core.designsystem.feedback.FeedbackDuration
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.domain.model.Category
import com.notepay.domain.repository.CategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryManagementViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        categoriesFlow: MutableStateFlow<List<Category>>,
        repo: CategoryRepository = mockk(relaxed = true) {
            every { observeCategories() } returns categoriesFlow
        },
    ): CategoryManagementViewModel {
        val context = mockk<Context>(relaxed = true) {
            every { getString(R.string.feedback_category_exists) } returns "Exists"
            every { getString(R.string.feedback_category_created) } returns "Created"
            every { getString(R.string.feedback_category_updated) } returns "Updated"
            every { getString(R.string.feedback_category_deleted) } returns "Deleted"
            every { getString(R.string.feedback_category_restored) } returns "Restored"
            every { getString(R.string.feedback_category_save_failed) } returns "Save failed"
            every { getString(R.string.feedback_undo) } returns "Undo"
            every { getString(R.string.action_retry) } returns "Retry"
        }
        return CategoryManagementViewModel(categoryRepository = repo, context = context)
    }

    @Test
    fun `addCategory warns on duplicate name and emits Success for valid name`() = runTest {
        val existing = Category(
            id = "CUSTOM_1",
            displayName = "Tra sua",
            colorArgb = 0xFFE57373L,
            isIncome = false,
            isCustom = true,
        )
        val flow = MutableStateFlow(listOf(existing))
        val repo = mockk<CategoryRepository>(relaxed = true) {
            every { observeCategories() } returns flow
        }
        val vm = createViewModel(flow, repo)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }

        vm.feedback.test {
            vm.addCategory("tra sua", 0xFFE57373L, "food", isIncome = false)
            val warn = awaitItem()
            assertThat(warn.type).isEqualTo(FeedbackType.Warning)
            assertThat(warn.message).isEqualTo("Exists")

            vm.addCategory("Ca phe", 0xFFE57373L, "coffee", isIncome = false)
            val ok = awaitItem()
            assertThat(ok.type).isEqualTo(FeedbackType.Success)
            assertThat(ok.message).isEqualTo("Created")
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { repo.addCustomCategory(match { it.displayName == "Ca phe" }) }
        collector.cancel()
    }

    @Test
    fun `deleteCategory emits Success with Undo and restores category on Undo`() = runTest {
        val custom = Category(
            id = "CUSTOM_9",
            displayName = "Bida",
            colorArgb = 0xFF64B5F6L,
            isIncome = false,
            isCustom = true,
        )
        val flow = MutableStateFlow(listOf(custom))
        val repo = mockk<CategoryRepository>(relaxed = true) {
            every { observeCategories() } returns flow
        }
        val vm = createViewModel(flow, repo)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }

        vm.feedback.test {
            vm.deleteCategory("CUSTOM_9")
            val deleted = awaitItem()
            assertThat(deleted.type).isEqualTo(FeedbackType.Success)
            assertThat(deleted.duration).isEqualTo(FeedbackDuration.Long)
            assertThat(deleted.actionLabel).isEqualTo("Undo")

            deleted.onAction?.invoke()
            val restored = awaitItem()
            assertThat(restored.type).isEqualTo(FeedbackType.Success)
            assertThat(restored.message).isEqualTo("Restored")
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { repo.deleteCustomCategory("CUSTOM_9") }
        coVerify(exactly = 1) { repo.addCustomCategory(custom) }
        collector.cancel()
    }
}
