package com.notepay.ui.feature.billsplit

import com.notepay.ui.theme.AppTheme

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.ui.util.MoneyFormatter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentReconciliationSheet(
    debtorName: String,
    totalDebt: Money,
    recentTransactions: List<Transaction>,
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onConfirm: (incomeTxId: Long?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var paymentMethod by remember { mutableStateOf("cash") } // "cash" or "transfer"
    var selectedIncomeTxId by remember { mutableStateOf<Long?>(null) }

    val incomeTransactions by remember(recentTransactions) {
        derivedStateOf {
            recentTransactions
                .filter { it.type == TransactionType.INCOME }
                .sortedByDescending { it.occurredAt }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Giữ dragHandle chuẩn M3 để người dùng vuốt xuống đóng sheet tự nhiên
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow // Đồng bộ token nền cao cấp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Thêm animateContentSize giúp Bottom Sheet co giãn chiều cao cực kỳ êm ái khi đổi tab
                .animateContentSize(animationSpec = tween(durationMillis = 250))
                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header vùng thông tin dư nợ
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Thu tiền nợ từ $debtorName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tổng dư nợ: ${MoneyFormatter.format(totalDebt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error, // Dùng màu Error chuẩn hệ thống
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.offset(x = 12.dp, y = (-8).dp) // Đẩy sát góc gọn gàng
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Đóng")
                }
            }

            // Thẻ lựa chọn Phương thức thanh toán (Cash / Transfer)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = AppTheme.shapes.corner20,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    // Lựa chọn 1: Tiền mặt
                    val isCash = paymentMethod == "cash"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCash) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable {
                                paymentMethod = "cash"
                                selectedIncomeTxId = null
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = isCash,
                            onClick = {
                                paymentMethod = "cash"
                                selectedIncomeTxId = null
                            }
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(AppTheme.shapes.circle)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nhận bằng Tiền mặt",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Giảm trực tiếp chi tiêu gốc, không đối soát giao dịch.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Lựa chọn 2: Chuyển khoản (Có đối soát)
                    val isTransfer = paymentMethod == "transfer"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isTransfer) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { paymentMethod = "transfer" }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = isTransfer,
                            onClick = { paymentMethod = "transfer" }
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(AppTheme.shapes.circle)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.billsplit_receive_by_transfer),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Đối soát và tự động liên kết với biến động số dư từ ngân hàng.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Phân vùng hiển thị danh sách giao dịch Ngân hàng (Chỉ bung ra khi chọn Chuyển khoản)
            if (paymentMethod == "transfer") {
                Text(
                    text = "Chọn giao dịch ngân hàng khớp đối soát:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                if (incomeTransactions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppTheme.shapes.corner16,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Không tìm thấy giao dịch thu nhập gần đây",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp), // Dùng heightIn linh hoạt thay vì ép cứng chiều cao
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(incomeTransactions, key = { it.id }) { tx ->
                            val isSelected = selectedIncomeTxId == tx.id
                            val walletName = wallets.find { it.id == tx.walletId }?.name ?: "Ví khác"

                            // Phối màu nền bám theo Token Theme khi item được chọn
                            val cardBgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerHigh
                            val borderStroke = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedIncomeTxId = tx.id },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                border = borderStroke
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val noteText = tx.note.trim()
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(AppTheme.shapes.circle)
                                            .background(
                                                if (tx.isAutoCapture) MaterialTheme.colorScheme.tertiaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (tx.isAutoCapture) Icons.Rounded.Smartphone else Icons.Rounded.AccountBalance,
                                            contentDescription = null,
                                            tint = if (tx.isAutoCapture) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.category.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (noteText.isNotBlank()) {
                                            Text(
                                                text = noteText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "$walletName • ${formatInstant(tx.occurredAt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "+${MoneyFormatter.format(tx.amount)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary // Màu chủ đạo đại diện cho thu nhập dương
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = "Đã chọn",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Nút kích hoạt hành động (Sticky Bottom Action)
            val isEnabled = paymentMethod == "cash" || selectedIncomeTxId != null
            Button(
                onClick = { onConfirm(selectedIncomeTxId) },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = AppTheme.shapes.corner16,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (paymentMethod == "cash") Icons.Rounded.Check else Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (paymentMethod == "cash") "Ghi nhận thu tiền mặt" else "Xác nhận đối soát & khớp số dư",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatInstant(instant: kotlin.time.Instant): String {
    val tz = TimeZone.currentSystemDefault()
    val localDateTime = instant.toLocalDateTime(tz)
    return String.format(
        Locale.US,
        "%02d/%02d %02d:%02d",
        localDateTime.day,
        localDateTime.month.number,
        localDateTime.hour,
        localDateTime.minute
    )
}
