package com.notepay.feature.settings.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.data.remote.CurrencyRateRepository
import com.notepay.data.remote.CurrencyRateSnapshot
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.CurrencyConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import javax.inject.Inject

data class CurrencyConverterUiState(
    val amountInput: String = "1000000",
    val sourceCurrency: String = "VND",
    val targetCurrency: String = "USD",
    val rates: CurrencyRateSnapshot,
    val isLoading: Boolean = false,
    val refreshFailed: Boolean = false,
)

@HiltViewModel
class CurrencyConverterViewModel @Inject constructor(
    private val currencyRateRepository: CurrencyRateRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private var refreshJob: Job? = null

    private val _uiState = MutableStateFlow(
        CurrencyConverterUiState(
            targetCurrency = defaultTargetCurrency(),
            rates = currencyRateRepository.offlineFallbackSnapshot(),
            isLoading = true,
        ),
    )
    val uiState: StateFlow<CurrencyConverterUiState> = _uiState.asStateFlow()

    init {
        refreshJob = viewModelScope.launch {
            withContext(ioDispatcher) {
                currencyRateRepository.readCachedSnapshot()
            }?.let { cached ->
                _uiState.update { it.copy(rates = cached) }
            }
            fetchRatesInternal(forceRefresh = false)
        }
    }

    fun setAmount(value: String) {
        if (value.isEmpty() || INPUT_PATTERN.matches(value)) {
            _uiState.update { it.copy(amountInput = value) }
        }
    }

    fun applyQuickAmount(raw: String) {
        if (INPUT_PATTERN.matches(raw)) {
            _uiState.update { it.copy(amountInput = raw) }
        }
    }

    fun selectSourceCurrency(code: String) {
        val normalized = code.uppercase(Locale.ROOT)
        if (normalized !in SUPPORTED_CURRENCIES) return
        _uiState.update { state ->
            if (state.sourceCurrency == normalized) return@update state
            val nextTarget = if (normalized == state.targetCurrency) state.sourceCurrency else state.targetCurrency
            state.copy(sourceCurrency = normalized, targetCurrency = nextTarget)
        }
    }

    fun selectTargetCurrency(code: String) {
        val normalized = code.uppercase(Locale.ROOT)
        if (normalized !in SUPPORTED_CURRENCIES) return
        _uiState.update { state ->
            if (state.targetCurrency == normalized) return@update state
            val nextSource = if (normalized == state.sourceCurrency) state.targetCurrency else state.sourceCurrency
            state.copy(sourceCurrency = nextSource, targetCurrency = normalized)
        }
    }

    fun swapCurrencies() {
        _uiState.update { state ->
            state.copy(
                sourceCurrency = state.targetCurrency,
                targetCurrency = state.sourceCurrency,
            )
        }
    }

    fun refreshRates(forceRefresh: Boolean = true) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            fetchRatesInternal(forceRefresh = forceRefresh)
        }
    }

    private suspend fun fetchRatesInternal(forceRefresh: Boolean) {
        _uiState.update { it.copy(isLoading = true, refreshFailed = false) }
        try {
            val snapshot = currencyRateRepository.getVndRates(forceRefresh = forceRefresh)
            _uiState.update { it.copy(rates = snapshot, isLoading = false, refreshFailed = false) }
        } catch (cancelled: CancellationException) {
            _uiState.update { it.copy(isLoading = false) }
            throw cancelled
        } catch (_: Exception) {
            _uiState.update { it.copy(isLoading = false, refreshFailed = true) }
        }
    }

    fun convertedAmount(state: CurrencyConverterUiState = _uiState.value): BigDecimal? {
        val allowDotAsThousands = state.sourceCurrency in CurrencyConverter.zeroDecimalCurrencies
        val parsed = CurrencyConverter.parseFlexibleAmount(state.amountInput, allowDotAsThousands) ?: return null
        val raw = CurrencyConverter.convertBetween(
            amount = parsed,
            fromCode = state.sourceCurrency,
            toCode = state.targetCurrency,
            rates = state.rates.rates,
        )
        return raw.setScale(fractionDigitsFor(state.targetCurrency, raw), RoundingMode.HALF_UP)
    }

    fun unitRate(state: CurrencyConverterUiState = _uiState.value): BigDecimal {
        val raw = CurrencyConverter.convertBetween(
            amount = BigDecimal.ONE,
            fromCode = state.sourceCurrency,
            toCode = state.targetCurrency,
            rates = state.rates.rates,
        )
        return raw.setScale(unitRateScaleFor(state.targetCurrency, raw), RoundingMode.HALF_UP)
    }

    fun quickReferenceRows(state: CurrencyConverterUiState = _uiState.value): List<Pair<String, BigDecimal>> {
        val allowDotAsThousands = state.sourceCurrency in CurrencyConverter.zeroDecimalCurrencies
        val parsed = CurrencyConverter.parseFlexibleAmount(state.amountInput, allowDotAsThousands) ?: return emptyList()
        return SUPPORTED_CURRENCIES
            .filter { it != state.sourceCurrency }
            .map { code ->
                val raw = CurrencyConverter.convertBetween(
                    amount = parsed,
                    fromCode = state.sourceCurrency,
                    toCode = code,
                    rates = state.rates.rates,
                )
                code to raw.setScale(fractionDigitsFor(code, raw), RoundingMode.HALF_UP)
            }
    }

    companion object {
        val SUPPORTED_CURRENCIES: List<String> = CurrencyConverter.supportedCurrencies.map { it.code }
        private val INPUT_PATTERN = Regex("^[\\d.,\\s]{0,24}$")

        private fun defaultTargetCurrency(): String {
            val active = MoneyFormatter.config.currencyCode.uppercase(Locale.ROOT)
            return if (active != "VND" && active in SUPPORTED_CURRENCIES) active else "USD"
        }

        private fun fractionDigitsFor(currencyCode: String, amount: BigDecimal): Int {
            val isTiny = amount > BigDecimal.ZERO && amount < BigDecimal("0.01")
            return when {
                isTiny -> 4
                currencyCode in CurrencyConverter.zeroDecimalCurrencies -> {
                    if (amount > BigDecimal.ZERO && amount < BigDecimal.ONE) 2 else 0
                }
                else -> 2
            }
        }

        private fun unitRateScaleFor(currencyCode: String, raw: BigDecimal): Int = when {
            raw < BigDecimal("0.01") -> 6
            raw < BigDecimal.ONE -> 4
            currencyCode in CurrencyConverter.zeroDecimalCurrencies -> 0
            else -> 2
        }
    }
}
