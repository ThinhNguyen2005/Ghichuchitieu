package com.notepay.feature.debt.detail

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.component.bar.GradientTopAppBar
import com.notepay.core.designsystem.component.dialog.ConfirmDeleteDialog
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.domain.model.DebtPayment
import com.notepay.domain.model.DebtType
import com.notepay.feature.debt.detail.component.DebtDetailActionCard
import com.notepay.feature.debt.detail.component.DebtDetailOverviewCard
import com.notepay.feature.debt.detail.component.DebtPaymentHistoryItem
import com.notepay.feature.debt.management.sheet.DebtRemindBottomSheet
import com.notepay.feature.debt.management.sheet.RecordPaymentDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    // Pane phải truyền id đang chọn; route độc lập đã có id trong nav args nên để null.
    debtId: Long? = null,
    paneMode: Boolean = false,
    viewModel: DebtDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showQrSheet by remember { mutableStateOf(false) }
    var showDeleteDebtDialog by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<DebtPayment?>(null) }

    val qrSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(debtId) {
        if (debtId != null) viewModel.select(debtId)
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is DebtDetailUiEvent.DebtDeleted -> {
                    Toast.makeText(context, resources.getString(R.string.debt_delete_success), Toast.LENGTH_SHORT).show()
                    onBack()
                }
                is DebtDetailUiEvent.PaymentRecorded -> {
                    Toast.makeText(context, resources.getString(R.string.debt_payment_success), Toast.LENGTH_SHORT).show()
                }
                is DebtDetailUiEvent.PaymentDeleted -> {
                    Toast.makeText(context, resources.getString(R.string.debt_delete_success), Toast.LENGTH_SHORT).show()
                }
                is DebtDetailUiEvent.ShowToast -> {
                    Toast.makeText(context, resources.getString(event.messageResId), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Không dùng Scaffold: màn này chỉ gồm top bar + nội dung, và ở chế độ hai pane nó được
    // lồng vào pane phải nên Scaffold nội bộ sẽ tạo thêm một lớp inset lồng nhau.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (paneMode) Color.Transparent else MaterialTheme.colorScheme.background),
    ) {
        GradientTopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.debt_detail_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            },
            actions = {
                if (uiState.debtWithHistory != null) {
                    IconButton(
                        onClick = { showDeleteDebtDialog = true },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            // Pane phải đã nằm dưới top bar của màn danh sách nên không nhận lại inset status bar.
            windowInsets = if (paneMode) WindowInsets(0, 0, 0, 0) else TopAppBarDefaults.windowInsets,
            // Dải gradient phải trùng màu nền thật của thanh, nếu không sẽ lộ một vệt sáng ở pane.
            topBarColor = if (paneMode) AppTheme.colors.surfaceSunken else MaterialTheme.colorScheme.background,
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val item = uiState.debtWithHistory
            if (item == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.debt_empty_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val debt = item.debt
                val isLend = debt.type == DebtType.LEND
                val accentColor = if (isLend) AppTheme.colors.success else AppTheme.colors.error
                val animatedProgress by animateFloatAsState(
                    targetValue = item.progressRatio.coerceIn(0f, 1f),
                    label = "debt_progress"
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Header Overview Card
                    item {
                        DebtDetailOverviewCard(
                            item = item,
                            animatedProgress = animatedProgress,
                            accentColor = accentColor,
                        )
                    }

                    // Action Buttons Card
                    item {
                        DebtDetailActionCard(
                            isSettled = debt.isSettled,
                            isLend = isLend,
                            onRecordPayment = { showPaymentDialog = true },
                            onToggleSettled = { viewModel.toggleSettled() },
                            onShowQrRemind = { showQrSheet = true },
                        )
                    }

                    // Payment History Section
                    item {
                        Text(
                            text = stringResource(R.string.debt_payment_history_title, item.payments.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }

                    if (item.payments.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppTheme.shapes.corner16,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(R.string.debt_payment_empty_history),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = item.payments.sortedByDescending { it.paidAt },
                            key = { it.id }
                        ) { payment ->
                            DebtPaymentHistoryItem(
                                payment = payment,
                                accentColor = accentColor,
                                onDeleteClick = { paymentToDelete = payment },
                            )
                        }
                    }
                }
            }
        }
    }

    // Record Payment Dialog
    val debtWithHistory = uiState.debtWithHistory
    if (showPaymentDialog && debtWithHistory != null) {
        RecordPaymentDialog(
            debtWithHistory = debtWithHistory,
            wallets = uiState.wallets,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, walletId, note, syncWithWallet ->
                viewModel.recordPayment(
                    debtWithHistory.debt,
                    amount,
                    walletId,
                    note,
                    syncWithWallet
                )
                showPaymentDialog = false
            }
        )
    }

    // VietQR Remind Bottom Sheet
    if (showQrSheet && debtWithHistory != null) {
        DebtRemindBottomSheet(
            debtWithHistory = debtWithHistory,
            wallets = uiState.wallets,
            banks = uiState.banks,
            sheetState = qrSheetState,
            onDismiss = { showQrSheet = false },
        )
    }

    // Delete Debt Confirmation Dialog
    if (showDeleteDebtDialog) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.debt_confirm_delete_title),
            itemName = debtWithHistory?.debt?.personName.orEmpty(),
            message = stringResource(R.string.debt_confirm_delete_desc),
            onConfirm = { viewModel.deleteDebt() },
            onDismiss = { showDeleteDebtDialog = false },
        )
    }

    // Delete Payment Confirmation Dialog
    paymentToDelete?.let { payment ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.debt_delete_payment_title),
            itemName = "",
            message = stringResource(R.string.debt_delete_payment_desc),
            onConfirm = { viewModel.deletePayment(payment.id) },
            onDismiss = { paymentToDelete = null },
        )
    }
}
