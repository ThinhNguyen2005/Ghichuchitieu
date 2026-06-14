package com.notepay.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.TestTransactionFactory
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetMonthlySummaryUseCaseTest {

    private val transactionRepo = mockk<TransactionRepository>()
    private val useCase = GetMonthlySummaryUseCase(transactionRepo)

    @Test
    fun `empty month returns zero summary`() = runTest {
        every { transactionRepo.observeByMonth(2026, 6) } returns flowOf(emptyList())

        useCase(2026, 6).test {
            val summary = awaitItem()
            assertThat(summary.totalIncome).isEqualTo(Money.ZERO)
            assertThat(summary.totalExpense).isEqualTo(Money.ZERO)
            assertThat(summary.balance).isEqualTo(Money.ZERO)
            assertThat(summary.byCategory).isEmpty()
            assertThat(summary.transactionCount).isEqualTo(0)
            awaitComplete()
        }
    }

    @Test
    fun `mixed transactions are correctly aggregated`() = runTest {
        val txs: List<Transaction> = listOf(
            TestTransactionFactory.income(amount = Money(5_000_000_00)),
            TestTransactionFactory.expense(
                amount = Money(50_000_00),
                category = Category.FOOD,
            ),
            TestTransactionFactory.expense(
                amount = Money(30_000_00),
                category = Category.FOOD,
            ),
            TestTransactionFactory.expense(
                amount = Money(20_000_00),
                category = Category.TRANSPORT,
            ),
        )
        every { transactionRepo.observeByMonth(2026, 6) } returns flowOf(txs)

        useCase(2026, 6).test {
            val s = awaitItem()
            assertThat(s.totalIncome).isEqualTo(Money(5_000_000_00))
            assertThat(s.totalExpense).isEqualTo(Money(100_000_00))
            assertThat(s.balance).isEqualTo(Money(4_900_000_00))
            assertThat(s.byCategory[Category.FOOD]).isEqualTo(Money(80_000_00))
            assertThat(s.byCategory[Category.TRANSPORT]).isEqualTo(Money(20_000_00))
            assertThat(s.transactionCount).isEqualTo(4)
            awaitComplete()
        }
    }

    @Test
    fun `income categories are excluded from byCategory`() = runTest {
        val txs: List<Transaction> = listOf(
            TestTransactionFactory.income(amount = Money(1_000_00), category = Category.SALARY),
            TestTransactionFactory.expense(amount = Money(50_00), category = Category.FOOD),
        )
        every { transactionRepo.observeByMonth(2026, 6) } returns flowOf(txs)

        useCase(2026, 6).test {
            val s = awaitItem()
            assertThat(s.byCategory).doesNotContainKey(Category.SALARY)
            assertThat(s.byCategory).containsKey(Category.FOOD)
            awaitComplete()
        }
    }
}
