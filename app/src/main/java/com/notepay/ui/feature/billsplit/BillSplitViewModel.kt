package com.notepay.ui.feature.billsplit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.data.remote.VietQrBankRepository
import com.notepay.domain.model.BillSplit
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.VietQrBank
import com.notepay.domain.repository.BillSplitRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.ui.feedback.FeedbackType
import com.notepay.ui.feedback.UiFeedback
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BillSplitViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val billSplitRepository: BillSplitRepository,
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
    private val addTransaction: AddTransactionUseCase,
    private val vietQrBankRepository: VietQrBankRepository,
) : ViewModel() {

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    private val _banks = MutableStateFlow<List<VietQrBank>>(emptyList())

    private fun str(resId: Int): String = appContext.getString(resId)
    private fun str(resId: Int, vararg args: Any): String = appContext.getString(resId, *args)

    init {
        viewModelScope.launch {
            _banks.value = vietQrBankRepository.getBanks()
        }
    }

    val state = combine(
        billSplitRepository.observeUnpaid(),
        billSplitRepository.observePaid(),
        transactionRepository.observeAll(),
        walletRepository.observeAll(),
        _banks,
    ) { unpaid, paid, transactions, wallets, banks ->
        val activeWallet = wallets.find { it.isActive } ?: wallets.firstOrNull()
        val unpaidItems = unpaid.map { split ->
            val parent = transactions.find { it.id == split.transactionId }
            val wallet = wallets.find { it.id == parent?.walletId }
            BillSplitItemState(split, parent, wallet)
        }

        val paidItems = paid.map { split ->
            val parent = transactions.find { it.id == split.transactionId }
            BillSplitItemState(split, parent, null)
        }

        BillSplitUiState(
            unpaidSplits = unpaidItems,
            paidSplits = paidItems,
            recentTransactions = transactions,
            wallets = wallets,
            activeWallet = activeWallet,
            banks = banks,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BillSplitUiState(),
    )

    fun createBillSplits(transactionId: Long, splits: List<Pair<String, Long>>) {
        viewModelScope.launch {
            try {
                val newSplits = splits.map { (debtorName, amountCents) ->
                    val memoCode = com.notepay.domain.billsplit.BillSplitEngine.formatMemoCode(transactionId, debtorName)

                    BillSplit(
                        id = 0L,
                        transactionId = transactionId,
                        debtorName = debtorName,
                        amount = Money(amountCents),
                        isPaid = false,
                        memoCode = memoCode,
                        paidAt = null,
                        createdAt = Clock.System.now(),
                    )
                }
                billSplitRepository.upsertAll(newSplits)
                _feedback.emit(UiFeedback(str(R.string.feedback_bill_split_created), type = FeedbackType.Success))
            } catch (e: Exception) {
                _feedback.emit(UiFeedback(str(R.string.feedback_bill_split_create_failed), type = FeedbackType.Error))
            }
        }
    }

    fun markAsPaidManually(splitId: Long) {
        viewModelScope.launch {
            try {
                val split = billSplitRepository.getById(splitId) ?: error(str(R.string.error_bill_split_not_found))
                if (split.isPaid) return@launch

                billSplitRepository.markAsPaid(splitId, Clock.System.now())
                _feedback.emit(UiFeedback(str(R.string.feedback_bill_split_marked_paid), type = FeedbackType.Success))
            } catch (e: Exception) {
                _feedback.emit(UiFeedback(str(R.string.feedback_bill_split_mark_failed), type = FeedbackType.Error))
            }
        }
    }

    fun deleteBillSplit(splitId: Long) {
        viewModelScope.launch {
            try {
                billSplitRepository.delete(splitId)
                _feedback.emit(UiFeedback(str(R.string.feedback_bill_split_deleted), type = FeedbackType.Success))
            } catch (e: Exception) {
                _feedback.emit(UiFeedback(str(R.string.feedback_bill_split_delete_failed), type = FeedbackType.Error))
            }
        }
    }

    fun updateWalletForQr(
        walletId: Long,
        bankBin: String,
        accountNumber: String,
        accountName: String,
    ) {
        viewModelScope.launch {
            try {
                val current = walletRepository.observeAll().firstOrNull()?.find { it.id == walletId }
                    ?: error(str(R.string.error_wallet_not_found))
                val updated = current.copy(
                    bankBin = bankBin.ifBlank { null },
                    accountNumber = accountNumber.ifBlank { null },
                    accountName = accountName.ifBlank { null },
                )
                walletRepository.upsert(updated)
                _feedback.emit(UiFeedback(str(R.string.feedback_vietqr_saved), type = FeedbackType.Success))
            } catch (e: Exception) {
                _feedback.emit(UiFeedback(str(R.string.feedback_vietqr_failed), type = FeedbackType.Error))
            }
        }
    }

    fun markDebtorAsPaid(debtorName: String, splitIds: List<Long>) {
        markDebtorAsPaidWithReconciliation(debtorName, splitIds, null)
    }

    fun markDebtorAsPaidWithReconciliation(debtorName: String, splitIds: List<Long>, incomeTxId: Long?) {
        viewModelScope.launch {
            try {
                val splitsToProcess = splitIds.mapNotNull { splitId ->
                    billSplitRepository.getById(splitId)?.takeUnless { it.isPaid }
                }
                splitsToProcess.forEach { split ->
                    billSplitRepository.markAsPaid(split.id, Clock.System.now())
                }

                val msg = if (incomeTxId != null) {
                    str(R.string.feedback_bill_split_reconciled)
                } else {
                    str(R.string.feedback_bill_split_marked_paid)
                }
                _feedback.emit(UiFeedback(msg, type = FeedbackType.Success))
            } catch (e: Exception) {
                _feedback.emit(UiFeedback(str(R.string.feedback_bill_split_mark_failed), type = FeedbackType.Error))
            }
        }
    }

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }
}
