package com.notepay.feature.billsplit.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.component.dialog.ConfirmDeleteDialog
import com.notepay.core.designsystem.component.effect.AppAtmosphericBackground
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.component.feedback.EmptyStateWithAction
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.formatter.PresentationDateFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.HomeHeroCardDarkBottom
import com.notepay.core.designsystem.theme.HomeHeroCardDarkRim
import com.notepay.core.designsystem.theme.HomeHeroCardDarkTop
import com.notepay.core.designsystem.theme.HomeHeroCardLightBottom
import com.notepay.core.designsystem.theme.HomeHeroCardLightRim
import com.notepay.core.designsystem.theme.HomeHeroCardLightTop
import com.notepay.core.designsystem.util.VietQrCardData
import com.notepay.core.designsystem.util.VietQrCardRenderer
import com.notepay.core.designsystem.util.VietQrGenerator
import com.notepay.core.navigation.NotePayAnimatedContent
import com.notepay.core.navigation.NotePayStaggeredEntrance
import com.notepay.domain.money.Money
import com.notepay.feature.billsplit.main.sheet.BillSplitCreateSheet
import com.notepay.feature.billsplit.main.sheet.VietQrConfigFullScreenDialog
import kotlinx.coroutines.launch

/** Clearance below list items so the extended FAB never obscures the last card. */
private val FabClearance = 104.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitScreen(
    onDebtorClick: (String) -> Unit,
    onFeedback: suspend (UiFeedback) -> Boolean,
    onBack: () -> Unit = {},
    onAddExpense: () -> Unit = {},
    initialShowCreate: Boolean = false,
    onClearShowCreate: () -> Unit = {},
    viewModel: BillSplitViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel, onFeedback) {
        viewModel.feedback.collect { feedback ->
            launch { onFeedback(feedback) }
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
    val historyTitle = stringResource(R.string.billsplit_tab_paid)
    val emptyAll = stringResource(R.string.bill_split_empty)
    val emptyUnpaid = stringResource(R.string.bill_split_empty_unpaid)
    val emptyPaid = stringResource(R.string.bill_split_empty_paid)
    val createCta = stringResource(R.string.billsplit_create_cta)
    val confirmDeleteTitle = stringResource(R.string.confirm_delete_bill_title)
    val confirmDeleteMessageFormat = stringResource(R.string.dialog_delete_confirm_format)
    val linkedBankLabel = stringResource(R.string.bank_linked)
    val sharePaymentRequestTitle = stringResource(R.string.billsplit_share_qr_image)
    val shareQrFailedMsg = stringResource(R.string.billsplit_share_qr_failed)
    val qrMissingMsg = stringResource(R.string.bill_split_vietqr_missing)
    val setupQrShortLabel = stringResource(R.string.billsplit_setup_vietqr_short)

    val activeWallet = state.activeWallet
    val activeBank = remember(activeWallet?.bankBin, state.banks) {
        state.banks.find { it.bin == activeWallet?.bankBin }
    }
    val hasActiveQr = activeWallet?.bankBin != null && !activeWallet.accountNumber.isNullOrBlank()
    val receiveAccountLabel = remember(activeWallet, activeBank) {
        activeWallet?.takeIf { !it.bankBin.isNullOrBlank() && !it.accountNumber.isNullOrBlank() }?.let { wallet ->
            val bankShort = activeBank?.shortName ?: wallet.bankBin
            val last4 = wallet.accountNumber?.takeLast(4)?.let { "\u2022\u2022$it" }
            listOfNotNull(bankShort, last4).joinToString(" ").ifBlank { wallet.name }
        }
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = navBottom + FabClearance
    val unpaidGroups = remember(state.unpaidSplits) {
        state.unpaidSplits.groupBy { it.split.debtorName }.entries.toList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppAtmosphericBackground(
            modifier = Modifier.matchParentSize(),
            forceSoftBlurOnPhoto = true,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            BillSplitHeader(
                title = if (showPaidHistory) historyTitle else title,
                showPaidHistory = showPaidHistory,
                onBackClick = {
                    if (showPaidHistory) showPaidHistory = false else onBack()
                },
                onToggleHistory = { showPaidHistory = !showPaidHistory },
            )

            NotePayAnimatedContent(
                targetState = showPaidHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "billsplit-mode",
            ) { showingHistory ->
                if (showingHistory) {
                    PaidHistoryContent(
                        paidSplits = state.paidSplits,
                        emptyTitle = emptyPaid,
                        bottomPadding = bottomPadding,
                        onDebtorClick = onDebtorClick,
                        onDelete = { pendingDeleteBill = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    val emptyTitle = if (state.paidSplits.isEmpty()) emptyAll else emptyUnpaid
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 8.dp,
                            end = 16.dp,
                            bottom = bottomPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            NotePayStaggeredEntrance(index = 0) {
                                BillSplitOverview(
                                    totalAmountCents = state.unpaidSplits.sumOf { it.split.amount.amountInCents },
                                    debtorCount = unpaidGroups.size,
                                    splitCount = state.unpaidSplits.size,
                                    hasQrConfigured = hasActiveQr,
                                    receiveAccountLabel = receiveAccountLabel,
                                    onChangeReceiveAccount = (activeWallet ?: state.wallets.firstOrNull())
                                        ?.let { wallet -> { qrConfigWalletId = wallet.id } },
                                )
                            }
                        }

                        if (unpaidGroups.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 24.dp, bottom = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    EmptyStateWithAction(
                                        title = emptyTitle,
                                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                                        actionLabel = createCta,
                                        onClick = { showCreateDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        } else {
                            items(unpaidGroups, key = { it.key }) { (debtorName, splits) ->
                                val totalAmountCents = splits.sumOf { it.split.amount.amountInCents }
                                val amountStr = MoneyFormatter.format(Money(totalAmountCents))
                                val qrBankBin = activeWallet?.bankBin
                                val qrAccountNumber = activeWallet?.accountNumber
                                val qrMemoCode = "NP " + debtorName
                                    .filter { it.isLetterOrDigit() || it.isWhitespace() }
                                    .trim()
                                    .uppercase()

                                DebtorGroupRow(
                                    debtorName = debtorName,
                                    totalAmountCents = totalAmountCents,
                                    splitCount = splits.size,
                                    hasQrConfigured = !qrBankBin.isNullOrBlank() && !qrAccountNumber.isNullOrBlank(),
                                    onClick = { onDebtorClick(debtorName) },
                                    onShare = {
                                        if (!qrBankBin.isNullOrBlank() && !qrAccountNumber.isNullOrBlank()) {
                                            val emv = VietQrGenerator.generateEmvCoPayload(
                                                bankBin = qrBankBin,
                                                accountNumber = qrAccountNumber,
                                                amountCents = totalAmountCents,
                                                memo = qrMemoCode,
                                                accountName = activeWallet.accountName,
                                            )
                                            val cardData = VietQrCardData(
                                                emvPayload = emv,
                                                bankShortName = activeBank?.shortName ?: linkedBankLabel,
                                                bankFullName = activeBank?.name ?: linkedBankLabel,
                                                bankLogoUrl = activeBank?.logoUrl,
                                                accountName = activeWallet.accountName.orEmpty(),
                                                accountNumber = qrAccountNumber,
                                                amountFormatted = amountStr,
                                                memo = qrMemoCode,
                                            )
                                            coroutineScope.launch {
                                                VietQrCardRenderer.shareVietQrCardImage(
                                                    context = context,
                                                    data = cardData,
                                                    chooserTitle = sharePaymentRequestTitle,
                                                    filePrefix = "billsplit_$debtorName",
                                                ).onFailure {
                                                    onFeedback(UiFeedback(shareQrFailedMsg, type = FeedbackType.Error))
                                                }
                                            }
                                        } else {
                                            val targetWalletId = activeWallet?.id ?: state.wallets.firstOrNull()?.id
                                            coroutineScope.launch {
                                                onFeedback(
                                                    UiFeedback(
                                                        message = qrMissingMsg,
                                                        type = FeedbackType.Warning,
                                                        actionLabel = setupQrShortLabel,
                                                        onAction = { targetWalletId?.let { qrConfigWalletId = it } },
                                                    ),
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!showPaidHistory && unpaidGroups.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = {
                    Text(
                        text = createCta,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                shape = AppTheme.shapes.capsule,
                containerColor = AppTheme.colors.ink,
                contentColor = AppTheme.colors.onInk,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
        }
    }

    qrConfigWallet?.let { wallet ->
        VietQrConfigFullScreenDialog(
            wallet = wallet,
            banks = state.banks,
            onDismiss = { qrConfigWalletId = null },
            onSave = { bin, accountNumber, accountName ->
                viewModel.updateWalletForQr(wallet.id, bin, accountNumber, accountName)
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
            onAddExpense = {
                showCreateDialog = false
                onAddExpense()
            },
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
private fun BillSplitHeader(
    title: String,
    showPaidHistory: Boolean,
    onBackClick: () -> Unit,
    onToggleHistory: () -> Unit,
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
            onClick = onBackClick,
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
                    contentDescription = stringResource(R.string.billsplit_cd_back),
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

        if (!showPaidHistory) {
            Surface(
                onClick = onToggleHistory,
                shape = CircleShape,
                color = pillColor,
                border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
                modifier = Modifier
                    .size(48.dp)
                    .iosCardShadow(shape = CircleShape, elevated = false),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = stringResource(R.string.billsplit_cd_history),
                        tint = AppTheme.colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaidHistoryContent(
    paidSplits: List<BillSplitItemState>,
    emptyTitle: String,
    bottomPadding: Dp,
    onDebtorClick: (String) -> Unit,
    onDelete: (BillSplitItemState) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (paidSplits.isEmpty()) {
        Box(
            modifier = modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = bottomPadding),
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
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp,
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
                color = AppTheme.colors.textSecondary,
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
    hasQrConfigured: Boolean,
    receiveAccountLabel: String?,
    onChangeReceiveAccount: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isLight = AppTheme.colors.isLight
    val cardShape = AppTheme.shapes.corner28
    val heroGradient = remember(isLight) {
        val top = if (isLight) HomeHeroCardLightTop else HomeHeroCardDarkTop
        val bottom = if (isLight) HomeHeroCardLightBottom else HomeHeroCardDarkBottom
        Brush.verticalGradient(listOf(top, bottom))
    }
    val rimColor = if (isLight) HomeHeroCardLightRim else HomeHeroCardDarkRim
    val stackTiles = LocalDensity.current.fontScale >= 1.45f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .iosCardShadow(shape = cardShape, elevated = true),
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, rimColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroGradient)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.CallReceived,
                        contentDescription = null,
                        tint = AppTheme.colors.primary,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.billsplit_total_to_receive),
                        style = AppTheme.typography.labelLarge,
                        color = AppTheme.colors.textSecondary,
                    )
                    Text(
                        text = MoneyFormatter.format(Money(totalAmountCents)),
                        style = AppTheme.typography.amountHero.copy(fontFeatureSettings = "tnum"),
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            val peopleText = pluralStringResource(R.plurals.billsplit_debtor_count, debtorCount, debtorCount)
            val splitsText = pluralStringResource(R.plurals.billsplit_split_to_reconcile_count, splitCount, splitCount)
            if (stackTiles) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroStatChip(text = peopleText, modifier = Modifier.fillMaxWidth())
                    HeroStatChip(text = splitsText, modifier = Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HeroStatChip(text = peopleText, modifier = Modifier.weight(1f))
                    HeroStatChip(text = splitsText, modifier = Modifier.weight(1f))
                }
            }

            Surface(
                shape = AppTheme.shapes.corner16,
                color = AppTheme.colors.surfaceSunken,
                border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCode2,
                        contentDescription = null,
                        tint = if (hasQrConfigured) AppTheme.colors.success else AppTheme.colors.warning,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (hasQrConfigured && receiveAccountLabel != null) {
                            stringResource(R.string.billsplit_vietqr_ready_format, receiveAccountLabel)
                        } else {
                            stringResource(R.string.billsplit_vietqr_not_ready)
                        },
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (onChangeReceiveAccount != null) {
                        Surface(
                            onClick = onChangeReceiveAccount,
                            shape = AppTheme.shapes.capsule,
                            color = if (hasQrConfigured) AppTheme.colors.surface else AppTheme.colors.sand,
                            border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
                            modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(
                                        if (hasQrConfigured) R.string.billsplit_change_receive_account
                                        else R.string.billsplit_setup_vietqr_short,
                                    ),
                                    style = AppTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasQrConfigured) AppTheme.colors.textPrimary else AppTheme.colors.onSand,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStatChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.corner12,
        color = AppTheme.colors.surfaceSunken,
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun DebtorGroupRow(
    debtorName: String,
    totalAmountCents: Long,
    splitCount: Int,
    hasQrConfigured: Boolean,
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
    val shape = AppTheme.shapes.corner20

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .iosCardShadow(shape = shape, elevated = false),
            shape = shape,
            color = AppTheme.colors.surface,
            border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial,
                        style = AppTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.primary,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debtorName,
                        style = AppTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = totalCountText,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = amount,
                        style = AppTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.primary,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = AppTheme.colors.textMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Surface(
            onClick = onShare,
            shape = AppTheme.shapes.corner16,
            color = if (hasQrConfigured) AppTheme.colors.ink else AppTheme.colors.surface,
            border = if (hasQrConfigured) null else BorderStroke(0.5.dp, AppTheme.colors.hairline),
            modifier = Modifier
                .size(52.dp)
                .iosCardShadow(shape = AppTheme.shapes.corner16, elevated = false),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (hasQrConfigured) Icons.Rounded.Share else Icons.Rounded.QrCode2,
                    contentDescription = stringResource(
                        if (hasQrConfigured) R.string.billsplit_share_qr_image else R.string.billsplit_setup_vietqr_short,
                    ),
                    tint = if (hasQrConfigured) AppTheme.colors.onInk else AppTheme.colors.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
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
    val shape = AppTheme.shapes.corner16

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .iosCardShadow(shape = shape, elevated = false),
        color = AppTheme.colors.surface,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.success.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = AppTheme.colors.success,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.billsplit_paid_by_format, split.debtorName),
                    style = AppTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.colors.textPrimary,
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
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = MoneyFormatter.format(split.amount),
                    style = AppTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.success,
                    maxLines = 1,
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.billsplit_delete_paid_format, split.debtorName),
                        tint = AppTheme.colors.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
