package com.notepay.feature.settings.currency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.isAppDarkTheme
import com.notepay.domain.model.CurrencyConverter
import com.notepay.domain.money.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsScreen(
    onBack: () -> Unit,
    onOpenConverter: () -> Unit = {},
    viewModel: CurrencySettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.background,
                ),
                title = {
                    Text(
                        text = stringResource(R.string.currency_settings_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 40.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                CurrencyLivePreviewCard(
                    uiState = uiState,
                    onRefreshRates = { viewModel.refreshRates(forceRefresh = true) },
                )
            }

            item {
                Button(
                    onClick = onOpenConverter,
                    shape = AppTheme.shapes.capsule,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.currency_open_converter_action))
                }
            }

            item {
                val cardBorder = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                )
                SectionHeader(title = stringResource(R.string.utilities_currency_title))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    border = cardBorder,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CurrencyConverter.supportedCurrencies.forEachIndexed { index, currency ->
                            val isSelected = uiState.currencyCode == currency.code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 56.dp)
                                    .clickable { viewModel.selectCurrency(currency.code) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = currency.symbol,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(currency.nameRes),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currency.code,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                )
                            }
                            if (index < CurrencyConverter.supportedCurrencies.lastIndex) {
                                ItemDivider()
                            }
                        }
                    }
                }
            }

            item {
                val cardBorder = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                )
                val segColors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                SectionHeader(title = stringResource(R.string.currency_symbol_position_title))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    border = cardBorder,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = uiState.symbolPosition == "after",
                                onClick = { viewModel.setSymbolPosition("after") },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                colors = segColors,
                                icon = {},
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.currency_position_after),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            SegmentedButton(
                                selected = uiState.symbolPosition == "before",
                                onClick = { viewModel.setSymbolPosition("before") },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                colors = segColors,
                                icon = {},
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.currency_position_before),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            item {
                val cardBorder = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                )
                val segColors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                SectionHeader(title = stringResource(R.string.currency_separator_title))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    border = cardBorder,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = uiState.thousandSeparator == "dot",
                                onClick = { viewModel.setThousandSeparator("dot") },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                colors = segColors,
                                icon = {},
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.currency_separator_dot),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            SegmentedButton(
                                selected = uiState.thousandSeparator == "comma",
                                onClick = { viewModel.setThousandSeparator("comma") },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                colors = segColors,
                                icon = {},
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.currency_separator_comma),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyLivePreviewCard(
    uiState: CurrencyUiState,
    onRefreshRates: () -> Unit,
) {
    val rates = uiState.ratesSnapshot?.rates
    val ratePerVnd = CurrencyConverter.ratePerVnd(uiState.currencyCode, rates)
    val isConverting = !uiState.currencyCode.equals("VND", ignoreCase = true)
    val decimalDigits = if (isConverting &&
        uiState.currencyCode.uppercase(Locale.ROOT) !in CurrencyConverter.zeroDecimalCurrencies
    ) {
        2
    } else {
        0
    }
    val symbol = MoneyFormatter.symbolForCode(uiState.currencyCode)
    val samplePositive = MoneyFormatter.formatCustom(
        money = Money(1_250_000_00L),
        currencySymbol = symbol,
        symbolPosition = uiState.symbolPosition,
        thousandSeparator = uiState.thousandSeparator,
        ratePerVnd = ratePerVnd,
        decimalDigits = decimalDigits,
    )
    val sampleNegative = MoneyFormatter.formatCustom(
        money = Money(450_000_00L),
        currencySymbol = symbol,
        symbolPosition = uiState.symbolPosition,
        thousandSeparator = uiState.thousandSeparator,
        ratePerVnd = ratePerVnd,
        decimalDigits = decimalDigits,
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner28,
        color = AppTheme.colors.ink,
        contentColor = AppTheme.colors.onInk,
        border = if (isAppDarkTheme()) BorderStroke(1.dp, AppTheme.colors.inkBorder) else null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Paid,
                    contentDescription = null,
                    tint = AppTheme.colors.sand,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.currency_preview_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.onInk,
                    modifier = Modifier.weight(1f),
                )
                if (isConverting) {
                    IconButton(
                        onClick = onRefreshRates,
                        enabled = !uiState.isRefreshingRates,
                        modifier = Modifier.size(48.dp),
                    ) {
                        if (uiState.isRefreshingRates) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = AppTheme.colors.sand,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.currency_converter_refresh_cd),
                                tint = AppTheme.colors.onInkMuted,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.currency_preview_positive),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.onInkMuted,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.ui_samplepositive, samplePositive),
                        style = AppTheme.typography.amountCard,
                        color = AppTheme.colors.onInkIncome,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = stringResource(R.string.currency_preview_negative),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.onInkMuted,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.ui_samplenegative, sampleNegative),
                        style = AppTheme.typography.amountCard,
                        color = AppTheme.colors.onInkExpense,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (isConverting) {
                val vndPerUnitText = formatVndRate(
                    vndPerUnit = CurrencyConverter.vndPerUnit(uiState.currencyCode, rates),
                    thousandSeparator = uiState.thousandSeparator,
                )
                val snapshot = uiState.ratesSnapshot
                val statusText = when {
                    uiState.isRefreshingRates -> stringResource(R.string.currency_rate_updating)
                    uiState.rateRefreshFailed -> stringResource(R.string.currency_rate_refresh_failed)
                    snapshot != null && !snapshot.isOfflineFallback && snapshot.lastUpdatedUtc.isNotBlank() ->
                        stringResource(
                            R.string.currency_rate_live_updated,
                            snapshot.sourceName,
                            snapshot.lastUpdatedUtc,
                        )
                    else -> stringResource(R.string.currency_rate_offline_badge)
                }
                Surface(
                    shape = AppTheme.shapes.corner12,
                    color = AppTheme.colors.inkRaised,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.currency_rate_unit_format,
                                uiState.currencyCode,
                                vndPerUnitText,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.sand,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.rateRefreshFailed) {
                                AppTheme.colors.onInkExpense
                            } else {
                                AppTheme.colors.onInkMuted
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.currency_preview_note),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.onInkMuted,
            )
        }
    }
}

private fun formatVndRate(
    vndPerUnit: BigDecimal,
    thousandSeparator: String,
): String {
    val groupingChar = if (thousandSeparator == "comma") ',' else '.'
    val decimalChar = if (groupingChar == '.') ',' else '.'
    val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
        groupingSeparator = groupingChar
        decimalSeparator = decimalChar
    }
    val rounded = vndPerUnit.setScale(if (vndPerUnit >= BigDecimal(100)) 0 else 2, RoundingMode.HALF_UP)
    val pattern = if (rounded.scale() > 0) "#,##0.##" else "#,##0"
    return "${DecimalFormat(pattern, symbols).format(rounded)} đ"
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}
