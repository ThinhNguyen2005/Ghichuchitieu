package com.notepay.domain.export

import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.model.WalletPurpose
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

class FinancialReportBuilder {

    fun build(
        transactions: List<Transaction>,
        wallets: List<Wallet>,
        filter: FinancialReportFilter,
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): FinancialReportData {
        val walletMap = wallets.associateBy { it.id }

        val filtered = transactions
            .filter { tx -> !tx.isInternalTransfer }
            .filter { tx ->
                val date = tx.occurredAt.toLocalDateTime(zone).date
                date >= filter.startDate && date <= filter.endDate
            }
            .filter { tx ->
                filter.walletIds.isEmpty() || tx.walletId in filter.walletIds
            }
            .filter { tx ->
                when (filter.typeFilter) {
                    ExportTypeFilter.ALL -> true
                    ExportTypeFilter.INCOME -> tx.type == TransactionType.INCOME
                    ExportTypeFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
                }
            }
            .sortedByDescending { it.occurredAt }

        val totalIncomeMajor = filtered
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount.toMajorUnitLong() }
        val totalExpenseMajor = filtered
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount.toMajorUnitLong() }
        val netProfitMajor = totalIncomeMajor - totalExpenseMajor
        val ratio = if (totalExpenseMajor > 0L) {
            totalIncomeMajor.toDouble() / totalExpenseMajor.toDouble()
        } else {
            null
        }

        val rows = filtered.map { tx ->
            val date = tx.occurredAt.toLocalDateTime(zone).date
            val wallet = walletMap[tx.walletId]
            ReportTransactionRow(
                dateIso = formatIsoDate(date),
                type = tx.type,
                categoryName = tx.category.displayName,
                walletName = wallet?.name.orEmpty(),
                amountMajor = tx.amount.toMajorUnitLong(),
                currencyCode = wallet?.currencyCode ?: "VND",
                note = tx.note.trim(),
            )
        }

        val walletBreakdowns = buildWalletBreakdowns(filtered, walletMap)
        val categoryBreakdowns = buildCategoryBreakdowns(filtered, totalIncomeMajor, totalExpenseMajor)

        return FinancialReportData(
            startDate = filter.startDate,
            endDate = filter.endDate,
            totalIncomeMajor = totalIncomeMajor,
            totalExpenseMajor = totalExpenseMajor,
            netProfitMajor = netProfitMajor,
            incomeExpenseRatio = ratio,
            walletBreakdowns = walletBreakdowns,
            rows = rows,
            categoryBreakdowns = categoryBreakdowns,
        )
    }

    private fun buildWalletBreakdowns(
        filtered: List<Transaction>,
        walletMap: Map<Long, Wallet>,
    ): List<WalletReportBreakdown> = filtered
        .groupBy { it.walletId }
        .entries
        .sortedBy { (walletId, _) -> walletId }
        .map { (walletId, txs) ->
            val wallet = walletMap[walletId]
            val income = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.toMajorUnitLong() }
            val expense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.toMajorUnitLong() }
            WalletReportBreakdown(
                walletId = walletId,
                walletName = wallet?.name ?: "#$walletId",
                purpose = wallet?.purpose ?: WalletPurpose.PERSONAL,
                currencyCode = wallet?.currencyCode ?: "VND",
                totalIncomeMajor = income,
                totalExpenseMajor = expense,
                netMajor = income - expense,
                transactionCount = txs.size,
            )
        }

    private fun buildCategoryBreakdowns(
        filtered: List<Transaction>,
        totalIncomeMajor: Long,
        totalExpenseMajor: Long,
    ): List<CategoryReportBreakdown> = filtered
        .groupBy { it.category.id to it.type }
        .entries
        .map { (_, txs) ->
            val sample = txs.first()
            val total = txs.sumOf { it.amount.toMajorUnitLong() }
            val denominator = if (sample.type == TransactionType.INCOME) totalIncomeMajor else totalExpenseMajor
            val share = if (denominator > 0L) total.toDouble() / denominator.toDouble() else 0.0
            CategoryReportBreakdown(
                categoryId = sample.category.id,
                categoryName = sample.category.displayName,
                type = sample.type,
                transactionCount = txs.size,
                totalMajor = total,
                shareRatio = share,
            )
        }
        .sortedByDescending { it.totalMajor }

    private fun formatIsoDate(date: LocalDate): String =
        String.format(Locale.ROOT, "%04d-%02d-%02d", date.year, date.month.number, date.day)
}
