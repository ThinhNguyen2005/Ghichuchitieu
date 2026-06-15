package com.notepay.ui.feature.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.domain.model.TransactionType
import com.notepay.ui.component.categoryIcon
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.util.MoneyFormatter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onCreateBillSplit: (Long) -> Unit,
    onCreateSubscription: (name: String, amountCents: Long) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết giao dịch") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.transaction == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(state.error ?: "Không tìm thấy giao dịch")
                }
                else -> {
                    val tx = state.transaction!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(PaddingValues(16.dp)),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SummaryCard(transaction = tx)

                        if (state.isAutoCapture) {
                            AutoCaptureBanner()
                            Text(
                                "Giao dịch tự động chỉ cho phép sửa ghi chú.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FilledTonalButton(
                                onClick = { onEdit(tx.id) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Sửa ghi chú")
                            }
                        } else {
                            MetaCard(transaction = tx)
                            ActionsBlock(
                                onEdit = { onEdit(tx.id) },
                                onCreateBillSplit = { onCreateBillSplit(tx.id) },
                                onCreateSubscription = {
                                    val name = tx.note.ifBlank { tx.category.displayName }
                                    onCreateSubscription(name, tx.amount.amountInCents)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(transaction: com.notepay.domain.model.Transaction) {
    val isIncome = transaction.type == TransactionType.INCOME
    val sign = if (isIncome) "+" else "−"
    val amountColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryAvatar(
                    category = transaction.category,
                    size = 48.dp,
                    iconSize = 24.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transaction.category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (transaction.note.isNotBlank()) {
                        Text(
                            transaction.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                text = "$sign${MoneyFormatter.format(transaction.amount)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
        }
    }
}

@Composable
private fun MetaCard(transaction: com.notepay.domain.model.Transaction) {
    val tz = TimeZone.currentSystemDefault()
    val date = transaction.occurredAt.toLocalDateTime(tz)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Ngày: ${date.date} • ${date.hour.toString().padStart(2, '0')}:${date.minute.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Wallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Ví ID: ${transaction.walletId}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun AutoCaptureBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(
                "Giao dịch tự động từ ngân hàng",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ActionsBlock(
    onEdit: () -> Unit,
    onCreateBillSplit: () -> Unit,
    onCreateSubscription: () -> Unit,
) {
    Text(
        "Hành động",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    FilledTonalButton(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Icon(Icons.Rounded.Edit, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Sửa giao dịch")
    }
    FilledTonalButton(
        onClick = onCreateBillSplit,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Icon(Icons.Rounded.CallSplit, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Tạo chia tiền")
    }
    FilledTonalButton(
        onClick = onCreateSubscription,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Tạo nhắc nhở gia hạn")
    }
}
