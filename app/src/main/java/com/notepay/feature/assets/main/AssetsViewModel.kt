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
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionNotePolicy
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.model.WalletStats
import com.notepay.domain.money.Money
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.TransferBetweenWalletsUseCase
import com.notepay.domain.usecase.debt.DebtSummary
import com.notepay.domain.usecase.debt.GetDebtSummaryUseCase
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class AssetsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val getDebtSummaryUseCase: GetDebtSummaryUseCase,
    private val transferBetweenWalletsUseCase: TransferBetweenWalletsUseCase,
    private val currencyRateRepository: CurrencyRateRepository,
) : ViewModel() {

    private val _transferForm = MutableStateFlow(TransferFormState())
    private val retryVersion = MutableStateFlow(0)
    private val _allocationMode = MutableStateFlow(AllocationMode.ASSET)
    private val _adjustBalanceWallet = MutableStateFlow<WalletAssetItem?>(null)
    private val _ratesSnapshot = MutableStateFlow<CurrencyRateSnapshot?>(null)

    val transferForm: StateFlow<TransferFormState> = _transferForm.asStateFlow()

    init {
        viewModelScope.launch {
            val snapshot = runCatching { currencyRateRepository.getVndRates() }.getOrNull()
            _ratesSnapshot.value = snapshot
        }
    }

    val state: StateFlow<AssetsUiState> = observeAssets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AssetsUiState(),
        )

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    private data class ObserveTuple(
        val period: AssetsPeriod,
        val allocMode: AllocationMode,
        val adjustWallet: WalletAssetItem?,
        val ratesSnapshot: CurrencyRateSnapshot?,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAssets(): Flow<AssetsUiState> = combine(
        assetsPeriodFlow(),
        retryVersion,
        _allocationMode,
        _adjustBalanceWallet,
        _ratesSnapshot,
    ) { period, _, allocMode, adjustWallet, ratesSnapshot ->
        ObserveTuple(period, allocMode, adjustWallet, ratesSnapshot)
    }.flatMapLatest { tuple ->
        combine(
            walletRepository.observeAll(),
            transactionRepository.observeWalletStats(tuple.period.monthStartMillis, tuple.period.monthEndMillis),
            transactionRepository.observeRecentNonTransfers(tuple.period.comparisonStartMillis),
            getDebtSummaryUseCase(),
        ) { wallets, walletStats, nonTransferTxs, debtSummary ->
            buildAssetsState(
                wallets = wallets,
                walletStats = walletStats,
                nonTransferTxs = nonTransferTxs,
                debtSummary = debtSummary,
                allocationMode = tuple.allocMode,
                adjustWallet = tuple.adjustWallet,
                ratesSnapshot = tuple.ratesSnapshot,
            )
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
        allocationMode: AllocationMode,
        adjustWallet: WalletAssetItem?,
        ratesSnapshot: CurrencyRateSnapshot?,
    ): AssetsUiState {
        val statsByWallet = walletStats.associateBy { it.walletId }
        val rates = ratesSnapshot?.rates

        val allItems = wallets.map { wallet ->
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

        val activeItems = allItems.filter { !it.wallet.isArchived }
        val archivedItems = allItems.filter { it.wallet.isArchived }

        val assetItemsRaw = activeItems.filter { it.wallet.isAsset }
        val liabilityItemsRaw = activeItems.filter { it.wallet.isLiability }

        val totalPositiveAssetVnd = assetItemsRaw
            .filter { it.convertedBalanceVnd.amountInCents > 0L }
            .sumOf { it.convertedBalanceVnd.amountInCents }

        val assetItems = assetItemsRaw.map { item ->
            val pct = if (totalPositiveAssetVnd > 0L && item.convertedBalanceVnd.amountInCents > 0L) {
                item.convertedBalanceVnd.amountInCents.toFloat() / totalPositiveAssetVnd.toFloat()
            } else {
                0f
            }
            item.copy(groupPercentage = pct)
        }

        val totalLiabilityDebtVnd = liabilityItemsRaw.sumOf { item ->
            val rawDebt = if (item.convertedBalanceVnd.amountInCents < 0L) {
                -item.convertedBalanceVnd.amountInCents
            } else {
                item.convertedBalanceVnd.amountInCents
            }
            rawDebt
        }

        val liabilityItems = liabilityItemsRaw.map { item ->
            val debt = if (item.convertedBalanceVnd.amountInCents < 0L) {
                -item.convertedBalanceVnd.amountInCents
            } else {
                item.convertedBalanceVnd.amountInCents
            }
            val pct = if (totalLiabilityDebtVnd > 0L && debt > 0L) {
                debt.toFloat() / totalLiabilityDebtVnd.toFloat()
            } else {
                0f
            }
            item.copy(groupPercentage = pct)
        }

        val totalAssetsCents = totalPositiveAssetVnd
        val totalLiabilitiesCents = totalLiabilityDebtVnd + debtSummary.totalToPay.amountInCents
        val netWorthCents = totalAssetsCents - totalLiabilitiesCents
        val totalNetWorth = Money(netWorthCents)

        val netFlow = nonTransferTxs.fold(Money.ZERO) { acc, tx ->
            when (tx.type) {
                TransactionType.INCOME -> acc + tx.amount
                TransactionType.EXPENSE -> acc - tx.amount
            }
        }
        val startBalance = totalNetWorth - netFlow
        val netWorthChangePercentage = if (startBalance.amountInCents != 0L) {
            (netFlow.amountInCents.toDouble() / abs(startBalance.amountInCents.toDouble()) * 100.0).toFloat()
        } else {
            null
        }

        val hasForeign = wallets.any { !it.currencyCode.equals("VND", ignoreCase = true) }
        val ratesUpdatedString = ratesSnapshot?.lastUpdatedUtc?.takeIf { it.isNotBlank() }
        val allocationItems = buildAllocation(activeItems, allocationMode, rates)

        return AssetsUiState(
            totalNetWorth = totalNetWorth,
            totalAssetsVnd = Money(totalAssetsCents),
            totalLiabilitiesVnd = Money(totalLiabilitiesCents),
            assetCount = assetItems.size,
            liabilityCount = liabilityItems.size,
            wallets = activeItems,
            assetWallets = assetItems,
            liabilityWallets = liabilityItems,
            archivedWallets = archivedItems,
            isLoading = false,
            allocationMode = allocationMode,
            allocationItems = allocationItems,
            netWorthChangePercentage = netWorthChangePercentage,
            debtSummary = debtSummary,
            hasForeignCurrency = hasForeign,
            ratesLastUpdated = ratesUpdatedString,
            adjustBalanceWallet = adjustWallet,
        )
    }

    fun onAllocationModeChanged(mode: AllocationMode) {
        _allocationMode.value = mode
    }

    fun openAdjustBalance(item: WalletAssetItem) {
        _adjustBalanceWallet.value = item
    }

    fun closeAdjustBalance() {
        _adjustBalanceWallet.value = null
    }

    fun submitAdjustBalance(walletId: Long, newBalanceCents: Long) {
        viewModelScope.launch {
            try {
                val targetWallet = walletRepository.getById(walletId) ?: return@launch
                val currentItem = state.value.wallets.firstOrNull { it.wallet.id == walletId }
                    ?: state.value.archivedWallets.firstOrNull { it.wallet.id == walletId }
                val currentBalanceCents = currentItem?.balance?.amountInCents ?: targetWallet.initialBalance.amountInCents
                val diff = newBalanceCents - currentBalanceCents
                val newInitial = targetWallet.initialBalance.amountInCents + diff
                val updatedWallet = targetWallet.copy(initialBalance = Money(newInitial))
                walletRepository.upsert(updatedWallet)
                _adjustBalanceWallet.value = null
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(R.string.wallet_adjust_success),
                        type = FeedbackType.Success,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(R.string.wallet_update_failed),
                        type = FeedbackType.Error,
                    ),
                )
            }
        }
    }

    fun openPayCreditCard(walletId: Long) {
        if (_transferForm.value.isSubmitting) return
        val currentWallets = state.value.wallets
        val target = currentWallets.firstOrNull { it.wallet.id == walletId } ?: return
        val fromId = currentWallets.firstOrNull { it.wallet.isAsset && it.wallet.id != walletId }?.wallet?.id
        val debtCents = if (target.balance.amountInCents < 0L) -target.balance.amountInCents else 0L
        val defaultAmount = if (debtCents > 0L) (debtCents / 100L).toString() else ""
        _transferForm.value = TransferFormState(
            isVisible = true,
            fromWalletId = fromId,
            toWalletId = walletId,
            amountInput = defaultAmount,
            note = "${context.getString(R.string.assets_card_action_pay_card)}: ${target.wallet.name}",
        )
    }

    fun setActiveWallet(walletId: Long) {
        viewModelScope.launch {
            try {
                walletRepository.setActive(walletId)
                _feedback.tryEmit(
                    UiFeedback(
                        message = context.getString(R.string.feedback_wallet_set_active),
                        type = FeedbackType.Success,
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _feedback.tryEmit(
                    UiFeedback(
                        message = context.getString(R.string.feedback_wallet_switch_failed),
                        type = FeedbackType.Error,
                    ),
                )
            }
        }
    }

    fun openTransferSheet(fromWalletId: Long? = null) {
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
        if (form.isSubmitting) return

        when (val validation = validateTransfer(
            fromWalletId = form.fromWalletId,
            toWalletId = form.toWalletId,
            amountInput = form.amountInput,
            wallets = state.value.wallets,
        )) {
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
            } catch (_: Exception) {
                _transferForm.update { it.copy(submitError = TransferError.Failed) }
            } finally {
                _transferForm.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
