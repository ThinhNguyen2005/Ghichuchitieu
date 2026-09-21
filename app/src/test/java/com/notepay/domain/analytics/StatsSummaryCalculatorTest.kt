package com.notepay.domain.analytics

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.TestData
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType
import org.junit.Test

class StatsSummaryCalculatorTest {

    @Test
    fun `summarize separates income and expense categories with percentages`() {
        val transactions = listOf(
            TestData.transaction(amount = Money(1_000L), type = TransactionType.INCOME, category = Category.SALARY),
            TestData.transaction(amount = Money(700L), type = TransactionType.EXPENSE, category = Category.FOOD),
            TestData.transaction(amount = Money(300L), type = TransactionType.EXPENSE, category = Category.SHOPPING),
        )

        val summary = StatsSummaryCalculator.summarize(transactions)

        assertThat(summary.totalIncomeInCents).isEqualTo(1_000L)
        assertThat(summary.totalExpenseInCents).isEqualTo(1_000L)
        assertThat(summary.expenseBreakdown.map { it.category }).containsExactly(Category.FOOD, Category.SHOPPING).inOrder()
        assertThat(summary.expenseBreakdown[0].share).isEqualTo(0.7f)
        assertThat(summary.incomeBreakdown.single().share).isEqualTo(1f)
    }

    @Test
    fun `summarize returns empty breakdown percentages when total is zero`() {
        val summary = StatsSummaryCalculator.summarize(emptyList())

        assertThat(summary.totalIncomeInCents).isEqualTo(0L)
        assertThat(summary.totalExpenseInCents).isEqualTo(0L)
        assertThat(summary.expenseBreakdown).isEmpty()
        assertThat(summary.incomeBreakdown).isEmpty()
    }
}
