package com.notepay.domain.analytics

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Các loại khoảng thời gian phân tích thống kê.
 */
enum class StatsRange {
    WEEK,
    MONTH,
    YEAR,
    ALL,
    CUSTOM,
}

/**
 * Khoảng ngày cụ thể với [start] (bao gồm) và [endExclusive] (không bao gồm).
 * Dùng làm kiểu trả về an toàn, rõ nghĩa cho các phép cắt ngày và đối chiếu cùng kỳ.
 */
data class DateRange(
    val start: LocalDate,
    val endExclusive: LocalDate,
) {
    init {
        require(start < endExclusive) {
            "start ($start) phải trước endExclusive ($endExclusive)"
        }
    }

    /** Kiểm tra xem một ngày có nằm trong khoảng này hay không. */
    operator fun contains(date: LocalDate): Boolean = date >= start && date < endExclusive
}

/**
 * Kỳ phân tích thống kê chuẩn hóa (Single Source of Truth cho toàn bộ màn hình Report/Stats).
 * [start] là ngày bắt đầu (bao gồm / inclusive), null nếu là [StatsRange.ALL].
 * [endExclusive] là ngày kết thúc (không bao gồm / exclusive), null nếu là [StatsRange.ALL].
 */
data class StatsPeriod(
    val range: StatsRange,
    val start: LocalDate?,
    val endExclusive: LocalDate?,
) {
    init {
        require((start == null) == (endExclusive == null)) {
            "start và endExclusive phải cùng null (đối với ALL) hoặc cùng có giá trị"
        }
        if (start != null && endExclusive != null) {
            require(start < endExclusive) {
                "start ($start) phải trước endExclusive ($endExclusive)"
            }
        }
    }

    /** Kiểm tra xem một ngày có nằm trong kỳ này hay không. */
    operator fun contains(date: LocalDate): Boolean {
        val afterOrAtStart = start?.let { date >= it } ?: true
        val beforeEnd = endExclusive?.let { date < it } ?: true
        return afterOrAtStart && beforeEnd
    }
}

/**
 * Tạo [StatsPeriod] tương ứng với [anchor] và [range].
 * Đối với [StatsRange.CUSTOM], bắt buộc phải tạo thông qua hàm [customPeriod].
 */
fun periodFor(anchor: LocalDate, range: StatsRange): StatsPeriod = when (range) {
    StatsRange.WEEK -> {
        val daysToSubtract = anchor.dayOfWeek.isoDayNumber - 1
        val start = anchor.minus(daysToSubtract, DateTimeUnit.DAY)
        StatsPeriod(range, start, start.plus(7, DateTimeUnit.DAY))
    }
    StatsRange.MONTH -> {
        val start = LocalDate(anchor.year, anchor.month, 1)
        StatsPeriod(range, start, start.plus(1, DateTimeUnit.MONTH))
    }
    StatsRange.YEAR -> {
        val start = LocalDate(anchor.year, 1, 1)
        StatsPeriod(range, start, start.plus(1, DateTimeUnit.YEAR))
    }
    StatsRange.ALL -> StatsPeriod(range, null, null)
    StatsRange.CUSTOM -> throw IllegalArgumentException(
        "StatsRange.CUSTOM must be created via customPeriod(start, endInclusive)"
    )
}

/**
 * Tạo [StatsPeriod] cho khoảng ngày tùy chọn với ngày kết thúc bao gồm [endInclusive].
 */
fun customPeriod(start: LocalDate, endInclusive: LocalDate): StatsPeriod {
    val normalizedStart = minOf(start, endInclusive)
    val normalizedEnd = maxOf(start, endInclusive)
    return StatsPeriod(
        range = StatsRange.CUSTOM,
        start = normalizedStart,
        endExclusive = normalizedEnd.plus(1, DateTimeUnit.DAY),
    )
}

/**
 * Lùi hoặc tiến `n` kỳ cùng loại.
 * Với [StatsRange.ALL] và [StatsRange.CUSTOM], không hỗ trợ shift (trả về null).
 */
fun StatsPeriod.shift(n: Int): StatsPeriod? {
    val s = start ?: return null
    val moved = when (range) {
        StatsRange.WEEK -> s.plus(7 * n, DateTimeUnit.DAY)
        StatsRange.MONTH -> s.plus(n, DateTimeUnit.MONTH)
        StatsRange.YEAR -> s.plus(n, DateTimeUnit.YEAR)
        StatsRange.ALL, StatsRange.CUSTOM -> return null
    }
    return periodFor(moved, range)
}

/**
 * Tính toán [DateRange] của kỳ trước để so sánh "cùng kỳ".
 *
 * Thuật toán:
 * 1. Lùi 1 kỳ bằng `shift(-1)`. Nếu không có kỳ trước (ALL, CUSTOM) -> trả về null.
 * 2. Xác định mốc thời gian đã trôi qua trong kỳ hiện tại `elapsedEnd`:
 *    - Nếu `today in current`: `elapsedEnd = today + 1 ngày` (tính hết ngày today).
 *    - Nếu `today` nằm ngoài kỳ hiện tại (kỳ đã kết thúc): `elapsedEnd = current.endExclusive`.
 * 3. Tính số ngày đã trôi qua: `elapsedDays = current.start.daysUntil(elapsedEnd)`.
 * 4. Ngày kết thúc kỳ trước: `minOf(prev.start + elapsedDays, prev.endExclusive)`.
 *    (Hàm `minOf` xử lý an toàn biên độ lệch ngày như 31 ngày so với tháng 2 chỉ có 28/29 ngày).
 */
fun comparablePreviousRange(current: StatsPeriod, today: LocalDate): DateRange? {
    val prev = current.shift(-1) ?: return null
    val curStart = current.start ?: return null
    val curEnd = current.endExclusive ?: return null
    val prevStart = prev.start ?: return null
    val prevEndExclusive = prev.endExclusive ?: return null

    val elapsedEnd = if (today in current) today.plus(1, DateTimeUnit.DAY) else curEnd
    val elapsedDays = curStart.daysUntil(elapsedEnd)
    if (elapsedDays <= 0) return null

    val calculatedPrevEnd = prevStart.plus(elapsedDays, DateTimeUnit.DAY)
    val prevEnd = minOf(calculatedPrevEnd, prevEndExclusive)

    if (prevStart >= prevEnd) return null
    return DateRange(prevStart, prevEnd)
}

/**
 * Kiểm tra xem người dùng có dữ liệu ở kỳ đối chiếu trước hay không.
 *
 * Nguyên tắc:
 * Banner chỉ nên hiển thị so sánh nếu giao dịch đầu tiên trong lịch sử [firstTransactionDate]
 * xảy ra trước khi kỳ đối chiếu kết thúc ([prevRange.endExclusive]).
 * Nếu người dùng mới cài app sau khi kỳ trước đã kết thúc, ta không thể so sánh vì họ chưa hề dùng app ở kỳ đó.
 */
fun hasComparableData(prevRange: DateRange?, firstTransactionDate: LocalDate?): Boolean {
    if (prevRange == null || firstTransactionDate == null) return false
    return firstTransactionDate < prevRange.endExclusive
}
