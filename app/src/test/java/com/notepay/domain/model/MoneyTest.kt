package com.notepay.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {

    @Test
    fun `zero is not positive nor negative`() {
        assertThat(Money.ZERO.isPositive()).isFalse()
        assertThat(Money.ZERO.isNegative()).isFalse()
        assertThat(Money.ZERO.isZero()).isTrue()
    }

    @Test
    fun `plus and minus produce correct cents`() {
        val a = Money(10_00)
        val b = Money(3_50)
        assertThat(a + b).isEqualTo(Money(13_50))
        assertThat(a - b).isEqualTo(Money(6_50))
    }

    @Test
    fun `unary minus flips sign`() {
        assertThat(-Money(5_00)).isEqualTo(Money(-5_00))
    }

    @Test
    fun `compareTo orders by cents`() {
        assertThat(Money(5_00) > Money(4_99)).isTrue()
        assertThat(Money(5_00) < Money(5_01)).isTrue()
    }

    @Test
    fun `fromMajorUnit scales correctly without floating error`() {
        val m = Money.fromMajorUnit(50_000.0)
        assertThat(m.amountInCents).isEqualTo(5_000_000L)
    }

    @Test
    fun `abs of negative returns positive`() {
        assertThat(Money(-7_00).abs()).isEqualTo(Money(7_00))
    }

    // --- Biên: mọi phép tính phải ném thay vì để Long wrap âm thầm ---

    @Test
    fun `plus beyond Long max throws instead of wrapping`() {
        assertThrows(ArithmeticException::class.java) {
            Money(Long.MAX_VALUE) + Money(1L)
        }
    }

    @Test
    fun `minus below Long min throws instead of wrapping`() {
        assertThrows(ArithmeticException::class.java) {
            Money(Long.MIN_VALUE) - Money(1L)
        }
    }

    @Test
    fun `times by Long beyond range throws`() {
        assertThrows(ArithmeticException::class.java) {
            Money(Long.MAX_VALUE / 2L) * 3L
        }
    }

    @Test
    fun `times by Double beyond range throws`() {
        assertThrows(ArithmeticException::class.java) {
            Money(Long.MAX_VALUE) * 2.0
        }
    }

    @Test
    fun `unaryMinus of Long min throws`() {
        assertThrows(ArithmeticException::class.java) {
            -Money(Long.MIN_VALUE)
        }
    }

    @Test
    fun `abs of Long min throws instead of staying negative`() {
        // kotlin.math.abs(Long.MIN_VALUE) trả về chính Long.MIN_VALUE, vẫn âm.
        assertThrows(ArithmeticException::class.java) {
            Money(Long.MIN_VALUE).abs()
        }
    }

    @Test
    fun `div of Long min by minus one throws`() {
        assertThrows(ArithmeticException::class.java) {
            Money(Long.MIN_VALUE) / -1L
        }
    }

    @Test
    fun `fromMajorUnit Long beyond range throws`() {
        assertThrows(ArithmeticException::class.java) {
            Money.fromMajorUnit(Long.MAX_VALUE / 100L + 1L)
        }
    }

    @Test
    fun `fromMajorUnit Long at boundary still works`() {
        val largest = Long.MAX_VALUE / 100L
        assertThat(Money.fromMajorUnit(largest).amountInCents).isEqualTo(largest * 100L)
    }

    @Test
    fun `summing many amounts beyond range throws`() {
        val huge = Money(Long.MAX_VALUE / 2L)
        assertThrows(ArithmeticException::class.java) {
            listOf(huge, huge, huge).reduce { acc, m -> acc + m }
        }
    }
}
