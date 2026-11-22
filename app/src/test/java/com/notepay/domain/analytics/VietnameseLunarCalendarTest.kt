package com.notepay.domain.analytics

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalDate
import org.junit.Test

class VietnameseLunarCalendarTest {

    @Test
    fun `returns exact Gregorian dates of Tet 2024 to 2028`() {
        assertThat(VietnameseLunarCalendar.tetDate(2024)).isEqualTo(LocalDate(2024, 2, 10))
        assertThat(VietnameseLunarCalendar.tetDate(2025)).isEqualTo(LocalDate(2025, 1, 29))
        assertThat(VietnameseLunarCalendar.tetDate(2026)).isEqualTo(LocalDate(2026, 2, 17))
        assertThat(VietnameseLunarCalendar.tetDate(2027)).isEqualTo(LocalDate(2027, 2, 6))
        assertThat(VietnameseLunarCalendar.tetDate(2028)).isEqualTo(LocalDate(2028, 1, 26))
    }

    @Test
    fun `covers full range 2020 through 2040`() {
        (2020..2040).forEach { year ->
            assertThat(VietnameseLunarCalendar.tetDate(year)).isNotNull()
        }
    }

    @Test
    fun `classifies PRE_TET POST_TET WEDDING_SEASON and SUMMER_TRAVEL accurately`() {
        // Tet 2026 is 2026-02-17
        assertThat(VietnameseLunarCalendar.primarySeason(LocalDate(2026, 2, 1)))
            .isEqualTo(VietnameseSeason.PRE_TET)
        assertThat(VietnameseLunarCalendar.primarySeason(LocalDate(2026, 2, 17)))
            .isEqualTo(VietnameseSeason.POST_TET)
        assertThat(VietnameseLunarCalendar.primarySeason(LocalDate(2026, 3, 25)))
            .isEqualTo(VietnameseSeason.WEDDING_SEASON)
        assertThat(VietnameseLunarCalendar.primarySeason(LocalDate(2026, 11, 15)))
            .isEqualTo(VietnameseSeason.WEDDING_SEASON)
        assertThat(VietnameseLunarCalendar.primarySeason(LocalDate(2026, 7, 10)))
            .isEqualTo(VietnameseSeason.SUMMER_TRAVEL)
        assertThat(VietnameseLunarCalendar.primarySeason(LocalDate(2026, 4, 15)))
            .isNull()
    }

    @Test
    fun `daysUntilNextTet computes remaining days across year boundary`() {
        assertThat(VietnameseLunarCalendar.daysUntilNextTet(LocalDate(2026, 2, 10))).isEqualTo(7)
        assertThat(VietnameseLunarCalendar.daysUntilNextTet(LocalDate(2026, 2, 17))).isEqualTo(0)
        // After Tet 2026 (Feb 17), next Tet is 2027-02-06
        val daysTo2027 = VietnameseLunarCalendar.daysUntilNextTet(LocalDate(2026, 12, 31))
        assertThat(daysTo2027).isEqualTo(37)
    }
}
