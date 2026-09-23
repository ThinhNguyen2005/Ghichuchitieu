package com.notepay.ui.feature.transaction

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AmountParserTest {

    @Test
    fun `parse blank returns empty error`() {
        val result = AmountParser.parse("")

        assertThat(result.amount).isNull()
        assertThat(result.error).isEqualTo(AmountParseError.EMPTY)
    }

    @Test
    fun `parse zero after leading zeros returns empty error`() {
        val result = AmountParser.parse("000")

        assertThat(result.amount).isNull()
        assertThat(result.error).isEqualTo(AmountParseError.EMPTY)
    }

    @Test
    fun `parse keeps digits and converts major unit to cents`() {
        val result = AmountParser.parse("1.250.000 ₫")

        assertThat(result.input).isEqualTo("1250000")
        assertThat(result.amount?.amountInCents).isEqualTo(125_000_000L)
        assertThat(result.error).isNull()
    }

    @Test
    fun `parse accepts maximum allowed major unit (999 billion)`() {
        val result = AmountParser.parse(AmountParser.MAX_AMOUNT_MAJOR_UNITS.toString())

        assertThat(result.amount).isEqualTo(
            com.notepay.domain.model.Money.fromMajorUnit(AmountParser.MAX_AMOUNT_MAJOR_UNITS),
        )
        assertThat(result.error).isNull()
    }

    @Test
    fun `parse rejects major unit above 999 billion bound`() {
        val result = AmountParser.parse((AmountParser.MAX_AMOUNT_MAJOR_UNITS + 1L).toString())

        assertThat(result.amount).isNull()
        assertThat(result.error).isEqualTo(AmountParseError.INVALID)
    }

    @Test
    fun `parse overflow returns invalid error`() {
        val result = AmountParser.parse("999999999999999999999999999")

        assertThat(result.amount).isNull()
        assertThat(result.error).isEqualTo(AmountParseError.INVALID)
        assertThat(result.input.length).isEqualTo(AmountParser.MAX_DIGITS)
    }

    @Test
    fun `parse spammed zeros and large digits clamps input safely`() {
        val spammed = "1" + "0".repeat(50)
        val result = AmountParser.parse(spammed)

        assertThat(result.amount).isNull()
        assertThat(result.error).isEqualTo(AmountParseError.INVALID)
        assertThat(result.input.length).isEqualTo(AmountParser.MAX_DIGITS)
    }
}
