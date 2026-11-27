package com.notepay.feature.stats.main

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.ai.OnDeviceBudgetAdvisor
import com.notepay.data.datastore.AdvisorFeedbackStore
import com.notepay.domain.TestData
import com.notepay.domain.analytics.AdvisorAvailability
import com.notepay.domain.analytics.PeriodBarBucketKind
import com.notepay.domain.analytics.StatsRange
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.domain.repository.SubscriptionRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `state populates periodExpenseBars and filters dynamically when category is selected`() = runTest {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val wallet = TestData.wallet(id = 1L)
        val foodTx = TestData.transaction(
            id = 1L,
            amount = Money(90_000_00L),
            type = TransactionType.EXPENSE,
            category = Category.FOOD,
            occurredAt = now,
            walletId = 1L,
        )
        val coffeeTx = TestData.transaction(
            id = 2L,
            amount = Money(35_000_00L),
            type = TransactionType.EXPENSE,
            category = Category.COFFEE,
            occurredAt = now,
            walletId = 1L,
        )
        val salaryTx = TestData.transaction(
            id = 3L,
            amount = Money(5_000_000_00L),
            type = TransactionType.INCOME,
            category = Category.SALARY,
            occurredAt = now,
            walletId = 1L,
        )

        val txRepo = mockk<TransactionRepository> {
            every { observeAll() } returns flowOf(listOf(foodTx, coffeeTx, salaryTx))
        }
        val walletRepo = mockk<WalletRepository> {
            every { observeAll() } returns flowOf(listOf(wallet))
        }
        val subRepo = mockk<SubscriptionRepository> {
            every { observeAll() } returns flowOf(emptyList())
        }
        val feedbackStore = mockk<AdvisorFeedbackStore> {
            every { feedbacks } returns flowOf(emptyMap())
        }
        val budgetAdvisor = mockk<OnDeviceBudgetAdvisor> {
            coEvery { availability() } returns AdvisorAvailability.STATISTICAL_ONLY
        }
        val context = mockk<Context>(relaxed = true)

        val vm = StatsViewModel(
            transactionRepo = txRepo,
            walletRepo = walletRepo,
            subscriptionRepo = subRepo,
            advisorFeedbackStore = feedbackStore,
            context = context,
            budgetAdvisor = budgetAdvisor,
        )

        vm.state.test {
            var current = awaitItem()
            while (current.isLoading) {
                current = awaitItem()
            }

            val todayIndex = today.day - 1
            assertThat(current.periodExpenseBars).isNotEmpty()
            assertThat(current.periodExpenseBars[todayIndex].bucketKind).isEqualTo(PeriodBarBucketKind.DAY_OF_MONTH)
            assertThat(current.periodExpenseBars[todayIndex].amount).isEqualTo(Money(125_000_00L))
            assertThat(current.periodIncomeBars[todayIndex].amount).isEqualTo(Money(5_000_000_00L))

            vm.selectCategory(Category.FOOD)
            var filtered = awaitItem()
            while (filtered.selectedCategory != Category.FOOD) {
                filtered = awaitItem()
            }
            assertThat(filtered.periodExpenseBars[todayIndex].amount).isEqualTo(Money(90_000_00L))

            vm.selectRange(StatsRange.WEEK)
            var weekState = awaitItem()
            while (weekState.currentPeriod?.range != StatsRange.WEEK) {
                weekState = awaitItem()
            }
            assertThat(weekState.periodExpenseBars).hasSize(7)
            assertThat(weekState.periodExpenseBars.map { it.bucketKind }.distinct())
                .containsExactly(PeriodBarBucketKind.WEEKDAY)
            assertThat(weekState.periodExpenseBars.sumOf { it.amount.amountInCents })
                .isEqualTo(125_000_00L)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `walletFilter_alwaysTracksPrimaryActiveWallet_inSingleCurrency`() = runTest {
        val now = Clock.System.now()
        val wallet1 = TestData.wallet(id = 1L, name = "Tiền mặt").copy(isActive = true)
        val wallet2 = TestData.wallet(id = 2L, name = "Ngân hàng").copy(isActive = false)
        val tx1 = TestData.transaction(
            id = 1L,
            amount = Money(90_000_00L),
            type = TransactionType.EXPENSE,
            category = Category.FOOD,
            occurredAt = now,
            walletId = 1L,
        )
        val tx2 = TestData.transaction(
            id = 2L,
            amount = Money(40_000_00L),
            type = TransactionType.EXPENSE,
            category = Category.COFFEE,
            occurredAt = now,
            walletId = 2L,
        )

        val vm = createViewModel(
            transactions = listOf(tx1, tx2),
            wallets = listOf(wallet1, wallet2),
        )

        vm.state.test {
            var current = awaitItem()
            while (current.isLoading) {
                current = awaitItem()
            }

            assertThat(current.selectedWallet?.id).isEqualTo(1L)
            assertThat(current.totalExpense).isEqualTo(Money(90_000_00L))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `walletFilter_autoSelectsPrimaryWallet_whenMultipleCurrencies`() = runTest {
        val now = Clock.System.now()
        val vndWallet = TestData.wallet(id = 1L, name = "Tiền mặt")
        val usdWallet = TestData.wallet(id = 2L, name = "USD Wallet").copy(
            isActive = false,
            currencyCode = "USD",
        )
        val vndTx = TestData.transaction(
            id = 1L,
            amount = Money(90_000_00L),
            type = TransactionType.EXPENSE,
            category = Category.FOOD,
            occurredAt = now,
            walletId = 1L,
        )
        val usdTx = TestData.transaction(
            id = 2L,
            amount = Money(50_00L),
            type = TransactionType.EXPENSE,
            category = Category.COFFEE,
            occurredAt = now,
            walletId = 2L,
        )

        val vm = createViewModel(
            transactions = listOf(vndTx, usdTx),
            wallets = listOf(vndWallet, usdWallet),
        )

        vm.state.test {
            var current = awaitItem()
            while (current.isLoading) {
                current = awaitItem()
            }

            assertThat(current.selectedWallet?.id).isEqualTo(1L)
            assertThat(current.totalExpense).isEqualTo(Money(90_000_00L))

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(
        transactions: List<Transaction>,
        wallets: List<Wallet>,
    ): StatsViewModel {
        val txRepo = mockk<TransactionRepository> {
            every { observeAll() } returns flowOf(transactions)
        }
        val walletRepo = mockk<WalletRepository> {
            every { observeAll() } returns flowOf(wallets)
        }
        val subRepo = mockk<SubscriptionRepository> {
            every { observeAll() } returns flowOf(emptyList())
        }
        val feedbackStore = mockk<AdvisorFeedbackStore> {
            every { feedbacks } returns flowOf(emptyMap())
        }
        val budgetAdvisor = mockk<OnDeviceBudgetAdvisor> {
            coEvery { availability() } returns AdvisorAvailability.STATISTICAL_ONLY
        }
        val context = mockk<Context>(relaxed = true)

        return StatsViewModel(
            transactionRepo = txRepo,
            walletRepo = walletRepo,
            subscriptionRepo = subRepo,
            advisorFeedbackStore = feedbackStore,
            context = context,
            budgetAdvisor = budgetAdvisor,
        )
    }
}
