package com.notepay.feature.assets.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.data.remote.CurrencyRateRepository
import com.notepay.data.remote.CurrencyRateSnapshot
import com.notepay.domain.model.CurrencyConverter
import com.notepay.domain.model.TransactionNotePolicy
import com.notepay.domain.money.Money
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.TransferBetweenWalletsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Root-scoped ViewModel powering [TransferBottomSheet] from the Quick Add menu (`NotePayNavHost`).
 */
@HiltViewModel
class QuickTransferViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    walletRepository: WalletRepository,
    transactionRepository: TransactionRepository,
    private val transferBetweenWalletsUseCase: TransferBetweenWalletsUseCase,
    private val currencyRateRepository: CurrencyRateRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(TransferFormState())
    val form: StateFlow<TransferFormState> = _form.asStateFlow()

    private val _ratesSnapshot = MutableStateFlow<CurrencyRateSnapshot?>(null)

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    init {
        viewModelScope.launch {
            _ratesSnapshot.value = runCatching { currencyRateRepository.getVndRates() }.getOrNull()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val wallets: StateFlow<List<WalletAssetItem>> = assetsPeriodFlow()
        .flatMapLatest { period ->
            combine(
                walletRepository.observeAll(),
                transactionRepository.observeWalletStats(period.monthStartMillis, period.monthEndMillis),
                _ratesSnapshot,
            ) { allWallets, statsList, ratesSnapshot ->
                val statsByWallet = statsList.associateBy { it.walletId }
                val rates = ratesSnapshot?.rates
                allWallets
                    .filter { !it.isArchived }
                    .map { wallet ->
                        val stats = statsByWallet[wallet.id]
                        val netBalance = wallet.initialBalance +
                            (stats?.allTimeIncome ?: Money.ZERO) -
                            (stats?.allTimeExpense ?: Money.ZERO)
                        val convertedCents = CurrencyConverter.convertToVndCents(
                            amountCents = netBalance.amountInCents,
                            fromCurrency = wallet.currencyCode,
                            rates = rates,
                        )
                        WalletAssetItem(
                            wallet = wallet,
                            balance = netBalance,
                            convertedBalanceVnd = Money(convertedCents),
                            monthlyIncome = stats?.currentMonthIncome ?: Money.ZERO,
                            monthlyExpense = stats?.currentMonthExpense ?: Money.ZERO,
                            transactionCount = stats?.txCount ?: 0,
                        )
                    }
            }
        }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun open(fromWalletId: Long? = null) {
        if (_form.value.isSubmitting) return
        val currentWallets = wallets.value
        val fromId = fromWalletId ?: currentWallets.firstOrNull()?.wallet?.id
        val toId = currentWallets.firstOrNull { it.wallet.id != fromId }?.wallet?.id
        _form.value = TransferFormState(
            isVisible = true,
            fromWalletId = fromId,
            toWalletId = toId,
        )
    }

    fun close() {
        _form.update { state ->
            if (state.isSubmitting) state else state.copy(isVisible = false)
        }
    }

    fun onFromWalletChanged(walletId: Long) {
        val walletIds = wallets.value.map { it.wallet.id }
        _form.update { state ->
            if (state.isSubmitting) return@update state
            val nextTo = if (state.toWalletId == walletId) {
                walletIds.firstOrNull { it != walletId }
            } else {
                state.toWalletId
            }
            state.copy(fromWalletId = walletId, toWalletId = nextTo, submitError = null)
        }
    }

    fun onToWalletChanged(walletId: Long) {
        _form.update { state ->
            if (state.isSubmitting) state else state.copy(toWalletId = walletId, submitError = null)
        }
    }

    fun onAmountChanged(input: String) {
        _form.update { state ->
            if (state.isSubmitting) {
                state
            } else {
                state.copy(amountInput = input.filter(Char::isDigit), submitError = null)
            }
        }
    }

    fun onNoteChanged(note: String) {
        _form.update { state ->
            if (state.isSubmitting) state else state.copy(note = note)
        }
    }

    fun executeTransfer() {
        val current = _form.value
        if (current.isSubmitting) return
        when (val validation = validateTransfer(current.fromWalletId, current.toWalletId, current.amountInput, wallets.value)) {
            is TransferValidation.Invalid -> Unit
            is TransferValidation.Valid -> submitTransfer(validation, current.note)
        }
    }

    private fun submitTransfer(transfer: TransferValidation.Valid, rawNote: String) {
        val noteText = rawNote.ifBlank { context.getString(R.string.transfer_note_default) }
        _form.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            try {
                val result = transferBetweenWalletsUseCase(
                    fromWalletId = transfer.from.wallet.id,
                    toWalletId = transfer.to.wallet.id,
                    amount = transfer.amount,
                    outgoingNote = TransactionNotePolicy.truncate(
                        context.getString(R.string.transfer_note_to, transfer.to.wallet.name, noteText),
                    ),
                    incomingNote = TransactionNotePolicy.truncate(
                        context.getString(R.string.transfer_note_from, transfer.from.wallet.name, noteText),
                    ),
                )
                if (result.isSuccess) {
                    _form.value = TransferFormState()
                    _feedback.tryEmit(
                        UiFeedback(
                            message = context.getString(R.string.feedback_transfer_success),
                            type = FeedbackType.Success,
                        ),
                    )
                } else {
                    _form.update { it.copy(submitError = TransferError.Failed) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _form.update { it.copy(submitError = TransferError.Failed) }
            } finally {
                _form.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
