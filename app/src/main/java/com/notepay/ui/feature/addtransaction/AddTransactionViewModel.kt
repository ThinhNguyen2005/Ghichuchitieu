package com.notepay.ui.feature.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.Instant

import com.notepay.domain.repository.CategoryRepository

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(AddTransactionUiState())
    val state = _state.asStateFlow()

    init {
        loadWallets()
        observeCategories()
    }

    fun onEvent(event: AddTransactionEvent) {
        when (event) {
            is AddTransactionEvent.AmountChanged -> updateAmount(event.text)
            is AddTransactionEvent.TypeChanged -> updateType(event.type)
            is AddTransactionEvent.CategoryChanged -> updateCategory(event.category)
            is AddTransactionEvent.NoteChanged -> updateNote(event.note)
            is AddTransactionEvent.DateChanged -> updateDate(event.instant)
            is AddTransactionEvent.WalletChanged -> updateWallet(event.walletId)
            is AddTransactionEvent.CreateCategory -> createCategory(event.displayName, event.colorArgb, event.isIncome)
            AddTransactionEvent.Save -> save()
            AddTransactionEvent.Cancel -> Unit
            AddTransactionEvent.Reset -> resetSavedFlag()
        }
    }

    private fun observeCategories() {
        viewModelScope.launch(ioDispatcher) {
            categoryRepository.observeCategories().collect { list ->
                _state.update { it.copy(availableCategories = list) }
            }
        }
    }

    private fun createCategory(displayName: String, colorArgb: Long, isIncome: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            val id = "CUSTOM_${System.currentTimeMillis()}"
            val newCategory = Category(
                id = id,
                displayName = displayName.trim(),
                colorArgb = colorArgb,
                isIncome = isIncome,
                isCustom = true
            )
            categoryRepository.addCustomCategory(newCategory)
            _state.update { it.copy(category = newCategory) }
        }
    }

    private fun loadWallets() {
        viewModelScope.launch(ioDispatcher) {
            val wallets = walletRepository.observeAll().first()
            val activeWallet = walletRepository.observeActive().first()
            _state.update {
                it.copy(
                    availableWallets = wallets,
                    walletId = activeWallet?.id ?: wallets.firstOrNull()?.id,
                )
            }
        }
    }

    private fun updateAmount(text: String) {
        val result = AmountParser.parse(text)
        val errors = _state.value.errors
            .minus(FieldError.AMOUNT_EMPTY)
            .minus(FieldError.AMOUNT_INVALID)
            .let { current -> result.error?.let(current::plus) ?: current }

        _state.update {
            it.copy(
                amountInput = result.input,
                amount = result.amount,
                errors = errors,
                saveErrorMessage = null,
            )
        }
    }

    private fun updateType(type: TransactionType) {
        val category = when (type) {
            TransactionType.EXPENSE -> Category.DEFAULT_EXPENSE
            TransactionType.INCOME -> Category.DEFAULT_INCOME
        }
        _state.update { it.copy(type = type, category = category) }
    }

    private fun updateCategory(category: Category) {
        _state.update { it.copy(category = category) }
    }

    private fun updateNote(note: String) {
        val errors = _state.value.errors
            .minus(FieldError.NOTE_TOO_LONG)
            .let { if (note.length > Transaction.MAX_NOTE_LENGTH) it + FieldError.NOTE_TOO_LONG else it }
        _state.update { it.copy(note = note, errors = errors, saveErrorMessage = null) }
    }

    private fun updateDate(instant: Instant) {
        _state.update { it.copy(occurredAt = instant) }
    }

    private fun updateWallet(walletId: Long) {
        _state.update {
            it.copy(
                walletId = walletId,
                errors = it.errors - FieldError.WALLET_MISSING,
            )
        }
    }

    private fun save() {
        val current = _state.value
        val errors = validate(current)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }

        _state.update { it.copy(isSaving = true, errors = emptySet(), saveErrorMessage = null) }
        viewModelScope.launch(ioDispatcher) {
            val transaction = Transaction(
                amount = current.amount!!,
                type = current.type,
                category = current.category,
                note = current.note.trim(),
                occurredAt = current.occurredAt,
                walletId = current.walletId!!,
            )
            val result = addTransactionUseCase(transaction)
            _state.update {
                if (result.isSuccess) {
                    it.copy(isSaving = false, savedSuccessfully = true, saveErrorMessage = null)
                } else {
                    it.copy(
                        isSaving = false,
                        saveErrorMessage = result.exceptionOrNull()?.message ?: "Không thể lưu giao dịch",
                    )
                }
            }
        }
    }

    private fun validate(state: AddTransactionUiState): Set<FieldError> = buildSet {
        if (state.amount == null || state.amount.amountInCents <= 0) add(FieldError.AMOUNT_EMPTY)
        if (state.walletId == null) add(FieldError.WALLET_MISSING)
        if (state.note.length > Transaction.MAX_NOTE_LENGTH) add(FieldError.NOTE_TOO_LONG)
    }

    private fun resetSavedFlag() {
        _state.update {
            it.copy(
                amountInput = "",
                amount = null,
                type = TransactionType.EXPENSE,
                category = Category.DEFAULT_EXPENSE,
                note = "",
                occurredAt = Clock.System.now(),
                isSaving = false,
                errors = emptySet(),
                saveErrorMessage = null,
                savedSuccessfully = false,
            )
        }
    }
}

