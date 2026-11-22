package com.notepay.domain.natural

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Before
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

class VietnameseDateKeywordParserTest {

    private lateinit var parser: VietnameseDateKeywordParser

    @Before
    fun setUp() {
        val fixedClock = object : Clock {
            override fun now(): Instant = Instant.parse("2026-10-20T05:00:00Z")
        }
        parser = VietnameseDateKeywordParser(fixedClock, TimeZone.of("Asia/Ho_Chi_Minh"))
    }

    @Test
    fun `parseFirst recognizes hom nay`() {
        val match = parser.parseFirst("hôm nay ăn sáng 35k")
        assertThat(match).isNotNull()
        assertThat(match?.date).isEqualTo(LocalDate(2026, 10, 20))
        assertThat(match?.rawText).isEqualTo("hôm nay")
    }

    @Test
    fun `parseFirst recognizes hom qua with and without accents`() {
        assertThat(parser.parseFirst("hôm qua đi grab 45k")?.date).isEqualTo(LocalDate(2026, 10, 19))
        assertThat(parser.parseFirst("hom qua di grab 45k")?.date).isEqualTo(LocalDate(2026, 10, 19))
    }

    @Test
    fun `parseFirst recognizes hom kia and tuan truoc`() {
        assertThat(parser.parseFirst("hôm kia mua đồ 200k")?.date).isEqualTo(LocalDate(2026, 10, 18))
        assertThat(parser.parseFirst("tuần trước đổ xăng 80k")?.date).isEqualTo(LocalDate(2026, 10, 13))
    }

    @Test
    fun `parseFirst resolves thang 8 relative to current clock year`() {
        val match = parser.parseFirst("trả tiền trọ tháng 8 ba triệu rưỡi")
        assertThat(match).isNotNull()
        assertThat(match?.date).isEqualTo(LocalDate(2026, 8, 1))
        assertThat(match?.rawText).isEqualTo("tháng 8")
    }

    @Test
    fun `parseFirst rolls future month without year back to previous year`() {
        val match = parser.parseFirst("đóng tiền bảo hiểm tháng 12")
        assertThat(match?.date).isEqualTo(LocalDate(2025, 12, 1))
    }

    @Test
    fun `parseFirst resolves ngay dd_MM in current year`() {
        val match = parser.parseFirst("ngày 15/10 đi siêu thị 250k")
        assertThat(match).isNotNull()
        assertThat(match?.date).isEqualTo(LocalDate(2026, 10, 15))
        assertThat(match?.rawText).isEqualTo("ngày 15/10")
    }

    @Test
    fun `parseFirst resolves explicit dd_MM_yyyy`() {
        val match = parser.parseFirst("ngày 05/01/2025 mua vé xe 500k")
        assertThat(match?.date).isEqualTo(LocalDate(2025, 1, 5))
    }

    @Test
    fun `parseFirst returns null for invalid calendar dates`() {
        assertThat(parser.parseFirst("ngày 31/02 ăn tối 100k")).isNull()
        assertThat(parser.parseFirst("tháng 13 tiền nhà 3tr")).isNull()
    }
}
