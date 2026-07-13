package com.notepay.ui.feature.billsplit

import com.notepay.ui.theme.AppTheme

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.notepay.ui.feedback.UiFeedback
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.model.Money
import com.notepay.ui.component.ConfirmDeleteDialog
import com.notepay.ui.component.EmptyState
import com.notepay.ui.util.MoneyFormatter
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillSplitScreen(
    onDebtorClick: (String) -> Unit,
    onFeedback: suspend (UiFeedback) -> Boolean,
    navigationBarOffset: Float = 0f,
    initialShowCreate: Boolean = false,
    onClearShowCreate: () -> Unit = {},
    viewModel: BillSplitViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.feedback.collect { feedback ->
            onFeedback(feedback)
        }
    }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var qrConfigWalletId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteBill by remember { mutableStateOf<BillSplitItemState?>(null) }

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
    val vietqrCd = stringResource(R.string.bill_split_vietqr_cd)
    val emptyAll = stringResource(R.string.bill_split_empty)
    val tabUnpaid = stringResource(R.string.bill_split_tab_unpaid)
    val tabPaid = stringResource(R.string.bill_split_tab_paid)
    val emptyUnpaid = stringResource(R.string.bill_split_empty_unpaid)
    val emptyPaid = stringResource(R.string.bill_split_empty_paid)
    val vietqrSavedFmt = stringResource(R.string.bill_split_vietqr_saved)
    val confirmDeleteTitle = stringResource(R.string.confirm_delete_bill_title)
    val confirmDeleteMsgFmt = stringResource(R.string.confirm_delete_permanent)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(AppTheme.shapes.corner8)
                            .clickable(
                                enabled = state.activeWallet != null,
                                onClick = {
                                    qrConfigWalletId = state.activeWallet?.id
                                }
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.notepay.R.drawable.logo_vietqr),
                            contentDescription = vietqrCd,
                            modifier = Modifier
                                .height(26.dp)
                                .graphicsLayer(alpha = if (state.activeWallet != null) 1f else 0.38f)
                        )
                    }
                }
            )
        }
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val bottomPadding = padding.calculateBottomPadding() + 96.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(layoutDirection)
                )
        ) {
            val allEmpty = state.unpaidSplits.isEmpty() && state.paidSplits.isEmpty()

            if (allEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        message = emptyAll,
                        icon = Icons.Rounded.CallReceived
                    )
                }
            } else {
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = {
                            val count = state.unpaidSplits.groupBy { it.split.debtorName }.size
                            if (count > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tabUnpaid)
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .background(MaterialTheme.colorScheme.error, AppTheme.shapes.circle),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "$count",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                        )
                                    }
                                }
                            } else {
                                Text(tabUnpaid)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = { Text(tabPaid) }
                    )
                }

                if (selectedTab == 0) {
                    val unpaidGroups = remember(state.unpaidSplits) {
                        state.unpaidSplits.groupBy { it.split.debtorName }
                    }

                    if (unpaidGroups.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            EmptyState(
                                message = emptyUnpaid,
                                icon = Icons.Rounded.CallReceived
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 16.dp,
                                top = 16.dp,
                                end = 16.dp,
                                bottom = bottomPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(unpaidGroups.entries.toList(), key = { it.key }) { (debtorName, splits) ->
                                val totalAmount = splits.sumOf { it.split.amount.amountInCents }
                                val splitCount = splits.size
                                DebtorGroupRow(
                                    debtorName = debtorName,
                                    totalAmountCents = totalAmount,
                                    splitCount = splitCount,
                                    onClick = { onDebtorClick(debtorName) }
                                )
                            }
                        }
                    }
                } else {
                    if (state.paidSplits.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            EmptyState(
                                message = emptyPaid,
                                icon = Icons.Rounded.CheckCircle
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 16.dp,
                                top = 16.dp,
                                end = 16.dp,
                                bottom = bottomPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.paidSplits, key = { item -> item.split.id }) { item ->
                                BillSplitItemRow(
                                    itemState = item,
                                    onClick = { onDebtorClick(item.split.debtorName) },
                                    onDelete = { pendingDeleteBill = item }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // BottomSheet cấu hình VietQR
    qrConfigWallet?.let { wallet ->
        VietQrConfigSheet(
            wallet = wallet,
            onDismiss = { qrConfigWalletId = null },
            onSave = { bin, accNum, accName ->
                viewModel.updateWalletForQr(wallet.id, bin, accNum, accName)
                qrConfigWalletId = null
                Toast.makeText(context, vietqrSavedFmt.format(wallet.name), Toast.LENGTH_SHORT).show()
            },
        )
    }

    // Modal Bottom Sheet tạo hóa đơn chia tiền (P0-3: thay cho AlertDialog lồng)
    if (showCreateDialog) {
        val allDebtorNames = remember(state.unpaidSplits, state.paidSplits) {
            (state.unpaidSplits.map { it.split.debtorName } + state.paidSplits.map { it.split.debtorName })
                .distinct()
                .filter { it.isNotBlank() }
                .sorted()
        }
        BillSplitCreateSheet(
            recentTransactions = state.recentTransactions,
            allDebtorNames = allDebtorNames,
            onConfirm = { parentTxId, entries ->
                viewModel.createBillSplits(parentTxId, entries)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    pendingDeleteBill?.let { item ->
        ConfirmDeleteDialog(
            title = confirmDeleteTitle,
            itemName = "${item.split.debtorName} • ${MoneyFormatter.format(item.split.amount)}",
            message = confirmDeleteMsgFmt.format(item.split.debtorName),
            onConfirm = { viewModel.deleteBillSplit(item.split.id) },
            onDismiss = { pendingDeleteBill = null },
        )
    }
}

@Composable
private fun DebtorGroupRow(
    debtorName: String,
    totalAmountCents: Long,
    splitCount: Int,
    onClick: () -> Unit
) {
    val amountStr = MoneyFormatter.format(Money(totalAmountCents))
    val totalCountFmt = androidx.compose.ui.res.stringResource(R.string.bill_split_total_count)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        shape = AppTheme.shapes.corner16
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f), AppTheme.shapes.circle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CallReceived,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debtorName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = totalCountFmt.format(splitCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = amountStr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillSplitItemRow(
    itemState: BillSplitItemState,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val split = itemState.split
    val parent = itemState.parentTransaction
    val amountStr = MoneyFormatter.format(split.amount)
    val note = parent?.note ?: "Hóa đơn chia tiền"

    if (split.isPaid) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onDelete
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = AppTheme.shapes.corner16
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFE8F5E9), shape = AppTheme.shapes.circle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${split.debtorName} đã trả nợ",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Gốc: $note • Trả: ${formatInstantDayMonth(split.paidAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = amountStr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onDelete
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = AppTheme.shapes.corner16
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f), AppTheme.shapes.circle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CallReceived,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${split.debtorName} nợ",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Gốc: $note • Mượn: ${formatInstantDayMonth(split.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = amountStr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatInstantDayMonth(instant: kotlinx.datetime.Instant?): String {
    if (instant == null) return ""
    val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
    val localDateTime = instant.toLocalDateTime(tz)
    val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
    val month = localDateTime.monthNumber.toString().padStart(2, '0')
    return "$day/$month"
}

private fun formatInstant(instant: kotlinx.datetime.Instant?): String {
    if (instant == null) return ""
    val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
    val localDateTime = instant.toLocalDateTime(tz)
    val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
    val month = localDateTime.monthNumber.toString().padStart(2, '0')
    val year = localDateTime.year
    return "$day/$month/$year"
}

