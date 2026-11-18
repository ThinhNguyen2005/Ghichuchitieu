package com.notepay.feature.transaction.form.calculator

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.money.Money
import org.junit.Test

class AmountParserTest {

    @Test
    fun `parse extracts digits and converts valid VND major units to cents`() {
        listOf(
            Triple("50000", "50000", Money.fromMajorUnit(50_000L)),
            Triple("1.250.000 đ", "1250000", Money.fromMajorUnit(1_250_000L)),
            Triple("00045000", "45000", Money.fromMajorUnit(45_000L)),
            Triple(
                AmountParser.MAX_AMOUNT_MAJOR_UNITS.toString(),
                AmountParser.MAX_AMOUNT_MAJOR_UNITS.toString(),
                Money.fromMajorUnit(AmountParser.MAX_AMOUNT_MAJOR_UNITS),
            ),
        ).forEach { (raw, expectedInput, expectedMoney) ->
            val result = AmountParser.parse(raw)
            assertThat(result.error).isNull()
            assertThat(result.input).isEqualTo(expectedInput)
            assertThat(result.amount).isEqualTo(expectedMoney)
        }
    }

    @Test
    fun `parse returns EMPTY for blank zero or non-digit strings`() {
        listOf("", "   ", "0", "000", "abc đ").forEach { raw ->
            val result = AmountParser.parse(raw)
            assertThat(result.error).isEqualTo(AmountParseError.EMPTY)
            assertThat(result.amount).isNull()
            assertThat(result.input).isEmpty()
        }
    }

    @Test
    fun `parse returns INVALID and clamps input when exceeding 12 digits`() {
        listOf(
            "1000000000000" to "100000000000",
            "99999999999999" to "999999999999",
        ).forEach { (raw, clamped) ->
            val result = AmountParser.parse(raw)
            assertThat(result.error).isEqualTo(AmountParseError.INVALID)
            assertThat(result.amount).isNull()
            assertThat(result.input).isEqualTo(clamped)
        }
    }
}
