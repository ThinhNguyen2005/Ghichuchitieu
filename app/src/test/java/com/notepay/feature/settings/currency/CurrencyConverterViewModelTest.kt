package com.notepay.feature.settings.currency

import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.data.remote.CurrencyRateRepository
import com.notepay.data.remote.CurrencyRateSnapshot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyConverterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val offlineRates = CurrencyRateSnapshot(
        baseCode = "VND",
        rates = mapOf(
            "VND" to BigDecimal.ONE,
            "USD" to BigDecimal("0.0000392"),
            "GBP" to BigDecimal("0.0000307"),
            "EUR" to BigDecimal("0.0000363"),
            "JPY" to BigDecimal("0.00606"),
            "KRW" to BigDecimal("0.05405"),
            "SGD" to BigDecimal("0.000052"),
            "THB" to BigDecimal("0.00135"),
        ),
        lastUpdatedUtc = "",
        nextUpdateEpochSeconds = 0L,
        isOfflineFallback = true,
        sourceName = CurrencyRateRepository.SOURCE_OFFLINE,
    )

    private val liveRates = CurrencyRateSnapshot(
        baseCode = "VND",
        rates = mapOf(
            "VND" to BigDecimal.ONE,
            "USD" to BigDecimal("0.00004"), // 1 USD = 25,000 VND
            "GBP" to BigDecimal("0.00003125"), // 1 GBP = 32,000 VND
            "EUR" to BigDecimal("0.000036"),
            "JPY" to BigDecimal("0.006"),
            "KRW" to BigDecimal("0.052"),
            "SGD" to BigDecimal("0.00005"),
            "THB" to BigDecimal("0.00132"),
        ),
        lastUpdatedUtc = "2026-11-22",
        nextUpdateEpochSeconds = Long.MAX_VALUE,
        isOfflineFallback = false,
        sourceName = CurrencyRateRepository.SOURCE_ER_API,
    )

    @After
    fun tearDown() {
        MoneyFormatter.resetToDefault()
    }

    @Test
    fun `converts across 8 currencies, supports swapCurrencies, and parses dotted VND`() = runTest {
        MoneyFormatter.resetToDefault()
        val repo = mockk<CurrencyRateRepository> {
            every { offlineFallbackSnapshot() } returns offlineRates
            every { readCachedSnapshot() } returns null
            coEvery { getVndRates(any()) } returns liveRates
        }

        val vm = CurrencyConverterViewModel(
            currencyRateRepository = repo,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        vm.setAmount("1.000.000")
        vm.selectTargetCurrency("GBP")

        assertThat(vm.uiState.value.isLoading).isFalse()
        assertThat(vm.uiState.value.refreshFailed).isFalse()
        assertThat(vm.uiState.value.sourceCurrency).isEqualTo("VND")
        assertThat(vm.uiState.value.targetCurrency).isEqualTo("GBP")
        assertThat(vm.convertedAmount()).isEqualTo(BigDecimal("31.25"))

        vm.setAmount("10")
        vm.swapCurrencies()
        assertThat(vm.uiState.value.sourceCurrency).isEqualTo("GBP")
        assertThat(vm.uiState.value.targetCurrency).isEqualTo("VND")
        assertThat(vm.convertedAmount()).isEqualTo(BigDecimal("320000"))

        assertThat(vm.quickReferenceRows().map { it.first })
            .containsExactly("VND", "USD", "EUR", "JPY", "KRW", "SGD", "THB")
    }

    @Test
    fun `keeps offline fallback rates available and marks refreshFailed when API fails`() = runTest {
        val repo = mockk<CurrencyRateRepository> {
            every { offlineFallbackSnapshot() } returns offlineRates
            every { readCachedSnapshot() } returns null
            coEvery { getVndRates(any()) } throws IllegalStateException("offline")
        }

        val vm = CurrencyConverterViewModel(
            currencyRateRepository = repo,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        assertThat(vm.uiState.value.isLoading).isFalse()
        assertThat(vm.uiState.value.refreshFailed).isTrue()
        assertThat(vm.uiState.value.rates.isOfflineFallback).isTrue()
        assertThat(vm.convertedAmount()).isGreaterThan(BigDecimal.ZERO)
    }
}
