package com.notepay.ui.feature.debt

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.model.debt.DebtPayment
import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.model.debt.DebtWithHistory
import com.notepay.ui.component.GradientTopAppBar
import com.notepay.ui.formatter.PresentationDateFormatter
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebtDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showQrSheet by remember { mutableStateOf(false) }
    var showDeleteDebtDialog by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<DebtPayment?>(null) }

    val qrSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is DebtDetailUiEvent.DebtDeleted -> {
                    Toast.makeText(context, context.getString(R.string.debt_delete_success), Toast.LENGTH_SHORT).show()
                    onBack()
                }
                is DebtDetailUiEvent.PaymentRecorded -> {
                    Toast.makeText(context, context.getString(R.string.debt_payment_success), Toast.LENGTH_SHORT).show()
                }
                is DebtDetailUiEvent.PaymentDeleted -> {
                    Toast.makeText(context, context.getString(R.string.debt_delete_success), Toast.LENGTH_SHORT).show()
                }
                is DebtDetailUiEvent.ShowToast -> {
                    Toast.makeText(context, context.getString(event.messageResId), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
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
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val item = uiState.debtWithHistory
            if (item == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
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
                val accentColor = if (isLend) Color(0xFF34C759) else Color(0xFFFF9500)
                val animatedProgress by animateFloatAsState(
                    targetValue = item.progressRatio.coerceIn(0f, 1f),
                    label = "debt_progress"
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Header Overview Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppTheme.shapes.corner24,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                // Top row: Avatar + Name + Badges
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    val initial = debt.personName.trim().take(1).uppercase()
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = initial,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 22.sp,
                                            color = accentColor,
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = debt.personName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(
                                                if (isLend) R.string.debt_type_lend_label else R.string.debt_type_borrow_label
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accentColor,
                                        )
                                    }
                                }

                                // Quick Phone Actions if present
                                if (!debt.phoneNumber.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(AppTheme.shapes.corner16)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = debt.phoneNumber,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                        )
                                        IconButton(
                                            onClick = {
                                                val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${debt.phoneNumber}")
                                                }
                                                context.startActivity(callIntent)
                                            },
                                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Call,
                                                contentDescription = stringResource(R.string.debt_action_call),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("smsto:${debt.phoneNumber}")
                                                }
                                                context.startActivity(smsIntent)
                                            },
                                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Message,
                                                contentDescription = stringResource(R.string.debt_action_sms),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // Remaining amount highlight
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = stringResource(R.string.debt_card_remaining, ""),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = MoneyFormatter.format(item.remainingAmount),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (item.isFullyPaid) Color(0xFF34C759) else accentColor,
                                    )
                                }

                                // Progress bar
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = accentColor,
                                        trackColor = accentColor.copy(alpha = 0.15f),
                                        strokeCap = StrokeCap.Round,
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.debt_card_paid,
                                                MoneyFormatter.format(item.totalPaid)
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.debt_card_total,
                                                MoneyFormatter.format(debt.originalAmount)
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }

                                // Status Badge & Due Date Info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    val (badgeText, badgeBg, badgeTextColor) = when {
                                        item.isFullyPaid -> Triple(
                                            stringResource(R.string.debt_status_settled),
                                            Color(0xFF007AFF).copy(alpha = 0.12f),
                                            Color(0xFF007AFF),
                                        )
                                        item.isOverdue() -> Triple(
                                            stringResource(R.string.debt_status_overdue),
                                            MaterialTheme.colorScheme.errorContainer,
                                            MaterialTheme.colorScheme.error,
                                        )
                                        item.isDueToday() -> Triple(
                                            stringResource(R.string.debt_status_due_today),
                                            Color(0xFFFF9500).copy(alpha = 0.15f),
                                            Color(0xFFFF9500),
                                        )
                                        debt.dueDate != null -> Triple(
                                            stringResource(
                                                R.string.debt_status_due_date,
                                                PresentationDateFormatter.formatDate(debt.dueDate)
                                            ),
                                            MaterialTheme.colorScheme.surfaceContainerHighest,
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        else -> Triple("", Color.Transparent, Color.Transparent)
                                    }

                                    if (badgeText.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(AppTheme.shapes.corner8)
                                                .background(badgeBg)
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                        ) {
                                            Text(
                                                text = badgeText,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeTextColor,
                                            )
                                        }
                                    }

                                    // Created date
                                    Text(
                                        text = PresentationDateFormatter.formatDate(debt.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                // Note if present
                                if (debt.note.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(AppTheme.shapes.corner12)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f))
                                            .padding(12.dp),
                                    ) {
                                        Text(
                                            text = debt.note,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppTheme.shapes.corner20,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // Record partial repayment
                                Button(
                                    onClick = { showPaymentDialog = true },
                                    shape = AppTheme.shapes.corner16,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor,
                                        contentColor = Color.White,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Payments,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.debt_action_record_payment),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }

                                // Mark settled toggle
                                FilledTonalButton(
                                    onClick = { viewModel.toggleSettled() },
                                    shape = AppTheme.shapes.corner16,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp),
                                ) {
                                    Icon(
                                        imageVector = if (item.debt.isSettled) Icons.Rounded.RestartAlt else Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(
                                            if (item.debt.isSettled) R.string.debt_action_mark_unsettled else R.string.debt_action_mark_settled
                                        ),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }

                                // VietQR reminder button (only for Lend / Receivable debts)
                                if (isLend) {
                                    OutlinedButton(
                                        onClick = { showQrSheet = true },
                                        shape = AppTheme.shapes.corner16,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.QrCode2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.debt_action_remind_vietqr),
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                }
                            }
                        }
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
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppTheme.shapes.corner16,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Payments,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = MoneyFormatter.format(payment.amount),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val dateStr = PresentationDateFormatter.formatDate(payment.paidAt)
                                        val noteText = if (payment.note.isNotBlank()) " • ${payment.note}" else ""
                                        Text(
                                            text = "$dateStr$noteText",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    IconButton(
                                        onClick = { paymentToDelete = payment },
                                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = stringResource(R.string.action_delete),
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Record Payment Dialog
    if (showPaymentDialog && uiState.debtWithHistory != null) {
        val debt = uiState.debtWithHistory!!.debt
        RecordPaymentDialog(
            debtWithHistory = uiState.debtWithHistory!!,
            wallets = uiState.wallets,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, walletId, note, syncWithWallet ->
                viewModel.recordPayment(debt, amount, walletId, note, syncWithWallet)
                showPaymentDialog = false
            }
        )
    }

    // VietQR Remind Bottom Sheet
    if (showQrSheet && uiState.debtWithHistory != null) {
        DebtRemindBottomSheet(
            debtWithHistory = uiState.debtWithHistory!!,
            wallets = uiState.wallets,
            banks = uiState.banks,
            sheetState = qrSheetState,
            onDismiss = { showQrSheet = false },
        )
    }

    // Delete Debt Confirmation Dialog
    if (showDeleteDebtDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDebtDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.debt_confirm_delete_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(text = stringResource(R.string.debt_confirm_delete_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDebt()
                        showDeleteDebtDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDebtDialog = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Delete Payment Confirmation Dialog
    paymentToDelete?.let { payment ->
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = {
                Text(
                    text = stringResource(R.string.debt_delete_payment_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(text = stringResource(R.string.debt_delete_payment_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePayment(payment.id)
                        paymentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { paymentToDelete = null },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
