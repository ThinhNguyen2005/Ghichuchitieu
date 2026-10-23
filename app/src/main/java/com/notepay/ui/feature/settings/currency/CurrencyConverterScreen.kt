package com.notepay.ui.feature.settings.currency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(
    onBack: () -> Unit,
    viewModel: CurrencyConverterViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    val output = viewModel.convertedAmount()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Quy đổi tiền tệ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshRates() }, enabled = !state.isLoading) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Cập nhật tỷ giá")
                    }
                },
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Công cụ tham khảo độc lập. Số tiền trong ví và giao dịch không bị thay đổi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Số tiền gốc", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = state.amountVnd,
                        onValueChange = viewModel::setAmount,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Việt Nam đồng (VND)") },
                        suffix = { Text("₫") },
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Đổi sang", style = MaterialTheme.typography.labelLarge)
                        androidx.compose.material3.Box {
                            Button(onClick = { currencyMenuExpanded = true }) {
                                Text(state.targetCurrency)
                            }
                            DropdownMenu(
                                expanded = currencyMenuExpanded,
                                onDismissRequest = { currencyMenuExpanded = false },
                            ) {
                                CurrencyConverterViewModel.SUPPORTED_CURRENCIES.forEach { code ->
                                    DropdownMenuItem(
                                        text = { Text(code) },
                                        onClick = {
                                            viewModel.selectTargetCurrency(code)
                                            currencyMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.CurrencyExchange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Số tiền quy đổi", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = output?.let { formatCurrencyResult(it, state.targetCurrency) }
                            ?: if (state.isLoading) "Đang tải tỷ giá…" else "Chưa có kết quả",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (state.isLoading && state.rates == null) {
                        CircularProgressIndicator()
                    }
                    state.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    state.rates?.let { snapshot ->
                        Text(
                            "Tỷ giá tham khảo • cập nhật: ${snapshot.lastUpdatedUtc}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.rates == null && !state.isLoading) {
                        Button(onClick = { viewModel.refreshRates() }) { Text("Thử lại") }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Tỷ giá tham khảo từ ExchangeRate-API, không phải giá mua/bán của ngân hàng. Có thể dùng dữ liệu đã lưu khi ngoại tuyến.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatCurrencyResult(value: String, currencyCode: String): String {
    val amount = value.toBigDecimalOrNull() ?: return "—"
    val digits = if (currencyCode == "JPY" || currencyCode == "KRW") 0 else 2
    val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    val formatter = DecimalFormat(if (digits == 0) "#,##0" else "#,##0.##", symbols)
    formatter.roundingMode = java.math.RoundingMode.HALF_UP
    return "${formatter.format(amount)} $currencyCode"
}
