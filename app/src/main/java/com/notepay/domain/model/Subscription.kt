package com.notepay.domain.model

import kotlin.time.Clock
import kotlinx.datetime.Instant

/**
 * Gói đăng ký / nhắc nhở gia hạn định kỳ.
 * Ví dụ: Spotify, Netflix, VPN...
 */
data class Subscription(
    val id: Long = 0L,
    val name: String,
    val amount: Money,
    val category: String = "subscription",
    val nextDueDate: Instant,
    val repeatMonths: Int,          // Chu kỳ: 1, 3, 6, 12 tháng
    val remindDaysBefore: Int,      // Nhắc trước: 1, 2, 3, 7 ngày
    val note: String = "",
    val isActive: Boolean = true,
    val createdAt: Instant = Clock.System.now(),
) {
    init {
        require(name.isNotBlank()) { "Subscription name must not be blank" }
        require(amount.amountInCents > 0) { "Amount must be positive" }
        require(repeatMonths in listOf(1, 3, 6, 12)) { "repeatMonths must be 1, 3, 6, or 12" }
        require(remindDaysBefore in 1..30) { "remindDaysBefore must be between 1 and 30" }
    }

    companion object {
        val REPEAT_OPTIONS = listOf(1, 3, 6, 12)
        val REMIND_OPTIONS = listOf(1, 2, 3, 7)
    }
}
