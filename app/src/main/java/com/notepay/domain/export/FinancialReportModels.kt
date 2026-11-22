package com.notepay.domain.export

import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.WalletPurpose
import kotlinx.datetime.LocalDate

enum class ExportFormat {
    XLSX,
    CSV,
}

enum class ExportTypeFilter {
    ALL,
    INCOME,
    EXPENSE,
}

enum class ExportDatePreset {
    THIS_MONTH,
    THIS_QUARTER,
    THIS_YEAR,
    CUSTOM,
}

data class FinancialReportFilter(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val walletIds: Set<Long> = emptySet(),
    val typeFilter: ExportTypeFilter = ExportTypeFilter.ALL,
)

data class WalletReportBreakdown(
    val walletId: Long,
    val walletName: String,
    val purpose: WalletPurpose,
    val currencyCode: String,
    val totalIncomeMajor: Long,
    val totalExpenseMajor: Long,
    val netMajor: Long,
    val transactionCount: Int,
)

data class CategoryReportBreakdown(
    val categoryId: String,
    val categoryName: String,
    val type: TransactionType,
    val transactionCount: Int,
    val totalMajor: Long,
    val shareRatio: Double,
)

data class ReportTransactionRow(
    val dateIso: String,
    val type: TransactionType,
    val categoryName: String,
    val walletName: String,
    val amountMajor: Long,
    val currencyCode: String,
    val note: String,
)

data class FinancialReportData(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalIncomeMajor: Long,
    val totalExpenseMajor: Long,
    val netProfitMajor: Long,
    val incomeExpenseRatio: Double?,
    val walletBreakdowns: List<WalletReportBreakdown>,
    val rows: List<ReportTransactionRow>,
    val categoryBreakdowns: List<CategoryReportBreakdown>,
)
