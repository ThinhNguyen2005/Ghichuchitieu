package com.notepay.domain.money

import kotlin.math.roundToLong

/**
 * Immutable value class representing monetary amount in minor units (cents / xu).
 * 1 Major Unit (VND) = 100 Minor Units (cents).
 */
@JvmInline
value class Money(val amountInCents: Long) : Comparable<Money> {
    
    operator fun plus(other: Money): Money = Money(amountInCents + other.amountInCents)
    operator fun minus(other: Money): Money = Money(amountInCents - other.amountInCents)
    operator fun unaryMinus(): Money = Money(-amountInCents)
    operator fun times(multiplier: Long): Money = Money(amountInCents * multiplier)
    operator fun times(multiplier: Double): Money = Money((amountInCents * multiplier).roundToLong())
    operator fun div(divisor: Long): Money = Money(amountInCents / divisor)
    
    fun abs(): Money = Money(kotlin.math.abs(amountInCents))
    fun isPositive(): Boolean = amountInCents > 0
    fun isNegative(): Boolean = amountInCents < 0
    fun isZero(): Boolean = amountInCents == 0L

    fun toMajorUnitDouble(): Double = amountInCents / 100.0
    fun toMajorUnitLong(): Long = amountInCents / 100L

    override fun compareTo(other: Money): Int = amountInCents.compareTo(other.amountInCents)

    companion object {
        val ZERO = Money(0L)

        fun fromMajorUnit(major: Double): Money {
            require(major.isFinite()) { "Amount must be finite, got $major" }
            return Money((major * 100.0).roundToLong())
        }

        fun fromMajorUnit(major: Long): Money = Money(major * 100L)
        fun fromMinorUnit(cents: Long): Money = Money(cents)
    }
}
