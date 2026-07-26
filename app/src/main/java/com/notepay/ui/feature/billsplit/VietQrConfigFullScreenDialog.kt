package com.notepay.ui.feature.billsplit

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.notepay.R
import com.notepay.domain.model.VietQrBank
import com.notepay.domain.model.Wallet
import com.notepay.ui.component.FirefliesBackground
import com.notepay.ui.theme.AppTheme
import java.text.Normalizer

private enum class VietQrStep { SelectBank, EnterAccount }

@Composable
fun VietQrConfigFullScreenDialog(
    wallet: Wallet,
    banks: List<VietQrBank>,
    onDismiss: () -> Unit,
    onSave: (bankBin: String, accountNumber: String, accountName: String) -> Unit,
) {
    var step by remember(wallet.bankBin) {
        mutableStateOf(if (wallet.bankBin.isNullOrBlank()) VietQrStep.SelectBank else VietQrStep.EnterAccount)
    }
    var selectedBank by remember(banks, wallet.bankBin) { mutableStateOf(banks.find { it.bin == wallet.bankBin }) }
    var accountNumber by remember(wallet.accountNumber) { mutableStateOf(wallet.accountNumber.orEmpty()) }
    var accountName by remember(wallet.accountName) { mutableStateOf(wallet.accountName.orEmpty()) }
    val view = LocalView.current
    val playHaptic = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            FirefliesBackground(Modifier.fillMaxSize())
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp),
            ) {
                VietQrDialogHeader(
                    step = step,
                    onBack = { playHaptic(); step = VietQrStep.SelectBank },
                    onDismiss = onDismiss,
                )
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState == VietQrStep.EnterAccount) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }
                    },
                    label = "vietqr-fullscreen-flow",
                ) { currentStep ->
                    when (currentStep) {
                        VietQrStep.SelectBank -> BankSelectionStep(
                            banks = banks,
                            selectedBank = selectedBank,
                            onBankSelected = { bank -> playHaptic(); selectedBank = bank; step = VietQrStep.EnterAccount },
                        )
                        VietQrStep.EnterAccount -> AccountDetailsStep(
                            walletName = wallet.name,
                            bank = selectedBank,
                            accountNumber = accountNumber,
                            onAccountNumberChange = { accountNumber = it.filter(Char::isDigit) },
                            accountName = accountName,
                            onAccountNameChange = { accountName = normalizeAccountName(it) },
                            onChangeBank = { playHaptic(); step = VietQrStep.SelectBank },
                            onSave = {
                                val bank = selectedBank ?: return@AccountDetailsStep
                                playHaptic()
                                onSave(bank.bin, accountNumber, accountName)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VietQrDialogHeader(step: VietQrStep, onBack: () -> Unit, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (step == VietQrStep.EnterAccount) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Chọn lại ngân hàng")
            }
        } else Spacer(Modifier.width(48.dp))
        StepIndicator(step, Modifier.weight(1f))
        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Đóng cấu hình VietQR")
        }
    }
}

@Composable
private fun StepIndicator(step: VietQrStep, modifier: Modifier = Modifier) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.outlineVariant
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        StepDot("1", "Ngân hàng", true, active)
        Box(Modifier.weight(1f).height(2.dp).background(if (step == VietQrStep.EnterAccount) active else inactive))
        StepDot("2", "Tài khoản", step == VietQrStep.EnterAccount, active)
    }
}

@Composable
private fun StepDot(number: String, label: String, active: Boolean, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(24.dp).clip(AppTheme.shapes.circle)
                .background(if (active) color else MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, style = MaterialTheme.typography.labelSmall, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun BankSelectionStep(banks: List<VietQrBank>, selectedBank: VietQrBank?, onBankSelected: (VietQrBank) -> Unit) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = remember(query) { normalizeSearch(query) }
    val filteredBanks = remember(banks, normalizedQuery) {
        if (normalizedQuery.isBlank()) banks else banks.filter { bank ->
            normalizeSearch(bank.shortName).contains(normalizedQuery) ||
                normalizeSearch(bank.name).contains(normalizedQuery) ||
                normalizeSearch(bank.code).contains(normalizedQuery)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Text("Chọn ngân hàng nhận tiền", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Chọn ngân hàng để tiếp tục cấu hình VietQR.", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            label = { Text("Tìm tên hoặc mã ngân hàng") },
            singleLine = true,
            shape = AppTheme.shapes.corner16,
        )
        if (filteredBanks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Không tìm thấy ngân hàng phù hợp.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp),
        ) {
            items(filteredBanks, key = { it.bin }) { bank ->
                BankOption(bank, bank.bin == selectedBank?.bin) { onBankSelected(bank) }
            }
        }
    }
}

@Composable
private fun BankOption(bank: VietQrBank, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(68.dp).clickable(onClick = onClick),
        shape = AppTheme.shapes.corner16,
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f) else MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BankLogo(bank.logoUrl, Modifier.size(40.dp))
            Column(Modifier.weight(1f)) {
                Text(bank.shortName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(bank.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AccountDetailsStep(
    walletName: String,
    bank: VietQrBank?,
    accountNumber: String,
    onAccountNumberChange: (String) -> Unit,
    accountName: String,
    onAccountNameChange: (String) -> Unit,
    onChangeBank: () -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Thông tin nhận tiền", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Ví: $walletName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onChangeBank),
            shape = AppTheme.shapes.corner16,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BankLogo(bank?.logoUrl, Modifier.size(40.dp))
                Column(Modifier.weight(1f)) {
                    Text(bank?.shortName ?: "Chưa chọn ngân hàng", fontWeight = FontWeight.Bold)
                    Text(bank?.name.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text("Đổi", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
        OutlinedTextField(
            value = accountNumber,
            onValueChange = onAccountNumberChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Số tài khoản nhận") },
            leadingIcon = { Icon(Icons.Rounded.CreditCard, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = AppTheme.shapes.corner16,
        )
        OutlinedTextField(
            value = accountName,
            onValueChange = onAccountNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.transfer_account_name)) },
            supportingText = { Text("Tên được chuẩn hóa thành CHỮ IN HOA không dấu.") },
            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
            singleLine = true,
            shape = AppTheme.shapes.corner16,
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onSave,
            enabled = bank != null && accountNumber.isNotBlank() && accountName.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = AppTheme.shapes.corner16,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Rounded.QrCode2, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Lưu cấu hình VietQR", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BankLogo(logoUrl: String?, modifier: Modifier = Modifier) {
    if (logoUrl.isNullOrBlank()) {
        Box(modifier.clip(AppTheme.shapes.corner12).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    } else AsyncImage(model = logoUrl, contentDescription = null, modifier = modifier.clip(AppTheme.shapes.corner12))
}

private fun normalizeSearch(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    .replace('đ', 'd')
    .replace('Đ', 'D')
    .lowercase()

private fun normalizeAccountName(value: String): String = normalizeSearch(value)
    .filter { it.isLetter() || it.isWhitespace() }
    .uppercase()
