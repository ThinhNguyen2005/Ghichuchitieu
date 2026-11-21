package com.notepay.feature.onboarding.steps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notepay.R
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.NotePayTheme
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.domain.model.CurrencyConverter
import com.notepay.domain.money.Money
import com.notepay.feature.onboarding.OnboardingUiState
import com.notepay.feature.onboarding.components.OnboardingInkStageCard
import com.notepay.feature.onboarding.components.OnboardingStepHeader
import java.util.Locale

@Composable
fun Step1CurrencyScreen(
    uiState: OnboardingUiState,
    onSelectCurrency: (String) -> Unit,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberNotePayReducedMotion()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OnboardingStepHeader(
            eyebrow = stringResource(R.string.onboarding_step1_eyebrow),
            title = stringResource(R.string.onboarding_currency_title),
            subtitle = stringResource(R.string.onboarding_currency_subtitle),
        )

        Step1InkLivePreview(currencyCode = uiState.currencyCode, reducedMotion = reducedMotion)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.onboarding_currency_section_currency),
                style = AppTheme.typography.eyebrow,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(start = 4.dp),
            )

            OnboardingUiState.CURRENCIES.chunked(4).forEach { rowCodes ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowCodes.forEach { code ->
                        CurrencyGridTile(
                            code = code,
                            selected = uiState.currencyCode == code,
                            reducedMotion = reducedMotion,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectCurrency(code)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.onboarding_currency_section_language),
                style = AppTheme.typography.eyebrow,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(start = 4.dp),
            )

            Surface(
                shape = AppTheme.shapes.capsule,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OnboardingUiState.LANGUAGES.forEach { option ->
                        val isSelected = uiState.languageCode == option.key
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(AppTheme.shapes.capsule)
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectLanguage(option.key)
                                    },
                                ),
                            shape = AppTheme.shapes.capsule,
                            color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, AppTheme.colors.border) else null,
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(option.labelRes),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun Step1InkLivePreview(
    currencyCode: String,
    reducedMotion: Boolean,
) {
    val rate = CurrencyConverter.ratePerVnd(currencyCode)
    val decimalDigits = if (currencyCode.uppercase(Locale.ROOT) in CurrencyConverter.zeroDecimalCurrencies) {
        0
    } else {
        2
    }
    val symbol = MoneyFormatter.symbolForCode(currencyCode)
    val balanceText = MoneyFormatter.formatCustom(
        money = Money(12_500_000_00L),
        currencySymbol = symbol,
        ratePerVnd = rate,
        decimalDigits = decimalDigits,
    )
    val incomeText = MoneyFormatter.formatCustom(
        money = Money(4_200_000_00L),
        currencySymbol = symbol,
        ratePerVnd = rate,
        decimalDigits = decimalDigits,
    )
    val expenseText = MoneyFormatter.formatCustom(
        money = Money(850_000_00L),
        currencySymbol = symbol,
        ratePerVnd = rate,
        decimalDigits = decimalDigits,
    )

    OnboardingInkStageCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = AppTheme.shapes.capsule,
                color = AppTheme.colors.inkRaised,
            ) {
                Text(
                    text = stringResource(R.string.onboarding_preview_balance_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTheme.colors.onInkMuted,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            Surface(
                shape = AppTheme.shapes.capsule,
                color = AppTheme.colors.sand,
            ) {
                Text(
                    text = currencyCode,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.onSand,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        AnimatedContent(
            targetState = balanceText,
            transitionSpec = {
                if (reducedMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                }
            },
            label = "Step1PreviewBalance",
        ) { target ->
            Text(
                text = target,
                style = AppTheme.typography.amountHero.copy(fontSize = 34.sp, lineHeight = 40.sp),
                color = AppTheme.colors.onInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                    contentDescription = null,
                    tint = AppTheme.colors.onInkIncome,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.ui_samplepositive, incomeText),
                    style = AppTheme.typography.amountRow,
                    color = AppTheme.colors.onInkIncome,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                    contentDescription = null,
                    tint = AppTheme.colors.onInkExpense,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.ui_samplenegative, expenseText),
                    style = AppTheme.typography.amountRow,
                    color = AppTheme.colors.onInkExpense,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CurrencyGridTile(
    code: String,
    selected: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "CurrencyTileScale",
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 180),
        label = "CurrencyTileBg",
    )

    Surface(
        shape = AppTheme.shapes.corner16,
        color = bgColor,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            },
        ),
        interactionSource = interactionSource,
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = MoneyFormatter.symbolForCode(code),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                maxLines = 1,
            )
            Text(
                text = code,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Step1CurrencyPreview() {
    NotePayTheme {
        Step1CurrencyScreen(
            uiState = OnboardingUiState(),
            onSelectCurrency = {},
            onSelectLanguage = {},
        )
    }
}
