package com.notepay.ui.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.model.Transaction
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val walletName: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val isAutoCapture: Boolean
        get() = transaction?.isAutoCapture == true
}

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val transactionId: Long =
        savedStateHandle.get<Long>(Route.TransactionDetail.ARG_ID) ?: -1L

    private val _state = MutableStateFlow(TransactionDetailUiState())
    val state = _state.asStateFlow()

    init {
        if (transactionId <= 0L) {
            _state.update { it.copy(isLoading = false, error = "Không tìm thấy giao dịch") }
        } else {
            load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val tx = transactionRepository.getById(transactionId)
                val walletName = tx?.let { walletRepository.getById(it.walletId)?.name }
                _state.update {
                    it.copy(
                        transaction = tx,
                        walletName = walletName,
                        isLoading = false,
                        error = if (tx == null) "Không tìm thấy giao dịch" else null,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Lỗi tải giao dịch") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
