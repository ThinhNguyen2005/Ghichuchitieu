package com.notepay.feature.assets

import com.google.common.truth.Truth.assertThat
import com.notepay.feature.assets.main.AssetsPeriod
import com.notepay.feature.assets.main.assetsPeriodAt
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.Test
import kotlin.time.Instant

class AssetsPeriodTest {

    private val zone = TimeZone.of("Asia/Ho_Chi_Minh")

    @Test
    fun `assetsPeriodAt starts the month on the first day at local midnight`() {
        val period = assetsPeriodAt(instantAt(2026, 3, 15, 10, 30), zone)

        val monthStart = Instant.fromEpochMilliseconds(period.monthStartMillis).toLocalDateTime(zone)
        assertThat(monthStart.date).isEqualTo(LocalDate(2026, 3, 1))
        assertThat(monthStart.time).isEqualTo(LocalTime(0, 0))
    }

    @Test
    fun `assetsPeriodAt ends the month right before the next month starts`() {
        val period = assetsPeriodAt(instantAt(2026, 3, 15, 10, 30), zone)

        val afterMonthEnd = Instant.fromEpochMilliseconds(period.monthEndMillis + 1).toLocalDateTime(zone)
        assertThat(afterMonthEnd.date).isEqualTo(LocalDate(2026, 4, 1))
        assertThat(afterMonthEnd.time).isEqualTo(LocalTime(0, 0))
    }

    @Test
    fun `assetsPeriodAt keeps the comparison window inclusive of today`() {
        val period = assetsPeriodAt(instantAt(2026, 3, 15, 10, 30), zone)

        val comparisonStart = Instant.fromEpochMilliseconds(period.comparisonStartMillis).toLocalDateTime(zone)
        val expectedDate = LocalDate(2026, 3, 15)
            .minus(AssetsPeriod.COMPARISON_WINDOW_DAYS, DateTimeUnit.DAY)
        assertThat(comparisonStart.date).isEqualTo(expectedDate)
        assertThat(comparisonStart.time).isEqualTo(LocalTime(0, 0))
    }

    @Test
    fun `assetsPeriodAt switches to the new month right after local midnight`() {
        val beforeMidnight = assetsPeriodAt(instantAt(2026, 3, 31, 23, 59), zone)
        val afterMidnight = assetsPeriodAt(instantAt(2026, 4, 1, 0, 1), zone)

        val newMonthStart = Instant.fromEpochMilliseconds(afterMidnight.monthStartMillis).toLocalDateTime(zone)
        assertThat(newMonthStart.date).isEqualTo(LocalDate(2026, 4, 1))
        assertThat(afterMidnight.monthStartMillis).isGreaterThan(beforeMidnight.monthStartMillis)
    }

    @Test
    fun `assetsPeriodAt handles february in a leap year`() {
        val period = assetsPeriodAt(instantAt(2028, 2, 10, 8, 0), zone)

        val afterMonthEnd = Instant.fromEpochMilliseconds(period.monthEndMillis + 1).toLocalDateTime(zone)
        assertThat(afterMonthEnd.date).isEqualTo(LocalDate(2028, 3, 1))
    }

    private fun instantAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant =
        LocalDateTime(year, month, day, hour, minute).toInstant(zone)
}
