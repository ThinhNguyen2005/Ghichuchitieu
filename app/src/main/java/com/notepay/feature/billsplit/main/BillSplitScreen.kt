package com.notepay.ui.feature.billsplit

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.money.Money
import com.notepay.ui.component.ConfirmDeleteDialog
import com.notepay.ui.component.EmptyStateWithAction
import com.notepay.ui.component.GradientTopAppBar
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.formatter.PresentationDateFormatter
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.VietQrGenerator

/** Khoảng trống cho FAB để item cuối của danh sách không bị nút che. */
private val FabClearance = 96.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitScreen(
    onDebtorClick: (String) -> Unit,
    onFeedback: suspend (UiFeedback) -> Boolean,
    initialShowCreate: Boolean = false,
    onClearShowCreate: () -> Unit = {},
    viewModel: BillSplitViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel, onFeedback) {
        viewModel.feedback.collect { feedback ->
            onFeedback(feedback)
        }
    }

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showPaidHistory by rememberSaveable { mutableStateOf(false) }
    var qrConfigWalletId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteBill by remember { mutableStateOf<BillSplitItemState?>(null) }

    BackHandler(enabled = showPaidHistory) {
        showPaidHistory = false
    }

    LaunchedEffect(initialShowCreate) {
        if (initialShowCreate) {
            showCreateDialog = true
            onClearShowCreate()
        }
    }

    val qrConfigWallet = qrConfigWalletId?.let { id ->
        state.wallets.find { it.id == id }
    }

    val title = stringResource(R.string.bill_split_title)
    val emptyAll = stringResource(R.string.bill_split_empty)
    val emptyUnpaid = stringResource(R.string.bill_split_empty_unpaid)
    val emptyPaid = stringResource(R.string.bill_split_empty_paid)
    val confirmDeleteTitle = stringResource(R.string.confirm_delete_bill_title)
    val confirmDeleteMessageFormat = stringResource(R.string.dialog_delete_confirm_format)
    val linkedBankLabel = stringResource(R.string.bank_linked)

    /*
     * Người nhận tiền của mã QR chính là ví đang dùng. Trước đây không có chỗ nào nói ra
     * điều đó, nên đổi ví là âm thầm đổi người nhận tiền trong tin nhắn chia sẻ.
     */
    val receiveAccountLabel = state.activeWallet?.let { wallet ->
        val bankShortName = state.banks.find { it.bin == wallet.bankBin }?.shortName
        val maskedAccount = wallet.accountNumber
            ?.takeLast(4)
            ?.let { last4 -> "\u2022\u2022$last4" }

        listOfNotNull(bankShortName, maskedAccount)
            .joinToString(" ")
            .ifBlank { wallet.name }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GradientTopAppBar(
                title = {
                    Text(if (showPaidHistory) stringResource(R.string.billsplit_tab_paid) else title)
                },
                navigationIcon = {
                    if (showPaidHistory) {
                        IconButton(
                            onClick = { showPaidHistory = false },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.billsplit_cd_back),
                            )
                        }
                    }
                },
                actions = {
                    // Nút cấu hình VietQR đã chuyển xuống dòng "Nhận về" trong thẻ
                    // tổng quan: logo ở đây trông như nhãn trang trí chứ không phải nút.
                    if (!showPaidHistory) {
                        IconButton(
                            onClick = { showPaidHistory = true },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = stringResource(R.string.billsplit_cd_history),
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!showPaidHistory) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.bill_split_create_btn)
                    )
                }
            }
        },
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val bottomPadding = padding.calculateBottomPadding() + FabClearance
        val horizontalPadding = Modifier.padding(
            start = padding.calculateStartPadding(layoutDirection),
            top = padding.calculateTopPadding(),
            end = padding.calculateEndPadding(layoutDirection),
        )

        if (showPaidHistory) {
            PaidHistoryContent(
                paidSplits = state.paidSplits,
                emptyTitle = emptyPaid,
                bottomPadding = bottomPadding,
                onDebtorClick = onDebtorClick,
                onDelete = { pendingDeleteBill = it },
                modifier = Modifier
                    .fillMaxSize()
                    .then(horizontalPadding),
            )
        } else {
            val unpaidGroups = remember(state.unpaidSplits) {
                state.unpaidSplits.groupBy { it.split.debtorName }.entries.toList()
            }
            val emptyTitle = if (state.paidSplits.isEmpty()) emptyAll else emptyUnpaid

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(horizontalPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = bottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    BillSplitOverview(
                        totalAmountCents = state.unpaidSplits.sumOf {
                            it.split.amount.amountInCents
                        },
                        debtorCount = state.unpaidSplits
                            .map { it.split.debtorName }
                            .distinct()
                            .size,
                        splitCount = state.unpaidSplits.size,
                        receiveAccountLabel = receiveAccountLabel,
                        onChangeReceiveAccount = state.activeWallet
                            ?.let { wallet -> { qrConfigWalletId = wallet.id } },
                    )
                }

                if (unpaidGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 240.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyStateWithAction(
                                title = emptyTitle,
                                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                } else {
                    items(unpaidGroups, key = { it.key }) { (debtorName, splits) ->
                        val totalAmountCents = splits.sumOf { it.split.amount.amountInCents }
                        val amountStr = MoneyFormatter.format(Money(totalAmountCents))
                        val qrWallet = state.activeWallet
                        val qrBankBin = qrWallet?.bankBin
                        val qrAccountNumber = qrWallet?.accountNumber
                        val qrMemoCode = "NP " + debtorName
                            .filter { it.isLetterOrDigit() || it.isWhitespace() }
                            .trim()
                            .uppercase()
                        // Kiểm tra null trực tiếp thay vì `!!`: ví có thể bị xoá cấu hình QR
                        // bất kỳ lúc nào, khi đó chỉ là không tạo được mã.
                        val qrUrl = if (qrBankBin != null && qrAccountNumber != null) {
                            VietQrGenerator.generateImageUrl(
                                bankBin = qrBankBin,
                                accountNumber = qrAccountNumber,
                                amountCents = totalAmountCents,
                                memo = qrMemoCode,
                                accountName = qrWallet?.accountName,
                            )
                        } else {
                            null
                        }
                        val shareWithQrMessage = if (qrUrl != null) {
                            stringResource(
                                R.string.billsplit_share_with_qr_format,
                                debtorName,
                                amountStr,
                                state.banks.find { it.bin == qrBankBin }?.shortName
                                    ?: linkedBankLabel,
                                qrAccountNumber.orEmpty(),
                                qrWallet?.accountName.orEmpty(),
                                qrMemoCode,
                                qrUrl,
                            )
                        } else {
                            null
                        }
                        val shareWithoutQrMessage = stringResource(
                            R.string.billsplit_share_without_qr_format,
                            debtorName,
                            amountStr,
                        )
                        val sharePaymentRequestTitle = stringResource(R.string.billsplit_share_payment_request)
                        val shareQuickTitle = stringResource(R.string.billsplit_share_quick)
                        DebtorGroupRow(
                            debtorName = debtorName,
                            totalAmountCents = totalAmountCents,
                            splitCount = splits.size,
                            onClick = { onDebtorClick(debtorName) },
                            onShare = {
                                val shareMessage = shareWithQrMessage ?: shareWithoutQrMessage
                                val chooserTitle = if (shareWithQrMessage != null) {
                                    sharePaymentRequestTitle
                                } else {
                                    shareQuickTitle
                                }
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(intent, chooserTitle))
                            }
                        )
                    }
                }
            }
        }
    }

    qrConfigWallet?.let { wallet ->
        VietQrConfigFullScreenDialog(
            wallet = wallet,
            banks = state.banks,
            onDismiss = { qrConfigWalletId = null },
            onSave = { bin, accountNumber, accountName ->
                viewModel.updateWalletForQr(
                    wallet.id,
                    bin,
                    accountNumber,
                    accountName,
                )
                // Không Toast: ViewModel đã phát feedback_vietqr_saved qua UiFeedback.
                qrConfigWalletId = null
            },
        )
    }

    if (showCreateDialog) {
        val allDebtorNames = remember(state.unpaidSplits, state.paidSplits) {
            (state.unpaidSplits.map { it.split.debtorName } +
                    state.paidSplits.map { it.split.debtorName })
                .distinct()
                .filter { it.isNotBlank() }
                .sorted()
        }
        BillSplitCreateSheet(
            recentTransactions = state.recentTransactions,
            allDebtorNames = allDebtorNames,
            onConfirm = { parentTransactionId, entries ->
                viewModel.createBillSplits(parentTransactionId, entries)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    pendingDeleteBill?.let { item ->
        ConfirmDeleteDialog(
            title = confirmDeleteTitle,
            itemName = "${item.split.debtorName} • ${MoneyFormatter.format(item.split.amount)}",
            message = confirmDeleteMessageFormat.format(item.split.debtorName),
            onConfirm = {
                viewModel.deleteBillSplit(item.split.id)
                pendingDeleteBill = null
            },
            onDismiss = { pendingDeleteBill = null },
        )
    }
}

@Composable
private fun PaidHistoryContent(
    paidSplits: List<BillSplitItemState>,
    emptyTitle: String,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onDebtorClick: (String) -> Unit,
    onDelete: (BillSplitItemState) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (paidSplits.isEmpty()) {
        Box(
            modifier = modifier.padding(bottom = bottomPadding),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStateWithAction(
                title = emptyTitle,
                icon = Icons.Rounded.CheckCircle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = pluralStringResource(
                    R.plurals.billsplit_split_completed_count,
                    paidSplits.size,
                    paidSplits.size,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        items(paidSplits, key = { it.split.id }) { item ->
            PaidBillSplitRow(
                itemState = item,
                onClick = { onDebtorClick(item.split.debtorName) },
                onDelete = { onDelete(item) },
            )
        }
    }
}

@Composable
private fun BillSplitOverview(
    totalAmountCents: Long,
    debtorCount: Int,
    splitCount: Int,
    receiveAccountLabel: String?,
    onChangeReceiveAccount: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = AppTheme.shapes.circle,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.CallReceived,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.billsplit_total_to_receive),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = MoneyFormatter.format(Money(totalAmountCents)),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(
                        R.string.billsplit_reconcile_summary_format,
                        pluralStringResource(
                            R.plurals.billsplit_debtor_count,
                            debtorCount,
                            debtorCount,
                        ),
                        pluralStringResource(
                            R.plurals.billsplit_split_to_reconcile_count,
                            splitCount,
                            splitCount,
                        ),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (receiveAccountLabel != null) {
                            stringResource(R.string.billsplit_receive_to_format, receiveAccountLabel)
                        } else {
                            stringResource(R.string.billsplit_receive_unlinked)
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (onChangeReceiveAccount != null) {
                        TextButton(
                            onClick = onChangeReceiveAccount,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.billsplit_change_receive_account),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtorGroupRow(
    debtorName: String,
    totalAmountCents: Long,
    splitCount: Int,
    onClick: () -> Unit,
    onShare: () -> Unit,
) {
    val amount = MoneyFormatter.format(Money(totalAmountCents))
    val totalCountText = pluralStringResource(
        R.plurals.bill_split_total_count,
        splitCount,
        splitCount,
    )
    val initial = debtorName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            shape = AppTheme.shapes.corner16,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = AppTheme.shapes.circle,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debtorName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = totalCountText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(AppTheme.shapes.corner16)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f))
                .clickable(onClick = onShare),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = stringResource(R.string.billsplit_share_quick),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun PaidBillSplitRow(
    itemState: BillSplitItemState,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val split = itemState.split
    val note = itemState.parentTransaction?.note ?: stringResource(R.string.billsplit_default_note)
    val paidColor = MaterialTheme.colorScheme.tertiary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = AppTheme.shapes.corner16,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = AppTheme.shapes.circle,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.billsplit_paid_by_format, split.debtorName),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.billsplit_paid_note_format,
                        note,
                        PresentationDateFormatter.formatDayMonth(split.paidAt),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = MoneyFormatter.format(split.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = paidColor,
                    maxLines = 1,
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.billsplit_delete_paid_format, split.debtorName),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
