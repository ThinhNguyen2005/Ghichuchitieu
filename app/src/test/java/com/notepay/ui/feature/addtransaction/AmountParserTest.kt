package com.notepay.ui.feature.addtransaction

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AmountParserTest {

    @Test
    fun `parse keeps digits and converts major unit to cents`() {
        val result = AmountParser.parse("1.250.000 ₫")

        assertThat(result.input).isEqualTo("1250000")
        assertThat(result.amount?.amountInCents).isEqualTo(125_000_000L)
        assertThat(result.error).isNull()
    }

    @Test
    fun `parse blank returns empty error`() {
        val result = AmountParser.parse("")

        assertThat(result.amount).isNull()
        assertThat(result.error).isEqualTo(FieldError.AMOUNT_EMPTY)
    }

    @Test
    fun `parse zero returns empty error`() {
        val result = AmountParser.parse("000")

        assertThat(result.amount).isNull()
        assertThat(result.error).isEqualTo(FieldError.AMOUNT_EMPTY)
    }

    @Test
    fun `parse overflow returns invalid error`() {
        val result = AmountParser.parse("999999999999999999999999999")

        assertThat(result.amount).isNull()
        assertThat(result.error).isEqualTo(FieldError.AMOUNT_INVALID)
    }
}
