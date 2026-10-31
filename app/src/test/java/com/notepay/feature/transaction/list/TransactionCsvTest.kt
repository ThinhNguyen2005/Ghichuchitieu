package com.notepay.feature.transaction.list

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.TestData
import com.notepay.domain.model.Category
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.time.Instant

class TransactionCsvTest {

    private val header = "Ngày,Giờ,Danh mục,Ví,Loại,Số tiền,Ghi chú"
    private val typeLabels = mapOf(
        TransactionType.EXPENSE to "Chi",
        TransactionType.INCOME to "Thu",
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant =
        LocalDateTime(LocalDate(year, month, day), LocalTime(hour, minute)).toInstant(TimeZone.UTC)

    private fun csvOf(
        transactions: List<com.notepay.domain.model.Transaction>,
        walletsMap: Map<Long, String> = mapOf(1L to "Tiền mặt"),
    ): List<String> = buildTransactionsCsv(
        transactions = transactions,
        walletsMap = walletsMap,
        header = header,
        typeLabels = typeLabels,
        transferLabel = "Chuyển khoản",
        zone = TimeZone.UTC,
    ).trimEnd('\n').split('\n')

    @Test
    fun `writes a utf8 bom, the header and one row per transaction`() {
        // Giờ cố định: dùng TestData.NOW sẽ khiến kỳ vọng phụ thuộc đồng hồ máy chạy test.
        val lines = csvOf(listOf(TestData.transaction(occurredAt = at(2026, 10, 29, 16, 34))))

        assertThat(lines).hasSize(2)
        // Excel chỉ nhận ra tệp là UTF-8 khi có BOM; thiếu nó là mất dấu tiếng Việt.
        assertThat(lines[0]).isEqualTo("\uFEFF$header")
        assertThat(lines[1]).isEqualTo("2026-10-29,16:34,Ăn uống,Tiền mặt,Chi,50000,Cơm trưa")
    }

    @Test
    fun `newest transaction comes first`() {
        val older = TestData.transaction(id = 1, note = "cũ", occurredAt = at(2026, 10, 28, 9, 0))
        val newer = TestData.transaction(id = 2, note = "mới", occurredAt = at(2026, 10, 29, 9, 0))

        val lines = csvOf(listOf(older, newer))

        // Dòng 0 là tiêu đề nên bản ghi mới nhất nằm ở dòng 1.
        assertThat(lines[1]).endsWith(",mới")
        assertThat(lines[2]).endsWith(",cũ")
    }

    @Test
    fun `amount is written in major units without grouping`() {
        val income = TestData.transaction(
            amount = Money(1_234_567_00),
            type = TransactionType.INCOME,
            category = Category.SALARY,
            note = "",
        )

        val lines = csvOf(listOf(income))

        // 1.234.567 ₫ phải thành 1234567 để Excel đọc được thành số, không phải chuỗi.
        assertThat(lines[1]).contains(",1234567,")
        assertThat(lines[1]).contains(",Thu,")
    }

    @Test
    fun `a note containing a comma is quoted so columns stay aligned`() {
        val lines = csvOf(listOf(TestData.transaction(note = "Trà sữa, trân châu")))

        assertThat(lines[1]).endsWith(",\"Trà sữa, trân châu\"")
        // Bảy cột: dấu phẩy trong ghi chú không được làm sinh thêm cột.
        assertThat(splitRespectingQuotes(lines[1])).hasSize(7)
    }

    @Test
    fun `a note containing a double quote has it doubled`() {
        val lines = csvOf(listOf(TestData.transaction(note = "Mua \"đồ\" cũ")))

        assertThat(lines[1]).endsWith(",\"Mua \"\"đồ\"\" cũ\"")
    }

    @Test
    fun `wallet name is resolved from the map and missing ids fall back to blank`() {
        val known = TestData.transaction(id = 1, walletId = 1, note = "a")
        val unknown = TestData.transaction(id = 2, walletId = 99, note = "b")

        val lines = csvOf(listOf(known, unknown))

        assertThat(lines[1]).contains(",Tiền mặt,")
        assertThat(lines[2]).contains(",,")
    }

    @Test
    fun `internal transfers use the transfer label instead of income or expense`() {
        val transfer = TestData.transaction(note = "Nạp ví").copy(isInternalTransfer = true)

        val lines = csvOf(listOf(transfer))

        assertThat(lines[1]).contains(",Chuyển khoản,")
    }

    /** Tách dòng CSV thành từng ô, bỏ qua dấu phẩy nằm trong cặp nháy kép. */
    private fun splitRespectingQuotes(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        fields.add(current.toString())
        return fields
    }
}
