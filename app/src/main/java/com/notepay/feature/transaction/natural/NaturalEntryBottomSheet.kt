package com.notepay.feature.transaction.natural

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.formatter.TransactionDateLabelFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.localizedName
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.core.platform.speech.AndroidVoiceInputController
import com.notepay.core.platform.speech.VoiceInputError
import com.notepay.core.platform.speech.VoiceInputState
import com.notepay.core.platform.speech.VoiceTranscriptPostProcessor
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.domain.natural.NaturalParseSource
import com.notepay.domain.natural.NaturalTransactionDraft
import com.notepay.feature.transaction.component.sheet.WalletPickerSheet
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * ModalBottomSheet độc lập để nhập nhanh nhiều giao dịch bằng giọng nói hoặc câu tiếng Việt tự nhiên.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NaturalEntryBottomSheet(
    onDismiss: () -> Unit,
    onFeedback: (UiFeedback) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NaturalEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val languageTag = remember(configuration) {
        val locales = configuration.locales
        val primary = if (locales.isEmpty) null else locales[0]
        primary?.toLanguageTag()?.takeIf { it.isNotBlank() } ?: "vi-VN"
    }
    val voicePrompt = stringResource(R.string.natural_entry_voice_prompt)
    val permissionDeniedMsg = stringResource(R.string.natural_entry_voice_permission_denied)
    val voiceUnavailableMsg = stringResource(R.string.natural_entry_voice_unavailable)

    val voiceController = remember(context, viewModel) {
        AndroidVoiceInputController(
            context = context,
            onPartialTranscript = viewModel::onVoicePartialTranscript,
            onFinalTranscript = viewModel::onVoiceFinalTranscript,
        )
    }
    val voiceState by voiceController.state.collectAsStateWithLifecycle()

    DisposableEffect(voiceController) {
        onDispose { voiceController.destroy() }
    }

    val speechActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                .orEmpty()
            val confidences = result.data?.getFloatArrayExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES)
            val spoken = VoiceTranscriptPostProcessor.selectBestCandidate(matches, confidences)
            if (spoken != null) {
                viewModel.onVoiceFinalTranscript(spoken)
            } else {
                viewModel.onVoiceSessionEnded()
            }
        } else {
            viewModel.onVoiceSessionEnded()
        }
    }

    val startVoiceRecognition = {
        keyboardController?.hide()
        viewModel.onVoiceSessionStarted()
        if (!voiceController.startListening(languageTag = languageTag, prompt = voicePrompt)) {
            try {
                speechActivityLauncher.launch(
                    voiceController.buildRecognizerIntent(
                        languageTag = languageTag,
                        prompt = voicePrompt,
                    ),
                )
            } catch (_: ActivityNotFoundException) {
                viewModel.onVoiceSessionEnded()
                onFeedback(UiFeedback(voiceUnavailableMsg, type = FeedbackType.Error))
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startVoiceRecognition()
        } else {
            onFeedback(UiFeedback(permissionDeniedMsg, type = FeedbackType.Error))
        }
    }

    val voiceError = voiceState.error
    val voiceErrorMsg = voiceError?.let { stringResource(it.toStringRes()) }
    LaunchedEffect(voiceError, voiceErrorMsg) {
        if (voiceError != null && voiceErrorMsg != null) {
            onFeedback(UiFeedback(voiceErrorMsg, type = FeedbackType.Error))
            viewModel.onVoiceSessionEnded()
            voiceController.clearError()
        }
    }

    var showWalletPicker by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        launch {
            viewModel.feedback.collect { onFeedback(it) }
        }
        launch {
            viewModel.savedSuccess.collect { onDismiss() }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val cancelVoiceSession = {
        if (voiceState.isListening) {
            voiceController.cancel()
        }
        viewModel.onVoiceSessionEnded()
    }

    val handleDismiss = {
        cancelVoiceSession()
        keyboardController?.hide()
        viewModel.reset()
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = handleDismiss,
        sheetState = sheetState,
        shape = AppTheme.shapes.sheetTop,
        containerColor = AppTheme.colors.surface,
        contentColor = AppTheme.colors.textPrimary,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        modifier = modifier,
    ) {
        NaturalEntrySheetContent(
            state = state,
            voiceState = voiceState,
            focusRequester = focusRequester,
            onInputChanged = { input ->
                if (voiceState.isListening) {
                    voiceController.cancel()
                    viewModel.onVoiceSessionEnded()
                }
                viewModel.onInputChanged(input)
            },
            onToggleVoice = {
                if (voiceState.isListening) {
                    voiceController.stopListening()
                } else if (
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    startVoiceRecognition()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onOpenWalletPicker = { showWalletPicker = true },
            onAnalyzeWithAi = {
                cancelVoiceSession()
                keyboardController?.hide()
                viewModel.onAnalyzeWithAi()
            },
            onUpdateDraft = viewModel::onUpdateDraft,
            onRemoveDraft = viewModel::onRemoveDraft,
            onSaveAllDrafts = {
                cancelVoiceSession()
                keyboardController?.hide()
                viewModel.onSaveAllDrafts()
            },
            onClose = handleDismiss,
        )
    }

    if (showWalletPicker && state.availableWallets.size > 1) {
        WalletPickerSheet(
            wallets = state.availableWallets,
            selectedWalletId = state.selectedWalletId,
            onWalletSelected = { id ->
                viewModel.onWalletSelected(id)
                showWalletPicker = false
            },
            onDismiss = { showWalletPicker = false },
        )
    }
}

@Composable
private fun NaturalEntrySheetContent(
    state: NaturalEntryUiState,
    voiceState: VoiceInputState,
    focusRequester: FocusRequester,
    onInputChanged: (String) -> Unit,
    onToggleVoice: () -> Unit,
    onOpenWalletPicker: () -> Unit,
    onAnalyzeWithAi: () -> Unit,
    onUpdateDraft: (NaturalTransactionDraft) -> Unit,
    onRemoveDraft: (String) -> Unit,
    onSaveAllDrafts: () -> Unit,
    onClose: () -> Unit,
) {
    var editingDraftId by remember { mutableStateOf<String?>(null) }
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val keyboardController = LocalSoftwareKeyboardController.current
    val todayPrefix = stringResource(R.string.date_today_prefix)
    val yesterdayPrefix = stringResource(R.string.date_yesterday_prefix)
    val monthSuffixFmt = stringResource(R.string.date_month_suffix)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NaturalEntryHeader(
            selectedWallet = state.selectedWallet,
            canSwitchWallet = state.availableWallets.size > 1,
            onOpenWalletPicker = onOpenWalletPicker,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.rawInput,
                onValueChange = onInputChanged,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        text = stringResource(R.string.natural_entry_placeholder),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                shape = AppTheme.shapes.input,
                minLines = 2,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            )

            VoiceMicButton(
                isListening = voiceState.isListening,
                rmsNormalized = voiceState.rmsNormalized,
                enabled = !state.isSavingBatch && !state.isAiLoading,
                onClick = onToggleVoice,
            )
        }

        if (voiceState.isListening) {
            VoiceListeningIndicatorRow()
        }

        if (state.drafts.isNotEmpty()) {
            PreviewHeaderRow(
                draftCount = state.drafts.size,
                source = state.source,
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = state.drafts, key = { it.id }) { draft ->
                    val dateText = remember(draft.date, todayPrefix, yesterdayPrefix, monthSuffixFmt) {
                        TransactionDateLabelFormatter.format(
                            target = draft.date,
                            today = today,
                            todayPrefix = todayPrefix,
                            yesterdayPrefix = yesterdayPrefix,
                            monthSuffixFormat = monthSuffixFmt,
                        )
                    }
                    NaturalDraftPreviewCard(
                        draft = draft,
                        dateLabel = dateText,
                        isEditing = editingDraftId == draft.id,
                        availableCategories = state.availableCategories,
                        onToggleEdit = {
                            editingDraftId = if (editingDraftId == draft.id) null else draft.id
                        },
                        onUpdateDraft = onUpdateDraft,
                        onRemoveDraft = {
                            if (editingDraftId == draft.id) editingDraftId = null
                            onRemoveDraft(draft.id)
                        },
                    )
                }
            }
        }

        if (state.showAiFallbackNotice) {
            Text(
                text = stringResource(R.string.natural_entry_ai_fallback_notice),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (state.isAiAvailable && state.rawInput.isNotBlank()) {
            DeepAiAnalyzeButton(
                isLoading = state.isAiLoading,
                highlighted = state.needsAiFallback,
                onClick = onAnalyzeWithAi,
            )
        }

        NaturalEntryBottomAction(
            draftCount = state.drafts.size,
            isSavingBatch = state.isSavingBatch,
            isAiLoading = state.isAiLoading,
            onSaveAllDrafts = onSaveAllDrafts,
            onClose = onClose,
        )
    }
}

@Composable
private fun NaturalEntryHeader(
    selectedWallet: Wallet?,
    canSwitchWallet: Boolean,
    onOpenWalletPicker: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.natural_entry_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (selectedWallet != null) {
            Surface(
                onClick = onOpenWalletPicker,
                enabled = canSwitchWallet,
                shape = AppTheme.shapes.capsule,
                color = AppTheme.colors.surfaceRaised,
                border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .widthIn(max = 156.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = AppTheme.colors.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = selectedWallet.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (canSwitchWallet) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.wallet_picker_title),
                            tint = AppTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NaturalEntryBottomAction(
    draftCount: Int,
    isSavingBatch: Boolean,
    isAiLoading: Boolean,
    onSaveAllDrafts: () -> Unit,
    onClose: () -> Unit,
) {
    if (draftCount > 0) {
        Button(
            onClick = onSaveAllDrafts,
            enabled = !isSavingBatch && !isAiLoading,
            shape = AppTheme.shapes.capsule,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.ink,
                contentColor = AppTheme.colors.onInk,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
        ) {
            if (isSavingBatch) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = AppTheme.colors.onInk,
                )
            } else {
                Text(
                    text = pluralStringResource(R.plurals.natural_entry_save_all, draftCount, draftCount),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    } else {
        OutlinedButton(
            onClick = onClose,
            shape = AppTheme.shapes.capsule,
            border = BorderStroke(1.dp, AppTheme.colors.borderStrong),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
        ) {
            Text(
                text = stringResource(R.string.action_close),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PreviewHeaderRow(
    draftCount: Int,
    source: NaturalParseSource,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.natural_entry_preview_header, draftCount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.size(8.dp))
        val isAi = source == NaturalParseSource.AI_FALLBACK
        Surface(
            color = if (isAi) AppTheme.colors.sand else AppTheme.colors.surfaceSunken,
            contentColor = if (isAi) AppTheme.colors.onSand else AppTheme.colors.textSecondary,
            shape = AppTheme.shapes.capsule,
        ) {
            Text(
                text = stringResource(
                    if (isAi) R.string.natural_entry_source_ai else R.string.natural_entry_source_device,
                ),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun DeepAiAnalyzeButton(
    isLoading: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (highlighted) AppTheme.colors.sand else AppTheme.colors.surfaceRaised
    val contentColor = if (highlighted) AppTheme.colors.onSand else AppTheme.colors.textPrimary
    val borderColor = if (highlighted) AppTheme.colors.sand else AppTheme.colors.borderStrong

    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        shape = AppTheme.shapes.capsule,
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.natural_entry_ai_analyzing),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.natural_entry_ai_button),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NaturalDraftPreviewCard(
    draft: NaturalTransactionDraft,
    dateLabel: String,
    isEditing: Boolean,
    availableCategories: List<Category>,
    onToggleEdit: () -> Unit,
    onUpdateDraft: (NaturalTransactionDraft) -> Unit,
    onRemoveDraft: () -> Unit,
) {
    Surface(
        color = AppTheme.colors.surfaceRaised,
        shape = AppTheme.shapes.corner20,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = MoneyFormatter.format(Money.fromMajorUnit(draft.amountVnd)),
                        style = AppTheme.typography.amountRow,
                        color = if (draft.type == TransactionType.INCOME) {
                            AppTheme.colors.success
                        } else {
                            AppTheme.colors.error
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = draft.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            onClick = onToggleEdit,
                            color = AppTheme.colors.surfaceSunken,
                            shape = AppTheme.shapes.capsule,
                        ) {
                            Text(
                                text = draft.category.localizedName(),
                                style = MaterialTheme.typography.labelMedium,
                                color = AppTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = AppTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                IconButton(
                    onClick = onToggleEdit,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.natural_entry_edit_cd),
                        tint = AppTheme.colors.textSecondary,
                    )
                }

                IconButton(
                    onClick = onRemoveDraft,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.natural_entry_delete_cd),
                        tint = AppTheme.colors.textSecondary,
                    )
                }
            }

            if (isEditing) {
                DraftInlineEditor(
                    draft = draft,
                    availableCategories = availableCategories,
                    onUpdateDraft = onUpdateDraft,
                    onDoneEdit = onToggleEdit,
                )
            }
        }
    }
}

@Composable
private fun DraftInlineEditor(
    draft: NaturalTransactionDraft,
    availableCategories: List<Category>,
    onUpdateDraft: (NaturalTransactionDraft) -> Unit,
    onDoneEdit: () -> Unit,
) {
    var amountText by remember(draft.id) { mutableStateOf(draft.amountVnd.toString()) }
    var noteText by remember(draft.id) { mutableStateOf(draft.note) }
    val matchingCategories = remember(availableCategories, draft.type) {
        val isIncome = draft.type == TransactionType.INCOME
        availableCategories.filter { it.isIncome == isIncome }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = amountText,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(12)
                amountText = digits
                val parsed = digits.toLongOrNull()
                if (parsed != null && parsed in 1..Money.MAX_MAJOR_UNITS) {
                    onUpdateDraft(draft.copy(amountVnd = parsed))
                }
            },
            label = { Text(stringResource(R.string.natural_entry_amount_label)) },
            shape = AppTheme.shapes.input,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = noteText,
            onValueChange = { raw ->
                val capped = raw.take(Transaction.MAX_NOTE_LENGTH)
                noteText = capped
                if (capped.isNotBlank()) {
                    onUpdateDraft(draft.copy(note = capped))
                }
            },
            label = { Text(stringResource(R.string.natural_entry_note_label)) },
            shape = AppTheme.shapes.input,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            matchingCategories.forEach { cat ->
                val selected = cat.id == draft.category.id
                FilterChip(
                    selected = selected,
                    onClick = { onUpdateDraft(draft.copy(category = cat)) },
                    label = {
                        Text(
                            text = cat.localizedName(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    shape = AppTheme.shapes.capsule,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppTheme.colors.sand,
                        selectedLabelColor = AppTheme.colors.onSand,
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = onDoneEdit,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.natural_entry_done_edit))
            }
        }
    }
}

@Composable
private fun VoiceMicButton(
    isListening: Boolean,
    rmsNormalized: Float,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    val targetScale = if (isListening && !reducedMotion) {
        1f + (rmsNormalized * 0.12f)
    } else {
        1f
    }
    val pulseScale by animateFloatAsState(
        targetValue = targetScale,
        label = "voiceMicPulse",
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (isListening) AppTheme.colors.sand else AppTheme.colors.surfaceRaised,
        contentColor = if (isListening) AppTheme.colors.onSand else AppTheme.colors.textPrimary,
        border = BorderStroke(
            width = 0.5.dp,
            color = if (isListening) AppTheme.colors.sand else AppTheme.colors.hairline,
        ),
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                contentDescription = stringResource(
                    if (isListening) {
                        R.string.natural_entry_voice_stop_cd
                    } else {
                        R.string.natural_entry_voice_start_cd
                    },
                ),
            )
        }
    }
}

@Composable
private fun VoiceListeningIndicatorRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = null,
            tint = AppTheme.colors.textSecondary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.natural_entry_voice_listening),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun VoiceInputError.toStringRes(): Int = when (this) {
    VoiceInputError.PERMISSION_DENIED -> R.string.natural_entry_voice_permission_denied
    VoiceInputError.UNAVAILABLE -> R.string.natural_entry_voice_unavailable
    VoiceInputError.NO_MATCH -> R.string.natural_entry_voice_no_match
    VoiceInputError.NETWORK -> R.string.natural_entry_voice_network_error
    VoiceInputError.AUDIO,
    VoiceInputError.UNKNOWN,
    -> R.string.natural_entry_voice_error
}
