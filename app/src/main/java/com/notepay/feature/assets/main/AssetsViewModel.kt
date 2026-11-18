package com.notepay.feature.assets.main

import android.content.Context
import com.notepay.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.money.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionNotePolicy
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.model.WalletStats
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.TransferBetweenWalletsUseCase
import com.notepay.domain.usecase.debt.DebtSummary
import com.notepay.domain.usecase.debt.GetDebtSummaryUseCase
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class AssetsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val getDebtSummaryUseCase: GetDebtSummaryUseCase,
    private val transferBetweenWalletsUseCase: TransferBetweenWalletsUseCase,
) : ViewModel() {

    /**
     * Trạng thái tức thời của form chuyển khoản. Tách khỏi dữ liệu Room để luồng dữ liệu
     * không phải khởi động lại mỗi lần người dùng gõ số tiền.
     */
    private val _transferForm = MutableStateFlow(TransferFormState())
    private val retryVersion = MutableStateFlow(0)

    /** Form chuyển khoản là một nguồn riêng, không chép lại từng trường sang [AssetsUiState]. */
    val transferForm: StateFlow<TransferFormState> = _transferForm.asStateFlow()

    /**
     * Dữ liệu tài sản dẫn xuất từ Room. `WhileSubscribed(5_000)` giữ kết nối thêm 5 giây
     * sau khi UI ngừng quan sát (đổi tab, xoay màn hình) rồi mới huỷ, nên khi app xuống
     * background sẽ không còn tốn CPU để dựng lại phân bổ tài sản.
     */
    val state: StateFlow<AssetsUiState> = observeAssets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AssetsUiState(),
        )

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    /**
     * Kỳ tháng đi vào như một tham số của query: sang ngày mới thì kỳ được phát lại và
     * thống kê tháng tự nạp lại, thay vì đóng băng ở tháng lúc mở màn hình.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAssets(): Flow<AssetsUiState> = combine(assetsPeriodFlow(), retryVersion) { period, _ ->
        period
    }.flatMapLatest { period ->
        combine(
            walletRepository.observeAll(),
            transactionRepository.observeWalletStats(period.monthStartMillis, period.monthEndMillis),
            transactionRepository.observeRecentNonTransfers(period.comparisonStartMillis),
            getDebtSummaryUseCase(),
        ) { wallets, walletStats, nonTransferTxs, debtSummary ->
            buildAssetsState(wallets, walletStats, nonTransferTxs, debtSummary)
        }.onStart { emit(AssetsUiState()) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(AssetsUiState(isLoading = false, hasError = true))
            }
    }

    fun retryLoading() {
        retryVersion.update { it + 1 }
    }

    private fun buildAssetsState(
        wallets: List<Wallet>,
        walletStats: List<WalletStats>,
        nonTransferTxs: List<Transaction>,
        debtSummary: DebtSummary,
    ): AssetsUiState {
        // Một lượt tra cứu dùng chung thay vì find() lặp lại cho từng ví.
        val statsByWallet = walletStats.associateBy { it.walletId }
        var totalBalance = Money.ZERO

        val items = wallets.map { wallet ->
            val stats = statsByWallet[wallet.id]
            val netBalance = wallet.initialBalance +
                (stats?.allTimeIncome ?: Money.ZERO) -
                (stats?.allTimeExpense ?: Money.ZERO)
            totalBalance += netBalance

            WalletAssetItem(
                wallet = wallet,
                balance = netBalance,
                monthlyIncome = stats?.currentMonthIncome ?: Money.ZERO,
                monthlyExpense = stats?.currentMonthExpense ?: Money.ZERO,
                transactionCount = stats?.txCount ?: 0,
            )
        }

        // Dòng tiền ròng của cửa sổ so sánh = các giao dịch không phải chuyển khoản nội bộ.
        val netFlow = nonTransferTxs.fold(Money.ZERO) { acc, tx ->
            when (tx.type) {
                TransactionType.INCOME -> acc + tx.amount
                TransactionType.EXPENSE -> acc - tx.amount
            }
        }
        val startBalance = totalBalance - netFlow
        // Số dư đầu kỳ bằng 0 thì tỷ lệ không xác định (null), khác hẳn "không thay đổi".
        val netWorthChangePercentage = if (startBalance.amountInCents != 0L) {
            (netFlow.amountInCents.toDouble() / abs(startBalance.amountInCents.toDouble()) * 100.0).toFloat()
        } else {
            null
        }

        return AssetsUiState(
            totalNetWorth = totalBalance,
            wallets = items,
            isLoading = false,
            allocationItems = buildAllocation(items),
            netWorthChangePercentage = netWorthChangePercentage,
            debtSummary = debtSummary,
        )
    }

    fun setActiveWallet(walletId: Long) {
        viewModelScope.launch {
            try {
                walletRepository.setActive(walletId)
                _feedback.tryEmit(UiFeedback(message = context.getString(R.string.feedback_wallet_set_active), type = FeedbackType.Success))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _feedback.tryEmit(UiFeedback(message = context.getString(R.string.feedback_wallet_switch_failed), type = FeedbackType.Error))
            }
        }
    }

    fun openTransferSheet(fromWalletId: Long? = null) {
        // Đang gửi thì không mở form mới: kết quả lần gửi trước sẽ ghi đè form này.
        if (_transferForm.value.isSubmitting) return
        val currentWallets = state.value.wallets
        val fromId = fromWalletId ?: currentWallets.firstOrNull()?.wallet?.id
        val toId = currentWallets.firstOrNull { it.wallet.id != fromId }?.wallet?.id
        _transferForm.value = TransferFormState(
            isVisible = true,
            fromWalletId = fromId,
            toWalletId = toId,
        )
    }

    fun closeTransferSheet() {
        // Đang gửi thì giữ sheet lại để kết quả không rơi vào hư không.
        _transferForm.update { form ->
            if (form.isSubmitting) form else form.copy(isVisible = false)
        }
    }

    fun onTransferFromWalletChanged(walletId: Long) {
        val walletIds = state.value.wallets.map { it.wallet.id }
        _transferForm.update { form ->
            if (form.isSubmitting) return@update form
            val toId = if (form.toWalletId == walletId) {
                walletIds.firstOrNull { it != walletId }
            } else {
                form.toWalletId
            }
            form.copy(
                fromWalletId = walletId,
                toWalletId = toId,
                submitError = null,
            )
        }
    }

    fun onTransferToWalletChanged(walletId: Long) {
        _transferForm.update { form ->
            if (form.isSubmitting) {
                form
            } else {
                form.copy(toWalletId = walletId, submitError = null)
            }
        }
    }

    /**
     * Chỉ nhận chữ số: [VietnamCurrencyVisualTransformation] định dạng chuỗi thô thành
     * "5.000.000 đ", nên state phải giữ dạng chữ số thuần.
     */
    fun onTransferAmountChanged(input: String) {
        _transferForm.update { form ->
            if (form.isSubmitting) {
                form
            } else {
                form.copy(
                    amountInput = input.filter(Char::isDigit),
                    submitError = null,
                )
            }
        }
    }

    fun onTransferNoteChanged(note: String) {
        _transferForm.update { form ->
            if (form.isSubmitting) form else form.copy(note = note)
        }
    }

    fun executeTransfer() {
        val form = _transferForm.value
        // Chống bấm đúp: nút trên UI chỉ tắt sau khi recompose, ViewModel phải tự chặn.
        if (form.isSubmitting) return

        when (val validation = validateTransfer(
            fromWalletId = form.fromWalletId,
            toWalletId = form.toWalletId,
            amountInput = form.amountInput,
            wallets = state.value.wallets,
        )) {
            // Lỗi nhập liệu đã hiện ngay tại trường tương ứng nên không báo thêm lần nữa.
            is TransferValidation.Invalid -> Unit
            is TransferValidation.Valid -> submitTransfer(validation, form.note)
        }
    }

    private fun submitTransfer(transfer: TransferValidation.Valid, rawNote: String) {
        val noteText = rawNote.ifBlank { context.getString(R.string.transfer_note_default) }
        _transferForm.update { it.copy(isSubmitting = true, submitError = null) }

        viewModelScope.launch {
            try {
                val result = transferBetweenWalletsUseCase(
                    fromWalletId = transfer.from.wallet.id,
                    toWalletId = transfer.to.wallet.id,
                    amount = transfer.amount,
                    outgoingNote = TransactionNotePolicy.truncate(context.getString(
                        R.string.transfer_note_to,
                        transfer.to.wallet.name,
                        noteText,
                    )),
                    incomingNote = TransactionNotePolicy.truncate(context.getString(
                        R.string.transfer_note_from,
                        transfer.from.wallet.name,
                        noteText,
                    )),
                )
                if (result.isSuccess) {
                    _transferForm.value = TransferFormState()
                    _feedback.tryEmit(
                        UiFeedback(
                            message = context.getString(R.string.feedback_transfer_success),
                            type = FeedbackType.Success,
                        ),
                    )
                } else {
                    _transferForm.update { it.copy(submitError = TransferError.Failed) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _transferForm.update { it.copy(submitError = TransferError.Failed) }
            } finally {
                // Không để form kẹt ở trạng thái "đang gửi" dù use case trả Result hay ném lỗi.
                _transferForm.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
