package com.notepay.ui.feature.transaction.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorEngineTest {

    @Test
    fun appendDigit_singleDigit_displaysDigit() {
        var state = CalculatorEngine.clear()
        state = CalculatorEngine.appendDigit(state, 5)

        assertEquals("5", CalculatorEngine.displayExpression(state))
        assertEquals(5L, CalculatorEngine.currentValue(state))
    }

    @Test
    fun applyOperator_afterDigit_displaysOnlyLeftAndOperator() {
        var state = CalculatorEngine.clear()
        state = CalculatorEngine.appendDigit(state, 5)
        state = CalculatorEngine.applyOperator(state, '+')

        // Fix verification: should show "5 +" and NOT "5 + 5"
        assertEquals("5 +", CalculatorEngine.displayExpression(state))
        assertEquals(5L, CalculatorEngine.currentValue(state))
    }

    @Test
    fun appendDigit_afterOperator_displaysFullExpression() {
        var state = CalculatorEngine.clear()
        state = CalculatorEngine.appendDigit(state, 5)
        state = CalculatorEngine.applyOperator(state, '+')
        state = CalculatorEngine.appendDigit(state, 3)

        assertEquals("5 + 3", CalculatorEngine.displayExpression(state))
        assertEquals(3L, CalculatorEngine.currentValue(state))
    }

    @Test
    fun equals_afterSecondOperand_evaluatesCorrectly() {
        var state = CalculatorEngine.clear()
        state = CalculatorEngine.appendDigit(state, 5)
        state = CalculatorEngine.applyOperator(state, '+')
        state = CalculatorEngine.appendDigit(state, 3)
        state = CalculatorEngine.equals(state)

        assertEquals("8", CalculatorEngine.displayExpression(state))
        assertEquals(8L, CalculatorEngine.currentValue(state))
    }

    @Test
    fun equals_immediatelyAfterOperator_returnsLeftOperand() {
        var state = CalculatorEngine.clear()
        state = CalculatorEngine.appendDigit(state, 5)
        state = CalculatorEngine.applyOperator(state, '+')
        state = CalculatorEngine.equals(state)

        assertEquals("5", CalculatorEngine.displayExpression(state))
        assertEquals(5L, CalculatorEngine.currentValue(state))
    }

    @Test
    fun changeOperator_beforeEnteringSecondOperand_updatesOperator() {
        var state = CalculatorEngine.clear()
        state = CalculatorEngine.appendDigit(state, 5)
        state = CalculatorEngine.applyOperator(state, '+')
        state = CalculatorEngine.applyOperator(state, '-')

        assertEquals("5 -", CalculatorEngine.displayExpression(state))
    }

    @Test
    fun backspace_immediatelyAfterOperator_cancelsOperator() {
        var state = CalculatorEngine.clear()
        state = CalculatorEngine.appendDigit(state, 5)
        state = CalculatorEngine.applyOperator(state, '+')
        state = CalculatorEngine.backspace(state)

        assertEquals("5", CalculatorEngine.displayExpression(state))
    }

    @Test
    fun appendThreeZeros_addsShortcutZeros() {
        var state = CalculatorEngine.clear()
        state = CalculatorEngine.appendDigit(state, 5)
        state = CalculatorEngine.appendThreeZeros(state)

        assertEquals("5.000", CalculatorEngine.displayExpression(state))
        assertEquals(5000L, CalculatorEngine.currentValue(state))
    }
}
