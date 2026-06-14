package com.notepay.domain.usecase

import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Tổng hợp thu/chi theo tháng, dùng cho Home + Stats screen.
 *
 * month: 1..12 (1-indexed cho khớp Calendar/DatePicker)
 */
class GetMonthlySummaryUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
) {
    data class Summary(
        val year: Int,
        val month: Int,
        val totalIncome: Money,
        val totalExpense: Money,
        val balance: Money,
        val byCategory: Map<Category, Money>,
        val byIncomeCategory: Map<Category, Money>,
        val transactions: List<Transaction>,
    ) {
        val transactionCount: Int get() = transactions.size
    }

    operator fun invoke(year: Int, month: Int): Flow<Summary> =
        transactionRepo.observeByMonth(year, month).map { txs ->
            val income = txs.asSequence()
                .filter { it.type == TransactionType.INCOME }
                .fold(Money.ZERO) { acc, t -> acc + t.amount }
            val expense = txs.asSequence()
                .filter { it.type == TransactionType.EXPENSE }
                .fold(Money.ZERO) { acc, t -> acc + t.amount }
            val byCategory = txs.asSequence()
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { (_, list) ->
                    list.fold(Money.ZERO) { acc, t -> acc + t.amount }
                }
            val byIncomeCategory = txs.asSequence()
                .filter { it.type == TransactionType.INCOME }
                .groupBy { it.category }
                .mapValues { (_, list) ->
                    list.fold(Money.ZERO) { acc, t -> acc + t.amount }
                }
            Summary(
                year = year,
                month = month,
                totalIncome = income,
                totalExpense = expense,
                balance = income - expense,
                byCategory = byCategory,
                byIncomeCategory = byIncomeCategory,
                transactions = txs,
            )
        }
}
