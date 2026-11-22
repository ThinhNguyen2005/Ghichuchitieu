package com.notepay.feature.assets.main.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.WalletUiHelper
import com.notepay.domain.model.AccountNature
import com.notepay.domain.model.CurrencyConverter
import com.notepay.domain.model.WalletPurpose
import com.notepay.domain.model.WalletType
import com.notepay.domain.money.Money
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WalletBasicInfoSection(
    state: AddWalletUiState,
    currencyTransformation: VisualTransformation,
    onNatureChanged: (AccountNature) -> Unit,
    onWalletTypeChanged: (WalletType) -> Unit,
    onPurposeChanged: (WalletPurpose) -> Unit,
    onTargetAmountChanged: (String) -> Unit,
    onTargetDateChanged: (Long?) -> Unit,
    onIncludeInTotalBalanceChanged: (Boolean) -> Unit,
    onNameChanged: (String) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onInitialBalanceChanged: (String) -> Unit,
    onCreditLimitChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currencyExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // 1. Bản chất tài khoản (Tài sản vs Nợ/Tín dụng)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.wallet_field_account_nature),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val natures = listOf(AccountNature.ASSET, AccountNature.LIABILITY)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                natures.forEachIndexed { index, nature ->
                    SegmentedButton(
                        selected = state.accountNature == nature,
                        onClick = { onNatureChanged(nature) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = natures.size),
                    ) {
                        Text(
                            text = stringResource(WalletUiHelper.getNatureTitleRes(nature)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (state.accountNature == nature) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }

        // 2. Phân loại tài khoản (WalletType)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.wallet_field_wallet_type),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val availableTypes = remember(state.accountNature) {
                WalletType.entries.filter { it.nature == state.accountNature }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                availableTypes.forEach { type ->
                    val isSelected = state.walletType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { onWalletTypeChanged(type) },
                        label = {
                            Text(
                                text = stringResource(WalletUiHelper.getWalletTypeTitleRes(type)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = WalletUiHelper.getWalletTypeIcon(type),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        shape = AppTheme.shapes.corner12,
                    )
                }
            }
        }

        // 2b. Mục đích sử dụng ví & Mục tiêu (Phase 2)
        WalletPurposeAndGoalSection(
            purpose = state.purpose,
            targetAmountInput = state.targetAmountInput,
            targetDateEpochDay = state.targetDateEpochDay,
            includeInTotalBalance = state.includeInTotalBalance,
            currencyCode = state.currencyCode,
            currencyTransformation = currencyTransformation,
            onPurposeChanged = onPurposeChanged,
            onTargetAmountChanged = onTargetAmountChanged,
            onTargetDateChanged = onTargetDateChanged,
            onIncludeInTotalBalanceChanged = onIncludeInTotalBalanceChanged,
        )

        // 3. Tên ví
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChanged,
            label = { Text(stringResource(R.string.wallet_field_name)) },
            placeholder = { Text(stringResource(R.string.wallet_preview_name_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            shape = AppTheme.shapes.corner12,
            singleLine = true,
        )

        // 4. Đơn vị tiền tệ
        ExposedDropdownMenuBox(
            expanded = currencyExpanded && !state.isCurrencyLocked,
            onExpandedChange = { if (!state.isCurrencyLocked) currencyExpanded = !currencyExpanded },
        ) {
            OutlinedTextField(
                value = state.currencyCode,
                onValueChange = {},
                readOnly = true,
                enabled = !state.isCurrencyLocked,
                label = { Text(stringResource(R.string.wallet_field_currency)) },
                shape = AppTheme.shapes.corner12,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                trailingIcon = {
                    if (state.isCurrencyLocked) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded)
                    }
                },
                supportingText = if (state.isCurrencyLocked) {
                    {
                        Text(
                            text = pluralStringResource(
                                R.plurals.wallet_currency_locked_hint,
                                state.transactionCount,
                                state.transactionCount,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                } else null,
            )
            ExposedDropdownMenu(
                expanded = currencyExpanded && !state.isCurrencyLocked,
                onDismissRequest = { currencyExpanded = false },
            ) {
                CurrencyConverter.supportedCurrencies.forEach { currency ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${currency.code} (${currency.symbol}) - ${stringResource(currency.nameRes)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (currency.code == state.currencyCode) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onCurrencyChanged(currency.code)
                            currencyExpanded = false
                        },
                    )
                }
            }
        }

        // 5. Số dư ban đầu
        val isInitialBalanceLocked = state.isEditMode && state.hasTransactions

        OutlinedTextField(
            value = state.initialBalanceInput,
            onValueChange = onInitialBalanceChanged,
            readOnly = isInitialBalanceLocked,
            label = { Text(stringResource(R.string.wallet_field_initial_balance)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = if (state.currencyCode == "VND") currencyTransformation else VisualTransformation.None,
            shape = AppTheme.shapes.corner12,
            singleLine = true,
            trailingIcon = if (isInitialBalanceLocked) {
                {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else null,
            supportingText = {
                if (isInitialBalanceLocked) {
                    Text(
                        text = stringResource(R.string.wallet_initial_balance_locked_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else if (state.isEditMode) {
                    Text(
                        text = stringResource(R.string.wallet_initial_balance_editable_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )

        // 6. Hạn mức tín dụng (cho Thẻ tín dụng hoặc Hạn mức thấu chi)
        if (state.walletType.isCreditCardOrLine) {
            OutlinedTextField(
                value = state.creditLimitInput,
                onValueChange = onCreditLimitChanged,
                label = { Text(stringResource(R.string.wallet_field_credit_limit)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = if (state.currencyCode == "VND") currencyTransformation else VisualTransformation.None,
                shape = AppTheme.shapes.corner12,
                singleLine = true,
            )
        }
    }
}

@Composable
internal fun BudgetAlertSection(
    hasBudgetLimit: Boolean,
    onHasBudgetLimitChanged: (Boolean) -> Unit,
    budgetPeriod: BudgetPeriod,
    onBudgetPeriodChanged: (BudgetPeriod) -> Unit,
    budgetLimitInput: String,
    onBudgetLimitChanged: (String) -> Unit,
    currencyTransformation: VisualTransformation,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner12,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.corner12)
                    .toggleable(
                        value = hasBudgetLimit,
                        onValueChange = onHasBudgetLimitChanged,
                        role = Role.Switch,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
                    onCheckedChange = null,
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup()
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
                            BudgetPeriod.MONTHLY to stringResource(R.string.subscription_repeat_monthly),
                        )
                        periods.forEach { (period, label) ->
                            val isSelected = budgetPeriod == period
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(AppTheme.shapes.corner12)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .selectable(
                                        selected = isSelected,
                                        onClick = { onBudgetPeriodChanged(period) },
                                        role = Role.RadioButton,
                                    )
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

                    val rawLimit = budgetLimitInput.toLongOrNull() ?: 0L
                    if (rawLimit > 0L) {
                        val monthlyEquivalent = when (budgetPeriod) {
                            BudgetPeriod.DAILY -> rawLimit * 30
                            BudgetPeriod.WEEKLY -> rawLimit * 4
                            BudgetPeriod.MONTHLY -> rawLimit
                        }
                        val formattedMonthly = remember(monthlyEquivalent) {
                            MoneyFormatter.format(Money(monthlyEquivalent * 100L))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletPurposeAndGoalSection(
    purpose: WalletPurpose,
    targetAmountInput: String,
    targetDateEpochDay: Long?,
    includeInTotalBalance: Boolean,
    currencyCode: String,
    currencyTransformation: VisualTransformation,
    onPurposeChanged: (WalletPurpose) -> Unit,
    onTargetAmountChanged: (String) -> Unit,
    onTargetDateChanged: (Long?) -> Unit,
    onIncludeInTotalBalanceChanged: (Boolean) -> Unit,
) {
    var showTargetDatePicker by remember { mutableStateOf(false) }
    val selectablePurposes = remember {
        listOf(
            WalletPurpose.PERSONAL,
            WalletPurpose.BUSINESS,
            WalletPurpose.SAVINGS_GOAL,
            WalletPurpose.TRAVEL,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.wallet_purpose_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            selectablePurposes.forEach { item ->
                val selected = purpose == item
                FilterChip(
                    selected = selected,
                    onClick = { onPurposeChanged(item) },
                    label = {
                        Text(
                            text = stringResource(WalletUiHelper.getPurposeTitleRes(item)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    shape = AppTheme.shapes.capsule,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppTheme.colors.sand,
                        selectedLabelColor = AppTheme.colors.onSand,
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = purpose.supportsGoalTarget,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = targetAmountInput,
                    onValueChange = onTargetAmountChanged,
                    label = { Text(stringResource(R.string.wallet_target_amount_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = if (currencyCode == "VND") currencyTransformation else VisualTransformation.None,
                    shape = AppTheme.shapes.corner12,
                    singleLine = true,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val dateLabel = if (targetDateEpochDay != null) {
                        val date = Instant.fromEpochMilliseconds(targetDateEpochDay * 86_400_000L)
                            .toLocalDateTime(TimeZone.UTC).date
                        String.format(Locale.ROOT, "%02d/%02d/%04d", date.day, date.month.number, date.year)
                    } else {
                        stringResource(R.string.wallet_target_date_placeholder)
                    }

                    OutlinedButton(
                        onClick = { showTargetDatePicker = true },
                        shape = AppTheme.shapes.corner12,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text(
                            text = "${stringResource(R.string.wallet_target_date_label)}: $dateLabel",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (targetDateEpochDay != null) {
                        TextButton(
                            onClick = { onTargetDateChanged(null) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        ) {
                            Text(stringResource(R.string.wallet_target_clear_date))
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = includeInTotalBalance,
                    role = Role.Switch,
                    onValueChange = onIncludeInTotalBalanceChanged,
                ),
            shape = AppTheme.shapes.corner12,
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceRaised),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.wallet_include_in_total_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.textPrimary,
                    )
                    Text(
                        text = stringResource(R.string.wallet_include_in_total_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary,
                    )
                }
                Switch(
                    checked = includeInTotalBalance,
                    onCheckedChange = null,
                )
            }
        }
    }

    if (showTargetDatePicker) {
        val initialMillis = targetDateEpochDay?.let { it * 86_400_000L }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showTargetDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                        onTargetDateChanged(date.toEpochDays().toLong())
                    }
                    showTargetDatePicker = false
                }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
