package com.notepay.ui.feature.transaction.calculator

import com.notepay.ui.feature.transaction.AmountParser

/**
 * Mini calculator engine for the custom keypad.
 *
 * Tracks an expression like "45000 + 30000" and evaluates it to a Long
 * (in major VND units). No floating-point; VND has no sub-unit.
 *
 * Precedence: left-to-right (simple calculator, not scientific).
 * Cap: AmountParser.MAX_AMOUNT_MAJOR_UNITS (999 billion VND).
 */
data class CalculatorState(
    val currentOperand: String = "0",
    val leftOperand: Long? = null,
    val pendingOperator: Char? = null,
    val resetOnNextDigit: Boolean = false,
)

object CalculatorEngine {

    private val MAX = AmountParser.MAX_AMOUNT_MAJOR_UNITS

    fun appendDigit(state: CalculatorState, digit: Int): CalculatorState {
        val current = if (state.resetOnNextDigit) "" else state.currentOperand
        val raw = if (current == "0" || current.isEmpty()) digit.toString() else current + digit
        val capped = if (raw.length > AmountParser.MAX_DIGITS) current else raw
        return state.copy(currentOperand = capped, resetOnNextDigit = false)
    }

    /** Append "000" to the current operand (shortcut for thousands). */
    fun appendThreeZeros(state: CalculatorState): CalculatorState {
        if (state.resetOnNextDigit) return state
        val current = state.currentOperand
        if (current == "0" || current.isEmpty()) return state
        val raw = current + "000"
        val capped = if (raw.length > AmountParser.MAX_DIGITS) current else raw
        return state.copy(currentOperand = capped)
    }

    fun applyOperator(state: CalculatorState, operator: Char): CalculatorState {
        val current = state.currentOperand.toLongOrNull() ?: 0L
        return if (state.leftOperand != null && state.pendingOperator != null && !state.resetOnNextDigit) {
            val result = evaluate(state.leftOperand, current, state.pendingOperator)
            state.copy(
                currentOperand = result.toString(),
                leftOperand = result,
                pendingOperator = operator,
                resetOnNextDigit = true,
            )
        } else if (state.leftOperand != null && state.pendingOperator != null && state.resetOnNextDigit) {
            state.copy(pendingOperator = operator)
        } else {
            state.copy(
                leftOperand = current,
                pendingOperator = operator,
                resetOnNextDigit = true,
            )
        }
    }

    fun equals(state: CalculatorState): CalculatorState {
        val current = state.currentOperand.toLongOrNull() ?: 0L
        return if (state.leftOperand != null && state.pendingOperator != null) {
            val result = if (state.resetOnNextDigit) {
                state.leftOperand
            } else {
                evaluate(state.leftOperand, current, state.pendingOperator)
            }
            CalculatorState(currentOperand = result.toString())
        } else {
            state.copy(pendingOperator = null, leftOperand = null, resetOnNextDigit = false)
        }
    }

    fun backspace(state: CalculatorState): CalculatorState {
        if (state.resetOnNextDigit) {
            return state.copy(pendingOperator = null, leftOperand = null, resetOnNextDigit = false)
        }
        val next = state.currentOperand.dropLast(1).ifEmpty { "0" }
        return state.copy(currentOperand = next)
    }

    fun clear(): CalculatorState = CalculatorState()

    fun currentValue(state: CalculatorState): Long? = state.currentOperand.toLongOrNull()

    fun displayExpression(state: CalculatorState): String {
        return if (state.leftOperand != null && state.pendingOperator != null) {
            val left = formatNumber(state.leftOperand)
            if (state.resetOnNextDigit) {
                "$left ${state.pendingOperator}"
            } else {
                val right = formatNumber(state.currentOperand.toLongOrNull() ?: 0L)
                "$left ${state.pendingOperator} $right"
            }
        } else {
            formatNumber(state.currentOperand.toLongOrNull() ?: 0L)
        }
    }

    private fun evaluate(left: Long, right: Long, op: Char): Long {
        val raw = when (op) {
            '+' -> left + right
            '-' -> maxOf(0L, left - right)
            '*' -> left * right
            '/' -> if (right == 0L) left else left / right
            else -> right
        }
        return raw.coerceIn(0L, MAX)
    }

    private fun formatNumber(value: Long): String {
        if (value == 0L) return "0"
        val formatter = java.text.DecimalFormat("#,###")
        return formatter.format(value).replace(",", ".")
    }
}
