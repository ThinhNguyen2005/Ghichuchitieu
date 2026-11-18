package com.notepay.feature.transaction.form.calculator

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CalculatorEngineTest {

    @Test
    fun `digit triple-zero backspace and clear respect 12-digit cap and leading zeros`() {
        var state = CalculatorEngine.clear()
        state = CalculatorEngine.appendThreeZeros(state)
        assertThat(state.currentOperand).isEqualTo("0")

        state = CalculatorEngine.appendDigit(state, 4)
        state = CalculatorEngine.appendDigit(state, 5)
        state = CalculatorEngine.appendThreeZeros(state)
        assertThat(state.currentOperand).isEqualTo("45000")

        // Thử nhập vượt quá MAX_DIGITS (12 chữ số): phải giữ nguyên độ dài tối đa 12.
        repeat(10) { state = CalculatorEngine.appendDigit(state, 9) }
        assertThat(state.currentOperand.length).isEqualTo(AmountParser.MAX_DIGITS)

        val afterBackspace = CalculatorEngine.backspace(CalculatorState(currentOperand = "7"))
        assertThat(afterBackspace.currentOperand).isEqualTo("0")
        assertThat(CalculatorEngine.currentValue(afterBackspace)).isEqualTo(0L)
    }

    @Test
    fun `binary operations evaluate left-to-right across plus minus multiply divide`() {
        data class Case(val left: Int, val op: Char, val right: Int, val expected: Long)
        listOf(
            Case(45_000, '+', 30_000, 75_000L),
            Case(100_000, '-', 40_000, 60_000L),
            Case(25_000, '*', 3, 75_000L),
            Case(90_000, '/', 3, 30_000L),
        ).forEach { c ->
            var state = CalculatorState(currentOperand = c.left.toString())
            state = CalculatorEngine.applyOperator(state, c.op)
            state = CalculatorEngine.appendDigit(state, 0)
            // Gán trực tiếp right bằng nhập từng chữ số để kiểm tra luồng bấm phím thật.
            state = state.copy(currentOperand = c.right.toString(), resetOnNextDigit = false)
            val evaluated = CalculatorEngine.equals(state)
            assertThat(CalculatorEngine.currentValue(evaluated)).isEqualTo(c.expected)
        }

        // Phép tính nối tiếp (left-to-right): 10 + 20 * 3 = 90
        var chained = CalculatorState(currentOperand = "10")
        chained = CalculatorEngine.applyOperator(chained, '+')
        chained = CalculatorEngine.appendDigit(chained, 2)
        chained = CalculatorEngine.appendDigit(chained, 0)
        chained = CalculatorEngine.applyOperator(chained, '*')
        chained = CalculatorEngine.appendDigit(chained, 3)
        assertThat(CalculatorEngine.currentValue(CalculatorEngine.equals(chained))).isEqualTo(90L)
    }

    @Test
    fun `edge cases clamp negative results to zero handle division by zero and cap at MAX`() {
        val negativeClamp = CalculatorEngine.equals(
            CalculatorState(currentOperand = "500", leftOperand = 100L, pendingOperator = '-'),
        )
        assertThat(CalculatorEngine.currentValue(negativeClamp)).isEqualTo(0L)

        val divByZero = CalculatorEngine.equals(
            CalculatorState(currentOperand = "0", leftOperand = 50_000L, pendingOperator = '/'),
        )
        assertThat(CalculatorEngine.currentValue(divByZero)).isEqualTo(50_000L)

        val overflowCap = CalculatorEngine.equals(
            CalculatorState(
                currentOperand = "2",
                leftOperand = AmountParser.MAX_AMOUNT_MAJOR_UNITS,
                pendingOperator = '*',
            ),
        )
        assertThat(CalculatorEngine.currentValue(overflowCap)).isEqualTo(AmountParser.MAX_AMOUNT_MAJOR_UNITS)

        val replacedOp = CalculatorEngine.applyOperator(
            CalculatorState(currentOperand = "500", leftOperand = 500L, pendingOperator = '+', resetOnNextDigit = true),
            '-',
        )
        assertThat(replacedOp.pendingOperator).isEqualTo('-')
        assertThat(CalculatorEngine.currentValue(CalculatorEngine.equals(replacedOp))).isEqualTo(500L)
    }

    @Test
    fun `displayExpression formats operands with dot grouping and pending operator`() {
        assertThat(CalculatorEngine.displayExpression(CalculatorState(currentOperand = "0"))).isEqualTo("0")
        assertThat(
            CalculatorEngine.displayExpression(
                CalculatorState(currentOperand = "45000", leftOperand = 45_000L, pendingOperator = '+', resetOnNextDigit = true),
            ),
        ).isEqualTo("45.000 +")
        assertThat(
            CalculatorEngine.displayExpression(
                CalculatorState(currentOperand = "30000", leftOperand = 45_000L, pendingOperator = '+', resetOnNextDigit = false),
            ),
        ).isEqualTo("45.000 + 30.000")
    }
}
