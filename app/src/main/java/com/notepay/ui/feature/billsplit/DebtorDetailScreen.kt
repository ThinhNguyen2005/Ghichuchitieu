package com.notepay.ui.feature.billsplit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.notepay.ui.feedback.UiFeedback
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Wallet
import com.notepay.ui.component.ConfirmDeleteDialog
import com.notepay.ui.component.categoryIcon
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.feature.wallet.SupportedBank
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.VietQrGenerator
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.datetime.TimeZone
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
            TopAppBar(
                title = { Text("Chi tiết nợ của $debtorName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Trở lại")
                    }
                }
            )
        },
        bottomBar = {
            if (unpaidDebtorSplits.isNotEmpty()) {
                Surface(
                    tonalElevation = 3.dp,
                    // P2-9: giảm shadowElevation 8dp → 1dp cho gọn, tránh cảm giác nặng nề.
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.markDebtorAsPaid(debtorName, unpaidDebtorSplits.map { it.split.id })
                                Toast.makeText(context, "Đã thu tiền nợ gộp thành công!", Toast.LENGTH_SHORT).show()
                                onBack()
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Payments, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Đã nhận tiền mặt", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        }
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
                    Text("Người này không có lịch sử nợ! ✨", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("Quay lại")
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero typography summary (Clean & Borderless)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (totalAmountCents > 0) "Tổng dư nợ hiện tại" else "Dư nợ hiện tại",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MoneyFormatter.format(Money(totalAmountCents)),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = if (totalAmountCents > 0) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                    )
                    Text(
                        text = if (totalAmountCents > 0) "${unpaidDebtorSplits.size} khoản nợ chưa thanh toán" else "Đã thanh toán hết nợ ✨",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (totalAmountCents > 0) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF4CAF50),
                        fontWeight = if (totalAmountCents > 0) FontWeight.Normal else FontWeight.Bold
                    )
                }
            }

            // VietQR Card
            item {
                if (qrCodeString != null && activeWallet != null && totalAmountCents > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        VietQrTemplateCard(
                            bankName = bankName,
                            accountNumber = activeWallet.accountNumber.orEmpty(),
                            accountName = activeWallet.accountName.orEmpty(),
                            qrCodeString = qrCodeString
                        )

                        Button(
                            onClick = {
                                shareVietQrImage(
                                    context = context,
                                    qrCodeString = qrCodeString,
                                    bankName = bankName,
                                    accountNumber = activeWallet.accountNumber.orEmpty(),
                                    accountName = activeWallet.accountName.orEmpty(),
                                    amountStr = MoneyFormatter.format(Money(totalAmountCents)),
                                    memo = memoCode
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Chia sẻ hình ảnh QR", fontWeight = FontWeight.SemiBold)
                        }

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
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Text("Chưa cấu hình VietQR cho ví hiện tại", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = "Vui lòng cấu hình Ngân hàng và Số tài khoản cho ví hoạt động hiện tại \"${activeWallet?.name ?: "Mặc định"}\" để sinh mã QR gộp tự động.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = { qrConfigWalletId = activeWallet?.id },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.QrCode2, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Cấu hình VietQR ngay")
                            }
                        }
                    }
                }
            }

            // List header
            item {
                Text(
                    text = "Lịch sử nợ chi tiết",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Unpaid & Paid splits list (Sao kê sổ nợ)
            items(allDebtorSplits, key = { it.split.id }) { item ->
                val parentTx = item.parentTransaction
                val note = parentTx?.note ?: "Giao dịch chia tiền"
                val isPaid = item.split.isPaid

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { /* No-op */ },
                            onLongClick = { pendingDeleteBill = item }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val category = parentTx?.category ?: Category.DEFAULT_EXPENSE
                        CategoryAvatar(category = category)

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isPaid) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFE8F5E9))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Đã trả",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            val mượnDate = formatInstant(item.split.createdAt)
                            val dateSubtext = if (isPaid) {
                                val trảDate = formatInstant(item.split.paidAt)
                                "Mượn: $mượnDate • Trả: $trảDate"
                            } else {
                                "Mượn: $mượnDate • Chờ thanh toán"
                            }
                            Text(
                                text = dateSubtext,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = MoneyFormatter.format(item.split.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaid) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // Config bottom sheet
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
}

@Composable
private fun VietQrLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.logo_vietqr),
        contentDescription = "VietQR Logo",
        modifier = modifier.height(44.dp)
    )
}

private fun getBankLogoRes(bankName: String): Int? {
    val name = bankName.lowercase(Locale.ROOT)
    return when {
        name.contains("vib") -> com.notepay.R.drawable.logo_vib
        name.contains("vietcom") || name.contains("vcb") -> com.notepay.R.drawable.logo_vcb
        name.contains("tpbank") || name.contains("tp bank") || name.contains(" tiên phong") -> com.notepay.R.drawable.logo_tp
        name.contains("mb") || name.contains("quân đội") -> com.notepay.R.drawable.logo_mb
        name.contains("acb") || name.contains("á châu") -> com.notepay.R.drawable.logo_acb
        name.contains("agri") || name.contains("nông nghiệp") -> com.notepay.R.drawable.logo_agrbank
        name.contains("sacom") -> com.notepay.R.drawable.logo_sacom
        name.contains("vp") || name.contains("thịnh vượng") -> com.notepay.R.drawable.logo_vpbank
        name.contains("hd") -> com.notepay.R.drawable.logo_hdbank
        name.contains("timo") -> com.notepay.R.drawable.logo_timo
        name.contains("shb") -> com.notepay.R.drawable.logo_shb
        name.contains("scb") -> com.notepay.R.drawable.logo_scb
        name.contains("bao viet") || name.contains("baoviet") -> com.notepay.R.drawable.logo_baoviet
        name.contains("viettel") -> com.notepay.R.drawable.logo_viettheomoney
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
    val bankColor = remember(bankName) { getBankColor(bankName) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_napas),
            contentDescription = "napas 247",
            modifier = Modifier.height(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(22.dp)
                .background(Color(0xFFE2E8F0))
        )
        Spacer(modifier = Modifier.width(12.dp))
        
        if (logoRes != null) {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = bankName,
                modifier = Modifier.height(32.dp)
            )
        } else {
            Text(
                text = bankName.uppercase(Locale.ROOT),
                color = bankColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. VietQR Logo
            VietQrLogo()

            // 2. QR Code Image (white container)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(8.dp)
            ) {
                QrCodeImage(content = qrCodeString)
            }

            // 3. napas 247 | BANK
            NapasBankRow(bankName = bankName)

            // 4. Account Details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = accountName.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = accountNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Thông tin chuyển khoản",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            CopyableDetailRow(
                label = "Ngân hàng",
                value = bankName.uppercase(Locale.ROOT),
                onCopy = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(bankName))
                    Toast.makeText(context, "Đã sao chép tên Ngân hàng", Toast.LENGTH_SHORT).show()
                }
            )

            CopyableDetailRow(
                label = "Số tài khoản",
                value = accountNumber,
                onCopy = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(accountNumber))
                    Toast.makeText(context, "Đã sao chép Số tài khoản", Toast.LENGTH_SHORT).show()
                }
            )

            CopyableDetailRow(
                label = "Tên chủ tài khoản",
                value = accountName.uppercase(Locale.ROOT),
                onCopy = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(accountName))
                    Toast.makeText(context, "Đã sao chép Tên chủ tài khoản", Toast.LENGTH_SHORT).show()
                }
            )

            CopyableDetailRow(
                label = "Số tiền",
                value = amountStr,
                valueColor = MaterialTheme.colorScheme.error,
                onCopy = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(amountRaw))
                    Toast.makeText(context, "Đã sao chép Số tiền: $amountRaw", Toast.LENGTH_SHORT).show()
                }
            )

            CopyableDetailRow(
                label = "Cú pháp chuyển khoản",
                value = memoCode,
                valueColor = MaterialTheme.colorScheme.primary,
                onCopy = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(memoCode))
                    Toast.makeText(context, "Đã sao chép Cú pháp chuyển khoản", Toast.LENGTH_SHORT).show()
                }
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Sao chép",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
private fun QrCodeImage(content: String, modifier: Modifier = Modifier) {
    val size = 200
    val bitMatrix = remember(content) {
        try {
            com.google.zxing.qrcode.QRCodeWriter().encode(
                content,
                com.google.zxing.BarcodeFormat.QR_CODE,
                size,
                size
            )
        } catch (e: Exception) {
            null
        }
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
                            topLeft = androidx.compose.ui.geometry.Offset(x * cellWidth, y * cellHeight),
                            size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight)
                        )
                    }
                }
            }
        }
    } else {
        Text("Không thể sinh mã QR")
    }
}

private fun generateVietQrCardBitmap(
    context: Context,
    qrCodeString: String,
    bankName: String,
    accountNumber: String,
    accountName: String,
    amountStr: String,
    memo: String
): Bitmap {
    val width = 600
    val height = 720
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint()

    // 1. Fill background (white)
    paint.color = 0xFFFFFFFF.toInt()
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    // 2. Draw VietQR logo centered
    val vietQrRaw = android.graphics.BitmapFactory.decodeResource(context.resources, com.notepay.R.drawable.logo_vietqr)
    val targetVietQrHeight = 80f
    val vietQrScale = targetVietQrHeight / vietQrRaw.height
    val vietQrTargetWidth = vietQrRaw.width * vietQrScale
    val vietQrLeft = (width - vietQrTargetWidth) / 2f
    val vietQrDest = android.graphics.RectF(vietQrLeft, 40f, vietQrLeft + vietQrTargetWidth, 40f + targetVietQrHeight)
    canvas.drawBitmap(vietQrRaw, null, vietQrDest, paint)

    // 3. Draw QR code centered
    val qrSize = 380
    val qrLeft = (width - qrSize) / 2
    val qrTop = 130

    try {
        val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(
            qrCodeString,
            com.google.zxing.BarcodeFormat.QR_CODE,
            qrSize,
            qrSize
        )
        paint.color = 0xFF000000.toInt()
        val pixelSize = qrSize / bitMatrix.width
        val offsetLeft = qrLeft + (qrSize - pixelSize * bitMatrix.width) / 2
        val offsetTop = qrTop + (qrSize - pixelSize * bitMatrix.height) / 2
        for (x in 0 until bitMatrix.width) {
            for (y in 0 until bitMatrix.height) {
                if (bitMatrix.get(x, y)) {
                    canvas.drawRect(
                        (offsetLeft + x * pixelSize).toFloat(),
                        (offsetTop + y * pixelSize).toFloat(),
                        (offsetLeft + (x + 1) * pixelSize).toFloat(),
                        (offsetTop + (y + 1) * pixelSize).toFloat(),
                        paint
                    )
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // 4. Draw napas 247 | BANK centered
    val targetNapasHeight = 48f
    val targetBankLogoHeight = 64f
    
    // Load Napas logo bitmap
    val napasRaw = android.graphics.BitmapFactory.decodeResource(context.resources, com.notepay.R.drawable.logo_napas)
    val napasScale = targetNapasHeight / napasRaw.height
    val napasTargetWidth = napasRaw.width * napasScale

    val bankLogoRes = getBankLogoRes(bankName)
    val bankRaw = bankLogoRes?.let {
        android.graphics.BitmapFactory.decodeResource(context.resources, it)
    }

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

    // 1. Draw Napas logo
    val napasDest = android.graphics.RectF(currentX, 530f, currentX + napasTargetWidth, 530f + targetNapasHeight)
    canvas.drawBitmap(napasRaw, null, napasDest, paint)
    currentX += napasTargetWidth

    // 2. Draw vertical divider
    paint.color = 0xFFE2E8F0.toInt()
    paint.strokeWidth = 2f
    canvas.drawLine(currentX + spacing / 2, 532f, currentX + spacing / 2, 532f + 44f, paint)
    currentX += spacing

    // 3. Draw Bank logo or Bank Name text
    if (bankRaw != null) {
        val bankDest = android.graphics.RectF(currentX, 522f, currentX + bankTargetWidth, 522f + targetBankLogoHeight)
        canvas.drawBitmap(bankRaw, null, bankDest, paint)
    } else {
        val upperBank = bankName.uppercase(Locale.ROOT)
        val bankColorInt = when {
            upperBank.contains("VIB") -> 0xFF0F3D8C.toInt()
            upperBank.contains("VIETCOMBANK") || upperBank.contains("VCB") -> 0xFF008A4E.toInt()
            upperBank.contains("TECHCOMBANK") || upperBank.contains("TCB") -> 0xFFE31837.toInt()
            upperBank.contains("TPBANK") || upperBank.contains("TPB") -> 0xFF5C287B.toInt()
            upperBank.contains("MB") -> 0xFF0054A6.toInt()
            upperBank.contains("BIDV") -> 0xFF008345.toInt()
            upperBank.contains("VIETINBANK") -> 0xFF0054A6.toInt()
            upperBank.contains("ACB") -> 0xFF0072BC.toInt()
            else -> 0xFF1B365D.toInt()
        }
        paint.color = bankColorInt
        paint.textSize = 32f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(upperBank, currentX, 565f, paint)
    }

    // 5. Draw Account Name and Number centered
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

private fun shareVietQrImage(
    context: Context,
    qrCodeString: String,
    bankName: String,
    accountNumber: String,
    accountName: String,
    amountStr: String,
    memo: String
) {
    try {
        val bitmap = generateVietQrCardBitmap(
            context,
            qrCodeString,
            bankName,
            accountNumber,
            accountName,
            amountStr,
            memo
        )
        val cachePath = File(context.cacheDir, "shared_images")
        cachePath.mkdirs()
        val file = File(cachePath, "vietqr_payment.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

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
    } catch (e: java.lang.Exception) {
        Toast.makeText(context, "Lỗi khi chia sẻ ảnh: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
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

