package com.notepay.feature.transaction.form.calculator

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class CalculatorError {
    DIVISION_BY_ZERO,
}

/**
 * Mini calculator engine for the custom keypad.
 *
 * Tracks an expression like "45.000 + 30.000" and evaluates it using signed [BigDecimal]
 * intermediate arithmetic (scale 10, [RoundingMode.HALF_UP]) with [RoundingMode.HALF_UP]
 * integer VND finalization (`0` decimal places) on `=` or `Save`.
 *
 * Precedence: sequential left-to-right immediate execution.
 * Cap: [-AmountParser.MAX_AMOUNT_MAJOR_UNITS, +AmountParser.MAX_AMOUNT_MAJOR_UNITS].
 */
data class CalculatorState(
    val currentOperand: String = "0",
    val leftOperand: BigDecimal? = null,
    val pendingOperator: Char? = null,
    val awaitingRightOperand: Boolean = false,
    val justEvaluated: Boolean = false,
    val error: CalculatorError? = null,
)

private const val MAX_CALC_MAJOR_UNITS = AmountParser.MAX_AMOUNT_MAJOR_UNITS
private val MAX_CALC_BD = BigDecimal.valueOf(MAX_CALC_MAJOR_UNITS)
private val MIN_CALC_BD = MAX_CALC_BD.negate()
private const val CALC_DIV_SCALE = 10

private sealed interface EvalResult {
    data class Ok(val value: BigDecimal) : EvalResult
    data object DivByZero : EvalResult
}

object CalculatorEngine {

    fun appendDigit(state: CalculatorState, digit: Int): CalculatorState {
        if (digit !in 0..9) return state
        val current = if (state.awaitingRightOperand || state.justEvaluated) "" else state.currentOperand
        val raw = if (current == "0" || current.isEmpty()) digit.toString() else current + digit
        val capped = if (raw.count(Char::isDigit) > AmountParser.MAX_DIGITS) current else raw
        return state.copy(
            currentOperand = capped,
            awaitingRightOperand = false,
            justEvaluated = false,
            error = null,
        )
    }

    /** Append "000" to the current operand (shortcut for thousands). */
    fun appendThreeZeros(state: CalculatorState): CalculatorState {
        if (state.awaitingRightOperand || state.justEvaluated) {
            return state.copy(error = null)
        }
        val current = state.currentOperand
        if (current == "0" || current.isEmpty() || current == "-") {
            return state.copy(error = null)
        }
        val raw = current + "000"
        val capped = if (raw.count(Char::isDigit) > AmountParser.MAX_DIGITS) current else raw
        return state.copy(currentOperand = capped, error = null)
    }

    fun applyOperator(state: CalculatorState, operator: Char): CalculatorState {
        if (state.leftOperand != null && state.pendingOperator != null) {
            if (state.awaitingRightOperand) {
                return state.copy(
                    pendingOperator = operator,
                    justEvaluated = false,
                    error = null,
                )
            }
            return when (val result = evaluate(state.leftOperand, parseCurrent(state.currentOperand), state.pendingOperator)) {
                EvalResult.DivByZero -> state.copy(
                    error = CalculatorError.DIVISION_BY_ZERO,
                    justEvaluated = false,
                )
                is EvalResult.Ok -> CalculatorState(
                    currentOperand = finalizeToInteger(result.value).toString(),
                    leftOperand = result.value,
                    pendingOperator = operator,
                    awaitingRightOperand = true,
                    justEvaluated = false,
                    error = null,
                )
            }
        }
        val current = parseCurrent(state.currentOperand)
        return CalculatorState(
            currentOperand = state.currentOperand,
            leftOperand = current,
            pendingOperator = operator,
            awaitingRightOperand = true,
            justEvaluated = false,
            error = null,
        )
    }

    @Suppress("WrongEqualsTypeParameter")
    fun equals(state: CalculatorState): CalculatorState {
        if (state.leftOperand != null && state.pendingOperator != null) {
            if (state.awaitingRightOperand) {
                val finalLong = finalizeToInteger(state.leftOperand)
                return CalculatorState(
                    currentOperand = finalLong.toString(),
                    justEvaluated = true,
                    error = null,
                )
            }
            return when (val result = evaluate(state.leftOperand, parseCurrent(state.currentOperand), state.pendingOperator)) {
                EvalResult.DivByZero -> state.copy(
                    error = CalculatorError.DIVISION_BY_ZERO,
                    justEvaluated = false,
                )
                is EvalResult.Ok -> CalculatorState(
                    currentOperand = finalizeToInteger(result.value).toString(),
                    justEvaluated = true,
                    error = null,
                )
            }
        }
        val finalLong = finalizeToInteger(parseCurrent(state.currentOperand))
        return CalculatorState(
            currentOperand = finalLong.toString(),
            justEvaluated = true,
            error = null,
        )
    }

    fun backspace(state: CalculatorState): CalculatorState {
        if (state.leftOperand != null && state.pendingOperator != null) {
            if (state.awaitingRightOperand) {
                return CalculatorState(
                    currentOperand = finalizeToInteger(state.leftOperand).toString(),
                    justEvaluated = false,
                    error = null,
                )
            }
            val rightText = state.currentOperand
            if (rightText.length <= 1 || (rightText.startsWith("-") && rightText.length == 2)) {
                return state.copy(
                    currentOperand = finalizeToInteger(state.leftOperand).toString(),
                    awaitingRightOperand = true,
                    justEvaluated = false,
                    error = null,
                )
            }
            val next = rightText.dropLast(1).let { if (it.isEmpty() || it == "-") "0" else it }
            return state.copy(currentOperand = next, justEvaluated = false, error = null)
        }
        val next = state.currentOperand.dropLast(1).let { if (it.isEmpty() || it == "-") "0" else it }
        return state.copy(currentOperand = next, justEvaluated = false, error = null)
    }

    fun clear(): CalculatorState = CalculatorState()

    fun currentValue(state: CalculatorState): Long? = state.currentOperand.toLongOrNull()

    /** Returns the evaluated rounded major VND units for preview/save, or null on math error. */
    fun previewMajorUnits(state: CalculatorState): Long? {
        if (state.error != null) return null
        if (state.leftOperand != null && state.pendingOperator != null) {
            if (state.awaitingRightOperand) {
                return finalizeToInteger(state.leftOperand)
            }
            return when (val result = evaluate(state.leftOperand, parseCurrent(state.currentOperand), state.pendingOperator)) {
                EvalResult.DivByZero -> null
                is EvalResult.Ok -> finalizeToInteger(result.value)
            }
        }
        return finalizeToInteger(parseCurrent(state.currentOperand))
    }

    fun activePendingOperator(state: CalculatorState): Char? =
        state.pendingOperator?.takeIf { state.awaitingRightOperand }

    fun displayExpression(state: CalculatorState): String {
        return if (state.leftOperand != null && state.pendingOperator != null) {
            val left = formatNumber(state.leftOperand)
            val op = displayOperator(state.pendingOperator)
            if (state.awaitingRightOperand) {
                "$left $op"
            } else {
                val right = formatNumber(parseCurrent(state.currentOperand))
                "$left $op $right"
            }
        } else {
            formatNumber(parseCurrent(state.currentOperand))
        }
    }
}

private fun evaluate(left: BigDecimal, right: BigDecimal, op: Char): EvalResult = when (op) {
    '+' -> EvalResult.Ok(clamp(left.add(right)))
    '-' -> EvalResult.Ok(clamp(left.subtract(right)))
    '*' -> EvalResult.Ok(clamp(left.multiply(right)))
    '/' -> if (right.compareTo(BigDecimal.ZERO) == 0) {
        EvalResult.DivByZero
    } else {
        EvalResult.Ok(clamp(left.divide(right, CALC_DIV_SCALE, RoundingMode.HALF_UP).stripTrailingZeros()))
    }
    else -> EvalResult.Ok(clamp(right))
}

private fun parseCurrent(operand: String): BigDecimal =
    operand.toBigDecimalOrNull() ?: BigDecimal.ZERO

private fun clamp(value: BigDecimal): BigDecimal = when {
    value > MAX_CALC_BD -> MAX_CALC_BD
    value < MIN_CALC_BD -> MIN_CALC_BD
    else -> value
}

private fun finalizeToInteger(value: BigDecimal): Long =
    clamp(value).setScale(0, RoundingMode.HALF_UP).toLong().coerceIn(-MAX_CALC_MAJOR_UNITS, MAX_CALC_MAJOR_UNITS)

private fun displayOperator(op: Char): Char = when (op) {
    '-' -> '−'
    '*' -> '×'
    '/' -> '÷'
    else -> op
}

private fun formatNumber(value: BigDecimal): String {
    if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
    val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = DecimalFormat("#,##0.##", symbols).apply {
        roundingMode = RoundingMode.HALF_UP
    }
    return formatter.format(value)
}
