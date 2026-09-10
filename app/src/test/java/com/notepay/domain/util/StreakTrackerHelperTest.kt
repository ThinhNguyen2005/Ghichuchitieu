package com.notepay.domain.util

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Test
import kotlin.time.Duration.Companion.hours

class StreakTrackerHelperTest {

    private val timeZone = TimeZone.UTC
    private val today = LocalDate(2026, 8, 18)

    @Test
    fun `empty transactions returns zero streak`() {
        val streak = StreakTrackerHelper.calculateStreak(emptyList(), today, timeZone)
        assertThat(streak).isEqualTo(0)
    }

    @Test
    fun `transaction today only returns 1 streak`() {
        val todayInstant = today.atStartOfDayIn(timeZone) + 10.hours
        val streak = StreakTrackerHelper.calculateStreak(listOf(todayInstant), today, timeZone)
        assertThat(streak).isEqualTo(1)
    }

    @Test
    fun `consecutive 3 days including today returns 3 streak`() {
        val d1 = LocalDate(2026, 8, 16).atStartOfDayIn(timeZone) + 8.hours
        val d2 = LocalDate(2026, 8, 17).atStartOfDayIn(timeZone) + 12.hours
        val d3 = LocalDate(2026, 8, 18).atStartOfDayIn(timeZone) + 15.hours

        val streak = StreakTrackerHelper.calculateStreak(listOf(d1, d2, d3), today, timeZone)
        assertThat(streak).isEqualTo(3)
    }

    @Test
    fun `consecutive 3 days ending yesterday returns 3 streak`() {
        val d1 = LocalDate(2026, 8, 15).atStartOfDayIn(timeZone) + 8.hours
        val d2 = LocalDate(2026, 8, 16).atStartOfDayIn(timeZone) + 12.hours
        val d3 = LocalDate(2026, 8, 17).atStartOfDayIn(timeZone) + 15.hours

        val streak = StreakTrackerHelper.calculateStreak(listOf(d1, d2, d3), today, timeZone)
        assertThat(streak).isEqualTo(3)
    }

    @Test
    fun `broken streak returns only recent consecutive count`() {
        val dOld = LocalDate(2026, 8, 10).atStartOfDayIn(timeZone) + 8.hours
        val d2 = LocalDate(2026, 8, 17).atStartOfDayIn(timeZone) + 12.hours
        val d3 = LocalDate(2026, 8, 18).atStartOfDayIn(timeZone) + 15.hours

        val streak = StreakTrackerHelper.calculateStreak(listOf(dOld, d2, d3), today, timeZone)
        assertThat(streak).isEqualTo(2)
    }

    @Test
    fun `last transaction 2 days ago returns 0 streak`() {
        val dOld = LocalDate(2026, 8, 16).atStartOfDayIn(timeZone) + 8.hours
        val streak = StreakTrackerHelper.calculateStreak(listOf(dOld), today, timeZone)
        assertThat(streak).isEqualTo(0)
    }
}
