package com.notepay.ui.feature.transaction.detail

import com.notepay.ui.theme.AppTheme

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
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.model.TransactionType
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.component.GradientTopAppBar
import com.notepay.ui.util.MoneyFormatter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.rounded.Person

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
            GradientTopAppBar(
                title = { Text(stringResource(R.string.transaction_details_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.transaction == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(state.error ?: stringResource(R.string.error_transaction_not_found))
                }
                else -> {
                    val tx = state.transaction!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = 16.dp,
                                top = padding.calculateTopPadding() + 16.dp,
                                end = 16.dp,
                                bottom = padding.calculateBottomPadding() + 24.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        TransactionHeader(transaction = tx)
                        
                        MetaCard(transaction = tx, walletName = state.walletName)

                        if (state.isAutoCapture) {
                            val isLightTheme = !androidx.compose.foundation.isSystemInDarkTheme()
                            val cardBg = if (isLightTheme) Color.White else MaterialTheme.colorScheme.surfaceContainer

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBg,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                shape = AppTheme.shapes.corner24,
                            ) {
                                ActionRow(
                                    icon = Icons.Rounded.Edit,
                                    title = stringResource(R.string.detail_edit_note_title),
                                    onClick = { onEdit(tx.id) }
                                )
                            }
                            
                            Text(
                                stringResource(R.string.detail_auto_edit_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        } else {
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
private fun TransactionHeader(transaction: com.notepay.domain.model.Transaction) {
    val isIncome = transaction.type == TransactionType.INCOME
    val sign = if (isIncome) "+" else "−"
    val amountColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryAvatar(
            category = transaction.category,
            size = 64.dp,
            iconSize = 32.dp,
        )
        
        Text(
            text = transaction.category.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "$sign${MoneyFormatter.format(transaction.amount)}",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = amountColor,
        )
        
        if (transaction.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                shape = AppTheme.shapes.corner16,
            ) {
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MetaCard(
    transaction: com.notepay.domain.model.Transaction,
    walletName: String?,
) {
    val tz = TimeZone.currentSystemDefault()
    val date = transaction.occurredAt.toLocalDateTime(tz)
    val isLightTheme = !androidx.compose.foundation.isSystemInDarkTheme()
    val cardBg = if (isLightTheme) Color.White else MaterialTheme.colorScheme.surfaceContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardBg,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = AppTheme.shapes.corner24,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.detail_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            TransactionMetadataRow(
                icon = Icons.Rounded.CalendarMonth,
                label = stringResource(R.string.detail_timestamp),
                value = stringResource(
                    R.string.detail_timestamp_format,
                    date.day,
                    date.month.number,
                    date.year,
                    date.hour,
                    date.minute,
                ),
            )
            
            TransactionMetadataRow(
                icon = Icons.Rounded.Wallet,
                label = stringResource(R.string.transaction_field_wallet),
                value = walletName ?: stringResource(R.string.detail_wallet_missing),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryAvatar(
                    category = transaction.category,
                    size = 36.dp,
                    iconSize = 19.dp
                )
                Column {
                    Text(stringResource(R.string.detail_category), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(transaction.category.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            val methodIcon = if (transaction.isAutoCapture) Icons.Rounded.AccountBalance else Icons.Rounded.Person
            val methodText = stringResource(
                if (transaction.isAutoCapture) R.string.detail_method_auto else R.string.detail_method_manual,
            )
            TransactionMetadataRow(
                icon = methodIcon,
                label = stringResource(R.string.detail_method_label),
                value = methodText,
            )
        }
    }
}

@Composable
private fun TransactionMetadataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}



@Composable
private fun ActionsBlock(
    onEdit: () -> Unit,
    onCreateBillSplit: () -> Unit,
    onCreateSubscription: () -> Unit,
) {
    val isLightTheme = !androidx.compose.foundation.isSystemInDarkTheme()
    val cardBg = if (isLightTheme) Color.White else MaterialTheme.colorScheme.surfaceContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardBg,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = AppTheme.shapes.corner24,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            ActionRow(
                icon = Icons.Rounded.Edit,
                title = stringResource(R.string.edit_transaction_title),
                onClick = onEdit
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            ActionRow(
                icon = Icons.AutoMirrored.Rounded.CallSplit,
                title = stringResource(R.string.detail_create_bill_split),
                onClick = onCreateBillSplit
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            ActionRow(
                icon = Icons.Rounded.NotificationsActive,
                title = stringResource(R.string.detail_create_subscription),
                onClick = onCreateSubscription
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
