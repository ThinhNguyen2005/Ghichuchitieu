package com.notepay.ui.feature.transaction.add

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.di.IoDispatcher
import com.notepay.R
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.SuggestCategoryUseCase
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.ai.LocalTransactionImageScanner
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.feedback.FeedbackType
import com.notepay.ui.feature.transaction.CategorySuggestionUiMapper
import com.notepay.ui.feature.transaction.AmountParseError
import com.notepay.ui.feature.transaction.AmountParser
import com.notepay.ui.feature.transaction.calculator.CalculatorEngine
import com.notepay.ui.feature.transaction.calculator.CalculatorState
import com.notepay.ui.feature.transaction.components.CalcKey
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant
import java.util.UUID

import com.notepay.domain.repository.CategoryRepository

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val suggestCategoryUseCase: SuggestCategoryUseCase,
    private val imageScanner: LocalTransactionImageScanner,
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(AddTransactionUiState())
    val state = _state.asStateFlow()

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    init {
        loadWallets()
        observeCategories()
    }

    fun onEvent(event: AddTransactionEvent) {
        when (event) {
            is AddTransactionEvent.AmountChanged   -> updateAmount(event.text)
            is AddTransactionEvent.TypeChanged     -> updateType(event.type)
            is AddTransactionEvent.CategoryChanged -> updateCategory(event.category)
            is AddTransactionEvent.NoteChanged     -> updateNote(event.note)
            is AddTransactionEvent.DateChanged     -> updateDate(event.instant)
            is AddTransactionEvent.WalletChanged   -> updateWallet(event.walletId)
            is AddTransactionEvent.ImageSelected   -> scanImage(event.uri)
            is AddTransactionEvent.CalcKeyPressed  -> handleCalcKey(event.key)
            is AddTransactionEvent.CreateCategory  -> createCategory(
                displayName = event.displayName,
                colorArgb = event.colorArgb,
                iconId = event.iconId,
                isIncome = event.isIncome,
            )
            AddTransactionEvent.BackspaceLong -> clearCalc()
            AddTransactionEvent.Save          -> save()
            AddTransactionEvent.Cancel        -> Unit
        }
    }

    // ── Calculator keypad ──────────────────────────────────────────────────────

    private fun handleCalcKey(key: CalcKey) {
        val current = _state.value.calcState
        val next = when (key) {
            is CalcKey.Digit    -> CalculatorEngine.appendDigit(current, key.value)
            is CalcKey.Operator -> CalculatorEngine.applyOperator(current, key.symbol)
            CalcKey.Equals      -> CalculatorEngine.equals(current)
            CalcKey.ThreeZeros  -> CalculatorEngine.appendThreeZeros(current)
            CalcKey.Backspace   -> CalculatorEngine.backspace(current)
            // Date / Note / Save keys are handled in the Screen layer
            CalcKey.Date, CalcKey.Note, CalcKey.Save -> current
        }
        commitCalcState(next)

        // Date/Note/Save: bubble up via separate events so Screen can react
        when (key) {
            CalcKey.Save -> save()
            else -> Unit
        }
    }

    private fun clearCalc() {
        commitCalcState(CalculatorEngine.clear())
    }

    /**
     * After every keypad press, compute the resolved amount in cents
     * and push it into the main state.
     */
    private fun commitCalcState(calc: CalculatorState) {
        val majorUnits = CalculatorEngine.currentValue(calc) ?: 0L
        val amountInCents = majorUnits * 100L
        val money = if (amountInCents > 0) Money(amountInCents) else null
        val errors = _state.value.errors
            .minus(FieldError.AMOUNT_EMPTY)
            .minus(FieldError.AMOUNT_INVALID)
            .let { if (amountInCents <= 0 && _state.value.errors.contains(FieldError.AMOUNT_EMPTY)) it + FieldError.AMOUNT_EMPTY else it }

        _state.update {
            it.copy(
                calcState = calc,
                amountInput = calc.currentOperand,
                amount = money,
                errors = errors,
            )
        }
    }

    // ── Other handlers (unchanged) ─────────────────────────────────────────────

    private fun observeCategories() {
        viewModelScope.launch(ioDispatcher) {
            categoryRepository.observeCategories().collect { list ->
                _state.update { it.copy(availableCategories = list) }
            }
        }
    }

    private fun createCategory(displayName: String, colorArgb: Long, iconId: String, isIncome: Boolean) {
        val cleanName = displayName.trim().replace(Regex("\\s+"), " ").take(40)
        if (cleanName.isBlank()) return
        if (_state.value.availableCategories.any {
                it.isIncome == isIncome && it.displayName.equals(cleanName, ignoreCase = true)
            }
        ) {
            _feedback.tryEmit(UiFeedback(context.getString(R.string.feedback_category_exists), type = FeedbackType.Error))
            return
        }
        viewModelScope.launch(ioDispatcher) {
            val id = "CUSTOM_${UUID.randomUUID()}"
            val newCategory = Category(
                id = id,
                displayName = cleanName,
                colorArgb = colorArgb,
                isIncome = isIncome,
                isCustom = true,
                iconId = iconId,
            )
            categoryRepository.addCustomCategory(newCategory)
            _state.update {
                it.copy(
                    category = newCategory,
                    isCategoryExplicitlySelected = true,
                )
            }
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
            .let { current -> result.error?.asFieldError()?.let(current::plus) ?: current }

        _state.update {
            it.copy(
                amountInput = result.input,
                amount = result.amount,
                errors = errors,
            )
        }
    }

    private fun scanImage(uri: android.net.Uri) {
        if (_state.value.isImageScanning) return
        _state.update {
            it.copy(
                isImageScanning = true,
                imageScanMessage = context.getString(R.string.image_scan_reading_device),
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val result = imageScanner.scan(uri)
            val parsed = result.amountInput?.let(AmountParser::parse)
            _state.update { current ->
                val updatedErrors = parsed?.let { parseResult ->
                    current.errors
                        .minus(FieldError.AMOUNT_EMPTY)
                        .minus(FieldError.AMOUNT_INVALID)
                        .let { errors -> parseResult.error?.asFieldError()?.let(errors::plus) ?: errors }
                } ?: current.errors
                // Also update calcState when OCR fills in an amount
                val scannedLong = parsed?.amount?.amountInCents?.div(100L) ?: 0L
                val newCalcState = if (scannedLong > 0)
                    CalculatorState(currentOperand = scannedLong.toString())
                else current.calcState
                current.copy(
                    amountInput = parsed?.input ?: current.amountInput,
                    amount = parsed?.amount ?: current.amount,
                    errors = updatedErrors,
                    isImageScanning = false,
                    imageScanMessage = result.message,
                    calcState = newCalcState,
                )
            }
        }
    }

    private fun updateType(type: TransactionType) {
        val suggestion = suggestCategoryUseCase.suggestDetailed(_state.value.note, type == TransactionType.INCOME)
        _state.update {
            it.copy(
                type = type,
                category = suggestion?.category ?: if (type == TransactionType.INCOME) Category.DEFAULT_INCOME else Category.DEFAULT_EXPENSE,
                isCategoryExplicitlySelected = false,
                suggestedCategory = suggestion?.category,
                suggestionReason = suggestion?.reason?.let { CategorySuggestionUiMapper.toText(context, it) },
            )
        }
    }

    private fun updateCategory(category: Category) {
        _state.update {
            it.copy(
                category = category,
                isCategoryExplicitlySelected = true,
            )
        }
    }

    private fun updateNote(note: String) {
        val cleanNote = note.take(Transaction.MAX_NOTE_LENGTH)
        val errors = _state.value.errors
            .minus(FieldError.NOTE_TOO_LONG)
            .let { if (note.length > Transaction.MAX_NOTE_LENGTH) it + FieldError.NOTE_TOO_LONG else it }

        val isIncome = _state.value.type == TransactionType.INCOME
        val suggestion = suggestCategoryUseCase.suggestDetailed(cleanNote, isIncome)
        val finalCategory = if (!_state.value.isCategoryExplicitlySelected) {
            suggestion?.category ?: if (isIncome) Category.DEFAULT_INCOME else Category.DEFAULT_EXPENSE
        } else {
            _state.value.category
        }

        _state.update {
            it.copy(
                note = cleanNote,
                category = finalCategory,
                suggestedCategory = suggestion?.category,
                suggestionReason = suggestion?.reason?.let { CategorySuggestionUiMapper.toText(context, it) },
                errors = errors,
            )
        }
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

        _state.update { it.copy(isSaving = true, errors = emptySet()) }
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
                if (result.isSuccess) it.copy(isSaving = false)
                else it.copy(isSaving = false)
            }
            if (result.isSuccess) {
                suggestCategoryUseCase.learn(
                    note = current.note.trim(),
                    categoryId = current.category.id,
                    isIncome = current.type == TransactionType.INCOME,
                )
                _feedback.emit(UiFeedback(context.getString(R.string.feedback_transaction_saved), type = FeedbackType.Success))
            } else {
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(R.string.feedback_transaction_save_failed),
                        type = FeedbackType.Error,
                    ),
                )
            }
        }
    }

    private fun validate(state: AddTransactionUiState): Set<FieldError> = buildSet {
        if (state.amount == null || state.amount.amountInCents <= 0) add(FieldError.AMOUNT_EMPTY)
        if (state.walletId == null) add(FieldError.WALLET_MISSING)
        if (state.note.length > Transaction.MAX_NOTE_LENGTH) add(FieldError.NOTE_TOO_LONG)
    }
}

private fun AmountParseError.asFieldError(): FieldError = when (this) {
    AmountParseError.EMPTY   -> FieldError.AMOUNT_EMPTY
    AmountParseError.INVALID -> FieldError.AMOUNT_INVALID
}
