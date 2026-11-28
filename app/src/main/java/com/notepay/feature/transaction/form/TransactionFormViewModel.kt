package com.notepay.feature.transaction.form

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.ai.LocalTransactionImageScanner
import com.notepay.core.designsystem.feedback.FeedbackDuration
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.navigation.Route
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.domain.repository.CategoryRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.shared.SharedWalletPermissionDeniedException
import com.notepay.domain.shared.SharedWalletPermissionGuard
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.SuggestCategoryUseCase
import com.notepay.feature.assets.main.assetsPeriodAt
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
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    // ID từ SavedStateHandle: null → Add mode, non-null → Edit mode
    private val transactionId: Long? = savedStateHandle.get<Long>(Route.EditTransaction.ARG_ID)?.takeIf { it > 0 }
    val isEditMode: Boolean = transactionId != null

    // Lưu transaction gốc khi edit để tránh mất các field không hiển thị trên form
    private var originalTransaction: Transaction? = null
    private var selectionGeneration: Long = 0L
    private var pendingCreatedCategoryId: String? = null

    private val _state = MutableStateFlow(
        TransactionFormUiState(
            isEditMode = isEditMode,
            step = if (isEditMode) TransactionFormStep.DETAILS else TransactionFormStep.SELECT_CATEGORY,
            isLoading = isEditMode,
        ),
    )
    val state = _state.asStateFlow()

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()
    private val _savedTransaction = MutableStateFlow<TransactionFormSaveResult?>(null)
    val savedTransaction = _savedTransaction.asStateFlow()

    init {
        loadWallets()
        observeCategories()
        if (isEditMode) {
            loadExistingTransaction(checkNotNull(transactionId))
        }
    }

    fun onEvent(event: TransactionFormUiEvent) {
        when (event) {
            is TransactionFormUiEvent.CalcKeyPressed       -> handleCalcKey(event.key)
            is TransactionFormUiEvent.QuickAmountSelected  -> applyQuickAmount(event.amountVnd)
            TransactionFormUiEvent.BackspaceLong           -> clearCalc()
            is TransactionFormUiEvent.TypeChanged          -> updateType(event.type)
            is TransactionFormUiEvent.CategorySelected     -> updateCategory(event.category)
            TransactionFormUiEvent.OpenCategorySelection   -> openCategorySelection()
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
            is TransactionFormUiEvent.SetNoteSheetVisible      -> _state.update { it.copy(isNoteSheetOpen = event.visible) }
            TransactionFormUiEvent.Save                        -> save()
        }
    }

    // ── Khởi tạo dữ liệu ─────────────────────────────────────────────────────

    private fun loadWallets() {
        viewModelScope.launch(ioDispatcher) {
            val allWallets = walletRepository.observeAll().first()
            val writableWallets = if (isEditMode) {
                allWallets
            } else {
                allWallets.filter { SharedWalletPermissionGuard.canAddTransaction(it) }
            }
            val activeWallet = walletRepository.observeActive().first()
                ?.takeIf { isEditMode || SharedWalletPermissionGuard.canAddTransaction(it) }
            val hasReadOnlyOnly = !isEditMode &&
                writableWallets.isEmpty() &&
                allWallets.any { !SharedWalletPermissionGuard.canAddTransaction(it) }
            _state.update {
                it.copy(
                    availableWallets = writableWallets,
                    hasReadOnlyWalletsOnly = hasReadOnlyOnly,
                    walletId = it.walletId ?: activeWallet?.id ?: writableWallets.firstOrNull()?.id,
                )
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch(ioDispatcher) {
            suggestCategoryUseCase.warmUp()
            categoryRepository.observeCategories().collect { list ->
                if (pendingCreatedCategoryId != null && list.any { it.id == pendingCreatedCategoryId }) {
                    pendingCreatedCategoryId = null
                }
                _state.update { current ->
                    val confirmedRemoved = !isEditMode &&
                        current.isCategoryExplicitlySelected &&
                        current.category.id != pendingCreatedCategoryId &&
                        list.none {
                            it.id == current.category.id &&
                                it.isIncome == (current.type == TransactionType.INCOME)
                        }
                    if (confirmedRemoved) {
                        current.copy(
                            availableCategories = list,
                            hasLoadedCategories = true,
                            step = TransactionFormStep.SELECT_CATEGORY,
                            isCategoryExplicitlySelected = false,
                            category = defaultCategoryFor(current.type),
                        )
                    } else {
                        current.copy(
                            availableCategories = list,
                            hasLoadedCategories = true,
                        )
                    }
                }
            }
        }
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
                    step = TransactionFormStep.DETAILS,
                    isLoading = false,
                    hasLoadedCategories = initialCategories.isNotEmpty() || it.hasLoadedCategories,
                    isAutoCapture = tx.isAutoCapture,
                    amountInput = amountLong.toString(),
                    amount = tx.amount,
                    calcState = calcState,
                    type = tx.type,
                    category = tx.category,
                    isCategoryExplicitlySelected = true,
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

    private fun applyQuickAmount(amountVnd: Long) {
        if (amountVnd <= 0L) return
        val currentState = _state.value
        if (currentState.isEditMode && currentState.isAutoCapture) return
        commitCalcState(CalculatorState(currentOperand = amountVnd.toString()))
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
        val current = _state.value
        if (!isEditMode && current.type == type) return
        selectionGeneration++
        if (!isEditMode) {
            pendingCreatedCategoryId = null
        }
        val suggestion = suggestCategoryUseCase.suggestDetailed(current.note, type == TransactionType.INCOME)
        _state.update {
            it.copy(
                type = type,
                step = if (isEditMode) TransactionFormStep.DETAILS else TransactionFormStep.SELECT_CATEGORY,
                category = suggestion?.category ?: defaultCategoryFor(type),
                isCategoryExplicitlySelected = false,
                suggestedCategory = suggestion?.category,
                suggestionReason = suggestion?.reason?.let { r -> CategorySuggestionUiMapper.toText(context, r) },
            )
        }
    }

    private fun updateCategory(category: Category) {
        val current = _state.value
        if (!isEditMode) {
            if (current.step != TransactionFormStep.SELECT_CATEGORY) return
            if (!isCategoryValidForType(category, current.type, current.availableCategories, current.hasLoadedCategories)) {
                return
            }
            selectionGeneration++
            if (pendingCreatedCategoryId != category.id) {
                pendingCreatedCategoryId = null
            }
            _state.update {
                it.copy(
                    category = category,
                    isCategoryExplicitlySelected = true,
                    step = TransactionFormStep.DETAILS,
                )
            }
            return
        }
        _state.update { it.copy(category = category, isCategoryExplicitlySelected = true) }
    }

    private fun openCategorySelection() {
        if (isEditMode || _state.value.isSaving) return
        selectionGeneration++
        _state.update {
            it.copy(
                step = TransactionFormStep.SELECT_CATEGORY,
                isDatePickerOpen = false,
                isWalletPickerOpen = false,
                isNoteSheetOpen = false,
            )
        }
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
        val startedFromCategoryStep = !isEditMode && _state.value.step == TransactionFormStep.SELECT_CATEGORY
        _state.update {
            it.copy(
                isImageScanning = true,
                imageScanMessage = context.getString(R.string.image_scan_reading_device),
                step = if (startedFromCategoryStep) TransactionFormStep.DETAILS else it.step,
            )
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
                    val shouldAutoConfirmCategory = startedFromCategoryStep && !current.isCategoryExplicitlySelected
                    current.copy(
                        amountInput = parsed?.input ?: current.amountInput,
                        amount = parsed?.amount ?: current.amount,
                        note = resolvedNote,
                        category = resolvedSuggestion?.category ?: current.category,
                        isCategoryExplicitlySelected = current.isCategoryExplicitlySelected || shouldAutoConfirmCategory,
                        suggestedCategory = resolvedSuggestion?.category ?: current.suggestedCategory,
                        errors = updatedErrors,
                        isImageScanning = false,
                        imageScanMessage = result.message,
                        calcState = newCalcState,
                    )
                }
                val scanFeedback = when {
                    parsed?.amount != null -> UiFeedback(message = result.message, type = FeedbackType.Success)
                    result.isAiKeyOrQuotaError -> UiFeedback(
                        message = result.message,
                        type = FeedbackType.Error,
                        actionLabel = context.getString(R.string.action_open_ai_settings),
                        duration = FeedbackDuration.Long,
                    )
                    else -> UiFeedback(message = result.message, type = FeedbackType.Warning)
                }
                _feedback.emit(scanFeedback)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val readError = context.getString(R.string.image_scan_read_error)
                _state.update {
                    it.copy(
                        isImageScanning = false,
                        imageScanMessage = readError,
                    )
                }
                _feedback.emit(UiFeedback(message = readError, type = FeedbackType.Error))
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
        val requestGeneration = ++selectionGeneration
        viewModelScope.launch(ioDispatcher) {
            val newCategory = Category(
                id = "CUSTOM_${UUID.randomUUID()}",
                displayName = cleanName,
                colorArgb = colorArgb,
                isIncome = isIncome,
                isCustom = true,
                iconId = iconId,
            )
            pendingCreatedCategoryId = newCategory.id
            try {
                categoryRepository.addCustomCategory(newCategory)
                var applied = false
                _state.update { current ->
                    val canApply = isEditMode || (
                        selectionGeneration == requestGeneration &&
                            current.step == TransactionFormStep.SELECT_CATEGORY &&
                            (current.type == TransactionType.INCOME) == isIncome
                        )
                    if (canApply) {
                        applied = true
                        current.copy(
                            category = newCategory,
                            isCategoryExplicitlySelected = true,
                            step = TransactionFormStep.DETAILS,
                        )
                    } else {
                        current
                    }
                }
                if (!applied && pendingCreatedCategoryId == newCategory.id) {
                    pendingCreatedCategoryId = null
                }
            } catch (e: CancellationException) {
                if (pendingCreatedCategoryId == newCategory.id) pendingCreatedCategoryId = null
                throw e
            } catch (_: Exception) {
                if (pendingCreatedCategoryId == newCategory.id) pendingCreatedCategoryId = null
                _feedback.emit(
                    UiFeedback(context.getString(R.string.feedback_category_create_failed), type = FeedbackType.Error),
                )
            }
        }
    }

    // ── Save (Add or Edit) ────────────────────────────────────────────────────

    private fun save() {
        val current = _state.value
        if (current.isSaving || _savedTransaction.value != null) return
        if (!isEditMode && !hasValidCategoryForSave(current)) {
            _feedback.tryEmit(
                UiFeedback(context.getString(R.string.transaction_validation_category), type = FeedbackType.Error),
            )
            return
        }
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
        if (!hasValidCategoryForSave(current)) {
            _feedback.tryEmit(
                UiFeedback(context.getString(R.string.transaction_validation_category), type = FeedbackType.Error),
            )
            return
        }
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
            val savedId = result.getOrNull()
            val targetWallet = current.availableWallets.firstOrNull { it.id == walletId }
            if (savedId != null) {
                publishSaved(
                    savedId,
                    transaction,
                    targetWallet = targetWallet,
                    isShared = targetWallet?.isShared == true,
                    successMessage = R.string.feedback_transaction_created,
                )
            } else {
                _state.update { it.copy(isSaving = false) }
                val isPermissionError = result.exceptionOrNull() is SharedWalletPermissionDeniedException
                val errMsgRes = if (isPermissionError) {
                    R.string.shared_wallet_viewer_cannot_write
                } else {
                    R.string.feedback_transaction_save_failed
                }
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(errMsgRes),
                        actionLabel = if (!isPermissionError) context.getString(R.string.action_retry) else null,
                        type = FeedbackType.Error,
                        duration = if (!isPermissionError) FeedbackDuration.Long else FeedbackDuration.Short,
                        onAction = if (!isPermissionError) {
                            { onEvent(TransactionFormUiEvent.Save) }
                        } else {
                            null
                        },
                    ),
                )
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
                val targetWallet = current.availableWallets.firstOrNull { it.id == tx.walletId }
                val isShared = targetWallet?.isShared == true
                if (isShared) {
                    addTransactionUseCase(updated).getOrThrow()
                } else {
                    transactionRepository.upsert(updated)
                }
                publishSaved(
                    tx.id,
                    updated,
                    targetWallet = targetWallet,
                    isShared = isShared,
                    successMessage = R.string.feedback_transaction_updated,
                )
            } catch (e: CancellationException) {
                _state.update { it.copy(isSaving = false) }
                throw e
            } catch (e: SharedWalletPermissionDeniedException) {
                _state.update { it.copy(isSaving = false) }
                _feedback.emit(UiFeedback(context.getString(R.string.shared_wallet_viewer_cannot_write), type = FeedbackType.Error))
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(R.string.feedback_transaction_update_failed),
                        actionLabel = context.getString(R.string.action_retry),
                        type = FeedbackType.Error,
                        duration = FeedbackDuration.Long,
                        onAction = { onEvent(TransactionFormUiEvent.Save) },
                    ),
                )
            }
        }
    }

    private suspend fun publishSaved(
        transactionId: Long,
        transaction: Transaction,
        targetWallet: Wallet?,
        isShared: Boolean,
        successMessage: Int,
    ) {
        runCatching {
            suggestCategoryUseCase.learn(
                note = transaction.note,
                categoryId = transaction.category.id,
                isIncome = transaction.type == TransactionType.INCOME,
            )
        }
        val followUpWarning = computeBudgetFollowUpWarning(transaction, targetWallet)
        _savedTransaction.value = TransactionFormSaveResult(
            transactionId = transactionId,
            feedback = UiFeedback(
                message = context.getString(if (isShared) R.string.feedback_transaction_pending_sync else successMessage),
                type = if (isShared) FeedbackType.Pending else FeedbackType.Success,
            ),
            followUpWarning = followUpWarning,
        )
    }

    private suspend fun computeBudgetFollowUpWarning(
        transaction: Transaction,
        wallet: Wallet?,
    ): UiFeedback? {
        if (transaction.type != TransactionType.EXPENSE) return null
        val budgetCents = wallet?.budgetLimit?.amountInCents?.takeIf { it > 0L } ?: return null
        return runCatching {
            val period = assetsPeriodAt(kotlin.time.Clock.System.now(), TimeZone.currentSystemDefault())
            val stats = transactionRepository
                .observeWalletStats(period.monthStartMillis, period.monthEndMillis)
                .first()
                .firstOrNull { it.walletId == wallet.id }
                ?: return null
            val spentCents = stats.currentMonthExpense.amountInCents
            val pct = ((spentCents * 100L) / budgetCents).toInt()
            when {
                pct >= 100 -> UiFeedback(
                    message = context.getString(R.string.budget_followup_exceeded_format, wallet.name, pct),
                    type = FeedbackType.Warning,
                    duration = FeedbackDuration.Long,
                )
                pct >= 80 -> UiFeedback(
                    message = context.getString(R.string.budget_followup_warning_format, wallet.name, pct),
                    type = FeedbackType.Warning,
                    duration = FeedbackDuration.Long,
                )
                else -> null
            }
        }.getOrNull()
    }

    private fun defaultCategoryFor(type: TransactionType): Category =
        if (type == TransactionType.INCOME) Category.DEFAULT_INCOME else Category.DEFAULT_EXPENSE

    private fun isCategoryValidForType(
        category: Category,
        type: TransactionType,
        availableCategories: List<Category>,
        hasLoadedCategories: Boolean,
    ): Boolean =
        category.isIncome == (type == TransactionType.INCOME) &&
            (!hasLoadedCategories || availableCategories.any {
                it.id == category.id && it.isIncome == (type == TransactionType.INCOME)
            })

    private fun hasValidCategoryForSave(state: TransactionFormUiState): Boolean =
        state.step == TransactionFormStep.DETAILS &&
            state.isCategoryExplicitlySelected &&
            (state.category.id == pendingCreatedCategoryId ||
                isCategoryValidForType(state.category, state.type, state.availableCategories, state.hasLoadedCategories))
}

