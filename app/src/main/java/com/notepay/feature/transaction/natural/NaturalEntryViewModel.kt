package com.notepay.feature.transaction.natural

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.ai.HybridNaturalTransactionParser
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.platform.speech.VoiceTranscriptMerger
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.domain.natural.NaturalFallbackNotice
import com.notepay.domain.natural.NaturalParseResult
import com.notepay.domain.natural.NaturalParseSource
import com.notepay.domain.natural.NaturalTransactionDraft
import com.notepay.domain.repository.CategoryRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.shared.SharedWalletPermissionGuard
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.SuggestCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Clock

@Immutable
data class NaturalEntryUiState(
    val rawInput: String = "",
    val drafts: List<NaturalTransactionDraft> = emptyList(),
    val needsAiFallback: Boolean = false,
    val source: NaturalParseSource = NaturalParseSource.ON_DEVICE,
    val isAiLoading: Boolean = false,
    val isAiAvailable: Boolean = false,
    val showAiFallbackNotice: Boolean = false,
    val isSavingBatch: Boolean = false,
    val selectedWalletId: Long? = null,
    val availableWallets: List<Wallet> = emptyList(),
    val availableCategories: List<Category> = Category.getAll(),
) {
    val selectedWallet: Wallet?
        get() = availableWallets.firstOrNull { it.id == selectedWalletId }
            ?: availableWallets.firstOrNull()
}

@HiltViewModel
@Suppress("LongParameterList")
class NaturalEntryViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val suggestCategoryUseCase: SuggestCategoryUseCase,
    private val hybridNaturalParser: HybridNaturalTransactionParser,
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(NaturalEntryUiState())
    val state = _state.asStateFlow()

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    private val _savedSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val savedSuccess = _savedSuccess.asSharedFlow()

    private var voiceBaseInput: String? = null

    init {
        observeWallets()
        observeCategories()
        observeAiAvailability()
    }

    fun onVoiceSessionStarted() {
        voiceBaseInput = _state.value.rawInput
    }

    fun onVoicePartialTranscript(partial: String) {
        val base = voiceBaseInput ?: _state.value.rawInput
        val merged = VoiceTranscriptMerger.merge(
            baseText = base,
            spokenSegment = partial,
            maxLength = MAX_NATURAL_INPUT_LENGTH,
        )
        onInputChanged(merged)
    }

    fun onVoiceFinalTranscript(finalText: String) {
        val base = voiceBaseInput ?: _state.value.rawInput
        val merged = VoiceTranscriptMerger.merge(
            baseText = base,
            spokenSegment = finalText,
            maxLength = MAX_NATURAL_INPUT_LENGTH,
        )
        voiceBaseInput = null
        onInputChanged(merged)
    }

    fun onVoiceSessionEnded() {
        voiceBaseInput = null
    }

    fun onInputChanged(input: String) {
        val capped = input.take(MAX_NATURAL_INPUT_LENGTH)
        val parsed = hybridNaturalParser.parseOnDevice(capped)
        _state.update {
            it.copy(
                rawInput = capped,
                drafts = parsed.drafts,
                needsAiFallback = parsed.needsAiFallback,
                source = NaturalParseSource.ON_DEVICE,
                showAiFallbackNotice = false,
            )
        }
    }

    fun onWalletSelected(walletId: Long) {
        _state.update { it.copy(selectedWalletId = walletId) }
    }

    fun onAnalyzeWithAi() {
        val current = _state.value
        val inputSnapshot = current.rawInput.trim()
        if (inputSnapshot.isBlank() || current.isAiLoading || !current.isAiAvailable) return

        val tier1Snapshot = NaturalParseResult(
            drafts = current.drafts,
            confidence = 0.4f,
            needsAiFallback = current.needsAiFallback,
            source = NaturalParseSource.ON_DEVICE,
        )
        _state.update { it.copy(isAiLoading = true, showAiFallbackNotice = false) }
        viewModelScope.launch(ioDispatcher) {
            try {
                val aiResult = hybridNaturalParser.parseWithAi(inputSnapshot, tier1Snapshot, forceAi = true)
                _state.update { state ->
                    if (state.rawInput.trim() != inputSnapshot) {
                        state.copy(isAiLoading = false)
                    } else {
                        state.copy(
                            drafts = aiResult.drafts,
                            needsAiFallback = aiResult.needsAiFallback,
                            source = aiResult.source,
                            isAiLoading = false,
                            showAiFallbackNotice = aiResult.fallbackNotice == NaturalFallbackNotice.AI_UNAVAILABLE,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                _state.update { it.copy(isAiLoading = false) }
                throw cancelled
            }
        }
    }

    fun onUpdateDraft(updated: NaturalTransactionDraft) {
        _state.update { state ->
            state.copy(
                drafts = state.drafts.map { if (it.id == updated.id) updated else it },
            )
        }
    }

    fun onRemoveDraft(draftId: String) {
        _state.update { state ->
            state.copy(drafts = state.drafts.filterNot { it.id == draftId })
        }
    }

    fun onSaveAllDrafts() {
        val current = _state.value
        if (current.isSavingBatch) return
        val drafts = current.drafts.filter { it.amountVnd in 1..Money.MAX_MAJOR_UNITS }
        val wallet = current.selectedWallet
        if (wallet == null) {
            _feedback.tryEmit(UiFeedback(context.getString(R.string.transaction_validation_wallet), type = FeedbackType.Error))
            return
        }
        if (!SharedWalletPermissionGuard.canAddTransaction(wallet)) {
            _feedback.tryEmit(UiFeedback(context.getString(R.string.shared_wallet_viewer_cannot_write), type = FeedbackType.Error))
            return
        }
        if (drafts.isEmpty()) return

        _state.update { it.copy(isSavingBatch = true) }
        viewModelScope.launch(ioDispatcher) {
            try {
                val (savedCount, failedCount) = persistDrafts(drafts, wallet.id)
                if (failedCount == 0 && savedCount > 0) {
                    reset()
                    _feedback.emit(
                        UiFeedback(
                            context.resources.getQuantityString(
                                R.plurals.natural_entry_saved_batch,
                                savedCount,
                                savedCount,
                            ),
                            type = FeedbackType.Success,
                        ),
                    )
                    _savedSuccess.emit(Unit)
                } else {
                    _state.update { it.copy(isSavingBatch = false) }
                    _feedback.emit(
                        UiFeedback(context.getString(R.string.natural_entry_partial_error), type = FeedbackType.Error),
                    )
                }
            } catch (cancelled: CancellationException) {
                _state.update { it.copy(isSavingBatch = false) }
                throw cancelled
            }
        }
    }

    fun reset() {
        voiceBaseInput = null
        _state.update {
            it.copy(
                rawInput = "",
                drafts = emptyList(),
                needsAiFallback = false,
                source = NaturalParseSource.ON_DEVICE,
                isAiLoading = false,
                showAiFallbackNotice = false,
                isSavingBatch = false,
            )
        }
    }

    private suspend fun persistDrafts(
        drafts: List<NaturalTransactionDraft>,
        walletId: Long,
    ): Pair<Int, Int> {
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
        return savedCount to failedCount
    }

    private fun observeWallets() {
        viewModelScope.launch(ioDispatcher) {
            combine(
                walletRepository.observeAll(),
                walletRepository.observeActive(),
            ) { allWallets, activeWallet ->
                val nonArchived = allWallets.filterNot { it.isArchived }
                val writable = nonArchived.filter { SharedWalletPermissionGuard.canAddTransaction(it) }
                val exposedWallets = writable.ifEmpty { nonArchived }
                val preferredActive = activeWallet?.takeIf { candidate ->
                    exposedWallets.any { it.id == candidate.id }
                }
                exposedWallets to preferredActive?.id
            }.collect { (wallets, activeId) ->
                _state.update { current ->
                    val currentStillExists = current.selectedWalletId != null &&
                        wallets.any { it.id == current.selectedWalletId }
                    val resolvedWalletId = if (currentStillExists) {
                        current.selectedWalletId
                    } else {
                        activeId ?: wallets.firstOrNull()?.id
                    }
                    current.copy(
                        availableWallets = wallets,
                        selectedWalletId = resolvedWalletId,
                    )
                }
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

    private fun observeAiAvailability() {
        viewModelScope.launch(ioDispatcher) {
            hybridNaturalParser.isAiConfiguredFlow.collect { available ->
                _state.update { it.copy(isAiAvailable = available) }
            }
        }
    }

    private companion object {
        const val MAX_NATURAL_INPUT_LENGTH = 500
    }
}
