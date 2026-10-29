package com.notepay.ui.feature.stats

import android.content.Context
import com.notepay.MainDispatcherRule
import com.notepay.ai.OnDeviceBudgetAdvisor
import com.notepay.data.datastore.AdvisorFeedbackStore
import com.notepay.domain.analytics.StatsRange
import com.notepay.domain.repository.SubscriptionRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import app.cash.turbine.test
import com.notepay.feature.stats.main.StatsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StatsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun testSelectRangeUpdatesState() = runTest {
        val transactionRepo = mockk<TransactionRepository> {
            every { observeAll() } returns flowOf(emptyList())
        }
        val walletRepo = mockk<WalletRepository> {
            every { observeAll() } returns flowOf(emptyList())
        }
        val subscriptionRepo = mockk<SubscriptionRepository> {
            every { observeAll() } returns flowOf(emptyList())
        }
        val advisorFeedbackStore = mockk<AdvisorFeedbackStore> {
            every { feedbacks } returns flowOf(emptyMap())
        }
        val context = mockk<Context> {
            // Tên danh mục cho trợ lý AI / phát hiện định kỳ:
            every { getString(any(), *anyVararg()) } returns ""
            every { getString(any()) } returns ""
        }
        val budgetAdvisor = mockk<OnDeviceBudgetAdvisor>(relaxed = true)

        val viewModel = StatsViewModel(
            transactionRepo = transactionRepo,
            walletRepo = walletRepo,
            subscriptionRepo = subscriptionRepo,
            advisorFeedbackStore = advisorFeedbackStore,
            context = context,
            budgetAdvisor = budgetAdvisor
        )

        viewModel.state.test {
            var item = awaitItem()
            
            viewModel.selectRange(StatsRange.WEEK)
            while (item.currentPeriod?.range != StatsRange.WEEK) {
                item = awaitItem()
            }
            assertEquals(StatsRange.WEEK, item.currentPeriod?.range)

            viewModel.selectRange(StatsRange.ALL)
            while (item.currentPeriod?.range != StatsRange.ALL) {
                item = awaitItem()
            }
            assertEquals(StatsRange.ALL, item.currentPeriod?.range)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
}
