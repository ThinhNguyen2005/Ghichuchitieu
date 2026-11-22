package com.notepay.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class CurrencyConverterTest {

    private val sampleRates = mapOf(
        "VND" to BigDecimal.ONE,
        "USD" to BigDecimal("0.00004"), // 1 USD = 25,000 VND
        "EUR" to BigDecimal("0.000036"),
        "GBP" to BigDecimal("0.00003"),
    )

    @Test
    fun `convertBetween converts VND to USD, USD to VND, cross pairs, and preserves identity`() {
        val vndToUsd = CurrencyConverter.convertBetween(
            amount = BigDecimal("1000000"),
            fromCode = "VND",
            toCode = "USD",
            rates = sampleRates,
        ).setScale(2, RoundingMode.HALF_UP)
        assertThat(vndToUsd).isEqualTo(BigDecimal("40.00"))

        val usdToVnd = CurrencyConverter.convertBetween(
            amount = BigDecimal("2"),
            fromCode = "USD",
            toCode = "VND",
            rates = sampleRates,
        ).setScale(0, RoundingMode.HALF_UP)
        assertThat(usdToVnd).isEqualTo(BigDecimal("50000"))

        val gbpToEur = CurrencyConverter.convertBetween(
            amount = BigDecimal("10"),
            fromCode = "GBP",
            toCode = "EUR",
            rates = sampleRates,
        ).setScale(2, RoundingMode.HALF_UP)
        assertThat(gbpToEur).isEqualTo(BigDecimal("12.00"))

        val identity = CurrencyConverter.convertBetween(
            amount = BigDecimal("123.45"),
            fromCode = "USD",
            toCode = "usd",
            rates = sampleRates,
        )
        assertThat(identity).isEqualTo(BigDecimal("123.45"))
    }

    @Test
    fun `parseFlexibleAmount handles grouping dots, decimal dots, commas, and invalid inputs`() {
        assertThat(CurrencyConverter.parseFlexibleAmount("1.000.000", allowDotAsThousands = true))
            .isEqualTo(BigDecimal("1000000"))
        assertThat(CurrencyConverter.parseFlexibleAmount("500.000", allowDotAsThousands = true))
            .isEqualTo(BigDecimal("500000"))
        assertThat(CurrencyConverter.parseFlexibleAmount("12.50", allowDotAsThousands = false))
            .isEqualTo(BigDecimal("12.50"))
        assertThat(CurrencyConverter.parseFlexibleAmount("12,50", allowDotAsThousands = false))
            .isEqualTo(BigDecimal("12.50"))
        assertThat(CurrencyConverter.parseFlexibleAmount("   ", allowDotAsThousands = true))
            .isNull()
        assertThat(CurrencyConverter.parseFlexibleAmount("abc", allowDotAsThousands = false))
            .isNull()
    }

    @Test
    fun `ratePerVnd and vndPerUnit use live rates and fallback when map is absent`() {
        assertThat(CurrencyConverter.ratePerVnd("USD", sampleRates)).isEqualTo(BigDecimal("0.00004"))
        assertThat(CurrencyConverter.vndPerUnit("USD", sampleRates).setScale(0, RoundingMode.HALF_UP))
            .isEqualTo(BigDecimal("25000"))

        val fallbackVndPerGbp = CurrencyConverter.vndPerUnit("GBP", rates = null)
            .setScale(0, RoundingMode.HALF_UP)
        assertThat(fallbackVndPerGbp).isEqualTo(BigDecimal("32500"))
    }
}
