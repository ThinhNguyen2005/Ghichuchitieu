package com.notepay.feature.assets.export

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.component.effect.NotePayModalBottomSheet
import com.notepay.core.designsystem.feedback.FeedbackDuration
import com.notepay.core.designsystem.feedback.FeedbackSnackbarOverlay
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.NotePayStatusBanner
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.feedback.showFeedback
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.domain.export.ExportDatePreset
import com.notepay.domain.export.ExportFormat
import com.notepay.domain.export.ExportTypeFilter
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportBottomSheet(
    onDismiss: () -> Unit,
    onFeedback: suspend (UiFeedback) -> Boolean,
    viewModel: ExportReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetSnackbarHostState = remember { SnackbarHostState() }
    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }

    val successMessage = stringResource(R.string.export_success_with_share)
    val exportShareLabel = stringResource(R.string.export_share_action)
    val failedMessage = stringResource(R.string.export_failed)
    val shareTitle = stringResource(R.string.export_share_chooser_title)

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(state.mimeType()),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = viewModel.writeToUri(uri)
                if (ok) {
                    sheetState.hide()
                    if (!sheetState.isVisible) onDismiss()
                    onFeedback(
                        UiFeedback(
                            message = successMessage,
                            type = FeedbackType.Success,
                            actionLabel = exportShareLabel,
                            duration = FeedbackDuration.Long,
                            onAction = {
                                runCatching {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = state.mimeType()
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, shareTitle))
                                }
                            },
                        ),
                    )
                } else {
                    sheetSnackbarHostState.showFeedback(
                        UiFeedback(message = failedMessage, type = FeedbackType.Error),
                    )
                }
            }
        }
    }

    NotePayModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            Text(
                text = stringResource(R.string.export_report_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // 1. Khoảng thời gian
            ExportSectionLabel(stringResource(R.string.export_report_period_label))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExportDatePreset.entries.forEach { preset ->
                    val labelRes = when (preset) {
                        ExportDatePreset.THIS_MONTH -> R.string.export_preset_month
                        ExportDatePreset.THIS_QUARTER -> R.string.export_preset_quarter
                        ExportDatePreset.THIS_YEAR -> R.string.export_preset_year
                        ExportDatePreset.CUSTOM -> R.string.export_preset_custom
                    }
                    SandFilterChip(
                        selected = state.datePreset == preset,
                        label = stringResource(labelRes),
                        onClick = { viewModel.selectPreset(preset) },
                    )
                }
            }

            if (state.datePreset == ExportDatePreset.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { pickingStartDate = true },
                        shape = AppTheme.shapes.input,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text(
                            text = "${stringResource(R.string.export_date_from)}: ${formatShortDate(state.startDate)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = { pickingEndDate = true },
                        shape = AppTheme.shapes.input,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text(
                            text = "${stringResource(R.string.export_date_to)}: ${formatShortDate(state.endDate)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // 2. Chọn ví
            ExportSectionLabel(stringResource(R.string.export_wallets_label))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SandFilterChip(
                    selected = state.selectedWalletIds.isEmpty(),
                    label = stringResource(R.string.export_wallets_all),
                    onClick = { viewModel.toggleWallet(null) },
                )
                state.availableWallets.forEach { wallet ->
                    SandFilterChip(
                        selected = wallet.id in state.selectedWalletIds,
                        label = wallet.name,
                        onClick = { viewModel.toggleWallet(wallet.id) },
                    )
                }
            }

            // 3. Loại giao dịch
            ExportSectionLabel(stringResource(R.string.export_type_label))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExportTypeFilter.entries.forEach { filter ->
                    val labelRes = when (filter) {
                        ExportTypeFilter.ALL -> R.string.export_type_all
                        ExportTypeFilter.INCOME -> R.string.transaction_type_income
                        ExportTypeFilter.EXPENSE -> R.string.transaction_type_expense
                    }
                    SandFilterChip(
                        selected = state.typeFilter == filter,
                        label = stringResource(labelRes),
                        onClick = { viewModel.selectTypeFilter(filter) },
                    )
                }
            }

            // 4. Định dạng
            ExportSectionLabel(stringResource(R.string.export_format_label))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExportFormat.entries.forEach { fmt ->
                    val labelRes = if (fmt == ExportFormat.XLSX) {
                        R.string.export_format_xlsx
                    } else {
                        R.string.export_format_csv
                    }
                    SandFilterChip(
                        selected = state.format == fmt,
                        label = stringResource(labelRes),
                        onClick = { viewModel.selectFormat(fmt) },
                    )
                }
            }

            Text(
                text = pluralStringResource(R.plurals.export_matching_count, state.matchingCount, state.matchingCount),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            NotePayStatusBanner(
                visible = state.isExporting,
                message = stringResource(R.string.export_pending_banner),
                type = FeedbackType.Pending,
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val shareUri = viewModel.createShareUri()
                            if (shareUri != null) {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = state.mimeType()
                                    putExtra(Intent.EXTRA_STREAM, shareUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, shareTitle))
                            } else {
                                onFeedback(UiFeedback(failedMessage, type = FeedbackType.Error))
                            }
                        }
                    },
                    enabled = !state.isExporting,
                    shape = AppTheme.shapes.capsule,
                    border = BorderStroke(1.dp, AppTheme.colors.borderStrong),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.export_share_action),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Button(
                    onClick = { saveDocumentLauncher.launch(state.suggestedFileName()) },
                    enabled = !state.isExporting,
                    shape = AppTheme.shapes.capsule,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.ink,
                        contentColor = AppTheme.colors.onInk,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    if (state.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AppTheme.colors.onInk,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.export_save_to_device),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        FeedbackSnackbarOverlay(hostState = sheetSnackbarHostState)
        }
    }

    if (pickingStartDate || pickingEndDate) {
        val initial = if (pickingStartDate) state.startDate else state.endDate
        val initialMillis = initial.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = {
                pickingStartDate = false
                pickingEndDate = false
            },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                        if (pickingStartDate) {
                            viewModel.setCustomStartDate(picked)
                        } else {
                            viewModel.setCustomEndDate(picked)
                        }
                    }
                    pickingStartDate = false
                    pickingEndDate = false
                }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pickingStartDate = false
                    pickingEndDate = false
                }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun ExportSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = AppTheme.colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SandFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
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

private fun formatShortDate(date: LocalDate): String =
    String.format(Locale.ROOT, "%02d/%02d/%04d", date.day, date.month.number, date.year)
