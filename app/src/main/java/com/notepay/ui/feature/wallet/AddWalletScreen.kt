package com.notepay.ui.feature.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.horizontalScroll
import com.notepay.ui.util.WalletUiHelper
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.ui.feedback.FeedbackType
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.util.VietnamCurrencyVisualTransformation
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletScreen(
    onSaved: suspend (UiFeedback) -> Unit,
    onBack: () -> Unit,
    onFeedback: suspend (UiFeedback) -> Boolean,
    viewModel: AddWalletViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currencyTransformation = remember { VietnamCurrencyVisualTransformation() }

    LaunchedEffect(Unit) {
        viewModel.feedback.collect { feedback ->
            if (feedback.type == FeedbackType.Success) {
                onSaved(feedback)
            } else {
                onFeedback(feedback)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Chỉnh sửa ví" else "Thêm ví mới") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Trở lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Tên ví") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.initialBalanceInput,
                onValueChange = viewModel::onInitialBalanceChanged,
                label = { Text("Số dư ban đầu") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = currencyTransformation,
                singleLine = true
            )

            // Budget Cap
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = "Đặt hạn mức cảnh báo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Cảnh báo khi tiêu lố tay ngân sách",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = state.hasBudgetLimit,
                    onCheckedChange = viewModel::onHasBudgetLimitChanged
                )
            }

            if (state.hasBudgetLimit) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Period Selector (Day, Week, Month)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val periods = listOf(
                            BudgetPeriod.DAILY to "Hàng ngày",
                            BudgetPeriod.WEEKLY to "Hàng tuần",
                            BudgetPeriod.MONTHLY to "Hàng tháng"
                        )
                        periods.forEach { (period, label) ->
                            val isSelected = state.budgetPeriod == period
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { viewModel.onBudgetPeriodChanged(period) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.budgetLimitInput,
                        onValueChange = viewModel::onBudgetLimitChanged,
                        label = { Text("Số tiền hạn mức") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = currencyTransformation,
                        singleLine = true
                    )

                    // Converted monthly amount display
                    val rawLimit = state.budgetLimitInput.toLongOrNull() ?: 0L
                    if (rawLimit > 0L) {
                        val monthlyEquivalent = when (state.budgetPeriod) {
                            BudgetPeriod.DAILY -> rawLimit * 30
                            BudgetPeriod.WEEKLY -> rawLimit * 4
                            BudgetPeriod.MONTHLY -> rawLimit
                        }
                        val formattedMonthly = remember(monthlyEquivalent) {
                            val formatter = java.text.DecimalFormat("#,###")
                            formatter.format(monthlyEquivalent).replace(",", ".")
                        }
                        Text(
                            text = "Quy đổi sang hàng tháng để cảnh báo: $formattedMonthly đ/tháng",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // Chọn Icon
            Text(
                text = "Biểu tượng ví",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WalletUiHelper.iconList.forEach { (key, vector, label) ->
                    val isSelected = state.iconKey == key
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { viewModel.onIconChanged(key) }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Chọn Màu sắc
            Text(
                text = "Màu sắc nhận diện",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WalletUiHelper.colorList.forEach { (key, colorValue) ->
                    val isSelected = state.colorKey == key
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colorValue)
                            .clickable { viewModel.onColorChanged(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            // Lưu ý: phần Liên kết Ngân hàng (VietQR) đã được chuyển sang nơi khác
            // — bấm icon QR ở top-bar trang Chia tiền để cấu hình nhanh.
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Cấu hình VietQR sau",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Sau khi tạo ví, vào Chia tiền → icon QR trên thanh trên cùng để cấu hình ngân hàng + STK + tên chủ TK nhanh chóng.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = { viewModel.save() },
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (state.isSaving) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Đang lưu...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else {
                    Text(if (state.isEditMode) "Lưu thay đổi" else "Tạo ví", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class SupportedBank(
    val name: String,
    val packageName: String,
    val bin: String?
) {
    companion object {
        val LIST = listOf(
            SupportedBank("Không liên kết", "", null),
            SupportedBank("TPBank", "com.tpb.mb.gprsandroid", "970423"),
            SupportedBank("Vietcombank", "com.VCB", "970436"),
            SupportedBank("Techcombank", "com.technologies.tcb", "970407"),
            SupportedBank("MB Bank", "com.mbmobile", "970422"),
            SupportedBank("BIDV", "com.bidv.smartbanking", "970418"),
            SupportedBank("VietinBank", "com.vietinbank.ipay", "970415"),
            SupportedBank("ACB", "vn.com.acb.mbanking", "970416"),
            SupportedBank("VIB", "vn.com.vib.vibmobile", "970441"),
            SupportedBank("Agribank", "com.vnpay.Agribank3g", "970405"),
            SupportedBank("Sacombank", "com.sacombank.mbanking", "970403"),
            SupportedBank("VPBank", "com.vpbank.neo", "970432"),
            SupportedBank("HDBank", "vn.com.hdbank.smartbanking", "970420"),
            SupportedBank("Timo (BVBank)", "vn.timo.digitalbank", "970454"),
            SupportedBank("SHB", "com.shb.mobile", "970443"),
            SupportedBank("SCB", "com.scb.mobile", "970429"),
            SupportedBank("BaoViet Bank", "com.baovietbank.bvmobile", "970438"),
            SupportedBank("Momo (Ví)", "com.mservice.momotransfer", null)
        )
    }
}
