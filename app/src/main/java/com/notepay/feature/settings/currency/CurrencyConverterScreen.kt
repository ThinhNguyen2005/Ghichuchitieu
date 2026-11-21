package com.notepay.feature.settings.currency

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.isAppDarkTheme
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.domain.model.CurrencyConverter
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val QUICK_AMOUNTS_ZERO_DECIMAL = listOf(
    "100000" to "100K",
    "500000" to "500K",
    "1000000" to "1M",
    "10000000" to "10M",
)

private val QUICK_AMOUNTS_DECIMAL = listOf(
    "10" to "10",
    "50" to "50",
    "100" to "100",
    "1000" to "1,000",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(
    onBack: () -> Unit,
    viewModel: CurrencyConverterViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val output = remember(state) { viewModel.convertedAmount(state) }
    val unitRate = remember(state) { viewModel.unitRate(state) }
    val quickRows = remember(state) { viewModel.quickReferenceRows(state) }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.background,
                ),
                title = {
                    Text(
                        text = stringResource(R.string.currency_converter_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshRates(forceRefresh = true) },
                        enabled = !state.isLoading,
                        modifier = Modifier.size(48.dp),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.currency_converter_refresh_cd),
                            )
                        }
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { insets ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "converter-subtitle") {
                Text(
                    text = stringResource(R.string.currency_converter_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                )
            }

            item(key = "converter-ink-result") {
                ConverterInkResultCard(
                    state = state,
                    convertedAmount = output,
                    unitRate = unitRate,
                    onRetry = { viewModel.refreshRates(forceRefresh = true) },
                )
            }

            item(key = "converter-inputs") {
                ConverterInputCard(
                    state = state,
                    onAmountChange = viewModel::setAmount,
                    onQuickAmount = viewModel::applyQuickAmount,
                    onSelectSource = viewModel::selectSourceCurrency,
                    onSelectTarget = viewModel::selectTargetCurrency,
                    onSwap = viewModel::swapCurrencies,
                )
            }

            if (quickRows.isNotEmpty()) {
                item(key = "converter-quick-table") {
                    ConverterQuickTableCard(
                        rows = quickRows,
                        selectedTarget = state.targetCurrency,
                        onSelectTarget = viewModel::selectTargetCurrency,
                    )
                }
            }

            item(key = "converter-disclaimer") {
                Text(
                    text = stringResource(R.string.currency_converter_disclaimer, state.rates.sourceName),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun ConverterInkResultCard(
    state: CurrencyConverterUiState,
    convertedAmount: BigDecimal?,
    unitRate: BigDecimal,
    onRetry: () -> Unit,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    val formattedOutput = convertedAmount?.let { formatConvertedValue(it, state.targetCurrency) }
    val formattedUnit = formatUnitRateValue(unitRate, state.targetCurrency)

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CurrencyExchange,
                        contentDescription = null,
                        tint = AppTheme.colors.sand,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.currency_converter_result_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = AppTheme.colors.onInkMuted,
                    )
                }
                Surface(
                    shape = AppTheme.shapes.capsule,
                    color = AppTheme.colors.inkRaised,
                ) {
                    Text(
                        text = if (state.rates.isOfflineFallback) {
                            stringResource(R.string.currency_rate_offline_badge)
                        } else {
                            state.rates.sourceName
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.sand,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            AnimatedContent(
                targetState = formattedOutput,
                transitionSpec = {
                    if (reducedMotion) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                    }
                },
                label = "ConverterOutput",
            ) { valueText ->
                Text(
                    text = valueText ?: stringResource(R.string.currency_converter_no_input),
                    style = if (valueText != null) {
                        AppTheme.typography.amountHero.copy(fontSize = 36.sp, lineHeight = 42.sp)
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    color = if (valueText != null) AppTheme.colors.onInk else AppTheme.colors.onInkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.currency_rate_unit_format,
                            state.sourceCurrency,
                            formattedUnit,
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.sand,
                    )
                    val statusLine = when {
                        state.isLoading -> stringResource(R.string.currency_rate_updating)
                        state.rates.isOfflineFallback -> stringResource(R.string.currency_rate_offline_badge)
                        state.rates.lastUpdatedUtc.isNotBlank() -> stringResource(
                            R.string.currency_rate_live_updated,
                            state.rates.sourceName,
                            state.rates.lastUpdatedUtc,
                        )
                        else -> state.rates.sourceName
                    }
                    Text(
                        text = statusLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.onInkMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (state.refreshFailed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = AppTheme.colors.onInkExpense,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.currency_rate_refresh_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.onInkExpense,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.currency_converter_retry),
                            color = AppTheme.colors.sand,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConverterInputCard(
    state: CurrencyConverterUiState,
    onAmountChange: (String) -> Unit,
    onQuickAmount: (String) -> Unit,
    onSelectSource: (String) -> Unit,
    onSelectTarget: (String) -> Unit,
    onSwap: () -> Unit,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    var swapTurns by remember { mutableIntStateOf(0) }
    val swapRotation by animateFloatAsState(
        targetValue = swapTurns * 180f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "SwapCurrencyRotation",
    )

    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    val quickOptions = if (state.sourceCurrency in CurrencyConverter.zeroDecimalCurrencies) {
        QUICK_AMOUNTS_ZERO_DECIMAL
    } else {
        QUICK_AMOUNTS_DECIMAL
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.currency_converter_source_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTheme.colors.textSecondary,
                )
                CurrencyDropdownChip(
                    selectedCode = state.sourceCurrency,
                    contentDescFormatRes = R.string.currency_converter_select_source_cd,
                    onSelect = onSelectSource,
                )
            }

            OutlinedTextField(
                value = state.amountInput,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = AppTheme.shapes.input,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                label = { Text(stringResource(R.string.currency_converter_amount_label)) },
                prefix = {
                    Text(
                        text = "${MoneyFormatter.symbolForCode(state.sourceCurrency)} ",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                trailingIcon = {
                    if (state.amountInput.isNotEmpty()) {
                        IconButton(onClick = { onAmountChange("") }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.currency_converter_clear_cd),
                            )
                        }
                    }
                },
                textStyle = AppTheme.typography.amountCard,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickOptions.forEach { (raw, label) ->
                    val isSelected = state.amountInput.trim() == raw
                    Surface(
                        onClick = { onQuickAmount(raw) },
                        shape = AppTheme.shapes.capsule,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            AppTheme.colors.surfaceSunken
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            },
                        ),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                Surface(
                    onClick = {
                        swapTurns += 1
                        onSwap()
                    },
                    shape = CircleShape,
                    color = AppTheme.colors.sand,
                    contentColor = AppTheme.colors.onSand,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SwapVert,
                            contentDescription = stringResource(R.string.currency_converter_swap_cd),
                            modifier = Modifier.graphicsLayer {
                                rotationZ = if (reducedMotion) 0f else swapRotation
                            },
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.currency_converter_target_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTheme.colors.textSecondary,
                )
                CurrencyDropdownChip(
                    selectedCode = state.targetCurrency,
                    contentDescFormatRes = R.string.currency_converter_select_target_cd,
                    onSelect = onSelectTarget,
                )
            }
        }
    }
}

@Composable
private fun CurrencyDropdownChip(
    selectedCode: String,
    contentDescFormatRes: Int,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val info = remember(selectedCode) { CurrencyConverter.getCurrencyInfo(selectedCode) }
    val chipCd = stringResource(contentDescFormatRes, selectedCode)

    Box {
        Surface(
            onClick = { expanded = true },
            shape = AppTheme.shapes.capsule,
            color = AppTheme.colors.surfaceSunken,
            border = BorderStroke(1.dp, AppTheme.colors.hairline),
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .semantics(mergeDescendants = true) {},
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = info.symbol,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = info.code,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = chipCd,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CurrencyConverter.supportedCurrencies.forEach { option ->
                val isSelected = option.code == selectedCode
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = option.symbol,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(24.dp),
                            )
                            Text(
                                text = stringResource(option.nameRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    onClick = {
                        onSelect(option.code)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ConverterQuickTableCard(
    rows: List<Pair<String, BigDecimal>>,
    selectedTarget: String,
    onSelectTarget: (String) -> Unit,
) {
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.currency_converter_quick_table_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
            rows.forEachIndexed { index, (code, value) ->
                val info = CurrencyConverter.getCurrencyInfo(code)
                val isTarget = code == selectedTarget
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .background(
                            if (isTarget) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        )
                        .clickable { onSelectTarget(code) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppTheme.colors.surfaceSunken),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = info.symbol,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(info.nameRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = formatConvertedValue(value, code),
                        style = AppTheme.typography.amountRow,
                        color = if (isTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (index < rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )
                }
            }
        }
    }
}

private fun formatConvertedValue(amount: BigDecimal, currencyCode: String): String {
    val cfg = MoneyFormatter.config
    val groupingChar = if (cfg.thousandSeparator == "comma") ',' else '.'
    val decimalChar = if (groupingChar == '.') ',' else '.'
    val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
        groupingSeparator = groupingChar
        decimalSeparator = decimalChar
    }
    val hasFraction = amount.stripTrailingZeros().scale() > 0
    val pattern = when {
        hasFraction && amount < BigDecimal("0.01") -> "#,##0.####"
        hasFraction -> "#,##0.##"
        else -> "#,##0"
    }
    val formatter = DecimalFormat(pattern, symbols).apply {
        roundingMode = RoundingMode.HALF_UP
    }
    val symbol = MoneyFormatter.symbolForCode(currencyCode)
    val numberText = formatter.format(amount)
    return "$numberText $symbol ($currencyCode)"
}

private fun formatUnitRateValue(unitRate: BigDecimal, targetCurrency: String): String {
    val cfg = MoneyFormatter.config
    val groupingChar = if (cfg.thousandSeparator == "comma") ',' else '.'
    val decimalChar = if (groupingChar == '.') ',' else '.'
    val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
        groupingSeparator = groupingChar
        decimalSeparator = decimalChar
    }
    val scale = unitRate.stripTrailingZeros().scale().coerceAtLeast(0).coerceAtMost(6)
    val pattern = if (scale > 0) "#,##0.${"#".repeat(scale)}" else "#,##0"
    val formatter = DecimalFormat(pattern, symbols).apply {
        roundingMode = RoundingMode.HALF_UP
    }
    return "${formatter.format(unitRate)} $targetCurrency"
}
