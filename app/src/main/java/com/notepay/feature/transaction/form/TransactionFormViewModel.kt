package com.notepay.feature.transaction.form

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.ai.HybridNaturalTransactionParser
import com.notepay.ai.LocalTransactionImageScanner
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.navigation.Route
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import com.notepay.domain.natural.NaturalFallbackNotice
import com.notepay.domain.natural.NaturalParseResult
import com.notepay.domain.natural.NaturalParseSource
import com.notepay.domain.natural.NaturalTransactionDraft
import com.notepay.domain.repository.CategoryRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.SuggestCategoryUseCase
import com.notepay.feature.transaction.component.CalcKey
import com.notepay.feature.transaction.form.calculator.AmountParser
import com.notepay.feature.transaction.form.calculator.CalculatorEngine
import com.notepay.feature.transaction.form.calculator.CalculatorError
import com.notepay.feature.transaction.form.calculator.CalculatorState
import com.notepay.feature.transaction.form.mapper.CategorySuggestionUiMapper
import com.notepay.feature.transaction.form.validation.FieldError
import com.notepay.feature.transaction.form.validation.TransactionFormValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

@HiltViewModel
@Suppress("LongParameterList")
class TransactionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val suggestCategoryUseCase: SuggestCategoryUseCase,
    private val imageScanner: LocalTransactionImageScanner,
    private val hybridNaturalParser: HybridNaturalTransactionParser,
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    // ID từ SavedStateHandle: null → Add mode, non-null → Edit mode
    private val transactionId: Long? = savedStateHandle.get<Long>(Route.EditTransaction.ARG_ID)?.takeIf { it > 0 }
    val isEditMode: Boolean = transactionId != null

    // Lưu transaction gốc khi edit để tránh mất các field không hiển thị trên form
    private var originalTransaction: Transaction? = null

    private val _state = MutableStateFlow(TransactionFormUiState(isEditMode = isEditMode, isLoading = isEditMode))
    val state = _state.asStateFlow()

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    init {
        loadWallets()
        observeCategories()
        observeNaturalAiAvailability()
        if (isEditMode) {
            loadExistingTransaction(checkNotNull(transactionId))
        }
    }

    fun onEvent(event: TransactionFormUiEvent) {
        when (event) {
            is TransactionFormUiEvent.CalcKeyPressed       -> handleCalcKey(event.key)
            TransactionFormUiEvent.BackspaceLong           -> clearCalc()
            is TransactionFormUiEvent.TypeChanged          -> updateType(event.type)
            is TransactionFormUiEvent.CategorySelected     -> updateCategory(event.category)
            is TransactionFormUiEvent.NoteChanged          -> updateNote(event.note)
            is TransactionFormUiEvent.DateChanged          -> updateDate(event.instant)
            is TransactionFormUiEvent.WalletChanged        -> updateWallet(event.walletId)
            is TransactionFormUiEvent.ImagePicked          -> scanImage(event.uri)
            is TransactionFormUiEvent.CreateCategory       -> createCategory(
                displayName = event.displayName,
                colorArgb = event.colorArgb,
                iconId = event.iconId,
                isIncome = event.isIncome,
            )
            is TransactionFormUiEvent.SetDatePickerVisible     -> _state.update { it.copy(isDatePickerOpen = event.visible) }
            is TransactionFormUiEvent.SetWalletPickerVisible   -> _state.update { it.copy(isWalletPickerOpen = event.visible) }
            is TransactionFormUiEvent.SetCategoryPickerVisible -> _state.update { it.copy(isCategoryPickerOpen = event.visible) }
            is TransactionFormUiEvent.SetNoteSheetVisible      -> setNoteSheetVisible(event.visible)
            is TransactionFormUiEvent.NaturalInputChanged      -> updateNaturalInput(event.input)
            TransactionFormUiEvent.AnalyzeNaturalInputWithAi   -> analyzeNaturalInputWithAi()
            is TransactionFormUiEvent.UpdateNaturalDraft       -> updateNaturalDraft(event.draft)
            is TransactionFormUiEvent.RemoveNaturalDraft       -> removeNaturalDraft(event.draftId)
            TransactionFormUiEvent.SaveNaturalDrafts           -> saveNaturalDrafts()
            TransactionFormUiEvent.Save                        -> save()
        }
    }

    // ── Khởi tạo dữ liệu ─────────────────────────────────────────────────────

    private fun loadWallets() {
        viewModelScope.launch(ioDispatcher) {
            val wallets = walletRepository.observeAll().first()
            val activeWallet = walletRepository.observeActive().first()
            _state.update {
                it.copy(
                    availableWallets = wallets,
                    walletId = it.walletId ?: activeWallet?.id ?: wallets.firstOrNull()?.id,
                )
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch(ioDispatcher) {
            suggestCategoryUseCase.warmUp()
            categoryRepository.observeCategories().collect { list ->
                _state.update { it.copy(availableCategories = list) }
            }
        }
    }

    private fun observeNaturalAiAvailability() {
        viewModelScope.launch(ioDispatcher) {
            hybridNaturalParser.isAiConfiguredFlow.collect { available ->
                _state.update { it.copy(naturalEntry = it.naturalEntry.copy(isAiAvailable = available)) }
            }
        }
    }

    // ── Natural language quick entry ──────────────────────────────────────────

    private fun setNoteSheetVisible(visible: Boolean) {
        if (!visible || isEditMode) {
            _state.update { it.copy(isNoteSheetOpen = visible) }
            return
        }
        val seedInput = _state.value.naturalEntry.rawInput.ifBlank { _state.value.note }
        val parsed = hybridNaturalParser.parseOnDevice(seedInput)
        _state.update {
            it.copy(
                isNoteSheetOpen = true,
                naturalEntry = it.naturalEntry.copy(
                    rawInput = seedInput,
                    drafts = parsed.drafts,
                    needsAiFallback = parsed.needsAiFallback,
                    source = parsed.source,
                    showAiFallbackNotice = false,
                ),
            )
        }
    }

    private fun updateNaturalInput(input: String) {
        val capped = input.take(MAX_NATURAL_INPUT_LENGTH)
        val parsed = hybridNaturalParser.parseOnDevice(capped)
        _state.update {
            it.copy(
                naturalEntry = it.naturalEntry.copy(
                    rawInput = capped,
                    drafts = parsed.drafts,
                    needsAiFallback = parsed.needsAiFallback,
                    source = NaturalParseSource.ON_DEVICE,
                    showAiFallbackNotice = false,
                ),
            )
        }
    }

    private fun analyzeNaturalInputWithAi() {
        val currentEntry = _state.value.naturalEntry
        val inputSnapshot = currentEntry.rawInput.trim()
        if (inputSnapshot.isBlank() || currentEntry.isAiLoading || !currentEntry.isAiAvailable) return

        val tier1Snapshot = NaturalParseResult(
            drafts = currentEntry.drafts,
            confidence = 0.4f,
            needsAiFallback = currentEntry.needsAiFallback,
            source = NaturalParseSource.ON_DEVICE,
        )
        _state.update {
            it.copy(naturalEntry = it.naturalEntry.copy(isAiLoading = true, showAiFallbackNotice = false))
        }
        viewModelScope.launch(ioDispatcher) {
            try {
                val aiResult = hybridNaturalParser.parseWithAi(inputSnapshot, tier1Snapshot, forceAi = true)
                _state.update { state ->
                    if (state.naturalEntry.rawInput.trim() != inputSnapshot) {
                        state.copy(naturalEntry = state.naturalEntry.copy(isAiLoading = false))
                    } else {
                        state.copy(
                            naturalEntry = state.naturalEntry.copy(
                                drafts = aiResult.drafts,
                                needsAiFallback = aiResult.needsAiFallback,
                                source = aiResult.source,
                                isAiLoading = false,
                                showAiFallbackNotice = aiResult.fallbackNotice == NaturalFallbackNotice.AI_UNAVAILABLE,
                            ),
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                _state.update { it.copy(naturalEntry = it.naturalEntry.copy(isAiLoading = false)) }
                throw cancelled
            }
        }
    }

    private fun updateNaturalDraft(updated: NaturalTransactionDraft) {
        _state.update { state ->
            state.copy(
                naturalEntry = state.naturalEntry.copy(
                    drafts = state.naturalEntry.drafts.map { if (it.id == updated.id) updated else it },
                ),
            )
        }
    }

    private fun removeNaturalDraft(draftId: String) {
        _state.update { state ->
            state.copy(
                naturalEntry = state.naturalEntry.copy(
                    drafts = state.naturalEntry.drafts.filterNot { it.id == draftId },
                ),
            )
        }
    }

    private fun saveNaturalDrafts() {
        val current = _state.value
        if (current.naturalEntry.isSavingBatch) return
        val drafts = current.naturalEntry.drafts.filter { it.amountVnd in 1..Money.MAX_MAJOR_UNITS }
        val walletId = current.walletId ?: current.availableWallets.firstOrNull()?.id
        if (walletId == null) {
            _feedback.tryEmit(UiFeedback(context.getString(R.string.transaction_validation_wallet), type = FeedbackType.Error))
            return
        }
        if (drafts.isEmpty()) return

        _state.update { it.copy(naturalEntry = it.naturalEntry.copy(isSavingBatch = true)) }
        viewModelScope.launch(ioDispatcher) {
            val zone = TimeZone.currentSystemDefault()
            val nowTime = Clock.System.now().toLocalDateTime(zone).time
            var savedCount = 0
            var failedCount = 0
            for (draft in drafts) {
                val occurredAt = LocalDateTime(
                    date = draft.date,
                    time = LocalTime(nowTime.hour, nowTime.minute, nowTime.second, nowTime.nanosecond),
                ).toInstant(zone)
                val tx = Transaction(
                    amount = Money.fromMajorUnit(draft.amountVnd),
                    type = draft.type,
                    category = draft.category,
                    note = draft.note.trim().take(Transaction.MAX_NOTE_LENGTH),
                    occurredAt = occurredAt,
                    walletId = walletId,
                )
                val res = addTransactionUseCase(tx)
                if (res.isSuccess) {
                    savedCount++
                    suggestCategoryUseCase.learn(
                        note = tx.note,
                        categoryId = tx.category.id,
                        isIncome = tx.type == TransactionType.INCOME,
                    )
                } else {
                    failedCount++
                }
            }

            if (failedCount == 0 && savedCount > 0) {
                _state.update {
                    it.copy(
                        isNoteSheetOpen = false,
                        note = "",
                        naturalEntry = NaturalEntryUiState(isAiAvailable = it.naturalEntry.isAiAvailable),
                    )
                }
                _feedback.emit(
                    UiFeedback(context.getString(R.string.natural_entry_saved_batch, savedCount), type = FeedbackType.Success),
                )
            } else {
                _state.update { it.copy(naturalEntry = it.naturalEntry.copy(isSavingBatch = false)) }
                _feedback.emit(
                    UiFeedback(context.getString(R.string.natural_entry_partial_error), type = FeedbackType.Error),
                )
            }
        }
    }

    private companion object {
        const val MAX_NATURAL_INPUT_LENGTH = 500
    }

    private fun loadExistingTransaction(id: Long) {
        viewModelScope.launch(ioDispatcher) {
            val tx = transactionRepository.getById(id)
            val initialCategories = categoryRepository.observeCategories().firstOrNull() ?: emptyList()

            if (tx == null) {
                _state.update { it.copy(isLoading = false) }
                _feedback.tryEmit(
                    UiFeedback(context.getString(R.string.error_transaction_not_found), type = FeedbackType.Error),
                )
                return@launch
            }

            originalTransaction = tx
            val isIncome = tx.type == TransactionType.INCOME
            val initialSuggestion = suggestCategoryUseCase.suggestDetailed(tx.note, isIncome)
            val amountLong = tx.amount.toMajorUnitLong()
            val calcState = CalculatorState(currentOperand = amountLong.toString())

            _state.update {
                it.copy(
                    isLoading = false,
                    isAutoCapture = tx.isAutoCapture,
                    amountInput = amountLong.toString(),
                    amount = tx.amount,
                    calcState = calcState,
                    type = tx.type,
                    category = tx.category,
                    note = tx.note,
                    occurredAt = tx.occurredAt,
                    walletId = tx.walletId,
                    availableCategories = initialCategories,
                    suggestedCategory = initialSuggestion?.category,
                    suggestionReason = initialSuggestion?.reason?.let { r -> CategorySuggestionUiMapper.toText(context, r) },
                )
            }
        }
    }

    // ── Calculator keypad ─────────────────────────────────────────────────────

    private fun handleCalcKey(key: CalcKey) {
        val currentState = _state.value
        if (currentState.isEditMode && currentState.isAutoCapture && key != CalcKey.Save) return
        if (key == CalcKey.Save) {
            save()
            return
        }
        val current = currentState.calcState
        val next = when (key) {
            is CalcKey.Digit    -> CalculatorEngine.appendDigit(current, key.value)
            is CalcKey.Operator -> CalculatorEngine.applyOperator(current, key.symbol)
            CalcKey.Equals      -> CalculatorEngine.equals(current)
            CalcKey.ThreeZeros  -> CalculatorEngine.appendThreeZeros(current)
            CalcKey.Backspace   -> CalculatorEngine.backspace(current)
            // Date / Note / Save: xử lý ở Screen layer qua SetXxxVisible / Save events
            CalcKey.Date, CalcKey.Note, CalcKey.Save -> current
        }
        commitCalcState(next)
        if (next.error == CalculatorError.DIVISION_BY_ZERO) {
            _feedback.tryEmit(
                UiFeedback(context.getString(R.string.error_division_by_zero), type = FeedbackType.Error),
            )
        }
    }

    private fun clearCalc() {
        val currentState = _state.value
        if (currentState.isEditMode && currentState.isAutoCapture) return
        commitCalcState(CalculatorEngine.clear())
    }

    private fun commitCalcState(calc: CalculatorState) {
        val majorUnits = CalculatorEngine.previewMajorUnits(calc)
        val amountInCents = if (majorUnits != null && majorUnits > 0L) majorUnits * 100L else 0L
        val money = if (amountInCents > 0L) Money(amountInCents) else null

        _state.update {
            it.copy(
                calcState = calc,
                amountInput = calc.currentOperand,
                amount = money,
                errors = TransactionFormValidator.amountErrors(it.errors, amountInCents),
            )
        }
    }

    // ── Form field handlers ───────────────────────────────────────────────────

    private fun updateType(type: TransactionType) {
        val suggestion = suggestCategoryUseCase.suggestDetailed(_state.value.note, type == TransactionType.INCOME)
        _state.update {
            it.copy(
                type = type,
                category = suggestion?.category ?: if (type == TransactionType.INCOME) Category.DEFAULT_INCOME else Category.DEFAULT_EXPENSE,
                isCategoryExplicitlySelected = false,
                suggestedCategory = suggestion?.category,
                suggestionReason = suggestion?.reason?.let { r -> CategorySuggestionUiMapper.toText(context, r) },
            )
        }
    }

    private fun updateCategory(category: Category) {
        _state.update { it.copy(category = category, isCategoryExplicitlySelected = true) }
    }

    private fun updateNote(note: String) {
        val cleanNote = note.take(Transaction.MAX_NOTE_LENGTH)
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
                suggestionReason = suggestion?.reason?.let { r -> CategorySuggestionUiMapper.toText(context, r) },
                errors = TransactionFormValidator.noteErrors(it.errors, note.length),
            )
        }
    }

    private fun updateDate(instant: Instant) {
        _state.update { it.copy(occurredAt = instant) }
    }

    private fun updateWallet(walletId: Long) {
        _state.update { it.copy(walletId = walletId, errors = it.errors - FieldError.WALLET_MISSING) }
    }

    // ── OCR image scan ────────────────────────────────────────────────────────

    private fun scanImage(uri: Uri) {
        if (_state.value.isImageScanning) return
        _state.update {
            it.copy(isImageScanning = true, imageScanMessage = context.getString(R.string.image_scan_reading_device))
        }
        viewModelScope.launch(ioDispatcher) {
            try {
                val result = imageScanner.scan(uri)
                val parsed = result.amountInput?.let(AmountParser::parse)
                _state.update { current ->
                    val updatedErrors = parsed?.let { TransactionFormValidator.amountErrors(current.errors, it) } ?: current.errors
                    val scannedLong = parsed?.amount?.amountInCents?.div(100L) ?: 0L
                    val newCalcState = if (scannedLong > 0) CalculatorState(currentOperand = scannedLong.toString()) else current.calcState
                    val resolvedNote = if (current.note.isBlank() && !result.note.isNullOrBlank()) result.note else current.note
                    val resolvedSuggestion = if (!current.isCategoryExplicitlySelected && resolvedNote.isNotBlank()) {
                        suggestCategoryUseCase.suggestDetailed(resolvedNote, current.type == TransactionType.INCOME)
                    } else null
                    current.copy(
                        amountInput = parsed?.input ?: current.amountInput,
                        amount = parsed?.amount ?: current.amount,
                        note = resolvedNote,
                        category = resolvedSuggestion?.category ?: current.category,
                        suggestedCategory = resolvedSuggestion?.category ?: current.suggestedCategory,
                        errors = updatedErrors,
                        isImageScanning = false,
                        imageScanMessage = result.message,
                        calcState = newCalcState,
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update { it.copy(isImageScanning = false, imageScanMessage = context.getString(R.string.image_scan_read_error)) }
            }
        }
    }

    // ── Create custom category ────────────────────────────────────────────────

    private fun createCategory(displayName: String, colorArgb: Long, iconId: String, isIncome: Boolean) {
        val cleanName = displayName.trim().replace(Regex("\\s+"), " ").take(40)
        if (cleanName.isBlank()) return
        if (_state.value.availableCategories.any { it.isIncome == isIncome && it.displayName.equals(cleanName, ignoreCase = true) }) {
            _feedback.tryEmit(UiFeedback(context.getString(R.string.feedback_category_exists), type = FeedbackType.Error))
            return
        }
        viewModelScope.launch(ioDispatcher) {
            val newCategory = Category(
                id = "CUSTOM_${UUID.randomUUID()}",
                displayName = cleanName,
                colorArgb = colorArgb,
                isIncome = isIncome,
                isCustom = true,
                iconId = iconId,
            )
            categoryRepository.addCustomCategory(newCategory)
            _state.update { it.copy(category = newCategory, isCategoryExplicitlySelected = true) }
        }
    }

    // ── Save (Add or Edit) ────────────────────────────────────────────────────

    private fun save() {
        val current = _state.value
        if (current.isSaving) return
        if (!(current.isEditMode && current.isAutoCapture)) {
            val evaluated = CalculatorEngine.equals(current.calcState)
            commitCalcState(evaluated)
            if (evaluated.error == CalculatorError.DIVISION_BY_ZERO) {
                _feedback.tryEmit(
                    UiFeedback(context.getString(R.string.error_division_by_zero), type = FeedbackType.Error),
                )
                return
            }
        }
        if (isEditMode) saveEdit() else saveNew()
    }

    private fun saveNew() {
        val current = _state.value
        if (current.isSaving) return
        val errors = TransactionFormValidator.validate(current.amount, current.walletId, current.note)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            val amountMissing = current.amount == null || current.amount.amountInCents <= 0L
            val messageRes = when {
                amountMissing && current.walletId == null -> R.string.transaction_validation_amount_wallet
                amountMissing -> R.string.transaction_validation_amount
                current.walletId == null -> R.string.transaction_validation_wallet
                else -> R.string.transaction_validation_form
            }
            _feedback.tryEmit(UiFeedback(context.getString(messageRes), type = FeedbackType.Error))
            return
        }
        val amount = current.amount ?: return
        val walletId = current.walletId ?: return

        _state.update { it.copy(isSaving = true, errors = emptySet()) }
        viewModelScope.launch(ioDispatcher) {
            val transaction = Transaction(
                amount = amount,
                type = current.type,
                category = current.category,
                note = current.note.trim(),
                occurredAt = current.occurredAt,
                walletId = walletId,
            )
            val result = addTransactionUseCase(transaction)
            _state.update { it.copy(isSaving = false) }
            if (result.isSuccess) {
                suggestCategoryUseCase.learn(
                    note = current.note.trim(),
                    categoryId = current.category.id,
                    isIncome = current.type == TransactionType.INCOME,
                )
                _feedback.emit(UiFeedback(context.getString(R.string.feedback_transaction_saved), type = FeedbackType.Success))
            } else {
                _feedback.emit(UiFeedback(context.getString(R.string.feedback_transaction_save_failed), type = FeedbackType.Error))
            }
        }
    }

    private fun saveEdit() {
        val current = _state.value
        if (current.isSaving) return
        val tx = originalTransaction ?: return
        if (tx.isInternalTransfer) {
            _feedback.tryEmit(UiFeedback(context.getString(R.string.error_transfer_read_only), type = FeedbackType.Error))
            return
        }

        val cents = if (current.isAutoCapture) {
            tx.amount.amountInCents
        } else {
            current.amount?.amountInCents ?: 0L
        }
        if (cents <= 0) {
            _feedback.tryEmit(UiFeedback(context.getString(R.string.error_amount_positive), type = FeedbackType.Error))
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch(ioDispatcher) {
            try {
                // Giữ nguyên giờ cũ, chỉ đổi ngày nếu user thay đổi
                val newOccurredAt: Instant = run {
                    val oldLocal = tx.occurredAt.toLocalDateTime(TimeZone.currentSystemDefault())
                    val newLocal = current.occurredAt.toLocalDateTime(TimeZone.currentSystemDefault())
                    LocalDateTime(
                        date = LocalDate(newLocal.year, newLocal.month.number, newLocal.day),
                        time = LocalTime(oldLocal.hour, oldLocal.minute, oldLocal.second, oldLocal.nanosecond),
                    ).toInstant(TimeZone.currentSystemDefault())
                }
                val updated = tx.copy(
                    amount = Money(cents),
                    category = current.category,
                    note = if (current.isAutoCapture) tx.note else current.note,
                    occurredAt = newOccurredAt,
                )
                transactionRepository.upsert(updated)
                suggestCategoryUseCase.learn(
                    note = updated.note.trim(),
                    categoryId = updated.category.id,
                    isIncome = updated.type == TransactionType.INCOME,
                )
                _state.update { it.copy(isSaving = false) }
                _feedback.emit(UiFeedback(context.getString(R.string.feedback_transaction_updated), type = FeedbackType.Success))
            } catch (e: CancellationException) {
                _state.update { it.copy(isSaving = false) }
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
                _feedback.emit(UiFeedback(context.getString(R.string.feedback_transaction_update_failed), type = FeedbackType.Error))
            }
        }
    }
}

