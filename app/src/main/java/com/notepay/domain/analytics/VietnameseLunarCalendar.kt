package com.notepay.domain.analytics

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

enum class VietnameseSeason {
    PRE_TET,
    POST_TET,
    WEDDING_SEASON,
    SUMMER_TRAVEL,
}

/**
 * Pure-Kotlin Vietnamese Lunar New Year (Tết Nguyên Đán, UTC+7) lookup table and seasonal
 * spending window classifier for years 2020–2040.
 */
object VietnameseLunarCalendar {

    private val TET_GREGORIAN_DATES: Map<Int, LocalDate> = mapOf(
        2020 to LocalDate(2020, 1, 25),
        2021 to LocalDate(2021, 2, 12),
        2022 to LocalDate(2022, 2, 1),
        2023 to LocalDate(2023, 1, 22),
        2024 to LocalDate(2024, 2, 10),
        2025 to LocalDate(2025, 1, 29),
        2026 to LocalDate(2026, 2, 17),
        2027 to LocalDate(2027, 2, 6),
        2028 to LocalDate(2028, 1, 26),
        2029 to LocalDate(2029, 2, 13),
        2030 to LocalDate(2030, 2, 2),
        2031 to LocalDate(2031, 1, 23),
        2032 to LocalDate(2032, 2, 11),
        2033 to LocalDate(2033, 1, 31),
        2034 to LocalDate(2034, 2, 19),
        2035 to LocalDate(2035, 2, 8),
        2036 to LocalDate(2036, 1, 28),
        2037 to LocalDate(2037, 2, 15),
        2038 to LocalDate(2038, 2, 4),
        2039 to LocalDate(2039, 1, 24),
        2040 to LocalDate(2040, 2, 12),
    )

    private const val PRE_TET_WINDOW_DAYS = 30
    private const val POST_TET_WINDOW_DAYS = 30

    fun tetDate(year: Int): LocalDate? = TET_GREGORIAN_DATES[year]

    fun daysUntilNextTet(from: LocalDate): Int? {
        val candidateYears = listOf(from.year, from.year + 1)
        val nextTet = candidateYears
            .mapNotNull(::tetDate)
            .firstOrNull { it >= from }
            ?: return null
        return (nextTet.toEpochDays() - from.toEpochDays()).toInt()
    }

    fun isPreTet(date: LocalDate): Boolean = listOf(date.year, date.year + 1)
        .mapNotNull(::tetDate)
        .any { tet ->
            val windowStart = tet.minus(DatePeriod(days = PRE_TET_WINDOW_DAYS))
            date >= windowStart && date < tet
        }

    fun isPostTet(date: LocalDate): Boolean = listOf(date.year - 1, date.year)
        .mapNotNull(::tetDate)
        .any { tet ->
            val windowEndExclusive = tet.plus(DatePeriod(days = POST_TET_WINDOW_DAYS))
            date >= tet && date < windowEndExclusive
        }

    fun seasonsForDate(date: LocalDate): Set<VietnameseSeason> {
        val seasons = linkedSetOf<VietnameseSeason>()
        val preTet = isPreTet(date)
        val postTet = isPostTet(date)
        if (preTet) seasons += VietnameseSeason.PRE_TET
        if (postTet) seasons += VietnameseSeason.POST_TET
        if (date.month.number in 6..8) {
            seasons += VietnameseSeason.SUMMER_TRAVEL
        }
        val inAutumnWedding = date.month.number in 10..12 && !preTet
        val inSpringWedding = date.month.number in 2..3 && !preTet && !postTet
        if (inAutumnWedding || inSpringWedding) {
            seasons += VietnameseSeason.WEDDING_SEASON
        }
        return seasons
    }

    fun primarySeason(date: LocalDate): VietnameseSeason? {
        val all = seasonsForDate(date)
        return when {
            VietnameseSeason.PRE_TET in all -> VietnameseSeason.PRE_TET
            VietnameseSeason.POST_TET in all -> VietnameseSeason.POST_TET
            VietnameseSeason.SUMMER_TRAVEL in all -> VietnameseSeason.SUMMER_TRAVEL
            VietnameseSeason.WEDDING_SEASON in all -> VietnameseSeason.WEDDING_SEASON
            else -> null
        }
    }
}
