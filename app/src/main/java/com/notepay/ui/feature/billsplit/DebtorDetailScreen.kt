package com.notepay.ui.feature.billsplit

import com.notepay.ui.theme.AppTheme

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import coil3.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.component.ConfirmDeleteDialog
import com.notepay.ui.component.GradientBottomActionBar
import com.notepay.ui.component.GradientTopAppBar
import com.notepay.ui.component.LiquidButton
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.VietQrGenerator
import java.util.Locale
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DebtorDetailScreen(
    debtorName: String,
    onBack: () -> Unit,
    onFeedback: suspend (UiFeedback) -> Boolean,
    viewModel: BillSplitViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.feedback.collect { feedback ->
            onFeedback(feedback)
        }
    }

    val unpaidDebtorSplits by remember(state.unpaidSplits, debtorName) {
        derivedStateOf {
            state.unpaidSplits.filter { it.split.debtorName.equals(debtorName, ignoreCase = true) }
        }
    }

    val paidDebtorSplits by remember(state.paidSplits, debtorName) {
        derivedStateOf {
            state.paidSplits.filter { it.split.debtorName.equals(debtorName, ignoreCase = true) }
        }
    }

    val allDebtorSplits by remember(unpaidDebtorSplits, paidDebtorSplits) {
        derivedStateOf {
            (unpaidDebtorSplits + paidDebtorSplits).sortedByDescending { it.split.createdAt }
        }
    }

    val totalAmountCents by remember(unpaidDebtorSplits) {
        derivedStateOf {
            unpaidDebtorSplits.sumOf { it.split.amount.amountInCents }
        }
    }

    val activeWallet = state.activeWallet
    val linkedBankName = stringResource(R.string.bank_linked)
    val vietQrConfiguredMessage = stringResource(R.string.feedback_vietqr_configured)
    var qrConfigWalletId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteBill by remember { mutableStateOf<BillSplitItemState?>(null) }
    var showReconciliationSheet by remember { mutableStateOf(false) }

    val qrConfigWallet = qrConfigWalletId?.let { id ->
        state.wallets.find { it.id == id }
    }

    val bankName = remember(activeWallet, state.banks, linkedBankName) {
        state.banks.find { it.bin == activeWallet?.bankBin }?.shortName ?: linkedBankName
    }

    val memoCode = remember(debtorName) {
        val sanitized = debtorName.filter { it.isLetterOrDigit() || it.isWhitespace() }.trim().uppercase()
        "NP $sanitized"
    }

    val qrImageUrl = remember(activeWallet, totalAmountCents, memoCode) {
        if (activeWallet?.bankBin != null && activeWallet.accountNumber != null) {
            VietQrGenerator.generateImageUrl(
                bankBin = activeWallet.bankBin,
                accountNumber = activeWallet.accountNumber,
                amountCents = totalAmountCents,
                memo = memoCode,
                accountName = activeWallet.accountName,
            )
        } else null
    }

    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.debtor_detail_title, debtorName), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        bottomBar = {
            if (unpaidDebtorSplits.isNotEmpty()) {
                GradientBottomActionBar {
                    // surfaceColor vẽ màu đục hoàn toàn, khác tint chỉ phủ alpha 0.32
                    // nên nút hành động chính không bị loãng trên nền kính mờ.
                    LiquidButton(
                        onClick = { showReconciliationSheet = true },
                        surfaceColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Rounded.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            stringResource(R.string.action_confirm_payment),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (allDebtorSplits.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.debtor_no_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack, shape = AppTheme.shapes.corner12) { Text(stringResource(R.string.action_back)) }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DebtSummaryCard(
                    debtorName = debtorName,
                    outstandingAmount = Money(totalAmountCents),
                    unpaidCount = unpaidDebtorSplits.size,
                )
            }

            item {
                if (qrImageUrl != null && activeWallet != null && totalAmountCents > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        VietQrTemplateCard(qrImageUrl = qrImageUrl)

                        TransferDetailsCopyCard(
                            bankName = bankName,
                            accountNumber = activeWallet.accountNumber.orEmpty(),
                            accountName = activeWallet.accountName.orEmpty(),
                            amountStr = MoneyFormatter.format(Money(totalAmountCents)),
                            amountRaw = (totalAmountCents / 100).toString(),
                            memoCode = memoCode
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppTheme.shapes.corner20,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Text(stringResource(R.string.bill_split_vietqr_missing), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = stringResource(R.string.bill_split_vietqr_prompt_format, activeWallet?.name ?: stringResource(R.string.wallet_default)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { qrConfigWalletId = activeWallet?.id },
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppTheme.shapes.corner12,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Rounded.QrCode2, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_configure_vietqr), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = stringResource(R.string.debt_reconciliation_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = stringResource(R.string.debt_reconciliation_summary_format, unpaidDebtorSplits.size, paidDebtorSplits.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(text = stringResource(R.string.debt_hold_to_delete), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items(allDebtorSplits, key = { it.split.id }) { item ->
                val parentTx = item.parentTransaction
                val note = parentTx?.note ?: stringResource(R.string.debt_transaction_fallback)
                val isPaid = item.split.isPaid

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppTheme.shapes.corner16)
                        .combinedClickable(
                            onClick = { /* No-op */ },
                            onLongClick = { pendingDeleteBill = item }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPaid) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = .16f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = .12f)
                        },
                    ),
                    shape = AppTheme.shapes.corner16,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val category = parentTx?.category ?: Category.OTHER
                        CategoryAvatar(category = category)

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = note, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isPaid) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(AppTheme.shapes.corner8)
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.debt_paid), style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(AppTheme.shapes.corner8)
                                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = .55f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.debt_pending_collection), style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            val muonDate = formatInstant(item.split.createdAt)
                            val dateSubtext = if (isPaid) {
                                val tradate = formatInstant(item.split.paidAt)
                                stringResource(R.string.debt_dates_paid_format, muonDate, tradate)
                            } else {
                                stringResource(R.string.debt_dates_pending_format, muonDate)
                            }
                            Text(text = dateSubtext, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            text = MoneyFormatter.format(item.split.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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
            onSave = { bin, accNum, accName ->
                viewModel.updateWalletForQr(wallet.id, bin, accNum, accName)
                qrConfigWalletId = null
                Toast.makeText(context, vietQrConfiguredMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }

    pendingDeleteBill?.let { item ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.confirm_delete_bill_title),
            itemName = "${item.split.debtorName} â€¢ ${MoneyFormatter.format(item.split.amount)}",
            message = stringResource(R.string.debt_delete_message),
            onConfirm = { viewModel.deleteBillSplit(item.split.id) },
            onDismiss = { pendingDeleteBill = null },
        )
    }

    if (showReconciliationSheet) {
        PaymentReconciliationSheet(
            debtorName = debtorName, totalDebt = Money(totalAmountCents),
            recentTransactions = state.recentTransactions, wallets = state.wallets,
            onDismiss = { showReconciliationSheet = false },
            onConfirm = { incomeTxId ->
                showReconciliationSheet = false
                viewModel.markDebtorAsPaidWithReconciliation(
                    debtorName = debtorName, splitIds = unpaidDebtorSplits.map { it.split.id }, incomeTxId = incomeTxId
                )
                onBack()
            }
        )
    }
}

@Composable
private fun DebtSummaryCard(
    debtorName: String,
    outstandingAmount: Money,
    unpaidCount: Int,
    modifier: Modifier = Modifier,
) {
    val isSettled = outstandingAmount.amountInCents <= 0L
    val accent = if (isSettled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(AppTheme.shapes.corner12)
                        .background(accent.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isSettled) Icons.Rounded.CheckCircle else Icons.Rounded.Payments,
                        contentDescription = null,
                        tint = accent,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isSettled) stringResource(R.string.debt_settled_with_format, debtorName) else stringResource(R.string.debt_collect_from_format, debtorName),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isSettled) stringResource(R.string.debt_no_pending) else stringResource(R.string.debt_pending_count_format, unpaidCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = AppTheme.shapes.corner8,
                    color = accent.copy(alpha = .12f),
                ) {
                    Text(
                        text = if (isSettled) stringResource(R.string.debt_complete) else stringResource(R.string.debt_collect),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                text = MoneyFormatter.format(outstandingAmount),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = accent,
            )

        }
    }
}

/**
 * Ảnh QR dùng template "compact2" của VietQR — bản thân ảnh đã in sẵn logo VietQR,
 * logo Napas, tên ngân hàng, tên chủ tài khoản và số tài khoản. Vì vậy card này chỉ
 * hiển thị đúng ảnh đó; vẽ lại các thông tin trên bằng Compose sẽ lặp thông tin 3 lần.
 * Dùng FillWidth thay cho khung 200.dp cố định để chữ in trong ảnh đủ lớn để đọc.
 */
@Composable
private fun VietQrTemplateCard(
    qrImageUrl: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AsyncImage(
            model = qrImageUrl,
            contentDescription = stringResource(R.string.cd_vietqr_logo),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(AppTheme.shapes.corner16)
        )
    }
}

private fun formatInstant(instant: kotlin.time.Instant?): String {
    if (instant == null) return ""
    val tz = TimeZone.currentSystemDefault()
    val localDateTime = instant.toLocalDateTime(tz)
    return String.format(
        Locale.US,
        "%02d/%02d/%d",
        localDateTime.day,
        localDateTime.month.number,
        localDateTime.year
    )
}

@Composable
private fun TransferDetailsCopyCard(
    bankName: String,
    accountNumber: String,
    accountName: String,
    amountStr: String,
    amountRaw: String,
    memoCode: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val copyBankMessage = stringResource(R.string.feedback_copy_bank)
    val copyAccountNumberMessage = stringResource(R.string.feedback_copy_account_number)
    val copyAccountNameMessage = stringResource(R.string.feedback_copy_account_name)
    val copyAmountMessage = stringResource(R.string.feedback_copy_amount_format, amountRaw)
    val copyMemoMessage = stringResource(R.string.feedback_copy_memo)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(R.string.transfer_details_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            CopyableDetailRow(label = stringResource(R.string.transfer_bank), value = bankName.uppercase(Locale.ROOT), onCopy = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(bankName))
                Toast.makeText(context, copyBankMessage, Toast.LENGTH_SHORT).show()
            })

            CopyableDetailRow(label = stringResource(R.string.transfer_account_number), value = accountNumber, onCopy = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(accountNumber))
                Toast.makeText(context, copyAccountNumberMessage, Toast.LENGTH_SHORT).show()
            })

            CopyableDetailRow(label = stringResource(R.string.transfer_account_name), value = accountName.uppercase(Locale.ROOT), onCopy = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(accountName))
                Toast.makeText(context, copyAccountNameMessage, Toast.LENGTH_SHORT).show()
            })

            CopyableDetailRow(label = stringResource(R.string.transfer_amount), value = amountStr, valueColor = MaterialTheme.colorScheme.error, onCopy = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(amountRaw))
                Toast.makeText(context, copyAmountMessage, Toast.LENGTH_SHORT).show()
            })

            CopyableDetailRow(label = stringResource(R.string.transfer_memo), value = memoCode, valueColor = MaterialTheme.colorScheme.primary, onCopy = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(memoCode))
                Toast.makeText(context, copyMemoMessage, Toast.LENGTH_SHORT).show()
            })
        }
    }
}

@Composable
private fun CopyableDetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onCopy).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
            Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.cd_copy), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}

