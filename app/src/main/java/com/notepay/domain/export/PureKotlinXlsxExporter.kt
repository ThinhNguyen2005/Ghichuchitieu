package com.notepay.domain.export

import com.notepay.domain.model.TransactionType
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pure Kotlin OOXML (.xlsx) workbook writer using standard SpreadsheetML + ZIP.
 * Avoids heavy Apache POI dependencies while producing numeric cells compatible with Excel SUM().
 */
class PureKotlinXlsxExporter {

    fun toByteArray(report: FinancialReportData): ByteArray {
        val baos = ByteArrayOutputStream()
        write(report, baos)
        return baos.toByteArray()
    }

    fun write(report: FinancialReportData, output: OutputStream) {
        ZipOutputStream(output).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", CONTENT_TYPES_XML)
            writeZipEntry(zip, "_rels/.rels", ROOT_RELS_XML)
            writeZipEntry(zip, "xl/workbook.xml", WORKBOOK_XML)
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML)
            writeZipEntry(zip, "xl/styles.xml", STYLES_XML)
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildOverviewSheetXml(report))
            writeZipEntry(zip, "xl/worksheets/sheet2.xml", buildTransactionsSheetXml(report))
            writeZipEntry(zip, "xl/worksheets/sheet3.xml", buildCategoriesSheetXml(report))
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, xmlContent: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(xmlContent.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildOverviewSheetXml(report: FinancialReportData): String {
        val rows = StringBuilder()
        rows.append(xmlRow(1, listOf(strCell(0, 1, "Chi tieu", style = 1), strCell(1, 1, "Gia tri", style = 1))))
        rows.append(xmlRow(2, listOf(strCell(0, 2, "Ky bao cao"), strCell(1, 2, "${report.startDate} - ${report.endDate}"))))
        rows.append(xmlRow(3, listOf(strCell(0, 3, "Tong thu"), numCell(1, 3, report.totalIncomeMajor, style = 2))))
        rows.append(xmlRow(4, listOf(strCell(0, 4, "Tong chi"), numCell(1, 4, report.totalExpenseMajor, style = 2))))
        rows.append(xmlRow(5, listOf(strCell(0, 5, "Loi nhuan rong (Thu - Chi)"), numCell(1, 5, report.netProfitMajor, style = 2))))

        val ratioCell = report.incomeExpenseRatio?.let { ratio ->
            decimalCell(1, 6, ratio, style = 3)
        } ?: strCell(1, 6, "-")
        rows.append(xmlRow(6, listOf(strCell(0, 6, "Ty le Thu / Chi"), ratioCell)))

        val headerRow = 8
        rows.append(
            xmlRow(
                headerRow,
                listOf("Vi", "Muc dich", "Tien te", "Tong thu", "Tong chi", "Rong", "So giao dich")
                    .mapIndexed { idx, label -> strCell(idx, headerRow, label, style = 1) },
            ),
        )

        report.walletBreakdowns.forEachIndexed { offset, w ->
            val r = headerRow + 1 + offset
            rows.append(
                xmlRow(
                    r,
                    listOf(
                        strCell(0, r, w.walletName),
                        strCell(1, r, w.purpose.name),
                        strCell(2, r, w.currencyCode),
                        numCell(3, r, w.totalIncomeMajor, style = 2),
                        numCell(4, r, w.totalExpenseMajor, style = 2),
                        numCell(5, r, w.netMajor, style = 2),
                        numCell(6, r, w.transactionCount.toLong(), style = 0),
                    ),
                ),
            )
        }
        return wrapWorksheetXml(rows.toString())
    }

    private fun buildTransactionsSheetXml(report: FinancialReportData): String {
        val rows = StringBuilder()
        rows.append(
            xmlRow(
                1,
                listOf("Ngay", "Loai", "Danh muc", "Vi", "So tien", "Tien te", "Ghi chu")
                    .mapIndexed { idx, label -> strCell(idx, 1, label, style = 1) },
            ),
        )
        report.rows.forEachIndexed { idx, item ->
            val r = idx + 2
            val typeLabel = if (item.type == TransactionType.INCOME) "Thu" else "Chi"
            rows.append(
                xmlRow(
                    r,
                    listOf(
                        strCell(0, r, item.dateIso),
                        strCell(1, r, typeLabel),
                        strCell(2, r, item.categoryName),
                        strCell(3, r, item.walletName),
                        numCell(4, r, item.amountMajor, style = 2),
                        strCell(5, r, item.currencyCode),
                        strCell(6, r, item.note),
                    ),
                ),
            )
        }
        return wrapWorksheetXml(rows.toString())
    }

    private fun buildCategoriesSheetXml(report: FinancialReportData): String {
        val rows = StringBuilder()
        rows.append(
            xmlRow(
                1,
                listOf("Danh muc", "Loai", "So giao dich", "Tong tien", "Ty trong (%)")
                    .mapIndexed { idx, label -> strCell(idx, 1, label, style = 1) },
            ),
        )
        report.categoryBreakdowns.forEachIndexed { idx, cat ->
            val r = idx + 2
            val typeLabel = if (cat.type == TransactionType.INCOME) "Thu" else "Chi"
            rows.append(
                xmlRow(
                    r,
                    listOf(
                        strCell(0, r, cat.categoryName),
                        strCell(1, r, typeLabel),
                        numCell(2, r, cat.transactionCount.toLong(), style = 0),
                        numCell(3, r, cat.totalMajor, style = 2),
                        decimalCell(4, r, cat.shareRatio, style = 3),
                    ),
                ),
            )
        }
        return wrapWorksheetXml(rows.toString())
    }

    private fun wrapWorksheetXml(sheetDataRows: String): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>$sheetDataRows</sheetData>
</worksheet>"""

    private fun xmlRow(rowNum: Int, cells: List<String>): String =
        "<row r=\"$rowNum\">${cells.joinToString("")}</row>"

    private fun strCell(col: Int, row: Int, value: String, style: Int = 0): String {
        val ref = cellRef(col, row)
        val safe = escapeXml(sanitizeXmlChars(value))
        return "<c r=\"$ref\" s=\"$style\" t=\"inlineStr\"><is><t xml:space=\"preserve\">$safe</t></is></c>"
    }

    private fun numCell(col: Int, row: Int, value: Long, style: Int): String =
        "<c r=\"${cellRef(col, row)}\" s=\"$style\" t=\"n\"><v>$value</v></c>"

    private fun decimalCell(col: Int, row: Int, value: Double, style: Int): String {
        val safeVal = if (value.isFinite()) String.format(Locale.ROOT, "%.4f", value) else "0"
        return "<c r=\"${cellRef(col, row)}\" s=\"$style\" t=\"n\"><v>$safeVal</v></c>"
    }

    private fun cellRef(colZeroBased: Int, rowOneBased: Int): String {
        var n = colZeroBased
        val colName = StringBuilder()
        while (n >= 0) {
            colName.insert(0, ('A' + (n % 26)))
            n = n / 26 - 1
        }
        return "$colName$rowOneBased"
    }

    private fun sanitizeXmlChars(text: String): String = buildString(text.length) {
        for (ch in text) {
            val valid = ch == '\t' || ch == '\n' || ch == '\r' || ch.code >= 0x20
            if (valid) append(ch)
        }
    }

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private companion object {
        const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

        const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        const val WORKBOOK_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Tong_quan" sheetId="1" r:id="rId1"/>
    <sheet name="Chi_tiet_giao_dich" sheetId="2" r:id="rId2"/>
    <sheet name="Theo_danh_muc" sheetId="3" r:id="rId3"/>
  </sheets>
</workbook>"""

        const val WORKBOOK_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

        const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="4">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="3" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
    <xf numFmtId="10" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
  </cellXfs>
</styleSheet>"""
    }
}
