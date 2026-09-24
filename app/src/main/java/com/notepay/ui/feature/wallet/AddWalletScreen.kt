package com.notepay.ui.feature.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.ui.component.GradientBottomActionBar
import com.notepay.ui.component.GradientTopAppBar
import com.notepay.ui.component.LiquidButton
import com.notepay.ui.feedback.FeedbackType
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.VietnamCurrencyVisualTransformation
import com.notepay.ui.util.WalletUiHelper
import java.text.DecimalFormat

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
            GradientTopAppBar(
                title = {
                    Text(
                        if (state.isEditMode) {
                            stringResource(R.string.action_edit_wallet)
                        } else {
                            stringResource(R.string.home_add_new_wallet)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.wallet_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            GradientBottomActionBar {
                LiquidButton(
                    onClick = { viewModel.save() },
                    enabled = state.canSave,
                    surfaceColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            stringResource(R.string.action_saving),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Text(
                            if (state.isEditMode) {
                                stringResource(R.string.action_save_changes)
                            } else {
                                stringResource(R.string.home_create_wallet)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 1. Live Preview Card
            WalletLivePreviewCard(state = state)

            // 2. Tên ví
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text(stringResource(R.string.wallet_field_name)) },
                placeholder = { Text(stringResource(R.string.wallet_preview_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                shape = AppTheme.shapes.corner12,
                singleLine = true,
            )

            // 3. Số dư ban đầu
            OutlinedTextField(
                value = state.initialBalanceInput,
                onValueChange = viewModel::onInitialBalanceChanged,
                label = { Text(stringResource(R.string.wallet_field_initial_balance)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = currencyTransformation,
                shape = AppTheme.shapes.corner12,
                singleLine = true,
            )

            // 4. Hạn mức cảnh báo ngân sách
            BudgetAlertSection(
                hasBudgetLimit = state.hasBudgetLimit,
                onHasBudgetLimitChanged = viewModel::onHasBudgetLimitChanged,
                budgetPeriod = state.budgetPeriod,
                onBudgetPeriodChanged = viewModel::onBudgetPeriodChanged,
                budgetLimitInput = state.budgetLimitInput,
                onBudgetLimitChanged = viewModel::onBudgetLimitChanged,
                currencyTransformation = currencyTransformation,
            )

            // 5. Chọn Biểu tượng
            WalletIconPicker(
                selectedIconKey = state.iconKey,
                onIconSelected = viewModel::onIconChanged,
            )

            // 6. Chọn Màu sắc
            WalletColorPicker(
                selectedColorKey = state.colorKey,
                usedColorKeys = state.usedColorKeys,
                isAutoColorAssigned = state.isAutoColorAssigned,
                onColorSelected = viewModel::onColorChanged,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WalletLivePreviewCard(state: AddWalletUiState) {
    val walletColor = WalletUiHelper.getColor(state.colorKey)
    val iconVector = WalletUiHelper.getIcon(state.iconKey)
    val displayName = if (state.name.isNotBlank()) {
        state.name
    } else {
        stringResource(R.string.wallet_preview_name_placeholder)
    }

    val balanceNumber = state.initialBalanceInput.toLongOrNull() ?: 0L
    val formattedBalance = remember(balanceNumber) {
        val formatter = DecimalFormat("#,###")
        formatter.format(balanceNumber).replace(",", ".")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp),
        shape = AppTheme.shapes.corner16,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            walletColor,
                            walletColor.copy(alpha = 0.88f),
                            walletColor.copy(alpha = 0.68f),
                        ),
                    ),
                )
                .padding(18.dp),
        ) {
            // Top row: Icon & Preview Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.24f),
                ) {
                    Text(
                        text = stringResource(R.string.wallet_preview_badge).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            // Bottom content: Tên ví & Số dư
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.assets_wallet_balance) + ":",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                    Text(
                        text = "$formattedBalance ₫",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetAlertSection(
    hasBudgetLimit: Boolean,
    onHasBudgetLimitChanged: (Boolean) -> Unit,
    budgetPeriod: BudgetPeriod,
    onBudgetPeriodChanged: (BudgetPeriod) -> Unit,
    budgetLimitInput: String,
    onBudgetLimitChanged: (String) -> Unit,
    currencyTransformation: VisualTransformation,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner12,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.wallet_budget_alert_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.wallet_budget_alert_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = hasBudgetLimit,
                    onCheckedChange = onHasBudgetLimitChanged,
                )
            }

            AnimatedVisibility(
                visible = hasBudgetLimit,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Bộ chọn chu kỳ (Ngày, Tuần, Tháng)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                AppTheme.shapes.corner12,
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val periods = listOf(
                            BudgetPeriod.DAILY to stringResource(R.string.wallet_period_daily),
                            BudgetPeriod.WEEKLY to stringResource(R.string.wallet_period_weekly),
                            BudgetPeriod.MONTHLY to stringResource(R.string.wallet_period_monthly),
                        )
                        periods.forEach { (period, label) ->
                            val isSelected = budgetPeriod == period
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(AppTheme.shapes.corner8)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onBudgetPeriodChanged(period) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = budgetLimitInput,
                        onValueChange = onBudgetLimitChanged,
                        label = { Text(stringResource(R.string.wallet_budget_amount)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = currencyTransformation,
                        shape = AppTheme.shapes.corner12,
                        singleLine = true,
                    )

                    // Quy đổi sang số tiền hàng tháng
                    val rawLimit = budgetLimitInput.toLongOrNull() ?: 0L
                    if (rawLimit > 0L) {
                        val monthlyEquivalent = when (budgetPeriod) {
                            BudgetPeriod.DAILY -> rawLimit * 30
                            BudgetPeriod.WEEKLY -> rawLimit * 4
                            BudgetPeriod.MONTHLY -> rawLimit
                        }
                        val formattedMonthly = remember(monthlyEquivalent) {
                            val formatter = DecimalFormat("#,###")
                            formatter.format(monthlyEquivalent).replace(",", ".")
                        }
                        Text(
                            text = stringResource(R.string.wallet_monthly_budget_format, formattedMonthly),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletIconPicker(
    selectedIconKey: String,
    onIconSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.wallet_icon_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WalletUiHelper.iconList.forEach { (key, vector, labelRes) ->
                val isSelected = selectedIconKey == key
                val label = stringResource(labelRes)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
                        .clip(AppTheme.shapes.corner12)
                        .clickable { onIconSelected(key) }
                        .padding(vertical = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = vector,
                            contentDescription = label,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletColorPicker(
    selectedColorKey: String,
    usedColorKeys: Set<String>,
    isAutoColorAssigned: Boolean,
    onColorSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.wallet_field_color),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (isAutoColorAssigned) {
            Text(
                text = stringResource(R.string.wallet_color_auto_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val inUseLabel = stringResource(R.string.wallet_color_in_use)

            WalletUiHelper.colorList.forEach { (key, colorValue) ->
                val isSelected = selectedColorKey == key
                val isUsed = key in usedColorKeys
                val contentDesc = if (isUsed) "$key - $inUseLabel" else key

                Box(
                    modifier = Modifier
                        .size(48.dp) // Touch target >= 48dp (@android-pro rule)
                        .clip(CircleShape)
                        .clickable(
                            onClickLabel = contentDesc,
                            onClick = { onColorSelected(key) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    // Viền ngoài nhẹ khi màu đang được chọn
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colorValue.copy(alpha = 0.28f)),
                        )
                    }

                    // Vòng tròn màu chính
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colorValue),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        } else if (isUsed) {
                            // Chấm tròn nhỏ biểu thị màu đã dùng ở ví khác
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.75f)),
                            )
                        }
                    }
                }
            }
        }
    }
}
