package com.notepay.domain.natural

import com.notepay.domain.money.IndexedNormalizedText
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

data class ParsedDateMatch(
    val date: LocalDate,
    val range: IntRange,
    val rawText: String,
)

/**
 * Deterministic parser for Vietnamese relative and calendar date expressions
 * ("hôm nay", "hôm qua", "hôm kia", "tuần trước", "tháng 8", "ngày 15/10").
 */
@Singleton
class VietnameseDateKeywordParser internal constructor(
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    @Inject
    constructor() : this(Clock.System, TimeZone.currentSystemDefault())

    private data class CandidateDate(
        val date: LocalDate,
        val normRange: IntRange,
    )

    fun today(): LocalDate = clock.now().toLocalDateTime(timeZone).date

    fun parseFirst(text: String): ParsedDateMatch? = parseAll(text).firstOrNull()

    fun parseAll(text: String): List<ParsedDateMatch> {
        if (text.isBlank()) return emptyList()
        val refToday = today()
        val indexed = IndexedNormalizedText.from(text)
        val norm = indexed.normalized
        val candidates = ArrayList<CandidateDate>()

        addRelativeMatch(HOM_NAY_REGEX, norm, refToday, candidates)
        addRelativeMatch(HOM_QUA_REGEX, norm, refToday.minus(DatePeriod(days = 1)), candidates)
        addRelativeMatch(HOM_KIA_REGEX, norm, refToday.minus(DatePeriod(days = 2)), candidates)
        addRelativeMatch(TUAN_TRUOC_REGEX, norm, refToday.minus(DatePeriod(days = 7)), candidates)
        addDayMonthMatches(norm, refToday, candidates)
        addMonthOnlyMatches(norm, refToday, candidates)

        return selectNonOverlapping(candidates).map { candidate ->
            val origRange = indexed.toOriginalRange(candidate.normRange)
            val raw = if (!origRange.isEmpty()) text.substring(origRange.first, origRange.last + 1) else ""
            ParsedDateMatch(
                date = candidate.date,
                range = origRange,
                rawText = raw,
            )
        }
    }

    private fun addRelativeMatch(
        regex: Regex,
        norm: String,
        targetDate: LocalDate,
        out: MutableList<CandidateDate>,
    ) {
        regex.findAll(norm).forEach { match ->
            out.add(CandidateDate(targetDate, match.range))
        }
    }

    private fun addDayMonthMatches(
        norm: String,
        refToday: LocalDate,
        out: MutableList<CandidateDate>,
    ) {
        DAY_MONTH_REGEX.findAll(norm).forEach { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return@forEach
            val month = match.groupValues[2].toIntOrNull() ?: return@forEach
            val explicitYear = match.groupValues[3].toIntOrNull()
            if (month !in 1..12 || day !in 1..31) return@forEach

            val date = resolveDayMonthDate(day, month, explicitYear, refToday) ?: return@forEach
            out.add(CandidateDate(date, match.range))
        }
    }

    private fun addMonthOnlyMatches(
        norm: String,
        refToday: LocalDate,
        out: MutableList<CandidateDate>,
    ) {
        MONTH_ONLY_REGEX.findAll(norm).forEach { match ->
            val month = match.groupValues[1].toIntOrNull() ?: return@forEach
            if (month !in 1..12) return@forEach
            val explicitYear = match.groupValues[2].toIntOrNull() ?: match.groupValues[3].toIntOrNull()
            val year = explicitYear ?: if (month <= refToday.month.number) refToday.year else refToday.year - 1
            if (year !in 1970..2100) return@forEach
            val date = safeLocalDate(year, month, 1) ?: return@forEach
            out.add(CandidateDate(date, match.range))
        }
    }

    private fun resolveDayMonthDate(
        day: Int,
        month: Int,
        explicitYear: Int?,
        refToday: LocalDate,
    ): LocalDate? {
        if (explicitYear != null) {
            return if (explicitYear in 1970..2100) safeLocalDate(explicitYear, month, day) else null
        }
        val currentYearDate = safeLocalDate(refToday.year, month, day)
        if (currentYearDate != null) {
            return if (currentYearDate <= refToday) {
                currentYearDate
            } else {
                safeLocalDate(refToday.year - 1, month, day) ?: currentYearDate
            }
        }
        return safeLocalDate(refToday.year - 1, month, day)
    }

    private fun safeLocalDate(year: Int, month: Int, day: Int): LocalDate? = try {
        LocalDate(year, month, day)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun selectNonOverlapping(candidates: List<CandidateDate>): List<CandidateDate> {
        val sorted = candidates.sortedWith(
            compareByDescending<CandidateDate> { it.normRange.last - it.normRange.first }
                .thenBy { it.normRange.first },
        )
        val chosen = ArrayList<CandidateDate>()
        for (candidate in sorted) {
            val overlaps = chosen.any { existing ->
                candidate.normRange.first <= existing.normRange.last &&
                    existing.normRange.first <= candidate.normRange.last
            }
            if (!overlaps) chosen.add(candidate)
        }
        return chosen.sortedBy { it.normRange.first }
    }

    private companion object {
        val HOM_NAY_REGEX = Regex("""\bhom\s+nay\b""")
        val HOM_QUA_REGEX = Regex("""\bhom\s+qua\b""")
        val HOM_KIA_REGEX = Regex("""\bhom\s+kia\b""")
        val TUAN_TRUOC_REGEX = Regex("""\btuan\s+truoc\b""")
        val DAY_MONTH_REGEX = Regex("""\b(?:ngay\s+)?(\d{1,2})\s*/\s*(\d{1,2})(?:\s*/\s*(\d{4}))?\b""")
        val MONTH_ONLY_REGEX = Regex("""\bthang\s+(\d{1,2})(?:\s*/\s*(\d{4})|\s+nam\s+(\d{4}))?\b""")
    }
}
