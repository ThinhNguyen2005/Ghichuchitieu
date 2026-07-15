package com.notepay.ui.feature.billsplit

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.notepay.domain.model.Wallet
import com.notepay.ui.feature.wallet.SupportedBank
import com.notepay.ui.theme.AppTheme
import java.text.Normalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VietQrConfigSheet(
    wallet: Wallet,
    onDismiss: () -> Unit,
    onSave: (bankBin: String, accountNumber: String, accountName: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val realBanks = remember { SupportedBank.LIST.filter { it.bin != null } }

    val context = LocalContext.current
    val localView = LocalView.current
    
    var selectedBank by remember {
        mutableStateOf(realBanks.find { it.bin == wallet.bankBin } ?: realBanks.first())
    }
    var accountNumber by remember { mutableStateOf(wallet.accountNumber.orEmpty()) }
    var accountName by remember { mutableStateOf(wallet.accountName.orEmpty()) }
    var isPickingBank by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val playHaptic = {
        try {
            localView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        AnimatedContent(
            targetState = isPickingBank,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { width -> width } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                }
            },
            label = "vietqr-sheet-flow"
        ) { picking ->
            if (picking) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { 
                            playHaptic()
                            isPickingBank = false 
                        }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Quay lại")
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Chọn ngân hàng",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Tìm kiếm ngân hàng…") },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Xóa")
                                }
                            }
                        },
                        singleLine = true,
                        shape = AppTheme.shapes.corner16,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = if (!isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = if (!isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val filteredBanks = remember(searchQuery) {
                        realBanks.filter {
                            it.name.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                    ) {
                        items(filteredBanks, key = { it.packageName }) { bank ->
                            BankRow(
                                bank = bank,
                                isSelected = bank.bin == selectedBank.bin,
                                onClick = {
                                    playHaptic()
                                    selectedBank = bank
                                    isPickingBank = false
                                },
                            )
                        }
                        if (filteredBanks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Không tìm thấy ngân hàng",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.QrCode2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Cấu hình VietQR",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Ví: ${wallet.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Đóng")
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppTheme.shapes.corner16,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Mã QR nhận tiền sẽ được tạo tự động khi chia hóa đơn. Thông tin tài khoản cần chính xác để hiển thị đúng mã chuyển tiền.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Text(
                        "Ngân hàng nhận",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val selectedBankIcon = remember(selectedBank.packageName) {
                        try {
                            context.packageManager.getApplicationIcon(selectedBank.packageName).toBitmap().asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                playHaptic()
                                isPickingBank = true 
                            },
                        shape = AppTheme.shapes.corner16,
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedBankIcon != null) {
                                Image(
                                    bitmap = selectedBankIcon,
                                    contentDescription = selectedBank.name,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(AppTheme.shapes.corner8)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(AppTheme.shapes.circle)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.AccountBalance,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedBank.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Icon(
                                Icons.Rounded.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it.filter(Char::isDigit) },
                        label = { Text("Số tài khoản nhận") },
                        placeholder = { Text("VD: 0123456789") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = AppTheme.shapes.corner16,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            focusedContainerColor = if (!isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = if (!isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = stripAccents(it) },
                        label = { Text("Tên chủ tài khoản") },
                        placeholder = { Text("NGUYEN VAN A") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        supportingText = {
                            Text(
                                "Nhập tiếng Việt in hoa, không dấu (khớp với tài khoản ngân hàng)",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        },
                        shape = AppTheme.shapes.corner16,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            focusedContainerColor = if (!isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = if (!isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            playHaptic()
                            val bin = selectedBank.bin
                            if (bin != null && accountNumber.isNotBlank() && accountName.isNotBlank()) {
                                onSave(bin, accountNumber, accountName)
                            }
                        },
                        enabled = accountNumber.isNotBlank() && accountName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = AppTheme.shapes.corner16,
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Lưu cấu hình", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BankRow(
    bank: SupportedBank,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val appIcon = remember(bank.packageName) {
        try {
            context.packageManager.getApplicationIcon(bank.packageName).toBitmap().asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        if (!isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppTheme.shapes.corner12,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = bank.name,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(AppTheme.shapes.corner8)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(AppTheme.shapes.circle)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Text(
                bank.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun stripAccents(input: String): String {
    val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
    val result = normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .replace("Đ", "D")
        .replace("đ", "d")
    return result.filter { it.isLetter() || it.isWhitespace() }.uppercase()
}
