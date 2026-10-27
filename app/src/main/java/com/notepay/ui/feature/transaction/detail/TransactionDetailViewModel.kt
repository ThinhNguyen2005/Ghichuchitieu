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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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

    /**
     * Route độc lập lấy id từ nav args; ở chế độ hai pane thì để `null` rồi gọi [select]
     * khi người dùng chọn một dòng trong danh sách.
     */
    private val initialTransactionId: Long? =
        savedStateHandle.get<Long>(Route.TransactionDetail.ARG_ID)

    private val _state = MutableStateFlow(TransactionDetailUiState())
    val state = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        select(initialTransactionId)
    }

    /**
     * Đổi giao dịch đang hiển thị. Job cũ bị hủy trước khi quan sát id mới để tránh
     * hai luồng repo cùng ghi vào [state] khi người dùng bấm nhanh nhiều dòng.
     */
    fun select(transactionId: Long?) {
        observeJob?.cancel()
        if (transactionId == null || transactionId <= 0L) {
            _state.value = TransactionDetailUiState(
                isLoading = false,
                error = context.getString(R.string.error_transaction_not_found),
            )
            return
        }
        observeJob = viewModelScope.launch {
            _state.value = TransactionDetailUiState()
            transactionRepository.observeById(transactionId)
                .catch { error ->
                    if (error is CancellationException) throw error
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: context.getString(R.string.error_transaction_load_failed),
                        )
                    }
                }
                .collect { tx ->
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
        }
    }
}
