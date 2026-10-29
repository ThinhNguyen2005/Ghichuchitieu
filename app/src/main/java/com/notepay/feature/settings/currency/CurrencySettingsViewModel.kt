package com.notepay.ui.feature.settings.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.data.preferences.AppSettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
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
)

@HiltViewModel
class CurrencySettingsViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
) : ViewModel() {

    val uiState: StateFlow<CurrencyUiState> = combine(
        appSettingsDataStore.currencyCode,
        appSettingsDataStore.currencySymbolPosition,
        appSettingsDataStore.currencyThousandSeparator,
    ) { code, position, separator ->
        CurrencyUiState(
            currencyCode = code,
            symbolPosition = position,
            thousandSeparator = separator,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CurrencyUiState(),
    )

    fun selectCurrency(code: String) {
        viewModelScope.launch {
            appSettingsDataStore.setCurrencyCode(code)
        }
    }

    fun setSymbolPosition(position: String) {
        viewModelScope.launch {
            appSettingsDataStore.setCurrencySymbolPosition(position)
        }
    }

    fun setThousandSeparator(separator: String) {
        viewModelScope.launch {
            appSettingsDataStore.setCurrencyThousandSeparator(separator)
        }
    }
}
