package com.notepay.ui.feature.stats

import android.content.Context
import android.content.SharedPreferences
import com.notepay.MainDispatcherRule
import com.notepay.ai.OnDeviceBudgetAdvisor
import com.notepay.domain.analytics.StatsRange
import com.notepay.domain.repository.SubscriptionRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import app.cash.turbine.test
import io.mockk.coEvery
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
        val sharedPrefs = mockk<SharedPreferences> {
            every { all } returns emptyMap<String, Any>()
        }
        val context = mockk<Context> {
            every { getSharedPreferences(any(), any()) } returns sharedPrefs
            // In case we need strings:
            every { getString(any(), *anyVararg()) } returns ""
            every { getString(any()) } returns ""
        }
        val budgetAdvisor = mockk<OnDeviceBudgetAdvisor>(relaxed = true)

        val viewModel = StatsViewModel(
            transactionRepo = transactionRepo,
            walletRepo = walletRepo,
            subscriptionRepo = subscriptionRepo,
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
