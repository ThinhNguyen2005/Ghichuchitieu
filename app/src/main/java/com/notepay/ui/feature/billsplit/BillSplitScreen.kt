package com.notepay.ui.feature.billsplit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.notepay.ui.util.VietQrGenerator
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CallReceived
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.model.Money
import com.notepay.ui.component.ConfirmDeleteDialog
import com.notepay.ui.component.EmptyStateWithAction
import com.notepay.ui.component.GradientTopAppBar
import com.notepay.ui.feature.wallet.SupportedBank
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitScreen(
    onDebtorClick: (String) -> Unit,
    onFeedback: suspend (UiFeedback) -> Boolean,
    navigationBarOffset: Float = 0f,
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
    val vietqrContentDescription = stringResource(R.string.bill_split_vietqr_cd)
    val emptyAll = stringResource(R.string.bill_split_empty)
    val emptyUnpaid = stringResource(R.string.bill_split_empty_unpaid)
    val emptyPaid = stringResource(R.string.bill_split_empty_paid)
    val vietqrSavedFormat = stringResource(R.string.bill_split_vietqr_saved)
    val confirmDeleteTitle = stringResource(R.string.confirm_delete_bill_title)
    val confirmDeleteMessageFormat = stringResource(R.string.confirm_delete_permanent)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GradientTopAppBar(
                title = {
                    if (showPaidHistory) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showPaidHistory = false },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowBack,
                                    contentDescription = "Quay lại quản lý chia tiền",
                                )
                            }
                            Text("Đã thanh toán")
                        }
                    } else {
                        Text(title)
                    }
                },
                actions = {
                    if (!showPaidHistory) {
                        IconButton(
                            onClick = { showPaidHistory = true },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = "Xem các khoản đã thanh toán",
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .clip(AppTheme.shapes.corner8)
                                .clickable(
                                    enabled = state.activeWallet != null,
                                    onClick = {
                                        qrConfigWalletId = state.activeWallet?.id
                                    },
                                )
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_vietqr),
                                contentDescription = vietqrContentDescription,
                                modifier = Modifier
                                    .height(26.dp)
                                    .graphicsLayer(
                                        alpha = if (state.activeWallet != null) 1f else 0.38f,
                                    ),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val bottomPadding = padding.calculateBottomPadding() + 96.dp
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
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Quản lý chia tiền",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${unpaidGroups.size} người",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (unpaidGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyStateWithAction(
                                title = emptyTitle,
                                icon = Icons.Rounded.CallReceived,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                } else {
                    items(unpaidGroups, key = { it.key }) { (debtorName, splits) ->
                        val totalAmountCents = splits.sumOf { it.split.amount.amountInCents }
                        val amountStr = MoneyFormatter.format(Money(totalAmountCents)).replace("\u00A0", "").replace(" ", "").replace("₫", "đ")
                        DebtorGroupRow(
                            debtorName = debtorName,
                            totalAmountCents = totalAmountCents,
                            splitCount = splits.size,
                            onClick = { onDebtorClick(debtorName) },
                            onShare = {
                                val qrWallet = state.activeWallet
                                val qrCodeString = qrWallet?.let { wallet ->
                                    val bankBin = wallet.bankBin ?: return@let null
                                    val accountNumber = wallet.accountNumber ?: return@let null
                                    val memoCode = "NP " + debtorName.filter { it.isLetterOrDigit() || it.isWhitespace() }.trim().uppercase()
                                    VietQrGenerator.generate(
                                        bankBin = bankBin,
                                        accountNumber = accountNumber,
                                        amountCents = totalAmountCents,
                                        memo = memoCode
                                    )
                                }

                                if (qrCodeString != null && qrWallet != null) {
                                    val walletBankName = SupportedBank.LIST.find { it.bin == qrWallet.bankBin }?.name ?: "Ngân hàng liên kết"
                                    shareVietQrImage(
                                        context = context,
                                        qrCodeString = qrCodeString,
                                        bankName = walletBankName,
                                        accountNumber = qrWallet.accountNumber.orEmpty(),
                                        accountName = qrWallet.accountName.orEmpty()
                                    )
                                } else {
                                    val shareMessage = "Chào $debtorName, bạn có khoản cần thanh toán là $amountStr."
                                    val intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareMessage)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Chia sẻ nhanh"))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    qrConfigWallet?.let { wallet ->
        VietQrConfigSheet(
            wallet = wallet,
            onDismiss = { qrConfigWalletId = null },
            onSave = { bin, accountNumber, accountName ->
                viewModel.updateWalletForQr(
                    wallet.id,
                    bin,
                    accountNumber,
                    accountName,
                )
                qrConfigWalletId = null
                Toast.makeText(
                    context,
                    vietqrSavedFormat.format(wallet.name),
                    Toast.LENGTH_SHORT,
                ).show()
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
                modifier = Modifier.fillMaxSize(),
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
                text = "${paidSplits.size} khoản đã hoàn tất",
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
    modifier: Modifier = Modifier,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val cardBgColor = if (isLightTheme) Color.White else MaterialTheme.colorScheme.surfaceContainer

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor,
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
                    imageVector = Icons.Rounded.CallReceived,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tổng cần thu",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = MoneyFormatter.format(Money(totalAmountCents)).replace("\u00A0", "").replace(" ", "").replace("₫", "đ"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "$debtorCount người · $splitCount khoản cần đối soát",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    val totalCountFormat = stringResource(R.string.bill_split_total_count)
    val initial = debtorName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    val cardBgColor = if (!isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.surfaceContainer
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppTheme.shapes.corner16,
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor,
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
                    text = totalCountFormat.format(splitCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "Chia sẻ nhanh",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = amount.replace("\u00A0", "").replace(" ", "").replace("₫", "đ"),
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
}

@Composable
private fun PaidBillSplitRow(
    itemState: BillSplitItemState,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val split = itemState.split
    val note = itemState.parentTransaction?.note ?: "Hóa đơn chia tiền"
    val paidColor = MaterialTheme.colorScheme.tertiary

    val cardBgColor = if (!isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.surfaceContainer
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor,
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
                    text = "${split.debtorName} đã thanh toán",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$note · ${formatInstantDayMonth(split.paidAt)}",
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
                        contentDescription = "Xóa khoản đã thu của ${split.debtorName}",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private fun formatInstantDayMonth(instant: Instant?): String {
    if (instant == null) return ""
    val tz = TimeZone.currentSystemDefault()
    val localDateTime = instant.toLocalDateTime(tz)
    val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
    val month = localDateTime.monthNumber.toString().padStart(2, '0')
    return "$day/$month"
}

private fun shareVietQrImage(
    context: Context,
    qrCodeString: String,
    bankName: String,
    accountNumber: String,
    accountName: String
) {
    try {
        val bitmap = generateVietQrCardBitmap(context, qrCodeString, bankName, accountNumber, accountName)
        val cachePath = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(cachePath, "vietqr_payment.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ mã QR thanh toán"))
        }
    } catch (_: Exception) {
        Toast.makeText(context, "Lỗi khi khởi tạo và chia sẻ hình ảnh VietQR", Toast.LENGTH_LONG).show()
    }
}

private fun generateVietQrCardBitmap(
    context: Context,
    qrCodeString: String,
    bankName: String,
    accountNumber: String,
    accountName: String
): Bitmap {
    val width = 600
    val height = 720
    val bitmap = createBitmap(width, height)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint()

    paint.color = 0xFFFFFFFF.toInt()
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    val vietQrRaw = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.logo_vietqr)
    val targetVietQrHeight = 80f
    val vietQrScale = targetVietQrHeight / vietQrRaw.height
    val vietQrTargetWidth = vietQrRaw.width * vietQrScale
    val vietQrLeft = (width - vietQrTargetWidth) / 2f
    val vietQrDest = android.graphics.RectF(vietQrLeft, 40f, vietQrLeft + vietQrTargetWidth, 40f + targetVietQrHeight)
    canvas.drawBitmap(vietQrRaw, null, vietQrDest, paint)

    val qrSize = 380
    val qrLeft = (width - qrSize) / 2
    val qrTop = 130

    try {
        val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(qrCodeString, com.google.zxing.BarcodeFormat.QR_CODE, qrSize, qrSize)
        paint.color = 0xFF000000.toInt()
        val pixelSize = qrSize / bitMatrix.width
        val offsetLeft = qrLeft + (qrSize - pixelSize * bitMatrix.width) / 2
        val offsetTop = qrTop + (qrSize - pixelSize * bitMatrix.height) / 2
        for (x in 0 until bitMatrix.width) {
            for (y in 0 until bitMatrix.height) {
                if (bitMatrix.get(x, y)) {
                    canvas.drawRect(
                        (offsetLeft + x * pixelSize).toFloat(), (offsetTop + y * pixelSize).toFloat(),
                        (offsetLeft + (x + 1) * pixelSize).toFloat(), (offsetTop + (y + 1) * pixelSize).toFloat(), paint
                    )
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }

    val targetNapasHeight = 48f
    val targetBankLogoHeight = 64f
    val napasRaw = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.logo_napas)
    val napasScale = targetNapasHeight / napasRaw.height
    val napasTargetWidth = napasRaw.width * napasScale

    val bankLogoRes = getBankLogoRes(bankName)
    val bankRaw = bankLogoRes?.let { android.graphics.BitmapFactory.decodeResource(context.resources, it) }
    val bankTargetWidth = if (bankRaw != null) {
        val bankScale = targetBankLogoHeight / bankRaw.height
        bankRaw.width * bankScale
    } else {
        paint.textSize = 32f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.measureText(bankName.uppercase(Locale.ROOT))
    }

    val spacing = 20f
    val totalRowWidth = napasTargetWidth + spacing + 2f + spacing + bankTargetWidth
    var currentX = (width - totalRowWidth) / 2f

    val napasDest = android.graphics.RectF(currentX, 530f, currentX + napasTargetWidth, 530f + targetNapasHeight)
    canvas.drawBitmap(napasRaw, null, napasDest, paint)
    currentX += napasTargetWidth

    paint.color = 0xFFE2E8F0.toInt()
    paint.strokeWidth = 2f
    canvas.drawLine(currentX + spacing / 2, 532f, currentX + spacing / 2, 532f + 44f, paint)
    currentX += spacing

    if (bankRaw != null) {
        val bankDest = android.graphics.RectF(currentX, 522f, currentX + bankTargetWidth, 522f + targetBankLogoHeight)
        canvas.drawBitmap(bankRaw, null, bankDest, paint)
    } else {
        val upperBank = bankName.uppercase(Locale.ROOT)
        paint.color = 0xFF1B365D.toInt()
        paint.textSize = 32f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(upperBank, currentX, 565f, paint)
    }

    paint.color = 0xFF1E293B.toInt()
    paint.textSize = 34f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val nameText = accountName.uppercase(Locale.ROOT)
    val nameWidth = paint.measureText(nameText)
    canvas.drawText(nameText, (width - nameWidth) / 2f, 625f, paint)

    paint.color = 0xFF64748B.toInt()
    paint.textSize = 30f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    val numWidth = paint.measureText(accountNumber)
    canvas.drawText(accountNumber, (width - numWidth) / 2f, 670f, paint)

    return bitmap
}

private fun getBankLogoRes(bankName: String): Int? {
    val name = bankName.lowercase(Locale.ROOT)
    return when {
        name.contains("vib") -> R.drawable.logo_vib
        name.contains("vietcom") || name.contains("vcb") -> R.drawable.logo_vcb
        name.contains("tpbank") || name.contains("tp bank") || name.contains(" tiên phong") -> R.drawable.logo_tp
        name.contains("mb") || name.contains("quân đội") -> R.drawable.logo_mb
        name.contains("acb") || name.contains("á châu") -> R.drawable.logo_acb
        name.contains("agri") || name.contains("nông nghiệp") -> R.drawable.logo_agrbank
        name.contains("sacom") -> R.drawable.logo_sacom
        name.contains("vp") || name.contains("thịnh vượng") -> R.drawable.logo_vpbank
        name.contains("hd") -> R.drawable.logo_hdbank
        name.contains("timo") -> R.drawable.logo_timo
        name.contains("shb") -> R.drawable.logo_shb
        name.contains("scb") -> R.drawable.logo_scb
        name.contains("bao viet") || name.contains("baoviet") -> R.drawable.logo_baoviet
        else -> null
    }
}
