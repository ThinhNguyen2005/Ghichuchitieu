package com.notepay.domain.model

import com.google.common.truth.Truth.assertThat
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
}
