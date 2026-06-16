package com.notepay.ui.feature.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.domain.model.Money
import com.notepay.domain.model.Wallet
import com.notepay.domain.repository.WalletRepository
import com.notepay.ui.feedback.FeedbackType
import com.notepay.ui.feedback.UiFeedback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class AddWalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val walletId: Long? = savedStateHandle.get<Long>("id")
    private var loadedWallet: Wallet? = null

    private val _state = MutableStateFlow(AddWalletUiState())
    val state = _state.asStateFlow()

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    init {
        if (walletId != null && walletId > 0L) {
            viewModelScope.launch {
                val wallet = walletRepository.getById(walletId)
                if (wallet != null) {
                    loadedWallet = wallet
                    _state.update {
                        it.copy(
                            name = wallet.name,
                            initialBalanceInput = (wallet.initialBalance.amountInCents / 100).toString(),
                            hasBudgetLimit = wallet.budgetLimit != null && wallet.budgetLimit.amountInCents > 0L,
                            budgetLimitInput = if (wallet.budgetLimit != null) (wallet.budgetLimit.amountInCents / 100).toString() else "",
                            budgetPeriod = BudgetPeriod.MONTHLY,
                            iconKey = wallet.iconKey,
                            colorKey = wallet.colorKey,
                            linkedPackageName = wallet.linkedPackageName ?: "",
                            bankBin = wallet.bankBin,
                            accountNumber = wallet.accountNumber ?: "",
                            accountName = wallet.accountName ?: "",
                            isEditMode = true
                        )
                    }
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun onInitialBalanceChanged(input: String) {
        val clean = input.filter(Char::isDigit)
        _state.update { it.copy(initialBalanceInput = clean) }
    }

    fun onHasBudgetLimitChanged(hasLimit: Boolean) {
        _state.update { it.copy(hasBudgetLimit = hasLimit) }
    }

    fun onBudgetLimitChanged(input: String) {
        val clean = input.filter(Char::isDigit)
        _state.update { it.copy(budgetLimitInput = clean) }
    }

    fun onBudgetPeriodChanged(period: BudgetPeriod) {
        _state.update { it.copy(budgetPeriod = period) }
    }

    fun onIconChanged(iconKey: String) {
        _state.update { it.copy(iconKey = iconKey) }
    }

    fun onColorChanged(colorKey: String) {
        _state.update { it.copy(colorKey = colorKey) }
    }

    fun onLinkedBankChanged(packageName: String, bin: String?) {
        _state.update { it.copy(linkedPackageName = packageName, bankBin = bin) }
    }

    fun onAccountNumberChanged(accountNumber: String) {
        // Chỉ lưu chữ số hoặc chữ cái (bình thường là số)
        val clean = accountNumber.filter { it.isLetterOrDigit() }
        _state.update { it.copy(accountNumber = clean) }
    }

    fun onAccountNameChanged(accountName: String) {
        _state.update { it.copy(accountName = accountName) }
    }

    fun save() {
        val current = _state.value
        if (!current.canSave) return

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val initialBalanceCents = current.initialBalanceInput.toLongOrNull()?.let { it * 100 } ?: 0L
                val budgetLimit = if (current.hasBudgetLimit) {
                    val rawLimit = current.budgetLimitInput.toLongOrNull() ?: 0L
                    val monthlyLimit = when (current.budgetPeriod) {
                        BudgetPeriod.DAILY -> rawLimit * 30
                        BudgetPeriod.WEEKLY -> rawLimit * 4
                        BudgetPeriod.MONTHLY -> rawLimit
                    }
                    Money(monthlyLimit * 100)
                } else {
                    null
                }

                val wallet = Wallet(
                    id = walletId ?: 0L,
                    name = current.name,
                    initialBalance = Money(initialBalanceCents),
                    iconKey = current.iconKey,
                    colorKey = current.colorKey,
                    isActive = loadedWallet?.isActive ?: false,
                    budgetLimit = budgetLimit,
                    linkedPackageName = current.linkedPackageName.ifBlank { null },
                    bankBin = current.bankBin,
                    accountNumber = current.accountNumber.ifBlank { null },
                    accountName = current.accountName.ifBlank { null },
                    createdAt = loadedWallet?.createdAt ?: Clock.System.now()
                )

                walletRepository.upsert(wallet)
                _state.update { it.copy(isSaving = false, error = null) }
                _feedback.emit(UiFeedback(if (current.isEditMode) "Đã cập nhật ví" else "Đã tạo ví", type = FeedbackType.Success))
            } catch (e: Exception) {
                val message = if (current.isEditMode) "Không thể cập nhật ví" else "Không thể tạo ví"
                _state.update { it.copy(isSaving = false, error = message) }
                _feedback.emit(UiFeedback(message, type = FeedbackType.Error))
            }
        }
    }
}
