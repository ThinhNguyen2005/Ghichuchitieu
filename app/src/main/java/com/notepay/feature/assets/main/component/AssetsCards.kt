package com.notepay.feature.assets.main.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.notepay.R
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.HomeHeroCardDarkBottom
import com.notepay.core.designsystem.theme.HomeHeroCardDarkRim
import com.notepay.core.designsystem.theme.HomeHeroCardDarkTop
import com.notepay.core.designsystem.theme.HomeHeroCardLightBottom
import com.notepay.core.designsystem.theme.HomeHeroCardLightRim
import com.notepay.core.designsystem.theme.HomeHeroCardLightTop
import com.notepay.core.designsystem.util.WalletUiHelper
import com.notepay.core.navigation.rememberNotePayHaptic
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.domain.model.CurrencyConverter
import com.notepay.domain.model.SharedWalletRole
import com.notepay.domain.model.WalletPurpose
import com.notepay.domain.money.Money
import com.notepay.domain.shared.SharedWalletPermissionGuard
import com.notepay.feature.assets.shared.RoleBadge
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import kotlin.time.Instant
import com.notepay.feature.assets.main.AllocationMode
import com.notepay.feature.assets.main.AllocationSegment
import com.notepay.feature.assets.main.WalletAllocationItem
import com.notepay.feature.assets.main.WalletAssetItem
import com.notepay.feature.assets.main.groupAllocation
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
private fun AllocationSegment.paintColor(): Color =
    if (isOther) AppTheme.colors.textMuted else WalletUiHelper.getColor(colorKey.orEmpty())

@Composable
internal fun AssetsInkHeroCard(
    totalNetWorth: Money,
    totalAssetsVnd: Money,
    totalLiabilitiesVnd: Money,
    assetCount: Int,
    liabilityCount: Int,
    changePercentage: Float?,
    allocationMode: AllocationMode,
    onAllocationModeChanged: (AllocationMode) -> Unit,
    allocationItems: List<WalletAllocationItem>,
    canTransfer: Boolean,
    hasForeignCurrency: Boolean,
    ratesLastUpdated: String?,
    onAddWallet: () -> Unit,
    onTransferClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val isLight = colors.isLight
    val reducedMotion = rememberNotePayReducedMotion()
    val isLargeFont = LocalDensity.current.fontScale >= 1.45f
    val cardShape = AppTheme.shapes.corner28
    val heroGradient = remember(isLight) {
        val top = if (isLight) HomeHeroCardLightTop else HomeHeroCardDarkTop
        val bottom = if (isLight) HomeHeroCardLightBottom else HomeHeroCardDarkBottom
        Brush.verticalGradient(listOf(top, bottom))
    }
    val rimColor = if (isLight) HomeHeroCardLightRim else HomeHeroCardDarkRim

    LaunchedEffect(liabilityCount, allocationMode) {
        if (liabilityCount == 0 && allocationMode == AllocationMode.LIABILITY) {
            onAllocationModeChanged(AllocationMode.ASSET)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .iosCardShadow(shape = cardShape, elevated = true),
        shape = cardShape,
        color = Color.Transparent,
        contentColor = colors.textPrimary,
        border = BorderStroke(1.dp, rimColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroGradient)
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.assets_total_balance),
                        style = AppTheme.typography.labelMedium,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))

                    if (changePercentage != null && changePercentage != 0f) {
                        val isPositive = changePercentage >= 0f
                        val absChange = abs(changePercentage)
                        val trendColor = if (isPositive) colors.success else colors.error
                        Surface(
                            shape = AppTheme.shapes.capsule,
                            color = trendColor.copy(alpha = if (isLight) 0.12f else 0.18f),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = AppTheme.dimensions.spaceSmall, vertical = 3.dp),
                            ) {
                                Icon(
                                    imageVector = if (isPositive) {
                                        Icons.AutoMirrored.Rounded.TrendingUp
                                    } else {
                                        Icons.AutoMirrored.Rounded.TrendingDown
                                    },
                                    contentDescription = null,
                                    tint = trendColor,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceExtraSmall))
                                val badgeText = if (absChange > 999f) {
                                    stringResource(
                                        if (isPositive) {
                                            R.string.percent_change_positive_cap
                                        } else {
                                            R.string.percent_change_negative_cap
                                        },
                                    )
                                } else {
                                    val locale = LocalLocale.current.platformLocale
                                    val formattedPct = String.format(locale, "%.1f", absChange)
                                    stringResource(
                                        if (isPositive) R.string.percent_change_positive else R.string.percent_change_negative,
                                        formattedPct,
                                    )
                                }
                                Text(
                                    text = badgeText,
                                    style = AppTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = trendColor,
                                )
                            }
                        }
                    } else if (changePercentage != null) {
                        Surface(
                            shape = AppTheme.shapes.capsule,
                            color = colors.surfaceSunken,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = AppTheme.dimensions.spaceSmall, vertical = 3.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceExtraSmall))
                                Text(
                                    text = stringResource(R.string.badge_stable),
                                    style = AppTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))

                AnimatedContent(
                    targetState = totalNetWorth,
                    transitionSpec = {
                        if (reducedMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            (fadeIn(tween(220)) + slideInVertically { it / 4 }) togetherWith
                                (fadeOut(tween(150)) + slideOutVertically { -it / 4 })
                        }
                    },
                    label = "AssetsNetWorthAnimation",
                ) { targetAmount ->
                    Text(
                        text = MoneyFormatter.format(targetAmount),
                        style = AppTheme.typography.amountHero,
                        color = colors.textPrimary,
                        maxLines = if (isLargeFont) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (totalNetWorth.amountInCents < 0L) {
                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                    Text(
                        text = stringResource(R.string.assets_negative_balance_hint),
                        style = AppTheme.typography.labelMedium,
                        color = colors.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))

            HorizontalDivider(thickness = 0.5.dp, color = colors.hairline)

            Spacer(modifier = Modifier.height(14.dp))

            val assetsTintAlpha = if (isLight) 0.10f else 0.16f
            val liabilitiesTintAlpha = if (isLight) 0.09f else 0.15f
            if (isLargeFont) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PortfolioKpiTile(
                        label = stringResource(R.string.assets_total_assets_label),
                        amountText = MoneyFormatter.format(totalAssetsVnd),
                        countText = stringResource(
                            R.string.assets_wallets_count_format,
                            assetCount,
                            stringResource(R.string.wallet_fallback),
                        ),
                        accentColor = colors.success,
                        containerAlpha = assetsTintAlpha,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PortfolioKpiTile(
                        label = stringResource(R.string.assets_total_liabilities_label),
                        amountText = MoneyFormatter.format(totalLiabilitiesVnd),
                        countText = stringResource(
                            R.string.assets_wallets_count_format,
                            liabilityCount,
                            stringResource(R.string.wallet_type_loan),
                        ),
                        accentColor = colors.error,
                        containerAlpha = liabilitiesTintAlpha,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceMedium),
                ) {
                    PortfolioKpiTile(
                        label = stringResource(R.string.assets_total_assets_label),
                        amountText = MoneyFormatter.format(totalAssetsVnd),
                        countText = stringResource(
                            R.string.assets_wallets_count_format,
                            assetCount,
                            stringResource(R.string.wallet_fallback),
                        ),
                        accentColor = colors.success,
                        containerAlpha = assetsTintAlpha,
                        modifier = Modifier.weight(1f),
                    )
                    PortfolioKpiTile(
                        label = stringResource(R.string.assets_total_liabilities_label),
                        amountText = MoneyFormatter.format(totalLiabilitiesVnd),
                        countText = stringResource(
                            R.string.assets_wallets_count_format,
                            liabilityCount,
                            stringResource(R.string.wallet_type_loan),
                        ),
                        accentColor = colors.error,
                        containerAlpha = liabilitiesTintAlpha,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (hasForeignCurrency) {
                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceMedium))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.assets_rates_updated_format,
                            ratesLastUpdated ?: stringResource(R.string.assets_rate_today),
                        ),
                        style = AppTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.assets_allocation_title),
                    style = AppTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                if (liabilityCount > 0) {
                    Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                    Row(
                        modifier = Modifier
                            .clip(AppTheme.shapes.capsule)
                            .background(colors.surfaceSunken)
                            .padding(3.dp)
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        val isAssetSelected = allocationMode == AllocationMode.ASSET
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget)
                                .clip(AppTheme.shapes.capsule)
                                .background(if (isAssetSelected) colors.sand else Color.Transparent)
                                .selectable(
                                    selected = isAssetSelected,
                                    role = Role.RadioButton,
                                    onClick = { onAllocationModeChanged(AllocationMode.ASSET) },
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.assets_allocation_tab_asset),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = if (isAssetSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAssetSelected) colors.onSand else colors.textSecondary,
                                maxLines = 1,
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget)
                                .clip(AppTheme.shapes.capsule)
                                .background(if (!isAssetSelected) colors.sand else Color.Transparent)
                                .selectable(
                                    selected = !isAssetSelected,
                                    role = Role.RadioButton,
                                    onClick = { onAllocationModeChanged(AllocationMode.LIABILITY) },
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.assets_allocation_tab_liability),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = if (!isAssetSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isAssetSelected) colors.onSand else colors.textSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceMedium))

            if (allocationItems.isNotEmpty()) {
                val segments = remember(allocationItems) { groupAllocation(allocationItems) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(AppTheme.shapes.capsule),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    segments.forEach { segment ->
                        Box(
                            modifier = Modifier
                                .weight(segment.percentage)
                                .fillMaxHeight()
                                .background(segment.paintColor()),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceMedium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    segments.forEach { segment ->
                        val pct = (segment.percentage * 100).roundToInt()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceExtraSmall),
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(segment.paintColor()),
                            )
                            Text(
                                text = segment.name ?: stringResource(R.string.assets_allocation_other),
                                style = AppTheme.typography.labelMedium,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.percent_format, pct),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = if (allocationMode == AllocationMode.ASSET) {
                        stringResource(R.string.stats_no_data)
                    } else {
                        stringResource(R.string.assets_allocation_no_liabilities)
                    },
                    style = AppTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceExtraLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceMedium),
            ) {
                Button(
                    onClick = onTransferClick,
                    enabled = canTransfer,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
                    shape = AppTheme.shapes.capsule,
                    contentPadding = PaddingValues(
                        horizontal = AppTheme.dimensions.spaceMedium,
                        vertical = AppTheme.dimensions.spaceMedium,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = colors.surfaceSunken,
                        disabledContentColor = colors.textMuted,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.CompareArrows,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                    Text(
                        text = stringResource(R.string.action_transfer),
                        style = AppTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Button(
                    onClick = onAddWallet,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
                    shape = AppTheme.shapes.capsule,
                    border = BorderStroke(0.5.dp, colors.hairline),
                    contentPadding = PaddingValues(
                        horizontal = AppTheme.dimensions.spaceMedium,
                        vertical = AppTheme.dimensions.spaceMedium,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceSunken,
                        contentColor = colors.textPrimary,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                    Text(
                        text = stringResource(R.string.action_add_wallet),
                        style = AppTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioKpiTile(
    label: String,
    amountText: String,
    countText: String,
    accentColor: Color,
    containerAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppTheme.shapes.corner16,
        color = accentColor.copy(alpha = containerAlpha),
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                )
                Text(
                    text = label,
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = amountText,
                style = AppTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = countText,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WalletAssetCard(
    item: WalletAssetItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    onPayCard: (() -> Unit)? = null,
    onAdjustBalance: (() -> Unit)? = null,
    onManageMembers: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val performHaptic = rememberNotePayHaptic()
    val wallet = item.wallet
    val colorValue = WalletUiHelper.getColor(wallet.colorKey)

    val cardBorder = if (wallet.isActive) {
        BorderStroke(1.5.dp, AppTheme.colors.sand)
    } else {
        BorderStroke(0.5.dp, AppTheme.colors.hairline)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .iosCardShadow(shape = AppTheme.shapes.corner20, elevated = false)
            .animateContentSize()
            .clip(AppTheme.shapes.corner20)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.assets_view_transactions),
                onClick = onClick,
            ),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface,
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.spaceLarge),
        ) {
            // Hàng đầu: Icon ví | Cột tên & Badges | Nút Sửa ví
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colorValue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    WalletAppIcon(
                        wallet = wallet,
                        modifier = Modifier,
                        iconSize = 24.dp,
                        tint = colorValue,
                        contentDescription = null,
                    )
                }

                Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceMedium))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = wallet.name,
                        style = AppTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val typeLabel = stringResource(WalletUiHelper.getWalletTypeTitleRes(wallet.type))
                    val showTypeBadge = !typeLabel.equals(wallet.name.trim(), ignoreCase = true)

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (item.balance.amountInCents < 0L) {
                            Surface(
                                shape = AppTheme.shapes.capsule,
                                color = AppTheme.colors.error.copy(alpha = 0.10f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = AppTheme.colors.error,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = if (wallet.isAsset) {
                                            stringResource(R.string.assets_overdraft_warning)
                                        } else {
                                            stringResource(R.string.asset_negative_balance_badge)
                                        },
                                        style = AppTheme.typography.labelSmall,
                                        color = AppTheme.colors.error,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }

                        if (showTypeBadge) {
                            Surface(
                                shape = AppTheme.shapes.corner12,
                                color = colorValue.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    text = typeLabel,
                                    style = AppTheme.typography.labelSmall,
                                    color = colorValue,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        if (wallet.isShared) {
                            RoleBadge(role = wallet.sharedRole ?: SharedWalletRole.VIEWER)
                        }

                        if (wallet.purpose != WalletPurpose.PERSONAL && !wallet.isShared) {
                            Surface(
                                shape = AppTheme.shapes.corner12,
                                color = AppTheme.colors.sand.copy(alpha = 0.35f),
                            ) {
                                Text(
                                    text = stringResource(WalletUiHelper.getPurposeTitleRes(wallet.purpose)),
                                    style = AppTheme.typography.labelSmall,
                                    color = AppTheme.colors.onSand,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        if (!wallet.includeInTotalBalance) {
                            Surface(
                                shape = AppTheme.shapes.corner12,
                                color = AppTheme.colors.surfaceSunken,
                            ) {
                                Text(
                                    text = stringResource(R.string.wallet_excluded_from_total_badge),
                                    style = AppTheme.typography.labelSmall,
                                    color = AppTheme.colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        if (wallet.isActive) {
                            Surface(
                                shape = AppTheme.shapes.capsule,
                                color = AppTheme.colors.sand,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(
                                        horizontal = AppTheme.dimensions.spaceSmall,
                                        vertical = 2.dp,
                                    ),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(AppTheme.colors.onSand),
                                    )
                                    Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceExtraSmall))
                                    Text(
                                        text = stringResource(R.string.wallet_badge_active),
                                        style = AppTheme.typography.labelSmall,
                                        color = AppTheme.colors.onSand,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.wallet_tx_count, item.transactionCount),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (!wallet.isShared || SharedWalletPermissionGuard.canManageMembers(wallet)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(AppTheme.dimensions.minTouchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.action_edit_wallet),
                            tint = AppTheme.colors.textSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceMedium))

            // Dòng số dư
            val isForeign = !wallet.currencyCode.equals("VND", ignoreCase = true)
            val primaryBalanceText = if (isForeign) {
                CurrencyConverter.formatForeignAmount(item.balance.amountInCents, wallet.currencyCode)
            } else {
                MoneyFormatter.format(item.balance)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = primaryBalanceText,
                    style = AppTheme.typography.amountCard,
                    color = if (item.balance.amountInCents < 0L) AppTheme.colors.error else AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                if (isForeign) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.assets_approx_vnd_format, MoneyFormatter.format(item.convertedBalanceVnd)),
                        style = AppTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Thanh tỷ trọng mảnh trong nhóm
            if (item.groupPercentage > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { item.groupPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(AppTheme.shapes.capsule),
                    color = colorValue,
                    trackColor = colorValue.copy(alpha = 0.12f),
                )
            }

            // Thanh tiến độ mục tiêu (SAVINGS_GOAL / TRAVEL)
            val targetAmount = wallet.targetAmount
            if (wallet.purpose.supportsGoalTarget && targetAmount != null && targetAmount.amountInCents > 0L) {
                Spacer(modifier = Modifier.height(10.dp))
                val ratio = (item.balance.amountInCents.toFloat() / targetAmount.amountInCents.toFloat())
                    .coerceIn(0f, 1f)
                val pctInt = (ratio * 100f).roundToInt()
                val formattedCurrent = if (isForeign) {
                    CurrencyConverter.formatForeignAmount(item.balance.amountInCents, wallet.currencyCode)
                } else {
                    MoneyFormatter.format(item.balance)
                }
                val formattedTarget = if (isForeign) {
                    CurrencyConverter.formatForeignAmount(targetAmount.amountInCents, wallet.currencyCode)
                } else {
                    MoneyFormatter.format(targetAmount)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.wallet_goal_progress_format,
                            formattedCurrent,
                            formattedTarget,
                            pctInt,
                        ),
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    wallet.targetDateEpochDay?.let { epochDay ->
                        val dueDate = Instant.fromEpochMilliseconds(epochDay * 86_400_000L)
                            .toLocalDateTime(TimeZone.UTC).date
                        val dueLabel = String.format(
                            Locale.ROOT,
                            "%02d/%02d/%04d",
                            dueDate.day,
                            dueDate.month.number,
                            dueDate.year,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.wallet_goal_due_format, dueLabel),
                            style = AppTheme.typography.labelSmall,
                            color = AppTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(AppTheme.shapes.capsule),
                    color = AppTheme.colors.sand,
                    trackColor = AppTheme.colors.surfaceSunken,
                )
            }

            // Thông tin hạn mức nếu là Thẻ tín dụng hoặc Hạn mức tín dụng
            if (wallet.type.isCreditCardOrLine && wallet.creditLimit != null && wallet.creditLimit.amountInCents > 0L) {
                Spacer(modifier = Modifier.height(8.dp))
                val usedDebt = if (item.balance.amountInCents < 0L) -item.balance.amountInCents else 0L
                val available = maxOf(0L, wallet.creditLimit.amountInCents - usedDebt)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.assets_credit_limit_format,
                            CurrencyConverter.formatForeignAmount(wallet.creditLimit.amountInCents, wallet.currencyCode),
                        ),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary,
                    )
                    Text(
                        text = stringResource(
                            R.string.assets_credit_available_format,
                            CurrencyConverter.formatForeignAmount(available, wallet.currencyCode),
                        ),
                        style = AppTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.textPrimary,
                    )
                }
            }

            // Dải thu chi tháng
            val hasIncome = item.monthlyIncome.amountInCents > 0L
            val hasExpense = item.monthlyExpense.amountInCents > 0L

            if (hasIncome || hasExpense) {
                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceMedium))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppTheme.shapes.corner12)
                        .background(AppTheme.colors.surfaceSunken)
                        .padding(
                            horizontal = AppTheme.dimensions.spaceMedium,
                            vertical = AppTheme.dimensions.spaceSmall,
                        ),
                    horizontalArrangement = if (hasIncome && hasExpense) {
                        Arrangement.SpaceBetween
                    } else {
                        Arrangement.Start
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (hasIncome) {
                        val incomeText = stringResource(
                            R.string.amount_positive_format,
                            MoneyFormatter.format(item.monthlyIncome),
                        )
                        val incomeCd = "${stringResource(R.string.home_income_month)}: $incomeText"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clearAndSetSemantics {
                                contentDescription = incomeCd
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = AppTheme.colors.success,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceExtraSmall))
                            Text(
                                text = incomeText,
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.success,
                            )
                        }
                    }

                    if (hasExpense) {
                        val expenseText = stringResource(
                            R.string.amount_negative_format,
                            MoneyFormatter.format(item.monthlyExpense),
                        )
                        val expenseCd = "${stringResource(R.string.home_expense_month)}: $expenseText"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clearAndSetSemantics {
                                contentDescription = expenseCd
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                                contentDescription = null,
                                tint = AppTheme.colors.error,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceExtraSmall))
                            Text(
                                text = expenseText,
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.error,
                            )
                        }
                    }
                }
            }

            // Hàng Action nhanh (Thanh toán thẻ / Cập nhật số dư / Thành viên / Đặt làm ví chính)
            val canPayCard = wallet.type.isCreditCardOrLine && item.balance.amountInCents < 0L && onPayCard != null
            val canAdjustBalance = (wallet.type.isInvestment || wallet.type.name == "SAVINGS") && onAdjustBalance != null
            val canShowMembers = wallet.isShared && onManageMembers != null
            val canSetDefault = !wallet.isActive && !wallet.isArchived

            if (listOf(canPayCard, canAdjustBalance, canShowMembers, canSetDefault).any { it }) {
                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    if (canShowMembers) {
                        OutlinedButton(
                            onClick = { onManageMembers() },
                            shape = AppTheme.shapes.capsule,
                            modifier = Modifier.defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Group,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.shared_wallet_members_action),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (canPayCard) {
                        OutlinedButton(
                            onClick = { onPayCard() },
                            shape = AppTheme.shapes.capsule,
                            modifier = Modifier.defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.assets_card_action_pay_card),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (canAdjustBalance) {
                        OutlinedButton(
                            onClick = { onAdjustBalance() },
                            shape = AppTheme.shapes.capsule,
                            modifier = Modifier.defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.assets_card_action_adjust_balance),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (canSetDefault) {
                        TextButton(
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                onSetDefault()
                            },
                            contentPadding = PaddingValues(
                                horizontal = AppTheme.dimensions.spaceMedium,
                                vertical = AppTheme.dimensions.spaceSmall,
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AppTheme.colors.textPrimary,
                            )
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                            Text(
                                text = stringResource(R.string.set_as_active_wallet),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.textPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}
