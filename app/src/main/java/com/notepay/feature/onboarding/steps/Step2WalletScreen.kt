package com.notepay.feature.onboarding.steps

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.NotePayTheme
import com.notepay.core.designsystem.util.WalletUiHelper
import com.notepay.core.navigation.NotePayAnimatedContent
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.feature.onboarding.OnboardingUiState
import com.notepay.feature.onboarding.components.OnboardingInkStageCard
import com.notepay.feature.onboarding.components.OnboardingStepHeader

@Composable
fun Step2WalletScreen(
    uiState: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onSelectIcon: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberNotePayReducedMotion()
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OnboardingStepHeader(
            eyebrow = stringResource(R.string.onboarding_step2_eyebrow),
            title = stringResource(R.string.onboarding_wallet_title),
            subtitle = stringResource(R.string.onboarding_wallet_subtitle),
        )

        Step2InkWalletPreview(uiState = uiState)

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
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = uiState.walletName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.onboarding_wallet_name_label)) },
                    placeholder = { Text(stringResource(R.string.onboarding_wallet_name_placeholder)) },
                    singleLine = true,
                    shape = AppTheme.shapes.input,
                    trailingIcon = {
                        if (uiState.walletName.isNotEmpty()) {
                            IconButton(
                                onClick = { onNameChange("") },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.onboarding_wallet_clear_cd),
                                )
                            }
                        }
                    },
                    supportingText = {
                        Text(
                            text = stringResource(
                                R.string.onboarding_wallet_name_counter_format,
                                uiState.walletName.length,
                                Wallet.MAX_NAME_LENGTH,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.onboarding_wallet_suggestions_label),
                    style = AppTheme.typography.eyebrow,
                    color = AppTheme.colors.textSecondary,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OnboardingUiState.WALLET_QUICK_SUGGESTIONS.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            pair.forEach { (labelRes, iconKey) ->
                                val suggestedName = stringResource(labelRes)
                                val isActive = uiState.walletName.trim().equals(suggestedName, ignoreCase = true)
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onNameChange(suggestedName)
                                        onSelectIcon(iconKey)
                                    },
                                    shape = AppTheme.shapes.capsule,
                                    color = if (isActive) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        AppTheme.colors.surfaceSunken
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isActive) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        },
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = suggestedName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isActive) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.onboarding_wallet_icon_label),
                    style = AppTheme.typography.eyebrow,
                    color = AppTheme.colors.textSecondary,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OnboardingUiState.WALLET_ICONS.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            pair.forEach { option ->
                                val selected = uiState.walletIconKey == option.key
                                val tileBg by animateColorAsState(
                                    targetValue = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        AppTheme.colors.surfaceSunken
                                    },
                                    animationSpec = tween(durationMillis = if (reducedMotion) 0 else 180),
                                    label = "WalletIconTileBg",
                                )
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectIcon(option.key)
                                    },
                                    shape = AppTheme.shapes.corner16,
                                    color = tileBg,
                                    border = BorderStroke(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        },
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 56.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = WalletUiHelper.getIcon(option.key),
                                            contentDescription = null,
                                            tint = if (selected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                AppTheme.colors.textSecondary
                                            },
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Text(
                                            text = stringResource(option.labelRes),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
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
private fun Step2InkWalletPreview(
    uiState: OnboardingUiState,
) {
    val selectedOption = OnboardingUiState.WALLET_ICONS.find { it.key == uiState.walletIconKey }
        ?: OnboardingUiState.WALLET_ICONS.first()
    val typeLabel = stringResource(selectedOption.labelRes)
    val displayName = uiState.walletName.trim().ifEmpty { typeLabel }
    val zeroBalanceText = MoneyFormatter.formatCustom(
        money = Money.ZERO,
        currencySymbol = MoneyFormatter.symbolForCode(uiState.currencyCode),
    )

    OnboardingInkStageCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = AppTheme.shapes.corner16,
                color = AppTheme.colors.inkRaised,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    NotePayAnimatedContent(
                        targetState = uiState.walletIconKey,
                        label = "Step2WalletIcon",
                    ) { iconKey ->
                        Icon(
                            imageVector = WalletUiHelper.getIcon(iconKey),
                            contentDescription = null,
                            tint = AppTheme.colors.sand,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    shape = AppTheme.shapes.capsule,
                    color = AppTheme.colors.sand,
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_wallet_primary_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.onSand,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.onInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = stringResource(R.string.onboarding_wallet_meta_format, typeLabel, uiState.currencyCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.onInkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        HorizontalDivider(color = AppTheme.colors.onInkMuted.copy(alpha = 0.16f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.onboarding_wallet_starting_balance_label),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.onInkMuted,
            )
            Text(
                text = zeroBalanceText,
                style = AppTheme.typography.amountCard,
                color = AppTheme.colors.onInk,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Step2WalletPreview() {
    NotePayTheme {
        Step2WalletScreen(
            uiState = OnboardingUiState(walletName = Wallet.default().name),
            onNameChange = {},
            onSelectIcon = {},
        )
    }
}
