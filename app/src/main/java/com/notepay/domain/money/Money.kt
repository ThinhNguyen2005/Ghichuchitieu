package com.notepay.domain.money

import kotlin.math.roundToLong

/**
 * Immutable value class representing monetary amount in minor units (cents / xu).
 * 1 Major Unit (VND) = 100 Minor Units (cents).
 *
 * ## Policy khi vượt giới hạn
 *
 * Mọi phép tính dùng arithmetic exact và **ném [ArithmeticException]** khi tràn, thay vì để
 * `Long` wrap âm thầm. Số tiền thật không bao giờ tới gần giới hạn `Long` (hơn 92 triệu tỉ
 * đồng), nên tràn luôn có nghĩa là dữ liệu hỏng — ví dụ file sao lưu bị sửa tay. Với dữ liệu
 * hỏng, dừng lại và báo lỗi an toàn hơn nhiều so với hiển thị số dư sai.
 *
 * Đường nhập dữ liệu đã bọc try/catch (BackupRestoreViewModel) nên exception hiện thành
 * thông báo lỗi, không làm crash app.
 */
@JvmInline
@Suppress("unused")
value class Money(val amountInCents: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(Math.addExact(amountInCents, other.amountInCents))
    operator fun minus(other: Money): Money = Money(Math.subtractExact(amountInCents, other.amountInCents))
    operator fun unaryMinus(): Money = Money(Math.negateExact(amountInCents))
    operator fun times(multiplier: Long): Money = Money(Math.multiplyExact(amountInCents, multiplier))

    operator fun times(multiplier: Double): Money {
        require(multiplier.isFinite()) { "Multiplier must be finite, got $multiplier" }
        val result = amountInCents * multiplier
        // roundToLong() bám vào Long.MAX/MIN chứ không ném, nên phải tự kiểm tra biên.
        if (!result.isFinite() || result > Long.MAX_VALUE.toDouble() || result < Long.MIN_VALUE.toDouble()) {
            throw ArithmeticException("Money overflow: $amountInCents * $multiplier")
        }
        return Money(result.roundToLong())
    }

    /**
     * Chú ý: Phép chia này dùng integer division (cắt phần dư về 0).
     * Tuyệt đối KHÔNG DÙNG hàm này để chia hoá đơn (tránh mất tiền lẻ).
     * Dùng [splitEvenly] nếu cần chia tiền và bảo toàn tổng số tiền.
     */
    operator fun div(divisor: Long): Money {
        // Long.MIN_VALUE / -1 tràn; các trường hợp chia 0 để Long tự ném ArithmeticException.
        if (amountInCents == Long.MIN_VALUE && divisor == -1L) {
            throw ArithmeticException("Money overflow: $amountInCents / $divisor")
        }
        return Money(amountInCents / divisor)
    }

    /**
     * Chia tiền thành [parts] phần bằng nhau một cách an toàn.
     * Phần dư (vài cents lẻ) sẽ được cộng vào các phần đầu tiên để đảm bảo
     * tổng các phần chính xác bằng tổng số tiền ban đầu.
     * 
     * @throws IllegalArgumentException nếu [parts] <= 0
     */
    fun splitEvenly(parts: Int): List<Money> {
        require(parts > 0) { "Cannot split into $parts parts" }
        val baseAmount = amountInCents / parts
        val remainder = (amountInCents % parts).toInt()
        val absRemainder = kotlin.math.abs(remainder)
        val sign = if (amountInCents < 0) -1 else 1
        
        return List(parts) { index ->
            if (index < absRemainder) {
                Money(baseAmount + sign)
            } else {
                Money(baseAmount)
            }
        }
    }

    /** abs(Long.MIN_VALUE) vẫn âm nếu dùng kotlin.math.abs, nên chặn riêng trường hợp đó. */
    fun abs(): Money {
        if (amountInCents == Long.MIN_VALUE) {
            throw ArithmeticException("Money overflow: abs($amountInCents)")
        }
        return Money(kotlin.math.abs(amountInCents))
    }

    fun isPositive(): Boolean = amountInCents > 0
    fun isNegative(): Boolean = amountInCents < 0
    fun isZero(): Boolean = amountInCents == 0L

    fun toMajorUnitDouble(): Double = amountInCents / 100.0
    fun toMajorUnitLong(): Long = amountInCents / 100L

    override fun compareTo(other: Money): Int = amountInCents.compareTo(other.amountInCents)

    companion object {
        val ZERO = Money(0L)
        const val MAX_MAJOR_UNITS: Long = Long.MAX_VALUE / 100L

        fun fromMajorUnit(major: Double): Money {
            require(major.isFinite()) { "Amount must be finite, got $major" }
            val cents = major * 100.0
            if (cents > Long.MAX_VALUE.toDouble() || cents < Long.MIN_VALUE.toDouble()) {
                throw ArithmeticException("Money overflow: $major major units")
            }
            return Money(cents.roundToLong())
        }

        fun fromMajorUnit(major: Long): Money = Money(Math.multiplyExact(major, 100L))
        fun fromMinorUnit(cents: Long): Money = Money(cents)
    }
}
