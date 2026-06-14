package com.notepay.domain.model

import kotlin.math.abs

/**
 * Value class biểu diễn số tiền bằng cents (Long) để tránh lỗi floating point
 * khi cộng dồn nhiều giao dịch.
 *
 * Quy ước:
 *  - amountInCents = 100 nghĩa là 1.00 (1 đơn vị tiền tệ, vd 1 VND)
 *  - amount dương = thu nhập
 *  - amount âm = chi tiêu
 *
 * KHÔNG cho phép amountInCents = 0 trong Transaction (validate tại init).
 */
@JvmInline
value class Money(val amountInCents: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(amountInCents + other.amountInCents)
    operator fun minus(other: Money): Money = Money(amountInCents - other.amountInCents)
    operator fun unaryMinus(): Money = Money(-amountInCents)

    override operator fun compareTo(other: Money): Int =
        amountInCents.compareTo(other.amountInCents)

    fun isPositive(): Boolean = amountInCents > 0L
    fun isNegative(): Boolean = amountInCents < 0L
    fun isZero(): Boolean = amountInCents == 0L
    fun abs(): Money = Money(abs(amountInCents))

    companion object {
        val ZERO = Money(0L)

        /** Tạo Money từ đơn vị lớn (vd: 50_000.0 VND). LÀM TRÒN xuống cents. */
        fun fromMajorUnit(major: Double): Money {
            require(major.isFinite()) { "Amount must be finite, got $major" }
            require(major >= 0.0) { "Use abs() for negative amounts" }
            return Money((major * 100.0).toLong())
        }
    }
}
