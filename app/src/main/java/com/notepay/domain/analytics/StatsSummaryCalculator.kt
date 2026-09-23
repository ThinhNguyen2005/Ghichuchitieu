package com.notepay.domain.analytics

import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType

data class CategorySummary(
    val category: Category,
    val amountInCents: Long,
    val share: Float,
)

data class StatsSummaryResult(
    val totalIncomeInCents: Long,
    val totalExpenseInCents: Long,
    val expenseBreakdown: List<CategorySummary>,
    val incomeBreakdown: List<CategorySummary>,
)

object StatsSummaryCalculator {
    fun summarize(transactions: List<Transaction>): StatsSummaryResult {
        val income = transactions.filter { it.type == TransactionType.INCOME }
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalIncome = income.sumOf { it.amount.amountInCents }
        val totalExpense = expenses.sumOf { it.amount.amountInCents }
        return StatsSummaryResult(
            totalIncomeInCents = totalIncome,
            totalExpenseInCents = totalExpense,
            expenseBreakdown = summarizeCategories(expenses, totalExpense),
            incomeBreakdown = summarizeCategories(income, totalIncome),
        )
    }

    private fun summarizeCategories(
        transactions: List<Transaction>,
        totalInCents: Long,
    ): List<CategorySummary> {
        return transactions
            .groupBy { it.category }
            .map { (category, categoryTransactions) ->
                val amount = categoryTransactions.sumOf { it.amount.amountInCents }
                CategorySummary(
                    category = category,
                    amountInCents = amount,
                    share = if (totalInCents > 0L) amount.toFloat() / totalInCents else 0f,
                )
            }
            .sortedByDescending { it.amountInCents }
    }
}
