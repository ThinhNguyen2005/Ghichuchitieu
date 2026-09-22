package com.notepay.ui.feature.transaction.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val transactionId: Long =
        savedStateHandle.get<Long>(Route.TransactionDetail.ARG_ID) ?: -1L

    private val _state = MutableStateFlow(TransactionDetailUiState())
    val state = _state.asStateFlow()

    init {
        if (transactionId <= 0L) {
            _state.update { it.copy(isLoading = false, error = context.getString(R.string.error_transaction_not_found)) }
        } else {
            observeTransaction()
        }
    }

    private fun observeTransaction() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                transactionRepository.observeById(transactionId).collect { tx ->
                    val walletName = tx?.let { walletRepository.getById(it.walletId)?.name }
                    _state.update {
                        it.copy(
                            transaction = tx,
                            walletName = walletName,
                            isLoading = false,
                            error = if (tx == null) context.getString(R.string.error_transaction_not_found) else null,
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.error_transaction_load_failed),
                    )
                }
            }
        }
    }
}
