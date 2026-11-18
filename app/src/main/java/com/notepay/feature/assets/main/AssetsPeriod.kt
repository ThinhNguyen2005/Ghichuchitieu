package com.notepay.feature.assets.main

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Kỳ thống kê mà tab Tài sản tổng hợp: tháng hiện tại và cửa sổ so sánh gần nhất.
 *
 * Hai mốc này là tham số của câu query Room, nên phải được tính lại khi sang ngày mới;
 * tính một lần lúc khởi tạo ViewModel sẽ giữ mãi tháng của lần mở màn hình đầu tiên.
 */
data class AssetsPeriod(
    val monthStartMillis: Long,
    val monthEndMillis: Long,
    val comparisonStartMillis: Long,
) {
    companion object {
        /** Cửa sổ so sánh tính cả hôm nay: lùi 29 ngày so với đầu ngày. */
        const val COMPARISON_WINDOW_DAYS = 29
    }
}

/** Tính kỳ thống kê tại thời điểm [now] theo [timeZone]. */
fun assetsPeriodAt(now: Instant, timeZone: TimeZone): AssetsPeriod {
    val today: LocalDate = now.toLocalDateTime(timeZone).date
    val monthStart = LocalDate(today.year, today.month.number, 1)
    val nextMonthStart = monthStart.plus(1, DateTimeUnit.MONTH)
    return AssetsPeriod(
        monthStartMillis = monthStart.atStartOfDayIn(timeZone).toEpochMilliseconds(),
        monthEndMillis = nextMonthStart.atStartOfDayIn(timeZone).toEpochMilliseconds() - 1,
        comparisonStartMillis = today
            .minus(AssetsPeriod.COMPARISON_WINDOW_DAYS, DateTimeUnit.DAY)
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds(),
    )
}

/**
 * Phát kỳ hiện tại rồi ngủ tới 00:00 kế tiếp. Danh sách phát chỉ emit lại khi ngày đổi,
 * nên vừa qua tháng mới thống kê tự cập nhật mà không phải restart cả màn hình.
 */
fun assetsPeriodFlow(): Flow<AssetsPeriod> = flow {
    while (true) {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        emit(assetsPeriodAt(now, timeZone))

        val nextDayStart = now.toLocalDateTime(timeZone).date
            .plus(1, DateTimeUnit.DAY)
            .atStartOfDayIn(timeZone)
        // Đồng hồ hệ thống bị chỉnh lùi có thể cho khoảng chờ ≤ 0 → chờ ngắn rồi tính lại.
        val wait: Duration = nextDayStart - now
        delay(if (wait > Duration.ZERO) wait else 1.minutes)
    }
}.distinctUntilChanged()
