package com.notepay.ui.feature.settings.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.data.remote.CurrencyRateRepository
import com.notepay.data.remote.CurrencyRateSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CurrencyConverterUiState(
    val amountVnd: String = "1000000",
    val targetCurrency: String = "USD",
    val rates: CurrencyRateSnapshot? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class CurrencyConverterViewModel @Inject constructor(
    private val currencyRateRepository: CurrencyRateRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CurrencyConverterUiState(isLoading = true))
    val uiState: StateFlow<CurrencyConverterUiState> = _uiState.asStateFlow()

    init {
        refreshRates()
    }

    fun setAmount(value: String) {
        // Keep input numeric and editable; accept one decimal separator.
        if (value.isEmpty() || value.matches(Regex("^\\d{0,15}([.,]\\d{0,2})?$"))) {
            _uiState.update { it.copy(amountVnd = value) }
        }
    }

    fun selectTargetCurrency(code: String) {
        if (code in SUPPORTED_CURRENCIES) {
            _uiState.update { it.copy(targetCurrency = code) }
        }
    }

    fun refreshRates(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val snapshot = currencyRateRepository.getVndRates(forceRefresh = forceRefresh)
                _uiState.update { it.copy(rates = snapshot, isLoading = false, errorMessage = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (it.rates == null) {
                            "Không thể tải tỷ giá. Kiểm tra kết nối mạng và thử lại."
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    fun convertedAmount(): String? {
        val state = _uiState.value
        val amount = state.amountVnd.replace(',', '.').toBigDecimalOrNull() ?: return null
        val rate = state.rates?.rates?.get(state.targetCurrency) ?: return null
        val fractionDigits = if (state.targetCurrency in ZERO_DECIMAL_CURRENCIES) 0 else 2
        return runCatching {
            amount.multiply(rate).setScale(fractionDigits, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString()
        }.getOrNull()
    }

    companion object {
        val SUPPORTED_CURRENCIES = listOf("USD", "EUR", "JPY", "KRW")
        private val ZERO_DECIMAL_CURRENCIES = setOf("JPY", "KRW")
    }
}
