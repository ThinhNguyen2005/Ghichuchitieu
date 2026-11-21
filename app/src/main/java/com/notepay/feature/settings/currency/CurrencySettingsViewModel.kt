package com.notepay.feature.settings.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.data.datastore.AppSettingsDataStore
import com.notepay.data.remote.CurrencyRateRepository
import com.notepay.data.remote.CurrencyRateSnapshot
import com.notepay.domain.model.CurrencyConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CurrencyUiState(
    val currencyCode: String = "VND",
    val symbolPosition: String = "after", // "after" | "before"
    val thousandSeparator: String = "dot", // "dot" | "comma"
    val ratesSnapshot: CurrencyRateSnapshot? = null,
    val isRefreshingRates: Boolean = false,
    val rateRefreshFailed: Boolean = false,
)

@HiltViewModel
class CurrencySettingsViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
    private val currencyRateRepository: CurrencyRateRepository,
) : ViewModel() {

    private val _ratesSnapshot = MutableStateFlow(currencyRateRepository.offlineFallbackSnapshot())
    private val _rateLoadStatus = MutableStateFlow(false to false)
    private var refreshJob: Job? = null

    init {
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            currencyRateRepository.readCachedSnapshot()?.let { cached ->
                _ratesSnapshot.value = cached
            }
            refreshRatesInternal(forceRefresh = false)
        }
    }

    val uiState: StateFlow<CurrencyUiState> = combine(
        appSettingsDataStore.currencyCode,
        appSettingsDataStore.currencySymbolPosition,
        appSettingsDataStore.currencyThousandSeparator,
        _ratesSnapshot,
        _rateLoadStatus,
    ) { code, position, separator, snapshot, loadStatus ->
        val ratePerVnd = CurrencyConverter.ratePerVnd(code, snapshot.rates)
        MoneyFormatter.updateConfig(
            currencyCode = code,
            symbolPosition = position,
            thousandSeparator = separator,
            ratePerVnd = ratePerVnd,
        )
        CurrencyUiState(
            currencyCode = code,
            symbolPosition = position,
            thousandSeparator = separator,
            ratesSnapshot = snapshot,
            isRefreshingRates = loadStatus.first,
            rateRefreshFailed = loadStatus.second,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CurrencyUiState(ratesSnapshot = _ratesSnapshot.value),
    )

    fun refreshRates(forceRefresh: Boolean = true) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            refreshRatesInternal(forceRefresh = forceRefresh)
        }
    }

    private suspend fun refreshRatesInternal(forceRefresh: Boolean) {
        _rateLoadStatus.value = true to false
        try {
            val live = currencyRateRepository.getVndRates(forceRefresh = forceRefresh)
            _ratesSnapshot.value = live
            _rateLoadStatus.value = false to false
        } catch (cancelled: CancellationException) {
            _rateLoadStatus.value = false to false
            throw cancelled
        } catch (_: Exception) {
            _rateLoadStatus.value = false to true
        }
    }

    fun selectCurrency(code: String) {
        val ratePerVnd = CurrencyConverter.ratePerVnd(code, _ratesSnapshot.value.rates)
        MoneyFormatter.updateConfig(currencyCode = code, ratePerVnd = ratePerVnd)
        viewModelScope.launch {
            appSettingsDataStore.setCurrencyCode(code)
        }
    }

    fun setSymbolPosition(position: String) {
        MoneyFormatter.updateConfig(symbolPosition = position)
        viewModelScope.launch {
            appSettingsDataStore.setCurrencySymbolPosition(position)
        }
    }

    fun setThousandSeparator(separator: String) {
        MoneyFormatter.updateConfig(thousandSeparator = separator)
        viewModelScope.launch {
            appSettingsDataStore.setCurrencyThousandSeparator(separator)
        }
    }
}
