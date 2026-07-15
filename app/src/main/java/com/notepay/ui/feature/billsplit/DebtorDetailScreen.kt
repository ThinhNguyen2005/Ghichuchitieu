package com.notepay.ui.feature.billsplit

import com.notepay.ui.theme.AppTheme

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
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
import com.notepay.ui.feature.wallet.SupportedBank
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.VietQrGenerator
import java.io.File
import java.io.FileOutputStream
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
    var qrConfigWalletId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteBill by remember { mutableStateOf<BillSplitItemState?>(null) }
    var showReconciliationSheet by remember { mutableStateOf(false) }

    val qrConfigWallet = qrConfigWalletId?.let { id ->
        state.wallets.find { it.id == id }
    }

    val bankName = remember(activeWallet) {
        val matchedBank = SupportedBank.LIST.find { it.bin == activeWallet?.bankBin }
        matchedBank?.name ?: "Ngân hàng liên kết"
    }

    val memoCode = remember(debtorName) {
        val sanitized = debtorName.filter { it.isLetterOrDigit() || it.isWhitespace() }.trim().uppercase()
        "NP $sanitized"
    }

    val qrCodeString = remember(activeWallet, totalAmountCents, memoCode) {
        if (activeWallet?.bankBin != null && activeWallet.accountNumber != null) {
            VietQrGenerator.generate(
                bankBin = activeWallet.bankBin,
                accountNumber = activeWallet.accountNumber,
                amountCents = totalAmountCents,
                memo = memoCode
            )
        } else null
    }

    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text("Nợ của $debtorName", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Trở lại")
                    }
                }
            )
        },
        bottomBar = {
            if (unpaidDebtorSplits.isNotEmpty()) {
                GradientBottomActionBar {
                    LiquidButton(
                        onClick = { showReconciliationSheet = true },
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Xác nhận đã thanh toán", fontWeight = FontWeight.Bold)
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
                    Text("Người này không có lịch sử nợ!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack, shape = AppTheme.shapes.corner12) { Text("Quay lại") }
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
                if (qrCodeString != null && activeWallet != null && totalAmountCents > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        VietQrTemplateCard(
                            bankName = bankName,
                            accountNumber = activeWallet.accountNumber.orEmpty(),
                            accountName = activeWallet.accountName.orEmpty(),
                            qrCodeString = qrCodeString
                        )

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
                                Text("Chưa cấu hình VietQR cho ví hiện tại", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Vui lòng cấu hình Ngân hàng và Số tài khoản cho ví hoạt động hiện tại \"${activeWallet?.name ?: "Mặc định"}\" để sinh mã QR gộp tự động.",
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
                                Text("Cấu hình VietQR ngay", fontWeight = FontWeight.Bold)
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
                        Text(text = "Lịch sử đối soát", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${unpaidDebtorSplits.size} cần thu · ${paidDebtorSplits.size} đã thanh toán",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(text = "Nhấn giữ để xóa", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items(allDebtorSplits, key = { it.split.id }) { item ->
                val parentTx = item.parentTransaction
                val note = parentTx?.note ?: "Giao dịch chia tiền"
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
                                            text = "Đã trả", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
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
                                            text = "Chờ thu", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            val muonDate = formatInstant(item.split.createdAt)
                            val dateSubtext = if (isPaid) {
                                val tradate = formatInstant(item.split.paidAt)
                                "Mượn: $muonDate • Trả: $tradate"
                            } else {
                                "Mượn: $muonDate • Chờ thanh toán"
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
        VietQrConfigSheet(
            wallet = wallet,
            onDismiss = { qrConfigWalletId = null },
            onSave = { bin, accNum, accName ->
                viewModel.updateWalletForQr(wallet.id, bin, accNum, accName)
                qrConfigWalletId = null
                Toast.makeText(context, "Đã cấu hình VietQR thành công!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    pendingDeleteBill?.let { item ->
        ConfirmDeleteDialog(
            title = "Xóa khoản nợ?",
            itemName = "${item.split.debtorName} • ${MoneyFormatter.format(item.split.amount)}",
            message = "Khoản nợ lẻ này sẽ bị xóa vĩnh viễn và không thể khôi phục.",
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
                        text = if (isSettled) "Đã đối soát với $debtorName" else "Khoản cần thu từ $debtorName",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isSettled) "Không còn khoản chờ thu" else "$unpaidCount khoản đang chờ thanh toán",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = AppTheme.shapes.corner8,
                    color = accent.copy(alpha = .12f),
                ) {
                    Text(
                        text = if (isSettled) "Hoàn tất" else "Cần thu",
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

@Composable
private fun VietQrLogo(modifier: Modifier = Modifier) {
    Image(painter = painterResource(id = R.drawable.logo_vietqr), contentDescription = "VietQR Logo", modifier = modifier.height(44.dp))
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
        name.contains("viettel") -> R.drawable.logo_viettheomoney
        else -> null
    }
}

private fun getBankColor(bankName: String): Color {
    val name = bankName.uppercase(Locale.ROOT)
    return when {
        name.contains("VIB") -> Color(0xFF0F3D8C)
        name.contains("VIETCOMBANK") || name.contains("VCB") -> Color(0xFF008A4E)
        name.contains("TECHCOMBANK") || name.contains("TCB") -> Color(0xFFE31837)
        name.contains("TPBANK") || name.contains("TPB") -> Color(0xFF5C287B)
        name.contains("MB") -> Color(0xFF0054A6)
        name.contains("BIDV") -> Color(0xFF008345)
        name.contains("VIETINBANK") -> Color(0xFF0054A6)
        name.contains("ACB") -> Color(0xFF0072BC)
        else -> Color(0xFF1B365D)
    }
}

@Composable
private fun NapasBankRow(bankName: String, modifier: Modifier = Modifier) {
    val logoRes = remember(bankName) { getBankLogoRes(bankName) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Image(painter = painterResource(id = R.drawable.logo_napas), contentDescription = "napas 247", modifier = Modifier.height(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Spacer(modifier = Modifier.width(12.dp))
        if (logoRes != null) {
            Image(painter = painterResource(id = logoRes), contentDescription = bankName, modifier = Modifier.height(32.dp))
        } else {
            // ĐÃ SỬA: Gọi trực tiếp hàm getBankColor inline loại bỏ cảnh báo Assigned value is never read
            Text(text = bankName.uppercase(Locale.ROOT), color = getBankColor(bankName), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VietQrTemplateCard(
    bankName: String,
    accountNumber: String,
    accountName: String,
    qrCodeString: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            VietQrLogo()

            Box(
                modifier = Modifier
                    .clip(AppTheme.shapes.corner16)
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                QrCodeImage(content = qrCodeString)
            }

            NapasBankRow(bankName = bankName)

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = accountName.uppercase(Locale.ROOT), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = accountNumber, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Thông tin chuyển khoản", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            CopyableDetailRow(label = "Ngân hàng", value = bankName.uppercase(Locale.ROOT), onCopy = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(bankName))
                Toast.makeText(context, "Đã sao chép tên Ngân hàng", Toast.LENGTH_SHORT).show()
            })

            CopyableDetailRow(label = "Số tài khoản", value = accountNumber, onCopy = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(accountNumber))
                Toast.makeText(context, "Đã sao chép Số tài khoản", Toast.LENGTH_SHORT).show()
            })

            CopyableDetailRow(label = "Tên chủ tài khoản", value = accountName.uppercase(Locale.ROOT), onCopy = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(accountName))
                Toast.makeText(context, "Đã sao chép Tên chủ tài khoản", Toast.LENGTH_SHORT).show()
            })

            CopyableDetailRow(label = "Số tiền", value = amountStr, valueColor = MaterialTheme.colorScheme.error, onCopy = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(amountRaw))
                Toast.makeText(context, "Đã sao chép Số tiền: $amountRaw", Toast.LENGTH_SHORT).show()
            })

            CopyableDetailRow(label = "Cú pháp chuyển khoản", value = memoCode, valueColor = MaterialTheme.colorScheme.primary, onCopy = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(memoCode))
                Toast.makeText(context, "Đã sao chép Cú pháp chuyển khoản", Toast.LENGTH_SHORT).show()
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
            Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = "Sao chép", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun QrCodeImage(content: String, modifier: Modifier = Modifier) {
    val size = 200
    val bitMatrix = remember(content) {
        try {
            com.google.zxing.qrcode.QRCodeWriter().encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
        } catch (e: Exception) { null }
    }

    if (bitMatrix != null) {
        Canvas(modifier = modifier.size(200.dp)) {
            val width = bitMatrix.width
            val height = bitMatrix.height
            val cellWidth = 200.dp.toPx() / width
            val cellHeight = 200.dp.toPx() / height

            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (bitMatrix.get(x, y)) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(x * cellWidth, y * cellHeight),
                            size = Size(cellWidth, cellHeight)
                        )
                    }
                }
            }
        }
    } else {
        Text("Không thể sinh mã QR")
    }
}

// ĐÃ SỬA: Loại bỏ các tham số thừa amountStr và memo để giải quyết triệt để cảnh báo Parameter never used
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

// ĐÃ SỬA: Đồng bộ chữ ký hàm loại bỏ tham số dư thừa để khớp logic sạch warnings
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
        // ĐÃ SỬA: Dùng toán tử gạch dưới "_" đại diện cho Exception không dùng đến trong Catch Block
    } catch (_: Exception) {
        Toast.makeText(context, "Lỗi khi khởi tạo và chia sẻ hình ảnh VietQR", Toast.LENGTH_LONG).show()
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
