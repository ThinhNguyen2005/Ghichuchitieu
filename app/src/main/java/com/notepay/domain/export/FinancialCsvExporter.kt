package com.notepay.domain.export

import com.notepay.domain.model.TransactionType

/**
 * Shared CSV utilities with UTF-8 BOM, RFC 4180 escaping, and OWASP CSV Formula Injection protection.
 */
internal fun sanitizeAndEscapeCsvField(value: String): String {
    val firstChar = value.firstOrNull()
    val firstNonSpace = value.trimStart().firstOrNull()
    val isDangerousPrefix = (firstChar != null && firstChar in FORMULA_TRIGGERS) ||
        (firstNonSpace != null && firstNonSpace in FORMULA_TRIMMED_TRIGGERS)

    val guarded = if (isDangerousPrefix) "'$value" else value
    val needsQuoting = guarded.any { it == ',' || it == '"' || it == '\n' || it == '\r' || it == ';' }
    if (!needsQuoting) return guarded
    return '"' + guarded.replace("\"", "\"\"") + '"'
}

private val FORMULA_TRIGGERS = charArrayOf('=', '+', '-', '@', '\t', '\r')
private val FORMULA_TRIMMED_TRIGGERS = charArrayOf('=', '+', '-', '@')

class FinancialCsvExporter {

    fun buildCsvString(
        report: FinancialReportData,
        incomeLabel: String = "Thu",
        expenseLabel: String = "Chi",
    ): String {
        val builder = StringBuilder(UTF8_BOM)
        builder.append("Ngày,Loại,Danh mục,Ví,Số tiền,Tiền tệ,Ghi chú\n")

        report.rows.forEach { row ->
            val typeLabel = if (row.type == TransactionType.INCOME) incomeLabel else expenseLabel
            builder.append(sanitizeAndEscapeCsvField(row.dateIso)).append(',')
            builder.append(sanitizeAndEscapeCsvField(typeLabel)).append(',')
            builder.append(sanitizeAndEscapeCsvField(row.categoryName)).append(',')
            builder.append(sanitizeAndEscapeCsvField(row.walletName)).append(',')
            builder.append(row.amountMajor.toString()).append(',')
            builder.append(sanitizeAndEscapeCsvField(row.currencyCode)).append(',')
            builder.append(sanitizeAndEscapeCsvField(row.note)).append('\n')
        }
        return builder.toString()
    }

    fun toUtf8Bytes(
        report: FinancialReportData,
        incomeLabel: String = "Thu",
        expenseLabel: String = "Chi",
    ): ByteArray = buildCsvString(report, incomeLabel, expenseLabel).toByteArray(Charsets.UTF_8)

    private companion object {
        const val UTF8_BOM = "\uFEFF"
    }
}
