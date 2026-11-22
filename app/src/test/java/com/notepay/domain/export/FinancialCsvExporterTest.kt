package com.notepay.domain.export

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test

class FinancialCsvExporterTest {

    @Test
    fun `writes utf8 bom Vietnamese accents RFC 4180 escapes and neutralizes formula injection`() {
        val wallet = Wallet(
            id = 1L,
            name = "=cmd|' /C calc'!A0",
            initialBalance = Money.ZERO,
            iconKey = "cash",
            colorKey = "primary",
        )
        val txs = listOf(
            Transaction(
                id = 1L,
                amount = Money.fromMajorUnit(75_000L),
                type = TransactionType.EXPENSE,
                category = Category.FOOD,
                note = "Trà sữa, \"đặc biệt\"\nhai ly",
                occurredAt = LocalDateTime(LocalDate(2026, 10, 15), LocalTime(12, 0)).toInstant(TimeZone.UTC),
                walletId = 1L,
            ),
            Transaction(
                id = 2L,
                amount = Money.fromMajorUnit(50_000L),
                type = TransactionType.EXPENSE,
                category = Category.OTHER,
                note = "=SUM(A1:A10)",
                occurredAt = LocalDateTime(LocalDate(2026, 10, 14), LocalTime(9, 0)).toInstant(TimeZone.UTC),
                walletId = 1L,
            ),
            Transaction(
                id = 3L,
                amount = Money.fromMajorUnit(20_000L),
                type = TransactionType.EXPENSE,
                category = Category.OTHER,
                note = "+1+1",
                occurredAt = LocalDateTime(LocalDate(2026, 10, 13), LocalTime(9, 0)).toInstant(TimeZone.UTC),
                walletId = 1L,
            ),
            Transaction(
                id = 4L,
                amount = Money.fromMajorUnit(30_000L),
                type = TransactionType.EXPENSE,
                category = Category.OTHER,
                note = "-2+3@SUM",
                occurredAt = LocalDateTime(LocalDate(2026, 10, 12), LocalTime(9, 0)).toInstant(TimeZone.UTC),
                walletId = 1L,
            ),
            Transaction(
                id = 5L,
                amount = Money.fromMajorUnit(40_000L),
                type = TransactionType.EXPENSE,
                category = Category.OTHER,
                note = "@IMPORTXML()",
                occurredAt = LocalDateTime(LocalDate(2026, 10, 11), LocalTime(9, 0)).toInstant(TimeZone.UTC),
                walletId = 1L,
            ),
        )

        val report = FinancialReportBuilder().build(
            transactions = txs,
            wallets = listOf(wallet),
            filter = FinancialReportFilter(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31)),
            zone = TimeZone.UTC,
        )
        val bytes = FinancialCsvExporter().toUtf8Bytes(report)

        // Exact UTF-8 BOM: EF BB BF
        assertThat(bytes[0]).isEqualTo(0xEF.toByte())
        assertThat(bytes[1]).isEqualTo(0xBB.toByte())
        assertThat(bytes[2]).isEqualTo(0xBF.toByte())

        val text = bytes.toString(Charsets.UTF_8)
        assertThat(text).startsWith("\uFEFFNgày,Loại,Danh mục,Ví,Số tiền,Tiền tệ,Ghi chú\n")
        assertThat(text).contains("\"Trà sữa, \"\"đặc biệt\"\"\nhai ly\"")
        assertThat(text).contains("'=cmd|' /C calc'!A0")
        assertThat(text).contains("'=SUM(A1:A10)")
        assertThat(text).contains("'+1+1")
        assertThat(text).contains("'-2+3@SUM")
        assertThat(text).contains("'@IMPORTXML()")
        assertThat(text).contains(",75000,")
    }
}
