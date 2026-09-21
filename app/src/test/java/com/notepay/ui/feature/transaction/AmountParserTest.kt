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
    fun `parse accepts maximum representable major unit`() {
        val result = AmountParser.parse(com.notepay.domain.model.Money.MAX_MAJOR_UNITS.toString())

        assertThat(result.amount).isEqualTo(
            com.notepay.domain.model.Money.fromMajorUnit(com.notepay.domain.model.Money.MAX_MAJOR_UNITS),
        )
        assertThat(result.error).isNull()
    }

    @Test
    fun `parse rejects major unit above shared money bound`() {
        val result = AmountParser.parse((com.notepay.domain.model.Money.MAX_MAJOR_UNITS + 1L).toString())

        assertThat(result.amount).isNull()
        assertThat(result.error).isEqualTo(AmountParseError.INVALID)
    }

    @Test
    fun `parse overflow returns invalid error`() {
        val result = AmountParser.parse("999999999999999999999999999")

        assertThat(result.amount).isNull()
        assertThat(result.error).isEqualTo(AmountParseError.INVALID)
    }
}
