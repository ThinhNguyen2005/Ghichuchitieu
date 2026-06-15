package com.notepay.ui.feature.billsplit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.model.BillSplit
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.repository.BillSplitRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.ui.feedback.FeedbackType
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.VietQrGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class BillSplitViewModel @Inject constructor(
    private val billSplitRepository: BillSplitRepository,
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
    private val addTransaction: AddTransactionUseCase,
) : ViewModel() {

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    val state = combine(
        billSplitRepository.observeUnpaid(),
        billSplitRepository.observePaid(),
        transactionRepository.observeAll(),
        walletRepository.observeAll(),
    ) { unpaid, paid, transactions, wallets ->
        val activeWallet = wallets.find { it.isActive } ?: wallets.firstOrNull()
        val unpaidItems = unpaid.map { split ->
            val parent = transactions.find { it.id == split.transactionId }
            val wallet = wallets.find { it.id == parent?.walletId }
            val qr = if (wallet?.bankBin != null && wallet.accountNumber != null) {
                VietQrGenerator.generate(
                    bankBin = wallet.bankBin,
                    accountNumber = wallet.accountNumber,
                    amountCents = split.amount.amountInCents,
                    memo = split.memoCode,
                )
            } else {
                null
            }
            BillSplitItemState(split, parent, qr, wallet)
        }

        val paidItems = paid.map { split ->
            val parent = transactions.find { it.id == split.transactionId }
            BillSplitItemState(split, parent, null, null)
        }

        BillSplitUiState(
            unpaidSplits = unpaidItems,
            paidSplits = paidItems,
            recentTransactions = transactions,
            wallets = wallets,
            activeWallet = activeWallet,
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
                    val sanitizedDebtor = debtorName.filter { it.isLetterOrDigit() || it.isWhitespace() }.trim()
                    val memoCode = "NP${transactionId} ${sanitizedDebtor.uppercase()}"

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
                _feedback.emit(UiFeedback("Đã tạo khoản chia tiền", type = FeedbackType.Success))
            } catch (e: Exception) {
                _feedback.emit(UiFeedback("Không thể tạo khoản chia tiền", type = FeedbackType.Error))
            }
        }
    }

    fun markAsPaidManually(splitId: Long) {
        viewModelScope.launch {
            try {
                val split = billSplitRepository.getById(splitId) ?: error("Không tìm thấy khoản chia tiền")
                if (split.isPaid) return@launch

                billSplitRepository.markAsPaid(splitId, Clock.System.now())
                reduceParentTransaction(split.debtorName, listOf(split))
                _feedback.emit(UiFeedback("Đã ghi nhận thanh toán", type = FeedbackType.Success))
            } catch (e: Exception) {
                _feedback.emit(UiFeedback("Không thể ghi nhận thanh toán", type = FeedbackType.Error))
            }
        }
    }

    fun deleteBillSplit(splitId: Long) {
        viewModelScope.launch {
            try {
                billSplitRepository.delete(splitId)
                _feedback.emit(UiFeedback("Đã xóa khoản chia tiền", type = FeedbackType.Success))
            } catch (e: Exception) {
                _feedback.emit(UiFeedback("Không thể xóa khoản chia tiền", type = FeedbackType.Error))
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
                    ?: error("Không tìm thấy ví")
                val updated = current.copy(
                    bankBin = bankBin.ifBlank { null },
                    accountNumber = accountNumber.ifBlank { null },
                    accountName = accountName.ifBlank { null },
                )
                walletRepository.upsert(updated)
                _feedback.emit(UiFeedback("Đã cập nhật cấu hình VietQR", type = FeedbackType.Success))
            } catch (e: Exception) {
                _feedback.emit(UiFeedback("Không thể cập nhật cấu hình VietQR", type = FeedbackType.Error))
            }
        }
    }

    fun markDebtorAsPaid(debtorName: String, splitIds: List<Long>) {
        viewModelScope.launch {
            try {
                val splitsToProcess = splitIds.mapNotNull { splitId ->
                    billSplitRepository.getById(splitId)?.takeUnless { it.isPaid }
                }
                val splitsByTx = splitsToProcess.groupBy { it.transactionId }

                splitsByTx.forEach { (_, splits) ->
                    splits.forEach { split ->
                        billSplitRepository.markAsPaid(split.id, Clock.System.now())
                    }
                    reduceParentTransaction(debtorName, splits)
                }
                _feedback.emit(UiFeedback("Đã ghi nhận thanh toán", type = FeedbackType.Success))
            } catch (e: Exception) {
                _feedback.emit(UiFeedback("Không thể ghi nhận thanh toán", type = FeedbackType.Error))
            }
        }
    }

    private suspend fun reduceParentTransaction(debtorName: String, splits: List<BillSplit>) {
        val parentTx = transactionRepository.getById(splits.first().transactionId)
            ?: error("Không tìm thấy giao dịch gốc")
        var currentAmountCents = parentTx.amount.amountInCents
        var currentNote = parentTx.note

        splits.forEach { split ->
            currentAmountCents = (currentAmountCents - split.amount.amountInCents).coerceAtLeast(0L)
            val paidNote = "$debtorName trả ${MoneyFormatter.format(split.amount)}"
            currentNote = if (currentNote.contains(" trả ")) {
                "$currentNote, $paidNote"
            } else {
                "$currentNote ($paidNote)"
            }.take(Transaction.MAX_NOTE_LENGTH)
        }

        transactionRepository.upsert(
            parentTx.copy(
                amount = Money(currentAmountCents),
                note = currentNote,
            ),
        )
    }

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }
}
