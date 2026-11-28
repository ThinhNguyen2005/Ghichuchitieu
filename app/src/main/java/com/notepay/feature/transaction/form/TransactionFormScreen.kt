package com.notepay.feature.transaction.form

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.component.effect.AppAtmosphericBackground
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.feedback.FeedbackSnackbarOverlay
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.NotePayStatusBanner
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.feedback.rememberLatestFeedbackDispatcher
import com.notepay.core.designsystem.formatter.TransactionDateLabelFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.HomeHeroCardDarkBottom
import com.notepay.core.designsystem.theme.HomeHeroCardDarkRim
import com.notepay.core.designsystem.theme.HomeHeroCardDarkTop
import com.notepay.core.designsystem.theme.HomeHeroCardLightBottom
import com.notepay.core.designsystem.theme.HomeHeroCardLightRim
import com.notepay.core.designsystem.theme.HomeHeroCardLightTop
import com.notepay.core.designsystem.util.localizedName
import com.notepay.core.navigation.NotePayEmphasizedDecelerate
import com.notepay.core.navigation.NotePayMotion
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.domain.model.TransactionType
import com.notepay.feature.category.mapper.CategoryAvatar
import com.notepay.feature.transaction.component.CalcKey
import com.notepay.feature.transaction.component.CategoryGridPicker
import com.notepay.feature.transaction.component.CategoryQuickSelectionRow
import com.notepay.feature.transaction.component.NotePreviewChip
import com.notepay.feature.transaction.component.TransactionAmountDisplay
import com.notepay.feature.transaction.component.TransactionCustomKeypad
import com.notepay.feature.transaction.component.TransactionNoteField
import com.notepay.feature.transaction.component.TransactionNumericKeypad
import com.notepay.feature.transaction.component.TransactionPrimarySaveButton
import com.notepay.feature.transaction.component.TransactionSuggestionChipRow
import com.notepay.feature.transaction.component.TransactionTypeSelector
import com.notepay.feature.transaction.component.TransactionWalletField
import com.notepay.feature.transaction.component.sheet.CategoryPickerSheet
import com.notepay.feature.transaction.component.sheet.NoteQuickEntrySheet
import com.notepay.feature.transaction.component.sheet.WalletPickerSheet
import com.notepay.feature.transaction.form.validation.FieldError
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    onSaved: (TransactionFormSaveResult) -> Unit,
    onBack: () -> Unit,
    onOpenAiSettings: () -> Unit = {},
    initialLaunchScan: Boolean = false,
    onClearLaunchScan: () -> Unit = {},
    viewModel: TransactionFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedTransaction by viewModel.savedTransaction.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val feedbackDispatcher = rememberLatestFeedbackDispatcher(snackbarHostState)
    val openAiSettingsLabel = stringResource(R.string.action_open_ai_settings)

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.onEvent(TransactionFormUiEvent.ImagePicked(it)) } }

    LaunchedEffect(initialLaunchScan) {
        if (initialLaunchScan) {
            onClearLaunchScan()
            imagePicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }

    val todayPrefix = stringResource(R.string.date_today_prefix)
    val yesterdayPrefix = stringResource(R.string.date_yesterday_prefix)
    val monthSuffixFmt = stringResource(R.string.date_month_suffix)

    LaunchedEffect(savedTransaction) {
        savedTransaction?.let(onSaved)
    }
    LaunchedEffect(viewModel, feedbackDispatcher, openAiSettingsLabel, onOpenAiSettings) {
        viewModel.feedback.collect { feedback ->
            val resolved = if (feedback.actionLabel == openAiSettingsLabel && feedback.onAction == null) {
                feedback.copy(onAction = onOpenAiSettings)
            } else {
                feedback
            }
            feedbackDispatcher.show(resolved)
        }
    }

    val systemTimeZone = TimeZone.currentSystemDefault()
    val dateLabel = remember(state.occurredAt, todayPrefix, yesterdayPrefix, monthSuffixFmt) {
        TransactionDateLabelFormatter.format(
            target = state.occurredAt.toLocalDateTime(systemTimeZone).date,
            today = Clock.System.now().toLocalDateTime(systemTimeZone).date,
            todayPrefix = todayPrefix,
            yesterdayPrefix = yesterdayPrefix,
            monthSuffixFormat = monthSuffixFmt,
        )
    }

    TransactionFormContent(
        state = state,
        dateLabel = dateLabel,
        onEvent = viewModel::onEvent,
        onScanImage = {
            imagePicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onBack = { if (!viewModel.state.value.isSaving) onBack() },
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionFormContent(
    state: TransactionFormUiState,
    dateLabel: String,
    onEvent: (TransactionFormUiEvent) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onScanImage: () -> Unit = {},
) {
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val feedbackDispatcher = rememberLatestFeedbackDispatcher(snackbarHostState)

    val isAddDetailsStep = !state.isEditMode && state.step == TransactionFormStep.DETAILS
    BackHandler(enabled = isAddDetailsStep || state.isSaving) {
        if (!state.isSaving) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            onEvent(TransactionFormUiEvent.OpenCategorySelection)
        }
    }

    LaunchedEffect(state.isEditMode, state.step) {
        if (!state.isEditMode && state.step == TransactionFormStep.DETAILS) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    val isLight = AppTheme.colors.isLight
    val headerPillColor = AppTheme.colors.surface.copy(alpha = if (isLight) 0.92f else 0.88f)
    val heroBrush = remember(isLight) {
        Brush.verticalGradient(
            colors = if (isLight) {
                listOf(HomeHeroCardLightTop, HomeHeroCardLightBottom)
            } else {
                listOf(HomeHeroCardDarkTop, HomeHeroCardDarkBottom)
            },
        )
    }
    val heroRimColor = if (isLight) HomeHeroCardLightRim else HomeHeroCardDarkRim
    val titleText = when {
        state.isEditMode && state.isAutoCapture -> stringResource(R.string.transaction_details_title)
        state.isEditMode -> stringResource(R.string.edit_transaction_title)
        state.step == TransactionFormStep.SELECT_CATEGORY -> stringResource(R.string.add_transaction_step_category_title)
        else -> stringResource(R.string.add_transaction_step_details_title)
    }
    val detailsAnnouncement = if (isAddDetailsStep) {
        stringResource(R.string.transaction_step_details_announced, state.category.localizedName())
    } else {
        null
    }

    Box(modifier = modifier.fillMaxSize()) {
        AppAtmosphericBackground(
            modifier = Modifier.matchParentSize(),
            forceSoftBlurOnPhoto = true,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TransactionFormHeader(
                title = titleText,
                detailsAnnouncement = detailsAnnouncement,
                isAutoCapture = state.isAutoCapture,
                isStepAnnouncementLive = !state.isEditMode,
                canGoBack = !state.isSaving,
                headerPillColor = headerPillColor,
                onBackClick = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    if (isAddDetailsStep) {
                        onEvent(TransactionFormUiEvent.OpenCategorySelection)
                    } else {
                        onBack()
                    }
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (state.isEditMode) {
                    EditModeFormContent(
                        state = state,
                        dateLabel = dateLabel,
                        heroBrush = heroBrush,
                        heroRimColor = heroRimColor,
                        onEvent = onEvent,
                        onShowFeedback = feedbackDispatcher::show,
                    )
                } else {
                    AddModeStepContent(
                        state = state,
                        dateLabel = dateLabel,
                        onEvent = onEvent,
                        onScanImage = onScanImage,
                        onShowFeedback = feedbackDispatcher::show,
                    )
                }
            }
        }

        FeedbackSnackbarOverlay(hostState = snackbarHostState)
    }

    TransactionFormDialogs(state = state, onEvent = onEvent)
}

@Composable
private fun TransactionFormHeader(
    title: String,
    detailsAnnouncement: String?,
    isAutoCapture: Boolean,
    isStepAnnouncementLive: Boolean,
    canGoBack: Boolean,
    headerPillColor: Color,
    onBackClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            onClick = onBackClick,
            enabled = canGoBack,
            shape = CircleShape,
            color = headerPillColor,
            border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
            modifier = Modifier
                .size(48.dp)
                .iosCardShadow(shape = CircleShape, elevated = false),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = AppTheme.colors.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = AppTheme.typography.displaySerif,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics {
                    heading()
                    if (isStepAnnouncementLive) {
                        liveRegion = LiveRegionMode.Polite
                        if (!detailsAnnouncement.isNullOrBlank()) {
                            contentDescription = detailsAnnouncement
                        }
                    }
                },
            )
            if (isAutoCapture) {
                Icon(
                    imageVector = Icons.Rounded.AccountBalance,
                    contentDescription = stringResource(R.string.transaction_auto_capture_cd),
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun AddModeStepContent(
    state: TransactionFormUiState,
    dateLabel: String,
    onEvent: (TransactionFormUiEvent) -> Unit,
    onScanImage: () -> Unit,
    onShowFeedback: (UiFeedback) -> Unit,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    val slideDistancePx = with(LocalDensity.current) { NotePayMotion.contentTranslation.roundToPx() }

    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            if (reducedMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val direction = if (targetState == TransactionFormStep.DETAILS) 1 else -1
                (
                    fadeIn(
                        tween(
                            durationMillis = NotePayMotion.contentDurationMillis,
                            easing = NotePayEmphasizedDecelerate,
                        ),
                    ) + slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = NotePayMotion.contentDurationMillis,
                            easing = NotePayEmphasizedDecelerate,
                        ),
                        initialOffsetX = { direction * slideDistancePx },
                    )
                    ) togetherWith (
                    fadeOut(
                        tween(
                            durationMillis = NotePayMotion.contentFadeOutDurationMillis,
                            easing = NotePayEmphasizedDecelerate,
                        ),
                    ) + slideOutHorizontally(
                        animationSpec = tween(
                            durationMillis = NotePayMotion.contentFadeOutDurationMillis,
                            easing = NotePayEmphasizedDecelerate,
                        ),
                        targetOffsetX = { -direction * slideDistancePx },
                    )
                    )
            }
        },
        modifier = Modifier.fillMaxSize(),
        label = "TransactionFormAddStep",
    ) { targetStep ->
        val isActiveStep = targetStep == state.step
        val stepModifier = if (isActiveStep) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxSize()
                .clearAndSetSemantics {}
        }

        when (targetStep) {
            TransactionFormStep.SELECT_CATEGORY -> AddCategorySelectionStep(
                state = state,
                onEvent = onEvent,
                modifier = stepModifier,
            )

            TransactionFormStep.DETAILS -> AddTransactionDetailsStep(
                state = state,
                dateLabel = dateLabel,
                onEvent = onEvent,
                onScanImage = onScanImage,
                onShowFeedback = onShowFeedback,
                modifier = stepModifier,
            )
        }
    }
}

@Composable
private fun AddCategorySelectionStep(
    state: TransactionFormUiState,
    onEvent: (TransactionFormUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TransactionTypeSelector(
            selectedType = state.type,
            onTypeChanged = { onEvent(TransactionFormUiEvent.TypeChanged(it)) },
            expenseLabel = stringResource(R.string.transaction_type_expense),
            incomeLabel = stringResource(R.string.transaction_type_income),
        )

        Surface(
            shape = AppTheme.shapes.corner24,
            color = AppTheme.colors.surface,
            border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .iosCardShadow(shape = AppTheme.shapes.corner24, elevated = false),
        ) {
            CategoryGridPicker(
                categories = state.availableCategories,
                selectedCategory = if (state.isCategoryExplicitlySelected) state.category else null,
                isIncome = state.type == TransactionType.INCOME,
                onCategoryChanged = { onEvent(TransactionFormUiEvent.CategorySelected(it)) },
                onCreateCategory = { name, color, iconId, isIncome ->
                    onEvent(TransactionFormUiEvent.CreateCategory(name, color, iconId, isIncome))
                },
                fillAvailableHeight = true,
                isLoading = !state.hasLoadedCategories,
                showHeader = true,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTransactionDetailsStep(
    state: TransactionFormUiState,
    dateLabel: String,
    onEvent: (TransactionFormUiEvent) -> Unit,
    onScanImage: () -> Unit,
    onShowFeedback: (UiFeedback) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryName = state.category.localizedName()
    val changeCategoryLabel = stringResource(R.string.action_change_category)
    val typeLabel = if (state.type == TransactionType.INCOME) {
        stringResource(R.string.transaction_type_income)
    } else {
        stringResource(R.string.transaction_type_expense)
    }
    val amountInvalidMsg = stringResource(R.string.transaction_validation_amount)
    val noteTooLongMsg = stringResource(R.string.transaction_note_too_long)
    val noWritableWallet = stringResource(R.string.transaction_no_writable_wallet)
    val noWritableWalletDesc = stringResource(R.string.transaction_no_writable_wallet_desc)
    val noOtherWallet = stringResource(R.string.transaction_no_other_wallet)
    val selectedWallet = state.availableWallets.firstOrNull { it.id == state.walletId }
    val hasAmountError = state.calcState.error != null ||
        FieldError.AMOUNT_EMPTY in state.errors ||
        FieldError.AMOUNT_INVALID in state.errors
    val hasWalletError = FieldError.WALLET_MISSING in state.errors
    val isLight = AppTheme.colors.isLight
    val categoryColor = Color(state.category.colorArgb)
    val isImeVisible = WindowInsets.isImeVisible

    Column(
        modifier = modifier
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            // Centered Category + Amount Hero with unclipped soft radial halo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Column(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(AppTheme.shapes.corner20)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$categoryName, $typeLabel"
                        }
                        .clickable(
                            role = Role.Button,
                            onClickLabel = changeCategoryLabel,
                            onClick = { onEvent(TransactionFormUiEvent.OpenCategorySelection) },
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .drawBehind {
                                    val radius = size.minDimension * 0.5f
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colorStops = arrayOf(
                                                0f to categoryColor.copy(alpha = if (isLight) 0.18f else 0.26f),
                                                0.55f to categoryColor.copy(alpha = if (isLight) 0.05f else 0.08f),
                                                1f to Color.Transparent,
                                            ),
                                            center = center,
                                            radius = radius,
                                        ),
                                        radius = radius,
                                        center = center,
                                    )
                                },
                        )
                        Box(contentAlignment = Alignment.BottomEnd) {
                            CategoryAvatar(
                                category = state.category,
                                size = 60.dp,
                                iconSize = 28.dp,
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.surface)
                                    .border(0.75.dp, AppTheme.colors.hairline, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = AppTheme.colors.textPrimary,
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = AppTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                TransactionAmountDisplay(
                    amountInput = state.displayExpression,
                    rawAmountForMagnitude = state.previewAmountMajorUnits,
                    hasError = hasAmountError,
                    showTitle = false,
                    modifier = Modifier.semantics {
                        if (hasAmountError) {
                            error(amountInvalidMsg)
                        }
                    },
                )

                if (state.isImageScanning || state.imageScanMessage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        if (state.isImageScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = AppTheme.colors.primary,
                            )
                        }
                        state.imageScanMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Single inline note card + quick note suggestions + inline scan shortcut
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TransactionNoteField(
                    value = state.note,
                    onValueChange = { onEvent(TransactionFormUiEvent.NoteChanged(it)) },
                    label = stringResource(R.string.transaction_field_note),
                    placeholder = stringResource(R.string.transaction_field_note_placeholder),
                    isError = FieldError.NOTE_TOO_LONG in state.errors,
                    errorText = noteTooLongMsg,
                    modifier = Modifier.fillMaxWidth(),
                )
                QuickNoteAndScanRow(
                    isIncome = state.type == TransactionType.INCOME,
                    currentNote = state.note,
                    isScanning = state.isImageScanning,
                    onSelectNote = { onEvent(TransactionFormUiEvent.NoteChanged(it)) },
                    onScanImage = onScanImage,
                )
            }

            // Compact Date + Wallet chips row directly above quick amount & keypad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DetailsActionChip(
                    icon = Icons.Rounded.CalendarMonth,
                    text = dateLabel,
                    isError = false,
                    onClick = { onEvent(TransactionFormUiEvent.SetDatePickerVisible(true)) },
                )

                DetailsActionChip(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    text = selectedWallet?.name ?: noWritableWallet,
                    isError = hasWalletError,
                    onClick = {
                        when {
                            state.availableWallets.isEmpty() -> onShowFeedback(
                                UiFeedback(noWritableWalletDesc, type = FeedbackType.Warning),
                            )
                            state.availableWallets.size > 1 -> onEvent(TransactionFormUiEvent.SetWalletPickerVisible(true))
                            else -> onShowFeedback(UiFeedback(noOtherWallet, type = FeedbackType.Info))
                        }
                    },
                    modifier = Modifier.weight(1f, fill = false),
                )
            }

            if (state.availableWallets.isEmpty() && !state.hasReadOnlyWalletsOnly) {
                Text(
                    text = noWritableWalletDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasWalletError) AppTheme.colors.error else AppTheme.colors.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .semantics {
                            if (hasWalletError) error(noWritableWalletDesc)
                        },
                )
            }

            NotePayStatusBanner(
                visible = state.isSelectedWalletReadOnly || state.hasReadOnlyWalletsOnly,
                message = stringResource(R.string.shared_wallet_viewer_cannot_write),
                type = FeedbackType.Warning,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Docked quick amount presets + 4-column numeric keypad + full-width Save CTA
        if (!isImeVisible) {
            QuickAmountPresetRow(
                selectedAmountMajorUnits = state.previewAmountMajorUnits,
                onSelectAmount = { onEvent(TransactionFormUiEvent.QuickAmountSelected(it)) },
                modifier = Modifier.padding(bottom = 8.dp),
            )
            TransactionNumericKeypad(
                activeOperator = state.activePendingOperator,
                isAmountLocked = false,
                onKey = { onEvent(TransactionFormUiEvent.CalcKeyPressed(it)) },
                onBackspaceLong = { onEvent(TransactionFormUiEvent.BackspaceLong) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        TransactionPrimarySaveButton(
            label = stringResource(R.string.action_add_transaction),
            canSave = state.canSave,
            isSaving = state.isSaving,
            onClick = { onEvent(TransactionFormUiEvent.Save) },
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun QuickNoteAndScanRow(
    isIncome: Boolean,
    currentNote: String,
    isScanning: Boolean,
    onSelectNote: (String) -> Unit,
    onScanImage: () -> Unit,
) {
    val chips = if (isIncome) {
        listOf(
            stringResource(R.string.transaction_quick_note_salary),
            stringResource(R.string.transaction_quick_note_bonus),
            stringResource(R.string.transaction_quick_note_refund),
        )
    } else {
        listOf(
            stringResource(R.string.transaction_quick_note_breakfast),
            stringResource(R.string.transaction_quick_note_lunch),
            stringResource(R.string.transaction_quick_note_coffee),
            stringResource(R.string.transaction_quick_note_fuel),
            stringResource(R.string.transaction_quick_note_groceries),
        )
    }
    val shape = AppTheme.shapes.capsule
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            onClick = onScanImage,
            enabled = !isScanning,
            shape = CircleShape,
            color = AppTheme.colors.surface,
            border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
            modifier = Modifier
                .size(48.dp)
                .iosCardShadow(shape = CircleShape, elevated = false),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = stringResource(R.string.cd_scan_receipt_inline),
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chips.forEach { suggestion ->
                val selected = currentNote.trim().equals(suggestion, ignoreCase = true)
                Surface(
                    onClick = { onSelectNote(suggestion) },
                    shape = shape,
                    color = if (selected) AppTheme.colors.sand else AppTheme.colors.surface,
                    border = BorderStroke(
                        width = 0.5.dp,
                        color = if (selected) AppTheme.colors.sand else AppTheme.colors.hairline,
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = suggestion,
                            style = AppTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) AppTheme.colors.onSand else AppTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAmountPresetRow(
    selectedAmountMajorUnits: Long,
    onSelectAmount: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets = remember {
        listOf(
            20_000L to "20k",
            50_000L to "50k",
            100_000L to "100k",
            200_000L to "200k",
            500_000L to "500k",
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEach { (amountVnd, label) ->
            val selected = selectedAmountMajorUnits == amountVnd
            Surface(
                onClick = { onSelectAmount(amountVnd) },
                shape = AppTheme.shapes.capsule,
                color = if (selected) AppTheme.colors.sand else AppTheme.colors.surface,
                border = BorderStroke(
                    width = 0.5.dp,
                    color = if (selected) AppTheme.colors.sand else AppTheme.colors.hairline,
                ),
                modifier = Modifier.defaultMinSize(minHeight = 40.dp),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = AppTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (selected) AppTheme.colors.onSand else AppTheme.colors.textPrimary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = AppTheme.colors.isLight
    val shape = AppTheme.shapes.corner12
    Surface(
        onClick = onClick,
        shape = shape,
        color = AppTheme.colors.surface.copy(alpha = if (isLight) 0.94f else 0.88f),
        border = BorderStroke(
            width = if (isError) 1.dp else 0.5.dp,
            color = if (isError) AppTheme.colors.error else AppTheme.colors.hairline,
        ),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .iosCardShadow(shape = shape, elevated = false),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isError) AppTheme.colors.error else AppTheme.colors.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isError) AppTheme.colors.error else AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EditModeFormContent(
    state: TransactionFormUiState,
    dateLabel: String,
    heroBrush: Brush,
    heroRimColor: Color,
    onEvent: (TransactionFormUiEvent) -> Unit,
    onShowFeedback: (UiFeedback) -> Unit,
) {
    val fieldWallet = stringResource(R.string.transaction_field_wallet)
    val noWalletSelected = stringResource(R.string.transaction_no_wallet_selected)
    val noOtherWalletMsg = stringResource(R.string.transaction_no_other_wallet)
    val expenseLabel = stringResource(R.string.transaction_type_expense)
    val incomeLabel = stringResource(R.string.transaction_type_income)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = AppTheme.shapes.corner28,
            color = Color.Transparent,
            border = BorderStroke(1.dp, heroRimColor),
            modifier = Modifier
                .fillMaxWidth()
                .iosCardShadow(shape = AppTheme.shapes.corner28, elevated = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(heroBrush)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TransactionTypeSelector(
                    selectedType = state.type,
                    onTypeChanged = { onEvent(TransactionFormUiEvent.TypeChanged(it)) },
                    expenseLabel = expenseLabel,
                    incomeLabel = incomeLabel,
                )

                TransactionAmountDisplay(
                    amountInput = state.displayExpression,
                    rawAmountForMagnitude = state.previewAmountMajorUnits,
                    hasError = state.calcState.error != null,
                )

                if (state.isAutoCapture) {
                    Text(
                        text = stringResource(R.string.transaction_auto_capture_amount_locked),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        }

        Surface(
            shape = AppTheme.shapes.corner20,
            color = AppTheme.colors.surface,
            border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
            modifier = Modifier
                .fillMaxWidth()
                .iosCardShadow(shape = AppTheme.shapes.corner20, elevated = false),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val suggested = state.suggestedCategory
                if (suggested != null) {
                    TransactionSuggestionChipRow(
                        suggestedCategory = suggested,
                        reason = state.suggestionReason,
                        isApplied = suggested == state.category,
                        onSelect = { onEvent(TransactionFormUiEvent.CategorySelected(it)) },
                    )
                }

                CategoryQuickSelectionRow(
                    categories = state.availableCategories,
                    selectedCategory = state.category,
                    isIncome = state.type == TransactionType.INCOME,
                    onCategoryChanged = { onEvent(TransactionFormUiEvent.CategorySelected(it)) },
                    onSeeAllClick = { onEvent(TransactionFormUiEvent.SetCategoryPickerVisible(true)) },
                )
            }
        }

        val selectedWallet = state.availableWallets.firstOrNull { it.id == state.walletId }
        TransactionWalletField(
            label = fieldWallet,
            value = selectedWallet?.name ?: noWalletSelected,
            onClick = {
                if (state.availableWallets.size > 1) {
                    onEvent(TransactionFormUiEvent.SetWalletPickerVisible(true))
                } else {
                    onShowFeedback(UiFeedback(noOtherWalletMsg, type = FeedbackType.Info))
                }
            },
        )

        NotePayStatusBanner(
            visible = state.isSelectedWalletReadOnly,
            message = stringResource(R.string.shared_wallet_viewer_cannot_write),
            type = FeedbackType.Warning,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.note.isNotBlank()) {
            NotePreviewChip(
                note = state.note,
                onClick = { onEvent(TransactionFormUiEvent.SetNoteSheetVisible(true)) },
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        TransactionCustomKeypad(
            dateLabel = dateLabel,
            noteLabel = state.note.take(14).ifBlank { "" },
            canSave = state.canSave,
            isSaving = state.isSaving,
            activeOperator = state.activePendingOperator,
            isAmountLocked = state.isAutoCapture,
            modifier = Modifier.fillMaxWidth(),
            onKey = { key ->
                when (key) {
                    CalcKey.Date -> onEvent(TransactionFormUiEvent.SetDatePickerVisible(true))
                    CalcKey.Note -> onEvent(TransactionFormUiEvent.SetNoteSheetVisible(true))
                    CalcKey.Save -> onEvent(TransactionFormUiEvent.Save)
                    else -> onEvent(TransactionFormUiEvent.CalcKeyPressed(key))
                }
            },
            onBackspaceLong = { onEvent(TransactionFormUiEvent.BackspaceLong) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionFormDialogs(
    state: TransactionFormUiState,
    onEvent: (TransactionFormUiEvent) -> Unit,
) {
    val cancelLabel = stringResource(R.string.action_cancel)
    val doneLabel = stringResource(R.string.action_done)

    if (state.isDatePickerOpen) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.occurredAt.toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { onEvent(TransactionFormUiEvent.SetDatePickerVisible(false)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onEvent(TransactionFormUiEvent.DateChanged(Instant.fromEpochMilliseconds(millis)))
                        }
                        onEvent(TransactionFormUiEvent.SetDatePickerVisible(false))
                    },
                ) { Text(doneLabel) }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(TransactionFormUiEvent.SetDatePickerVisible(false)) }) {
                    Text(cancelLabel)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (state.isWalletPickerOpen) {
        WalletPickerSheet(
            wallets = state.availableWallets,
            selectedWalletId = state.walletId,
            onWalletSelected = { id ->
                onEvent(TransactionFormUiEvent.WalletChanged(id))
                onEvent(TransactionFormUiEvent.SetWalletPickerVisible(false))
            },
            onDismiss = { onEvent(TransactionFormUiEvent.SetWalletPickerVisible(false)) },
        )
    }

    if (state.isCategoryPickerOpen) {
        CategoryPickerSheet(
            categories = state.availableCategories,
            selectedCategory = state.category,
            isIncome = state.type == TransactionType.INCOME,
            onCategoryChanged = {
                onEvent(TransactionFormUiEvent.CategorySelected(it))
                onEvent(TransactionFormUiEvent.SetCategoryPickerVisible(false))
            },
            onDismiss = { onEvent(TransactionFormUiEvent.SetCategoryPickerVisible(false)) },
            onCreateCategory = { name, color, iconId, isIncome ->
                onEvent(TransactionFormUiEvent.CreateCategory(name, color, iconId, isIncome))
            },
        )
    }

    if (state.isNoteSheetOpen) {
        NoteQuickEntrySheet(
            currentNote = state.note,
            onNoteChanged = { onEvent(TransactionFormUiEvent.NoteChanged(it)) },
            onDismiss = { onEvent(TransactionFormUiEvent.SetNoteSheetVisible(false)) },
        )
    }
}
