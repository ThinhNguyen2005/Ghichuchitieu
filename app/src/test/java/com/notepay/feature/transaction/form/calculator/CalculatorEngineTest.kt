package com.notepay.feature.transaction.form.calculator

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

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
            state = state.copy(currentOperand = c.right.toString(), awaitingRightOperand = false)
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
    fun `division rounds half-up on equals and preserves precision across chained multiply`() {
        // 5 / 2 = 2.5 -> 3 (HALF_UP)
        var halfUp = CalculatorState(currentOperand = "5")
        halfUp = CalculatorEngine.applyOperator(halfUp, '/')
        halfUp = CalculatorEngine.appendDigit(halfUp, 2)
        assertThat(CalculatorEngine.currentValue(CalculatorEngine.equals(halfUp))).isEqualTo(3L)

        // 100_000 / 3 = 33_333; 200_000 / 3 = 66_667
        var oneHundredKDiv3 = CalculatorState(currentOperand = "100000")
        oneHundredKDiv3 = CalculatorEngine.applyOperator(oneHundredKDiv3, '/')
        oneHundredKDiv3 = CalculatorEngine.appendDigit(oneHundredKDiv3, 3)
        assertThat(CalculatorEngine.currentValue(CalculatorEngine.equals(oneHundredKDiv3))).isEqualTo(33_333L)

        var twoHundredKDiv3 = CalculatorState(currentOperand = "200000")
        twoHundredKDiv3 = CalculatorEngine.applyOperator(twoHundredKDiv3, '/')
        twoHundredKDiv3 = CalculatorEngine.appendDigit(twoHundredKDiv3, 3)
        assertThat(CalculatorEngine.currentValue(CalculatorEngine.equals(twoHundredKDiv3))).isEqualTo(66_667L)

        // Chained precision: 100_000 / 3 * 2 = 66_667 (no intermediate truncation to 66_666)
        var chainedDivMul = CalculatorState(currentOperand = "100000")
        chainedDivMul = CalculatorEngine.applyOperator(chainedDivMul, '/')
        chainedDivMul = CalculatorEngine.appendDigit(chainedDivMul, 3)
        chainedDivMul = CalculatorEngine.applyOperator(chainedDivMul, '*')
        assertThat(CalculatorEngine.displayExpression(chainedDivMul)).isEqualTo("33.333,33 ×")
        chainedDivMul = CalculatorEngine.appendDigit(chainedDivMul, 2)
        assertThat(CalculatorEngine.currentValue(CalculatorEngine.equals(chainedDivMul))).isEqualTo(66_667L)

        // 1 / 2 * 100_000 = 50_000 (does not collapse to 0)
        var smallDivMul = CalculatorState(currentOperand = "1")
        smallDivMul = CalculatorEngine.applyOperator(smallDivMul, '/')
        smallDivMul = CalculatorEngine.appendDigit(smallDivMul, 2)
        smallDivMul = CalculatorEngine.applyOperator(smallDivMul, '*')
        smallDivMul = CalculatorEngine.appendDigit(smallDivMul, 1)
        smallDivMul = CalculatorEngine.appendDigit(smallDivMul, 0)
        smallDivMul = CalculatorEngine.appendDigit(smallDivMul, 0)
        smallDivMul = CalculatorEngine.appendThreeZeros(smallDivMul)
        assertThat(CalculatorEngine.currentValue(CalculatorEngine.equals(smallDivMul))).isEqualTo(50_000L)
    }

    @Test
    fun `division by zero sets error preserves left operand and recovers when divisor is edited`() {
        var state = CalculatorState(currentOperand = "50000")
        state = CalculatorEngine.applyOperator(state, '/')
        state = CalculatorEngine.appendDigit(state, 0)

        val onEquals = CalculatorEngine.equals(state)
        assertThat(onEquals.error).isEqualTo(CalculatorError.DIVISION_BY_ZERO)
        assertThat(onEquals.leftOperand).isEqualTo(BigDecimal(50_000L))
        assertThat(onEquals.pendingOperator).isEqualTo('/')
        assertThat(CalculatorEngine.previewMajorUnits(onEquals)).isNull()

        val onChain = CalculatorEngine.applyOperator(state, '+')
        assertThat(onChain.error).isEqualTo(CalculatorError.DIVISION_BY_ZERO)

        val recovered = CalculatorEngine.appendDigit(onEquals, 2)
        assertThat(recovered.error).isNull()
        assertThat(CalculatorEngine.currentValue(CalculatorEngine.equals(recovered))).isEqualTo(25_000L)
    }

    @Test
    fun `negative intermediates are preserved and overflow clamps without Long wraparound`() {
        // 50 - 100 + 80 = 30
        var signedChain = CalculatorState(currentOperand = "50")
        signedChain = CalculatorEngine.applyOperator(signedChain, '-')
        signedChain = signedChain.copy(currentOperand = "100", awaitingRightOperand = false)
        signedChain = CalculatorEngine.applyOperator(signedChain, '+')
        signedChain = signedChain.copy(currentOperand = "80", awaitingRightOperand = false)
        assertThat(CalculatorEngine.currentValue(CalculatorEngine.equals(signedChain))).isEqualTo(30L)

        // 0 - 50 + 80 = 30
        var leadingMinus = CalculatorState(currentOperand = "0")
        leadingMinus = CalculatorEngine.applyOperator(leadingMinus, '-')
        leadingMinus = leadingMinus.copy(currentOperand = "50", awaitingRightOperand = false)
        leadingMinus = CalculatorEngine.applyOperator(leadingMinus, '+')
        leadingMinus = leadingMinus.copy(currentOperand = "80", awaitingRightOperand = false)
        assertThat(CalculatorEngine.currentValue(CalculatorEngine.equals(leadingMinus))).isEqualTo(30L)

        // 100 - 500 = -400
        val negativeFinal = CalculatorEngine.equals(
            CalculatorState(currentOperand = "500", leftOperand = BigDecimal(100L), pendingOperator = '-'),
        )
        assertThat(CalculatorEngine.currentValue(negativeFinal)).isEqualTo(-400L)
        assertThat(CalculatorEngine.previewMajorUnits(negativeFinal)).isEqualTo(-400L)
        assertThat(CalculatorEngine.displayExpression(negativeFinal)).isEqualTo("-400")

        // Backspace on "-5" must produce "0", never "-"
        val backspaceNeg = CalculatorEngine.backspace(CalculatorState(currentOperand = "-5"))
        assertThat(backspaceNeg.currentOperand).isEqualTo("0")

        // Multiplication overflow clamps to MAX without signed Long wraparound
        val wrapCandidate1 = CalculatorEngine.equals(
            CalculatorState(
                currentOperand = "10000000",
                leftOperand = BigDecimal.valueOf(AmountParser.MAX_AMOUNT_MAJOR_UNITS),
                pendingOperator = '*',
            ),
        )
        assertThat(CalculatorEngine.currentValue(wrapCandidate1)).isEqualTo(AmountParser.MAX_AMOUNT_MAJOR_UNITS)

        val wrapCandidate2 = CalculatorEngine.equals(
            CalculatorState(
                currentOperand = "18446744",
                leftOperand = BigDecimal.valueOf(AmountParser.MAX_AMOUNT_MAJOR_UNITS),
                pendingOperator = '*',
            ),
        )
        assertThat(CalculatorEngine.currentValue(wrapCandidate2)).isEqualTo(AmountParser.MAX_AMOUNT_MAJOR_UNITS)
    }

    @Test
    fun `post equals entry repeated equals trailing operator and backspace across operator work cleanly`() {
        // 100 + 50 = 150 -> digit 2 starts fresh "2"
        var evaluated = CalculatorEngine.equals(
            CalculatorState(currentOperand = "50", leftOperand = BigDecimal(100L), pendingOperator = '+'),
        )
        assertThat(evaluated.currentOperand).isEqualTo("150")
        assertThat(evaluated.justEvaluated).isTrue()
        assertThat(CalculatorEngine.appendDigit(evaluated, 2).currentOperand).isEqualTo("2")

        // Post-= 000 is a no-op
        assertThat(CalculatorEngine.appendThreeZeros(evaluated).currentOperand).isEqualTo("150")

        // Repeated = is idempotent
        assertThat(CalculatorEngine.equals(evaluated).currentOperand).isEqualTo("150")

        // 100 + = cancels trailing operator -> 100
        val trailingEq = CalculatorEngine.equals(
            CalculatorState(
                currentOperand = "100",
                leftOperand = BigDecimal(100L),
                pendingOperator = '+',
                awaitingRightOperand = true,
            ),
        )
        assertThat(trailingEq.currentOperand).isEqualTo("100")

        // Operator switch: 10 + * 2 = 20
        val replacedOp = CalculatorEngine.applyOperator(
            CalculatorState(
                currentOperand = "10",
                leftOperand = BigDecimal(10L),
                pendingOperator = '+',
                awaitingRightOperand = true,
            ),
            '*',
        )
        assertThat(replacedOp.pendingOperator).isEqualTo('*')
        val multiplied = CalculatorEngine.appendDigit(replacedOp, 2)
        assertThat(CalculatorEngine.currentValue(CalculatorEngine.equals(multiplied))).isEqualTo(20L)

        // Backspace across operator: 100 + 5 -> 100 + -> 100 -> 10
        var bs = CalculatorState(currentOperand = "100")
        bs = CalculatorEngine.applyOperator(bs, '+')
        bs = CalculatorEngine.appendDigit(bs, 5)
        assertThat(CalculatorEngine.displayExpression(bs)).isEqualTo("100 + 5")
        bs = CalculatorEngine.backspace(bs)
        assertThat(CalculatorEngine.displayExpression(bs)).isEqualTo("100 +")
        assertThat(bs.awaitingRightOperand).isTrue()
        bs = CalculatorEngine.backspace(bs)
        assertThat(CalculatorEngine.displayExpression(bs)).isEqualTo("100")
        assertThat(bs.pendingOperator).isNull()
        bs = CalculatorEngine.backspace(bs)
        assertThat(CalculatorEngine.displayExpression(bs)).isEqualTo("10")
    }

    @Test
    fun `displayExpression formats operands with dot grouping and unicode operators`() {
        assertThat(CalculatorEngine.displayExpression(CalculatorState(currentOperand = "0"))).isEqualTo("0")
        assertThat(
            CalculatorEngine.displayExpression(
                CalculatorState(
                    currentOperand = "45000",
                    leftOperand = BigDecimal(45_000L),
                    pendingOperator = '+',
                    awaitingRightOperand = true,
                ),
            ),
        ).isEqualTo("45.000 +")
        assertThat(
            CalculatorEngine.displayExpression(
                CalculatorState(
                    currentOperand = "30000",
                    leftOperand = BigDecimal(45_000L),
                    pendingOperator = '-',
                    awaitingRightOperand = false,
                ),
            ),
        ).isEqualTo("45.000 − 30.000")
        assertThat(
            CalculatorEngine.displayExpression(
                CalculatorState(
                    currentOperand = "3",
                    leftOperand = BigDecimal(90_000L),
                    pendingOperator = '*',
                    awaitingRightOperand = false,
                ),
            ),
        ).isEqualTo("90.000 × 3")
        assertThat(
            CalculatorEngine.displayExpression(
                CalculatorState(
                    currentOperand = "2",
                    leftOperand = BigDecimal(90_000L),
                    pendingOperator = '/',
                    awaitingRightOperand = false,
                ),
            ),
        ).isEqualTo("90.000 ÷ 2")
    }
}
