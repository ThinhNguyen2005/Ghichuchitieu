package com.notepay.feature.subscription

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.R
import com.notepay.core.designsystem.feedback.FeedbackDuration
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.domain.model.Subscription
import com.notepay.domain.money.Money
import com.notepay.domain.repository.SubscriptionRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.feature.subscription.main.SubscriptionViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        subsFlow: MutableStateFlow<List<Subscription>> = MutableStateFlow(emptyList()),
        subRepo: SubscriptionRepository = mockk(relaxed = true) {
            every { observeAll() } returns subsFlow
            every { observeUpcoming(any()) } returns flowOf(emptyList())
        },
    ): SubscriptionViewModel {
        val txRepo = mockk<TransactionRepository>(relaxed = true) {
            every { observeAll() } returns flowOf(emptyList())
        }
        val context = mockk<Context>(relaxed = true) {
            every { getString(R.string.feedback_subscription_saved) } returns "Sub saved"
            every { getString(R.string.feedback_subscription_save_failed) } returns "Sub save failed"
            every { getString(R.string.feedback_subscription_deleted) } returns "Sub deleted"
            every { getString(R.string.feedback_subscription_restored) } returns "Sub restored"
            every { getString(R.string.feedback_undo) } returns "Undo"
            every { getString(R.string.action_retry) } returns "Retry"
        }
        return SubscriptionViewModel(subRepo, txRepo, context)
    }

    @Test
    fun `saveSubscription emits Error with Retry on failure and Success on retry`() = runTest {
        val subRepo = mockk<SubscriptionRepository>(relaxed = true) {
            every { observeAll() } returns flowOf(emptyList())
            every { observeUpcoming(any()) } returns flowOf(emptyList())
        }
        coEvery { subRepo.upsert(any()) } throws IllegalStateException("db locked") andThen 1L
        val vm = createViewModel(subRepo = subRepo)
        vm.onNameChanged("Netflix")
        vm.onAmountChanged("260000")

        vm.feedback.test {
            vm.saveSubscription()
            val err = awaitItem()
            assertThat(err.type).isEqualTo(FeedbackType.Error)
            assertThat(err.actionLabel).isEqualTo("Retry")

            err.onAction?.invoke()
            val ok = awaitItem()
            assertThat(ok.type).isEqualTo(FeedbackType.Success)
            assertThat(ok.message).isEqualTo("Sub saved")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteSubscription emits Success with Undo and restores on Undo`() = runTest {
        val sub = Subscription(
            id = 12L,
            name = "iCloud",
            amount = Money.fromMajorUnit(19_000L),
            category = "subscription",
            nextDueDate = Instant.fromEpochMilliseconds(1_800_000_000_000L),
            repeatMonths = 1,
            remindDaysBefore = 3,
            note = "",
        )
        val subsFlow = MutableStateFlow(listOf(sub))
        val subRepo = mockk<SubscriptionRepository>(relaxed = true) {
            every { observeAll() } returns subsFlow
            every { observeUpcoming(any()) } returns flowOf(emptyList())
        }
        val vm = createViewModel(subsFlow = subsFlow, subRepo = subRepo)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.collect {}
        }

        vm.feedback.test {
            vm.deleteSubscription(12L)
            val fb = awaitItem()
            assertThat(fb.type).isEqualTo(FeedbackType.Success)
            assertThat(fb.duration).isEqualTo(FeedbackDuration.Long)
            assertThat(fb.actionLabel).isEqualTo("Undo")

            fb.onAction?.invoke()
            val restored = awaitItem()
            assertThat(restored.type).isEqualTo(FeedbackType.Success)
            assertThat(restored.message).isEqualTo("Sub restored")
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { subRepo.delete(12L) }
        coVerify(exactly = 1) { subRepo.upsert(match { it.id == 0L && it.name == "iCloud" }) }
        collector.cancel()
    }
}
