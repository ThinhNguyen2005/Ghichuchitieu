package com.notepay.domain.export

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.model.WalletPurpose
import com.notepay.domain.money.Money
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.time.Instant

class PureKotlinXlsxExporterTest {

    private fun atUtc(year: Int, month: Int, day: Int, hour: Int = 10): Instant =
        LocalDateTime(LocalDate(year, month, day), LocalTime(hour, 0)).toInstant(TimeZone.UTC)

    @Test
    fun `generates valid zip xlsx with 3 worksheets accurate totals and numeric SUM cells`() {
        val personal = Wallet(
            id = 1L,
            name = "Cá nhân",
            initialBalance = Money.ZERO,
            iconKey = "cash",
            colorKey = "primary",
            purpose = WalletPurpose.PERSONAL,
        )
        val business = Wallet(
            id = 2L,
            name = "Shop & Online <VIP>",
            initialBalance = Money.ZERO,
            iconKey = "business",
            colorKey = "secondary",
            purpose = WalletPurpose.BUSINESS,
            includeInTotalBalance = false,
        )

        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = Money.fromMajorUnit(5_000_000L),
                type = TransactionType.INCOME,
                category = Category.SALARY,
                note = "Khách thanh toán đơn #101 & ship",
                occurredAt = atUtc(2026, 10, 5),
                walletId = 2L,
            ),
            Transaction(
                id = 2L,
                amount = Money.fromMajorUnit(1_200_000L),
                type = TransactionType.EXPENSE,
                category = Category.SHOPPING,
                note = "Nhập hàng đợt 1",
                occurredAt = atUtc(2026, 10, 6),
                walletId = 2L,
            ),
            Transaction(
                id = 3L,
                amount = Money.fromMajorUnit(300_000L),
                type = TransactionType.EXPENSE,
                category = Category.FOOD,
                note = "Bữa trưa đội ngũ",
                occurredAt = atUtc(2026, 10, 7),
                walletId = 2L,
            ),
            // Internal transfer MUST be ignored from totals and transaction sheet
            Transaction(
                id = 4L,
                amount = Money.fromMajorUnit(2_000_000L),
                type = TransactionType.EXPENSE,
                category = Category.OTHER,
                note = "Chuyển về cá nhân",
                occurredAt = atUtc(2026, 10, 8),
                walletId = 2L,
                isInternalTransfer = true,
            ),
            // Personal wallet transaction outside business wallet filter
            Transaction(
                id = 5L,
                amount = Money.fromMajorUnit(50_000L),
                type = TransactionType.EXPENSE,
                category = Category.FOOD,
                note = "Ăn sáng",
                occurredAt = atUtc(2026, 10, 9),
                walletId = 1L,
            ),
        )

        val report = FinancialReportBuilder().build(
            transactions = transactions,
            wallets = listOf(personal, business),
            filter = FinancialReportFilter(
                startDate = LocalDate(2026, 10, 1),
                endDate = LocalDate(2026, 10, 31),
                walletIds = setOf(2L),
                typeFilter = ExportTypeFilter.ALL,
            ),
            zone = TimeZone.UTC,
        )

        val xlsxBytes = PureKotlinXlsxExporter().toByteArray(report)
        val entries = unzipEntries(xlsxBytes)

        assertThat(entries.keys).containsAtLeast(
            "[Content_Types].xml",
            "_rels/.rels",
            "xl/workbook.xml",
            "xl/_rels/workbook.xml.rels",
            "xl/styles.xml",
            "xl/worksheets/sheet1.xml",
            "xl/worksheets/sheet2.xml",
            "xl/worksheets/sheet3.xml",
        )

        val workbookDoc = parseXml(entries.getValue("xl/workbook.xml"))
        val sheetNodes = workbookDoc.getElementsByTagName("sheet")
        val sheetNames = (0 until sheetNodes.length).map {
            (sheetNodes.item(it) as Element).getAttribute("name")
        }
        assertThat(sheetNames).containsExactly("Tong_quan", "Chi_tiet_giao_dich", "Theo_danh_muc").inOrder()

        val sheet1Doc = parseXml(entries.getValue("xl/worksheets/sheet1.xml"))
        val sheet1NumericValues = extractNumericCells(sheet1Doc)
        assertThat(sheet1NumericValues["B3"]).isEqualTo("5000000")
        assertThat(sheet1NumericValues["B4"]).isEqualTo("1500000")
        assertThat(sheet1NumericValues["B5"]).isEqualTo("3500000")

        val sheet2Doc = parseXml(entries.getValue("xl/worksheets/sheet2.xml"))
        val sheet2Rows = sheet2Doc.getElementsByTagName("row")
        // 1 header row + 3 non-transfer transactions for wallet 2L
        assertThat(sheet2Rows.length).isEqualTo(4)

        val sheet2Numeric = extractNumericCells(sheet2Doc)
        // Newest first (tx 3 = 300000, tx 2 = 1200000, tx 1 = 5000000)
        assertThat(sheet2Numeric["E2"]).isEqualTo("300000")
        assertThat(sheet2Numeric["E3"]).isEqualTo("1200000")
        assertThat(sheet2Numeric["E4"]).isEqualTo("5000000")

        val sheet2XmlText = entries.getValue("xl/worksheets/sheet2.xml").toString(Charsets.UTF_8)
        assertThat(sheet2XmlText).contains("Khách thanh toán đơn #101 &amp; ship")
        assertThat(sheet2XmlText).contains("Shop &amp; Online &lt;VIP&gt;")

        val sheet3Doc = parseXml(entries.getValue("xl/worksheets/sheet3.xml"))
        assertThat(sheet3Doc.getElementsByTagName("row").length).isEqualTo(4) // header + 3 categories
        val sheet3Numeric = extractNumericCells(sheet3Doc)
        assertThat(sheet3Numeric["D2"]).isEqualTo("5000000")
    }

    private fun unzipEntries(bytes: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                result[entry.name] = zis.readBytes()
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return result
    }

    private fun parseXml(bytes: ByteArray) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(bytes))

    private fun extractNumericCells(doc: org.w3c.dom.Document): Map<String, String> {
        val cells = doc.getElementsByTagName("c")
        val map = linkedMapOf<String, String>()
        for (i in 0 until cells.length) {
            val el = cells.item(i) as Element
            if (el.getAttribute("t") == "n") {
                val ref = el.getAttribute("r")
                val v = el.getElementsByTagName("v").item(0)?.textContent
                if (v != null) map[ref] = v
            }
        }
        return map
    }
}
