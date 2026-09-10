package com.notepay.domain.money

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CurrencyParserEngineTest {

    @Test
    fun `parseInputText parses standard numbers`() {
        val result = CurrencyParserEngine.parseInputText("50000")
        assertThat(result).isInstanceOf(MoneyParseResult.Success::class.java)
        val success = result as MoneyParseResult.Success
        assertThat(success.money.amountInCents).isEqualTo(5_000_000L)
    }

    @Test
    fun `parseInputText parses shorthand k M B tr`() {
        val kRes = CurrencyParserEngine.parseInputText("50k") as MoneyParseResult.Success
        assertThat(kRes.money.amountInCents).isEqualTo(5_000_000L)

        val mRes = CurrencyParserEngine.parseInputText("1.5M") as MoneyParseResult.Success
        assertThat(mRes.money.amountInCents).isEqualTo(150_000_000L)

        val trRes = CurrencyParserEngine.parseInputText("2tr") as MoneyParseResult.Success
        assertThat(trRes.money.amountInCents).isEqualTo(200_000_000L)
    }

    @Test
    fun `parseInputText handles thousands separators and currency symbols`() {
        val res = CurrencyParserEngine.parseInputText("100.000 đ") as MoneyParseResult.Success
        assertThat(res.money.amountInCents).isEqualTo(10_000_000L)

        val resVnd = CurrencyParserEngine.parseInputText("50,000 VND") as MoneyParseResult.Success
        assertThat(resVnd.money.amountInCents).isEqualTo(5_000_000L)
    }

    @Test
    fun `parseInputText handles negative numbers`() {
        val res = CurrencyParserEngine.parseInputText("-50k") as MoneyParseResult.Success
        assertThat(res.money.amountInCents).isEqualTo(-5_000_000L)
    }

    @Test
    fun `parseInputText returns invalid for empty or blank input`() {
        val res = CurrencyParserEngine.parseInputText("  ")
        assertThat(res).isInstanceOf(MoneyParseResult.InvalidInput::class.java)
    }
}
