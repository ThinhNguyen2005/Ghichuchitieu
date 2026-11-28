package com.notepay.feature.billsplit.main.detail

import com.notepay.core.designsystem.theme.AppTheme

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.component.dialog.ConfirmDeleteDialog
import com.notepay.core.designsystem.component.effect.AppAtmosphericBackground
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.component.qr.BrandedVietQrCard
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.formatter.PresentationDateFormatter
import com.notepay.core.designsystem.theme.HomeHeroCardDarkBottom
import com.notepay.core.designsystem.theme.HomeHeroCardDarkRim
import com.notepay.core.designsystem.theme.HomeHeroCardDarkTop
import com.notepay.core.designsystem.theme.HomeHeroCardLightBottom
import com.notepay.core.designsystem.theme.HomeHeroCardLightRim
import com.notepay.core.designsystem.theme.HomeHeroCardLightTop
import com.notepay.core.designsystem.util.VietQrCardData
import com.notepay.core.designsystem.util.VietQrCardRenderer
import com.notepay.core.designsystem.util.VietQrGenerator
import com.notepay.domain.model.Category
import com.notepay.domain.money.Money
import com.notepay.feature.billsplit.main.BillSplitItemState
import com.notepay.feature.billsplit.main.BillSplitViewModel
import com.notepay.feature.billsplit.main.sheet.PaymentReconciliationSheet
import com.notepay.feature.billsplit.main.sheet.VietQrConfigFullScreenDialog
import com.notepay.feature.category.mapper.CategoryAvatar
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DebtorDetailScreen(
    debtorName: String,
    onBack: () -> Unit,
    onFeedback: suspend (UiFeedback) -> Boolean,
    onSettled: (UiFeedback) -> Unit = { onBack() },
    viewModel: BillSplitViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel, onFeedback) {
        viewModel.feedback.collect { feedback ->
            launch { onFeedback(feedback) }
        }
    }

    LaunchedEffect(viewModel, onSettled) {
        viewModel.settledFeedback.collect { feedback ->
            onSettled(feedback)
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
    val context = LocalContext.current
    val linkedBankName = stringResource(R.string.bank_linked)
    val shareQrChooserTitle = stringResource(R.string.billsplit_share_qr_image)
    val shareQrFailedMsg = stringResource(R.string.billsplit_share_qr_failed)
    var qrConfigWalletId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteBill by remember { mutableStateOf<BillSplitItemState?>(null) }
    var showReconciliationSheet by remember { mutableStateOf(false) }

    val qrConfigWallet = qrConfigWalletId?.let { id ->
        state.wallets.find { it.id == id }
    }

    val matchedBank = remember(activeWallet, state.banks) {
        state.banks.find { it.bin == activeWallet?.bankBin }
    }
    val bankName = matchedBank?.shortName ?: linkedBankName

    val memoCode = remember(debtorName) {
        val sanitized = debtorName.filter { it.isLetterOrDigit() || it.isWhitespace() }.trim().uppercase()
        "NP $sanitized"
    }

    val emvPayload = remember(activeWallet, totalAmountCents, memoCode) {
        if (activeWallet?.bankBin != null && activeWallet.accountNumber != null) {
            VietQrGenerator.generateEmvCoPayload(
                bankBin = activeWallet.bankBin,
                accountNumber = activeWallet.accountNumber,
                amountCents = totalAmountCents,
                memo = memoCode,
                accountName = activeWallet.accountName,
            )
        } else null
    }

    val qrCardData = remember(emvPayload, matchedBank, bankName, activeWallet, totalAmountCents, memoCode) {
        if (emvPayload != null && activeWallet != null && totalAmountCents > 0L) {
            VietQrCardData(
                emvPayload = emvPayload,
                bankShortName = bankName,
                bankFullName = matchedBank?.name ?: bankName,
                bankLogoUrl = matchedBank?.logoUrl,
                accountName = activeWallet.accountName.orEmpty(),
                accountNumber = activeWallet.accountNumber.orEmpty(),
                amountFormatted = MoneyFormatter.format(Money(totalAmountCents)),
                memo = memoCode,
            )
        } else null
    }

    val shareQrImage: () -> Unit = {
        qrCardData?.let { data ->
            coroutineScope.launch {
                VietQrCardRenderer.shareVietQrCardImage(
                    context = context,
                    data = data,
                    chooserTitle = shareQrChooserTitle,
                    filePrefix = "qr_$debtorName",
                ).onFailure {
                    onFeedback(UiFeedback(shareQrFailedMsg, type = FeedbackType.Error))
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppAtmosphericBackground(
            modifier = Modifier.matchParentSize(),
            forceSoftBlurOnPhoto = true,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            DebtorDetailHeader(
                title = stringResource(R.string.debtor_detail_title, debtorName),
                canShareQr = qrCardData != null,
                onBack = onBack,
                onShareQr = shareQrImage,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = AppTheme.colors.primary)
                    }
                } else if (allDebtorSplits.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AppTheme.colors.primary, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.debtor_no_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onBack,
                                shape = AppTheme.shapes.capsule,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppTheme.colors.ink,
                                    contentColor = AppTheme.colors.onInk,
                                ),
                            ) { Text(stringResource(R.string.action_back)) }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 8.dp,
                            end = 16.dp,
                            bottom = if (unpaidDebtorSplits.isNotEmpty()) 104.dp else 32.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            DebtorSplitSummaryCard(
                                debtorName = debtorName,
                                outstandingAmount = Money(totalAmountCents),
                                unpaidCount = unpaidDebtorSplits.size,
                            )
                        }

                        item {
                            if (qrCardData != null && activeWallet != null && totalAmountCents > 0L) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    BrandedVietQrCard(data = qrCardData)

                                    Button(
                                        onClick = shareQrImage,
                                        shape = AppTheme.shapes.capsule,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AppTheme.colors.ink,
                                            contentColor = AppTheme.colors.onInk,
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp),
                                    ) {
                                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(shareQrChooserTitle, fontWeight = FontWeight.Bold)
                                    }

                                    TransferDetailsCopyCard(
                                        bankName = bankName,
                                        accountNumber = activeWallet.accountNumber.orEmpty(),
                                        accountName = activeWallet.accountName.orEmpty(),
                                        amountStr = MoneyFormatter.format(Money(totalAmountCents)),
                                        amountRaw = (totalAmountCents / 100).toString(),
                                        memoCode = memoCode,
                                        onFeedback = { feedback ->
                                            coroutineScope.launch { onFeedback(feedback) }
                                        },
                                    )
                                }
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .iosCardShadow(shape = AppTheme.shapes.corner20, elevated = false),
                                    shape = AppTheme.shapes.corner20,
                                    color = AppTheme.colors.surface,
                                    border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Rounded.Info, contentDescription = null, tint = AppTheme.colors.warning)
                                            Text(stringResource(R.string.bill_split_vietqr_missing), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = stringResource(R.string.bill_split_vietqr_prompt_format, activeWallet?.name ?: stringResource(R.string.wallet_default)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppTheme.colors.textSecondary,
                                        )
                                        Button(
                                            onClick = { qrConfigWalletId = (activeWallet ?: state.wallets.firstOrNull())?.id },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 48.dp),
                                            shape = AppTheme.shapes.capsule,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AppTheme.colors.ink,
                                                contentColor = AppTheme.colors.onInk,
                                            ),
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
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(text = stringResource(R.string.debt_reconciliation_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = stringResource(R.string.debt_reconciliation_summary_format, unpaidDebtorSplits.size, paidDebtorSplits.size),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppTheme.colors.textSecondary,
                                    )
                                }
                                Text(text = stringResource(R.string.debt_hold_to_delete), style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textSecondary)
                            }
                        }

                        items(allDebtorSplits, key = { it.split.id }) { item ->
                            DebtorHistoryItemCard(
                                item = item,
                                onLongClick = { pendingDeleteBill = item },
                            )
                        }
                    }
                }
            }
        }

        if (unpaidDebtorSplits.isNotEmpty()) {
            Surface(
                color = AppTheme.colors.surface.copy(alpha = 0.96f),
                border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                Button(
                    onClick = { showReconciliationSheet = true },
                    shape = AppTheme.shapes.capsule,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.ink,
                        contentColor = AppTheme.colors.onInk,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .defaultMinSize(minHeight = 52.dp),
                ) {
                    Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_confirm_payment), fontWeight = FontWeight.Bold)
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
            }
        )
    }

    pendingDeleteBill?.let { item ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.confirm_delete_bill_title),
            itemName = "${item.split.debtorName} • ${MoneyFormatter.format(item.split.amount)}",
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
                    debtorName = debtorName,
                    splitIds = unpaidDebtorSplits.map { it.split.id },
                    incomeTxId = incomeTxId
                )
            }
        )
    }
}

@Composable
private fun DebtorDetailHeader(
    title: String,
    canShareQr: Boolean,
    onBack: () -> Unit,
    onShareQr: () -> Unit,
) {
    val pillColor = AppTheme.colors.surface.copy(alpha = if (AppTheme.colors.isLight) 0.92f else 0.88f)
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
            color = pillColor,
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
                )
            }
        }

        Text(
            text = title,
            style = AppTheme.typography.displaySerif,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )

        if (canShareQr) {
            Surface(
                onClick = onShareQr,
                shape = CircleShape,
                color = pillColor,
                border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
                modifier = Modifier
                    .size(48.dp)
                    .iosCardShadow(shape = CircleShape, elevated = false),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.billsplit_share_qr_image),
                        tint = AppTheme.colors.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtorHistoryItemCard(
    item: BillSplitItemState,
    onLongClick: () -> Unit,
) {
    val parentTx = item.parentTransaction
    val note = parentTx?.note ?: stringResource(R.string.debt_transaction_fallback)
    val isPaid = item.split.isPaid
    val shape = AppTheme.shapes.corner16

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .iosCardShadow(shape = shape, elevated = false)
            .clip(shape)
            .combinedClickable(onClick = {}, onLongClick = onLongClick),
        color = AppTheme.colors.surface,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val category = parentTx?.category ?: Category.OTHER
            CategoryAvatar(category = category)

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    val badgeBg = if (isPaid) AppTheme.colors.success.copy(alpha = 0.14f) else AppTheme.colors.error.copy(alpha = 0.12f)
                    val badgeFg = if (isPaid) AppTheme.colors.success else AppTheme.colors.error
                    Surface(shape = AppTheme.shapes.corner12, color = badgeBg) {
                        Text(
                            text = stringResource(if (isPaid) R.string.debt_paid else R.string.debt_pending_collection),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeFg,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                val muonDate = PresentationDateFormatter.formatDate(item.split.createdAt)
                val dateSubtext = if (isPaid) {
                    val tradate = PresentationDateFormatter.formatDate(item.split.paidAt)
                    stringResource(R.string.debt_dates_paid_format, muonDate, tradate)
                } else {
                    stringResource(R.string.debt_dates_pending_format, muonDate)
                }
                Text(text = dateSubtext, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
            }

            Text(
                text = MoneyFormatter.format(item.split.amount),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = if (isPaid) AppTheme.colors.success else AppTheme.colors.error,
            )
        }
    }
}

@Composable
private fun DebtorSplitSummaryCard(
    debtorName: String,
    outstandingAmount: Money,
    unpaidCount: Int,
    modifier: Modifier = Modifier,
) {
    val isSettled = outstandingAmount.amountInCents <= 0L
    val accent = if (isSettled) AppTheme.colors.success else AppTheme.colors.error
    val isLight = AppTheme.colors.isLight
    val shape = AppTheme.shapes.corner24
    val heroGradient = remember(isLight) {
        val top = if (isLight) HomeHeroCardLightTop else HomeHeroCardDarkTop
        val bottom = if (isLight) HomeHeroCardLightBottom else HomeHeroCardDarkBottom
        Brush.verticalGradient(listOf(top, bottom))
    }
    val rimColor = if (isLight) HomeHeroCardLightRim else HomeHeroCardDarkRim

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .iosCardShadow(shape = shape, elevated = true),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, rimColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroGradient)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(AppTheme.shapes.corner12)
                        .background(accent.copy(alpha = 0.14f)),
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
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isSettled) {
                            stringResource(R.string.debt_no_pending)
                        } else {
                            pluralStringResource(R.plurals.debt_pending_count, unpaidCount, unpaidCount)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary,
                    )
                }
                Surface(
                    shape = AppTheme.shapes.capsule,
                    color = accent.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = if (isSettled) stringResource(R.string.debt_complete) else stringResource(R.string.debt_collect),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                text = MoneyFormatter.format(outstandingAmount),
                style = AppTheme.typography.amountHero.copy(fontFeatureSettings = "tnum"),
                color = accent,
            )
        }
    }
}

@Composable
private fun TransferDetailsCopyCard(
    bankName: String,
    accountNumber: String,
    accountName: String,
    amountStr: String,
    amountRaw: String,
    memoCode: String,
    onFeedback: (UiFeedback) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val copyBankMessage = stringResource(R.string.feedback_copy_bank)
    val copyAccountNumberMessage = stringResource(R.string.feedback_copy_account_number)
    val copyAccountNameMessage = stringResource(R.string.feedback_copy_account_name)
    val copyAmountMessage = stringResource(R.string.feedback_copy_amount_format, amountRaw)
    val copyMemoMessage = stringResource(R.string.feedback_copy_memo)

    fun copyToClipboard(text: String, feedbackMessage: String) {
        coroutineScope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("NotePay", text)))
            onFeedback(UiFeedback(feedbackMessage, type = FeedbackType.Success))
        }
    }

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
                copyToClipboard(bankName, copyBankMessage)
            })

            CopyableDetailRow(label = stringResource(R.string.transfer_account_number), value = accountNumber, onCopy = {
                copyToClipboard(accountNumber, copyAccountNumberMessage)
            })

            CopyableDetailRow(label = stringResource(R.string.transfer_account_name), value = accountName.uppercase(Locale.ROOT), onCopy = {
                copyToClipboard(accountName, copyAccountNameMessage)
            })

            CopyableDetailRow(label = stringResource(R.string.transfer_amount), value = amountStr, valueColor = MaterialTheme.colorScheme.error, onCopy = {
                copyToClipboard(amountRaw, copyAmountMessage)
            })

            CopyableDetailRow(label = stringResource(R.string.transfer_memo), value = memoCode, valueColor = MaterialTheme.colorScheme.primary, onCopy = {
                copyToClipboard(memoCode, copyMemoMessage)
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
            Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.content_description_copy), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}

