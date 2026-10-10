package com.notepay.domain.analytics

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertThrows
import org.junit.Test

class StatsPeriodTest {

    @Test
    fun `StatsPeriod invariant enforces start and endExclusive consistency`() {
        // start >= endExclusive phải ném ngoại lệ
        assertThrows(IllegalArgumentException::class.java) {
            StatsPeriod(StatsRange.MONTH, LocalDate(2026, 5, 10), LocalDate(2026, 5, 10))
        }
        assertThrows(IllegalArgumentException::class.java) {
            StatsPeriod(StatsRange.MONTH, LocalDate(2026, 5, 10), LocalDate(2026, 5, 5))
        }
        // Một bên null một bên có giá trị phải ném ngoại lệ
        assertThrows(IllegalArgumentException::class.java) {
            StatsPeriod(StatsRange.ALL, LocalDate(2026, 5, 1), null)
        }
    }

    @Test
    fun `periodFor CUSTOM throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            periodFor(LocalDate(2026, 5, 1), StatsRange.CUSTOM)
        }
    }

    @Test
    fun `customPeriod normalizes reversed start and end dates`() {
        val start = LocalDate(2026, 10, 15)
        val end = LocalDate(2026, 10, 5)
        val period = customPeriod(start, end)

        assertThat(period.range).isEqualTo(StatsRange.CUSTOM)
        assertThat(period.start).isEqualTo(LocalDate(2026, 10, 5))
        assertThat(period.endExclusive).isEqualTo(LocalDate(2026, 10, 16))
    }

    @Test
    fun `periodFor WEEK handles week spanning across year boundary`() {
        // Ngày 1/1/2026 là thứ Năm -> Tuần bắt đầu từ thứ Hai (29/12/2025) đến thứ Hai (5/1/2026)
        val anchor = LocalDate(2026, 1, 1)
        val period = periodFor(anchor, StatsRange.WEEK)

        assertThat(period.range).isEqualTo(StatsRange.WEEK)
        assertThat(period.start).isEqualTo(LocalDate(2025, 12, 29))
        assertThat(period.endExclusive).isEqualTo(LocalDate(2026, 1, 5))

        // Lùi 1 tuần về trước (22/12/2025 - 29/12/2025)
        val prevWeek = period.shift(-1)
        assertThat(prevWeek?.start).isEqualTo(LocalDate(2025, 12, 22))
        assertThat(prevWeek?.endExclusive).isEqualTo(LocalDate(2025, 12, 29))

        // Tiến 1 tuần về sau (5/1/2026 - 12/1/2026)
        val nextWeek = period.shift(1)
        assertThat(nextWeek?.start).isEqualTo(LocalDate(2026, 1, 5))
        assertThat(nextWeek?.endExclusive).isEqualTo(LocalDate(2026, 1, 12))

        // Shift 52 tuần vượt mốc năm
        val nextYearWeek = period.shift(52)
        assertThat(nextYearWeek?.start).isEqualTo(LocalDate(2026, 12, 28))
    }

    @Test
    fun `shift handles multi-month shifts across year boundary`() {
        // Tháng 3/2026 shift(-13) -> Tháng 2/2025
        val marchPeriod = periodFor(LocalDate(2026, 3, 15), StatsRange.MONTH)
        val shifted = marchPeriod.shift(-13)

        assertThat(shifted?.start).isEqualTo(LocalDate(2025, 2, 1))
        assertThat(shifted?.endExclusive).isEqualTo(LocalDate(2025, 3, 1))
    }

    @Test
    fun `periodFor MONTH handles transition from December to January and vice versa`() {
        val decPeriod = periodFor(LocalDate(2025, 12, 15), StatsRange.MONTH)
        assertThat(decPeriod.start).isEqualTo(LocalDate(2025, 12, 1))
        assertThat(decPeriod.endExclusive).isEqualTo(LocalDate(2026, 1, 1))

        val janPeriod = decPeriod.shift(1)
        assertThat(janPeriod?.start).isEqualTo(LocalDate(2026, 1, 1))
        assertThat(janPeriod?.endExclusive).isEqualTo(LocalDate(2026, 2, 1))

        val shiftedBack = janPeriod?.shift(-1)
        assertThat(shiftedBack?.start).isEqualTo(LocalDate(2025, 12, 1))
        assertThat(shiftedBack?.endExclusive).isEqualTo(LocalDate(2026, 1, 1))
    }

    @Test
    fun `periodFor MONTH handles leap year and non-leap year February`() {
        val leapFeb = periodFor(LocalDate(2024, 2, 10), StatsRange.MONTH)
        assertThat(leapFeb.start).isEqualTo(LocalDate(2024, 2, 1))
        assertThat(leapFeb.endExclusive).isEqualTo(LocalDate(2024, 3, 1))
        assertThat(leapFeb.contains(LocalDate(2024, 2, 29))).isTrue()
        assertThat(leapFeb.contains(LocalDate(2024, 3, 1))).isFalse()

        val nonLeapFeb = periodFor(LocalDate(2025, 2, 10), StatsRange.MONTH)
        assertThat(nonLeapFeb.start).isEqualTo(LocalDate(2025, 2, 1))
        assertThat(nonLeapFeb.endExclusive).isEqualTo(LocalDate(2025, 3, 1))
        assertThat(nonLeapFeb.contains(LocalDate(2025, 2, 28))).isTrue()
        assertThat(nonLeapFeb.contains(LocalDate(2025, 3, 1))).isFalse()
    }

    @Test
    fun `comparablePreviousRange handles day 31 compared to 28-day February`() {
        val marchPeriod = periodFor(LocalDate(2025, 3, 15), StatsRange.MONTH)
        val today = LocalDate(2025, 3, 31)

        val prevRange = comparablePreviousRange(marchPeriod, today)
        assertThat(prevRange).isNotNull()
        assertThat(prevRange?.start).isEqualTo(LocalDate(2025, 2, 1))
        assertThat(prevRange?.endExclusive).isEqualTo(LocalDate(2025, 3, 1))
    }

    @Test
    fun `comparablePreviousRange handles day 31 compared to 30-day month`() {
        val mayPeriod = periodFor(LocalDate(2025, 5, 10), StatsRange.MONTH)
        val today = LocalDate(2025, 5, 31)

        val prevRange = comparablePreviousRange(mayPeriod, today)
        assertThat(prevRange).isNotNull()
        assertThat(prevRange?.start).isEqualTo(LocalDate(2025, 4, 1))
        assertThat(prevRange?.endExclusive).isEqualTo(LocalDate(2025, 5, 1))
    }

    @Test
    fun `comparablePreviousRange slices partial month when current period is ongoing`() {
        val octPeriod = periodFor(LocalDate(2026, 10, 1), StatsRange.MONTH)
        val today = LocalDate(2026, 10, 7)

        val prevRange = comparablePreviousRange(octPeriod, today)
        assertThat(prevRange).isNotNull()
        assertThat(prevRange?.start).isEqualTo(LocalDate(2026, 9, 1))
        assertThat(prevRange?.endExclusive).isEqualTo(LocalDate(2026, 9, 8))
    }

    @Test
    fun `comparablePreviousRange uses full duration when viewing completed past period`() {
        val augPeriod = periodFor(LocalDate(2026, 8, 1), StatsRange.MONTH)
        val today = LocalDate(2026, 10, 7)

        val prevRange = comparablePreviousRange(augPeriod, today)
        assertThat(prevRange).isNotNull()
        assertThat(prevRange?.start).isEqualTo(LocalDate(2026, 7, 1))
        assertThat(prevRange?.endExclusive).isEqualTo(LocalDate(2026, 8, 1))
    }

    @Test
    fun `comparablePreviousRange returns full previous range if today is before period`() {
        // Xem kỳ tương lai tháng 12/2026 khi hôm nay là 7/10/2026
        val futurePeriod = periodFor(LocalDate(2026, 12, 1), StatsRange.MONTH)
        val today = LocalDate(2026, 10, 7)

        val prevRange = comparablePreviousRange(futurePeriod, today)
        assertThat(prevRange).isNotNull()
        assertThat(prevRange?.start).isEqualTo(LocalDate(2026, 11, 1))
        assertThat(prevRange?.endExclusive).isEqualTo(LocalDate(2026, 12, 1))
    }

    @Test
    fun `shift and comparablePreviousRange return null for ALL and CUSTOM`() {
        val allPeriod = periodFor(LocalDate(2026, 10, 7), StatsRange.ALL)
        assertThat(allPeriod.start).isNull()
        assertThat(allPeriod.endExclusive).isNull()
        assertThat(allPeriod.shift(1)).isNull()
        assertThat(allPeriod.shift(-1)).isNull()
        assertThat(comparablePreviousRange(allPeriod, LocalDate(2026, 10, 7))).isNull()

        val custom = customPeriod(LocalDate(2026, 10, 1), LocalDate(2026, 10, 15))
        assertThat(custom.range).isEqualTo(StatsRange.CUSTOM)
        assertThat(custom.start).isEqualTo(LocalDate(2026, 10, 1))
        assertThat(custom.endExclusive).isEqualTo(LocalDate(2026, 10, 16))
        assertThat(custom.shift(1)).isNull()
        assertThat(custom.shift(-1)).isNull()
        assertThat(comparablePreviousRange(custom, LocalDate(2026, 10, 7))).isNull()
    }

    @Test
    fun `contains operator accurately validates dates`() {
        val period = periodFor(LocalDate(2026, 10, 7), StatsRange.MONTH)
        assertThat(period.contains(LocalDate(2026, 9, 30))).isFalse()
        assertThat(period.contains(LocalDate(2026, 10, 1))).isTrue()
        assertThat(period.contains(LocalDate(2026, 10, 31))).isTrue()
        assertThat(period.contains(LocalDate(2026, 11, 1))).isFalse()

        val allPeriod = periodFor(LocalDate(2026, 10, 7), StatsRange.ALL)
        assertThat(allPeriod.contains(LocalDate(1990, 1, 1))).isTrue()
        assertThat(allPeriod.contains(LocalDate(2099, 12, 31))).isTrue()
    }

    @Test
    fun `hasComparableData returns true only if first transaction is before prevRange ends`() {
        val prevRange = DateRange(LocalDate(2026, 9, 1), LocalDate(2026, 10, 1))

        // Người dùng đã có giao dịch trong tháng 9 hoặc trước đó -> Có dữ liệu đối chiếu
        assertThat(hasComparableData(prevRange, LocalDate(2026, 9, 15))).isTrue()
        assertThat(hasComparableData(prevRange, LocalDate(2026, 8, 1))).isTrue()

        // Người dùng mới cài app và có giao dịch đầu tiên từ ngày 1/10 trở đi -> Không có dữ liệu tháng 9
        assertThat(hasComparableData(prevRange, LocalDate(2026, 10, 1))).isFalse()
        assertThat(hasComparableData(prevRange, LocalDate(2026, 10, 5))).isFalse()

        // Null checks
        assertThat(hasComparableData(prevRange, null)).isFalse()
        assertThat(hasComparableData(null, LocalDate(2026, 9, 15))).isFalse()
    }
}
