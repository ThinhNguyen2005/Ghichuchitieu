package com.notepay.domain.model

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.money.Money
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {

    @Test
    fun `arithmetic sign and comparison operators produce exact cents`() {
        assertThat(Money.ZERO.isPositive()).isFalse()
        assertThat(Money.ZERO.isNegative()).isFalse()
        assertThat(Money.ZERO.isZero()).isTrue()

        val a = Money(10_00)
        val b = Money(3_50)
        assertThat(a + b).isEqualTo(Money(13_50))
        assertThat(a - b).isEqualTo(Money(6_50))
        assertThat(-Money(5_00)).isEqualTo(Money(-5_00))
        assertThat(Money(-7_00).abs()).isEqualTo(Money(7_00))
        assertThat(Money(5_00) > Money(4_99)).isTrue()
        assertThat(Money(5_00) < Money(5_01)).isTrue()
    }

    @Test
    fun `fromMajorUnit scales double and Long boundary accurately`() {
        assertThat(Money.fromMajorUnit(50_000.0).amountInCents).isEqualTo(5_000_000L)
        val largestSafe = Long.MAX_VALUE / 100L
        assertThat(Money.fromMajorUnit(largestSafe).amountInCents).isEqualTo(largestSafe * 100L)
    }

    @Test
    fun `arithmetic operations beyond Long bounds throw ArithmeticException`() {
        val huge = Money(Long.MAX_VALUE / 2L)
        val overflowOperations: List<() -> Any> = listOf(
            { Money(Long.MAX_VALUE) + Money(1L) },
            { Money(Long.MIN_VALUE) - Money(1L) },
            { Money(Long.MAX_VALUE / 2L) * 3L },
            { Money(Long.MAX_VALUE) * 2.0 },
            { -Money(Long.MIN_VALUE) },
            { Money(Long.MIN_VALUE).abs() },
            { Money(Long.MIN_VALUE) / -1L },
            { Money.fromMajorUnit(Long.MAX_VALUE / 100L + 1L) },
            { listOf(huge, huge, huge).reduce { acc, m -> acc + m } },
        )

        overflowOperations.forEach { op ->
            assertThrows(ArithmeticException::class.java) { op() }
        }
    }
}
