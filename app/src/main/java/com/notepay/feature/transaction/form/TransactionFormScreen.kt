package com.notepay.feature.transaction.form

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.component.effect.AppAtmosphericBackground
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.formatter.TransactionDateLabelFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.HomeHeroCardDarkBottom
import com.notepay.core.designsystem.theme.HomeHeroCardDarkRim
import com.notepay.core.designsystem.theme.HomeHeroCardDarkTop
import com.notepay.core.designsystem.theme.HomeHeroCardLightBottom
import com.notepay.core.designsystem.theme.HomeHeroCardLightRim
import com.notepay.core.designsystem.theme.HomeHeroCardLightTop
import com.notepay.feature.transaction.component.CalcKey
import com.notepay.feature.transaction.component.CategoryQuickSelectionRow
import com.notepay.feature.transaction.component.NotePreviewChip
import com.notepay.feature.transaction.component.TransactionAmountDisplay
import com.notepay.feature.transaction.component.TransactionCustomKeypad
import com.notepay.feature.transaction.component.TransactionSuggestionChipRow
import com.notepay.feature.transaction.component.TransactionTypeSelector
import com.notepay.feature.transaction.component.TransactionWalletField
import com.notepay.feature.transaction.component.sheet.CategoryPickerSheet
import com.notepay.feature.transaction.component.sheet.NoteQuickEntrySheet
import com.notepay.feature.transaction.component.sheet.WalletPickerSheet
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: TransactionFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // OCR image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.onEvent(TransactionFormUiEvent.ImagePicked(it)) } }

    // Strings
    val backCd = stringResource(R.string.action_back)
    val fieldWallet = stringResource(R.string.transaction_field_wallet)
    val noWalletSelected = stringResource(R.string.transaction_no_wallet_selected)
    val noOtherWalletToast = stringResource(R.string.transaction_no_other_wallet)
    val cancelLabel = stringResource(R.string.action_cancel)
    val doneLabel = stringResource(R.string.action_done)
    val expenseLabel = stringResource(R.string.transaction_type_expense)
    val incomeLabel = stringResource(R.string.transaction_type_income)
    val todayPrefix = stringResource(R.string.date_today_prefix)
    val yesterdayPrefix = stringResource(R.string.date_yesterday_prefix)
    val monthSuffixFmt = stringResource(R.string.date_month_suffix)

    // Feedback collector: Success → popBackStack, Error → snackbar
    LaunchedEffect(Unit) {
        viewModel.feedback.collect { feedback ->
            if (feedback.type == FeedbackType.Success) {
                onSaved()
            } else {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(feedback.message)
            }
        }
    }

    // Loading state: chờ load dữ liệu Edit mode
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
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

    // ── AppBar title: Add vs Edit / AutoCapture ───────────────────────────────
    val titleRes = when {
        !state.isEditMode -> R.string.add_transaction_title
        state.isAutoCapture -> R.string.transaction_details_title
        else -> R.string.edit_transaction_title
    }

    // Tái sử dụng Scaffold từ NotePayNavHost — KHÔNG nest Scaffold thêm
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

    Box(modifier = Modifier.fillMaxSize()) {
        AppAtmosphericBackground(
            modifier = Modifier.matchParentSize(),
            forceSoftBlurOnPhoto = true,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Frosted iOS Editorial Header ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    onClick = onBack,
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
                            contentDescription = backCd,
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
                        text = stringResource(titleRes),
                        style = AppTheme.typography.displaySerif,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (state.isAutoCapture) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalance,
                            contentDescription = stringResource(R.string.transaction_auto_capture_cd),
                            tint = AppTheme.colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Nút quét ảnh OCR: chỉ hiển thị ở Add mode
                if (!state.isEditMode) {
                    Surface(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !state.isImageScanning,
                        shape = CircleShape,
                        color = headerPillColor,
                        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
                        modifier = Modifier
                            .size(48.dp)
                            .iosCardShadow(shape = CircleShape, elevated = false),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (state.isImageScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = AppTheme.colors.primary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoLibrary,
                                    contentDescription = stringResource(R.string.scan_image_button),
                                    tint = AppTheme.colors.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }

            // ── Body ─────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Luminous iOS Hero Card cho Loại giao dịch + Số tiền
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
                            onTypeChanged = { viewModel.onEvent(TransactionFormUiEvent.TypeChanged(it)) },
                            expenseLabel = expenseLabel,
                            incomeLabel = incomeLabel,
                        )

                        // Thông báo từ OCR nếu có
                        state.imageScanMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary,
                            )
                        }

                        // Số tiền (read-only display — bàn phím tự chế điều khiển)
                        TransactionAmountDisplay(
                            amountInput = state.displayExpression,
                            rawAmountForMagnitude = state.previewAmountMajorUnits,
                            hasError = state.calcState.error != null,
                        )
                        if (state.isEditMode && state.isAutoCapture) {
                            Text(
                                text = stringResource(R.string.transaction_auto_capture_amount_locked),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            )
                        }
                    }
                }

                // Khối Gợi ý & Chọn nhanh danh mục
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
                                onSelect = { viewModel.onEvent(TransactionFormUiEvent.CategorySelected(it)) },
                            )
                        }

                        CategoryQuickSelectionRow(
                            categories = state.availableCategories,
                            selectedCategory = state.category,
                            isIncome = state.type == com.notepay.domain.model.TransactionType.INCOME,
                            onCategoryChanged = { viewModel.onEvent(TransactionFormUiEvent.CategorySelected(it)) },
                            onSeeAllClick = { viewModel.onEvent(TransactionFormUiEvent.SetCategoryPickerVisible(true)) },
                        )
                    }
                }

                // Chọn ví
                val selectedWallet = state.availableWallets.firstOrNull { it.id == state.walletId }
                TransactionWalletField(
                    label = fieldWallet,
                    value = selectedWallet?.name ?: noWalletSelected,
                    onClick = {
                        if (state.availableWallets.size > 1) {
                            viewModel.onEvent(TransactionFormUiEvent.SetWalletPickerVisible(true))
                        } else {
                            Toast.makeText(context, noOtherWalletToast, Toast.LENGTH_SHORT).show()
                        }
                    },
                )

                // Ghi chú preview (nhập qua phím 📝 trên keypad)
                if (state.note.isNotBlank()) {
                    NotePreviewChip(
                        note = state.note,
                        onClick = { viewModel.onEvent(TransactionFormUiEvent.SetNoteSheetVisible(true)) },
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bàn phím tự chế 4×5
                TransactionCustomKeypad(
                    dateLabel = dateLabel,
                    noteLabel = state.note.take(14).ifBlank { "" },
                    canSave = state.canSave,
                    isSaving = state.isSaving,
                    activeOperator = state.activePendingOperator,
                    isAmountLocked = state.isEditMode && state.isAutoCapture,
                    modifier = Modifier.fillMaxWidth(),
                    onKey = { key ->
                        when (key) {
                            CalcKey.Date -> viewModel.onEvent(TransactionFormUiEvent.SetDatePickerVisible(true))
                            CalcKey.Note -> viewModel.onEvent(TransactionFormUiEvent.SetNoteSheetVisible(true))
                            CalcKey.Save -> viewModel.onEvent(TransactionFormUiEvent.Save)
                            else -> viewModel.onEvent(TransactionFormUiEvent.CalcKeyPressed(key))
                        }
                    },
                    onBackspaceLong = { viewModel.onEvent(TransactionFormUiEvent.BackspaceLong) },
                )
            }
        }

        // Dùng SnackbarHost standalone ở đáy màn hình
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
    }

    // ── Dialogs & Sheets (state-driven) ───────────────────────────────────────

    if (state.isDatePickerOpen) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.occurredAt.toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.onEvent(TransactionFormUiEvent.SetDatePickerVisible(false)) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.onEvent(TransactionFormUiEvent.DateChanged(Instant.fromEpochMilliseconds(millis)))
                    }
                    viewModel.onEvent(TransactionFormUiEvent.SetDatePickerVisible(false))
                }) { Text(doneLabel) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(TransactionFormUiEvent.SetDatePickerVisible(false)) }) { Text(cancelLabel) }
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
                viewModel.onEvent(TransactionFormUiEvent.WalletChanged(id))
                viewModel.onEvent(TransactionFormUiEvent.SetWalletPickerVisible(false))
            },
            onDismiss = { viewModel.onEvent(TransactionFormUiEvent.SetWalletPickerVisible(false)) },
        )
    }

    if (state.isCategoryPickerOpen) {
        CategoryPickerSheet(
            categories = state.availableCategories,
            selectedCategory = state.category,
            isIncome = state.type == com.notepay.domain.model.TransactionType.INCOME,
            onCategoryChanged = {
                viewModel.onEvent(TransactionFormUiEvent.CategorySelected(it))
                viewModel.onEvent(TransactionFormUiEvent.SetCategoryPickerVisible(false))
            },
            onDismiss = { viewModel.onEvent(TransactionFormUiEvent.SetCategoryPickerVisible(false)) },
            onCreateCategory = { name, color, iconId, isIncome ->
                viewModel.onEvent(TransactionFormUiEvent.CreateCategory(name, color, iconId, isIncome))
            },
        )
    }

    if (state.isNoteSheetOpen) {
        NoteQuickEntrySheet(
            currentNote = state.note,
            enableNaturalParsing = !state.isEditMode,
            naturalEntry = state.naturalEntry,
            availableCategories = state.availableCategories,
            onNoteChanged = { viewModel.onEvent(TransactionFormUiEvent.NoteChanged(it)) },
            onNaturalInputChanged = { viewModel.onEvent(TransactionFormUiEvent.NaturalInputChanged(it)) },
            onAnalyzeWithAi = { viewModel.onEvent(TransactionFormUiEvent.AnalyzeNaturalInputWithAi) },
            onUpdateDraft = { viewModel.onEvent(TransactionFormUiEvent.UpdateNaturalDraft(it)) },
            onRemoveDraft = { viewModel.onEvent(TransactionFormUiEvent.RemoveNaturalDraft(it)) },
            onSaveAllDrafts = { viewModel.onEvent(TransactionFormUiEvent.SaveNaturalDrafts) },
            onDismiss = { viewModel.onEvent(TransactionFormUiEvent.SetNoteSheetVisible(false)) },
        )
    }
}

