package com.notepay.ui.feature.billsplit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.model.BillSplit
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.BillSplitRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.ui.util.VietQrGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import javax.inject.Inject

@HiltViewModel
class BillSplitViewModel @Inject constructor(
    private val billSplitRepository: BillSplitRepository,
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
    private val addTransaction: AddTransactionUseCase
) : ViewModel() {

    val state = combine(
        billSplitRepository.observeUnpaid(),
        billSplitRepository.observePaid(),
        transactionRepository.observeAll(),
        walletRepository.observeAll()
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
                    memo = split.memoCode
                )
            } else null
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
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BillSplitUiState()
    )

    /**
     * Tạo khoản chia tiền mới cho một giao dịch.
     *
     * @param transactionId Giao dịch chi tiêu gốc.
     * @param splits Danh sách tên người nợ và số tiền nợ (cents).
     */
    fun createBillSplits(transactionId: Long, splits: List<Pair<String, Long>>) {
        viewModelScope.launch {
            val newSplits = splits.map { (debtorName, amountCents) ->
                // Tạo mã chuyển khoản duy nhất, vd: NP15 BAN A
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
                    createdAt = Clock.System.now()
                )
            }
            billSplitRepository.upsertAll(newSplits)
        }
    }

    /**
     * Đánh dấu đã thanh toán thủ công (trả tiền mặt).
     *
     * @param splitId ID của khoản chia tiền.
     */
    fun markAsPaidManually(splitId: Long) {
        viewModelScope.launch {
            val split = billSplitRepository.getById(splitId) ?: return@launch
            if (split.isPaid) return@launch

            // 1. Đánh dấu đã trả trong DB
            billSplitRepository.markAsPaid(splitId, Clock.System.now())

            // 2. Lấy ví và giao dịch gốc để ghi nhận khoản thu nhập
            val parentTx = transactionRepository.getById(split.transactionId) ?: return@launch
            
            val incomeTx = Transaction(
                id = 0L,
                amount = split.amount,
                type = TransactionType.INCOME,
                category = com.notepay.domain.model.Category.DEFAULT_INCOME,
                note = "${split.debtorName} trả tiền: ${parentTx.note}",
                occurredAt = Clock.System.now(),
                walletId = parentTx.walletId
            )
            addTransaction(incomeTx)
        }
    }

    fun deleteBillSplit(splitId: Long) {
        viewModelScope.launch {
            billSplitRepository.delete(splitId)
        }
    }

    /**
     * Cập nhật nhanh thông tin ngân hàng của ví để có thể tạo VietQR.
     */
    fun updateWalletForQr(
        walletId: Long,
        bankBin: String,
        accountNumber: String,
        accountName: String,
    ) {
        viewModelScope.launch {
            val current = walletRepository.observeAll().firstOrNull()?.find { it.id == walletId }
                ?: return@launch
            val updated = current.copy(
                bankBin = bankBin.ifBlank { null },
                accountNumber = accountNumber.ifBlank { null },
                accountName = accountName.ifBlank { null },
            )
            walletRepository.upsert(updated)
        }
    }

    /**
     * Đánh dấu đã thanh toán tất cả các khoản nợ của một người nợ cùng lúc.
     */
    fun markDebtorAsPaid(debtorName: String, splitIds: List<Long>) {
        viewModelScope.launch {
            splitIds.forEach { splitId ->
                val split = billSplitRepository.getById(splitId) ?: return@forEach
                if (split.isPaid) return@forEach

                // 1. Đánh dấu đã trả trong DB
                billSplitRepository.markAsPaid(splitId, Clock.System.now())

                // 2. Lấy giao dịch gốc để ghi nhận khoản thu nhập
                val parentTx = transactionRepository.getById(split.transactionId) ?: return@forEach
                
                val incomeTx = Transaction(
                    id = 0L,
                    amount = split.amount,
                    type = TransactionType.INCOME,
                    category = com.notepay.domain.model.Category.DEFAULT_INCOME,
                    note = "$debtorName trả tiền: ${parentTx.note}",
                    occurredAt = Clock.System.now(),
                    walletId = parentTx.walletId
                )
                addTransaction(incomeTx)
            }
        }
    }
}
