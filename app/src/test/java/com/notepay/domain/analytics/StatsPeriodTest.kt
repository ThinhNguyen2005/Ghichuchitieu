package com.notepay.domain.analytics

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertThrows
import org.junit.Test

class StatsPeriodTest {

    @Test
    fun `StatsPeriod and periodFor reject invalid arguments`() {
        val invalidBuilders: List<() -> Any> = listOf(
            { StatsPeriod(StatsRange.MONTH, LocalDate(2026, 5, 10), LocalDate(2026, 5, 10)) },
            { StatsPeriod(StatsRange.MONTH, LocalDate(2026, 5, 10), LocalDate(2026, 5, 5)) },
            { StatsPeriod(StatsRange.ALL, LocalDate(2026, 5, 1), null) },
            { periodFor(LocalDate(2026, 5, 1), StatsRange.CUSTOM) },
        )
        invalidBuilders.forEach { build ->
            assertThrows(IllegalArgumentException::class.java) { build() }
        }
    }

    @Test
    fun `customPeriod normalizes reversed start and end dates`() {
        val period = customPeriod(LocalDate(2026, 10, 15), LocalDate(2026, 10, 5))
        assertThat(period.range).isEqualTo(StatsRange.CUSTOM)
        assertThat(period.start).isEqualTo(LocalDate(2026, 10, 5))
        assertThat(period.endExclusive).isEqualTo(LocalDate(2026, 10, 16))
    }

    @Test
    fun `periodFor and shift handle week year boundaries month transitions and leap years`() {
        val weekPeriod = periodFor(LocalDate(2026, 1, 1), StatsRange.WEEK)
        assertThat(weekPeriod.start).isEqualTo(LocalDate(2025, 12, 29))
        assertThat(weekPeriod.endExclusive).isEqualTo(LocalDate(2026, 1, 5))
        assertThat(weekPeriod.shift(-1)?.start).isEqualTo(LocalDate(2025, 12, 22))
        assertThat(weekPeriod.shift(1)?.start).isEqualTo(LocalDate(2026, 1, 5))
        assertThat(weekPeriod.shift(52)?.start).isEqualTo(LocalDate(2026, 12, 28))

        listOf(
            Triple(LocalDate(2026, 3, 15), -13, LocalDate(2025, 2, 1) to LocalDate(2025, 3, 1)),
            Triple(LocalDate(2025, 12, 15), 1, LocalDate(2026, 1, 1) to LocalDate(2026, 2, 1)),
            Triple(LocalDate(2026, 1, 10), -1, LocalDate(2025, 12, 1) to LocalDate(2026, 1, 1)),
            Triple(LocalDate(2024, 2, 10), 0, LocalDate(2024, 2, 1) to LocalDate(2024, 3, 1)),
            Triple(LocalDate(2025, 2, 10), 0, LocalDate(2025, 2, 1) to LocalDate(2025, 3, 1)),
        ).forEach { (anchor, offset, expectedRange) ->
            val p = periodFor(anchor, StatsRange.MONTH).let { if (offset == 0) it else it.shift(offset) }
            assertThat(p?.start).isEqualTo(expectedRange.first)
            assertThat(p?.endExclusive).isEqualTo(expectedRange.second)
        }
    }

    @Test
    fun `comparablePreviousRange handles month lengths ongoing slicing completed and future periods`() {
        data class Case(
            val anchor: LocalDate,
            val today: LocalDate,
            val expectedStart: LocalDate,
            val expectedEnd: LocalDate,
        )
        listOf(
            Case(LocalDate(2025, 3, 15), LocalDate(2025, 3, 31), LocalDate(2025, 2, 1), LocalDate(2025, 3, 1)),
            Case(LocalDate(2025, 5, 10), LocalDate(2025, 5, 31), LocalDate(2025, 4, 1), LocalDate(2025, 5, 1)),
            Case(LocalDate(2026, 10, 1), LocalDate(2026, 10, 7), LocalDate(2026, 9, 1), LocalDate(2026, 9, 8)),
            Case(LocalDate(2026, 8, 1), LocalDate(2026, 10, 7), LocalDate(2026, 7, 1), LocalDate(2026, 8, 1)),
            Case(LocalDate(2026, 12, 1), LocalDate(2026, 10, 7), LocalDate(2026, 11, 1), LocalDate(2026, 12, 1)),
        ).forEach { case ->
            val prev = comparablePreviousRange(periodFor(case.anchor, StatsRange.MONTH), case.today)
            assertThat(prev).isNotNull()
            assertThat(prev?.start).isEqualTo(case.expectedStart)
            assertThat(prev?.endExclusive).isEqualTo(case.expectedEnd)
        }
    }

    @Test
    fun `shift and comparablePreviousRange return null for ALL and CUSTOM`() {
        val allPeriod = periodFor(LocalDate(2026, 10, 7), StatsRange.ALL)
        val custom = customPeriod(LocalDate(2026, 10, 1), LocalDate(2026, 10, 15))
        listOf(allPeriod, custom).forEach { period ->
            assertThat(period.shift(1)).isNull()
            assertThat(period.shift(-1)).isNull()
            assertThat(comparablePreviousRange(period, LocalDate(2026, 10, 7))).isNull()
        }
    }

    @Test
    fun `contains and hasComparableData validate date boundaries accurately`() {
        val octPeriod = periodFor(LocalDate(2026, 10, 7), StatsRange.MONTH)
        assertThat(octPeriod.contains(LocalDate(2026, 9, 30))).isFalse()
        assertThat(octPeriod.contains(LocalDate(2026, 10, 1))).isTrue()
        assertThat(octPeriod.contains(LocalDate(2026, 10, 31))).isTrue()
        assertThat(octPeriod.contains(LocalDate(2026, 11, 1))).isFalse()

        val allPeriod = periodFor(LocalDate(2026, 10, 7), StatsRange.ALL)
        assertThat(allPeriod.contains(LocalDate(1990, 1, 1))).isTrue()
        assertThat(allPeriod.contains(LocalDate(2099, 12, 31))).isTrue()

        val prevRange = DateRange(LocalDate(2026, 9, 1), LocalDate(2026, 10, 1))
        listOf(
            Triple(prevRange, LocalDate(2026, 9, 15), true),
            Triple(prevRange, LocalDate(2026, 8, 1), true),
            Triple(prevRange, LocalDate(2026, 10, 1), false),
            Triple(prevRange, LocalDate(2026, 10, 5), false),
            Triple(prevRange, null, false),
            Triple(null, LocalDate(2026, 9, 15), false),
        ).forEach { (range, firstTx, expected) ->
            assertThat(hasComparableData(range, firstTx)).isEqualTo(expected)
        }
    }
}
