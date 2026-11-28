package com.notepay.feature.assets.main.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.notepay.core.navigation.NotePayAnimatedContent
import com.notepay.core.navigation.rememberNotePayHaptic
import com.notepay.domain.model.CurrencyConverter
import com.notepay.domain.model.SharedWalletRole
import com.notepay.domain.money.Money
import com.notepay.domain.shared.SharedWalletPermissionGuard
import com.notepay.feature.assets.shared.RoleBadge
import java.util.Locale
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
    hasForeignCurrency: Boolean,
    ratesLastUpdated: String?,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val isLight = colors.isLight
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

                NotePayAnimatedContent(
                    targetState = totalNetWorth,
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

@Composable
internal fun WalletAssetCard(
    item: WalletAssetItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    modifier: Modifier = Modifier,
    onPayCard: (() -> Unit)? = null,
    onAdjustBalance: (() -> Unit)? = null,
    onManageMembers: (() -> Unit)? = null,
) {
    val performHaptic = rememberNotePayHaptic()
    val wallet = item.wallet
    val colorValue = WalletUiHelper.getColor(wallet.colorKey)
    val typeLabel = stringResource(WalletUiHelper.getWalletTypeTitleRes(wallet.type))
    val isForeign = !wallet.currencyCode.equals("VND", ignoreCase = true)
    val primaryBalanceText = if (isForeign) {
        CurrencyConverter.formatForeignAmount(item.balance.amountInCents, wallet.currencyCode)
    } else {
        MoneyFormatter.format(item.balance)
    }

    val canEdit = !wallet.isShared || SharedWalletPermissionGuard.canManageMembers(wallet)
    val canPayCard = wallet.type.isCreditCardOrLine && item.balance.amountInCents < 0L && onPayCard != null
    val canAdjustBalance = (wallet.type.isInvestment || wallet.type.name == "SAVINGS") && onAdjustBalance != null
    val canShowMembers = wallet.isShared && onManageMembers != null
    val canSetDefault = !wallet.isActive && !wallet.isArchived
    val hasMenuActions = canEdit || canShowMembers || canPayCard || canAdjustBalance || canSetDefault
    var menuExpanded by remember { mutableStateOf(false) }

    val cardBorder = if (wallet.isActive) {
        BorderStroke(1.5.dp, AppTheme.colors.sand)
    } else {
        BorderStroke(0.5.dp, AppTheme.colors.hairline)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .iosCardShadow(shape = AppTheme.shapes.corner20, elevated = false)
            .clip(AppTheme.shapes.corner20)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.assets_view_transactions),
                onClick = onClick,
            ),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.dimensions.spaceLarge, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(AppTheme.shapes.corner16)
                        .background(colorValue.copy(alpha = 0.14f)),
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

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = wallet.name,
                            style = AppTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (wallet.isActive) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.sand),
                            )
                        }
                        if (wallet.isShared) {
                            RoleBadge(role = wallet.sharedRole ?: SharedWalletRole.VIEWER)
                        }
                    }

                    Text(
                        text = primaryBalanceText,
                        style = AppTheme.typography.amountCard,
                        color = if (item.balance.amountInCents < 0L) AppTheme.colors.error else AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.assets_type_currency_format,
                                typeLabel,
                                wallet.currencyCode.uppercase(Locale.ROOT),
                            ),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (isForeign) {
                            Text(
                                text = stringResource(
                                    R.string.assets_approx_vnd_format,
                                    MoneyFormatter.format(item.convertedBalanceVnd),
                                ),
                                style = AppTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = AppTheme.colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (hasMenuActions) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(AppTheme.dimensions.minTouchTarget),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.cd_wallet_actions_more, wallet.name),
                                tint = AppTheme.colors.textSecondary,
                            )
                        }
                        WalletOverflowMenu(
                            expanded = menuExpanded,
                            onDismiss = { menuExpanded = false },
                            canEdit = canEdit,
                            canShowMembers = canShowMembers,
                            canPayCard = canPayCard,
                            canAdjustBalance = canAdjustBalance,
                            canSetDefault = canSetDefault,
                            onEdit = onEdit,
                            onManageMembers = onManageMembers,
                            onPayCard = onPayCard,
                            onAdjustBalance = onAdjustBalance,
                            onSetDefault = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                onSetDefault()
                            },
                        )
                    }
                }
            }

            WalletCardCompactFooter(
                item = item,
                isForeign = isForeign,
            )
        }
    }
}

@Composable
private fun WalletOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    canEdit: Boolean,
    canShowMembers: Boolean,
    canPayCard: Boolean,
    canAdjustBalance: Boolean,
    canSetDefault: Boolean,
    onEdit: () -> Unit,
    onManageMembers: (() -> Unit)?,
    onPayCard: (() -> Unit)?,
    onAdjustBalance: (() -> Unit)?,
    onSetDefault: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        if (canEdit) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_edit_wallet)) },
                onClick = {
                    onDismiss()
                    onEdit()
                },
            )
        }
        if (canShowMembers && onManageMembers != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.shared_wallet_members_action)) },
                onClick = {
                    onDismiss()
                    onManageMembers()
                },
            )
        }
        if (canPayCard && onPayCard != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.assets_card_action_pay_card)) },
                onClick = {
                    onDismiss()
                    onPayCard()
                },
            )
        }
        if (canAdjustBalance && onAdjustBalance != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.assets_card_action_adjust_balance)) },
                onClick = {
                    onDismiss()
                    onAdjustBalance()
                },
            )
        }
        if (canSetDefault) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.set_as_active_wallet)) },
                onClick = {
                    onDismiss()
                    onSetDefault()
                },
            )
        }
    }
}

@Composable
private fun WalletCardCompactFooter(
    item: WalletAssetItem,
    isForeign: Boolean,
) {
    val wallet = item.wallet
    val targetAmount = wallet.targetAmount
    val hasGoal = wallet.purpose.supportsGoalTarget && targetAmount != null && targetAmount.amountInCents > 0L
    val hasCreditLimit = wallet.type.isCreditCardOrLine &&
        wallet.creditLimit != null &&
        wallet.creditLimit.amountInCents > 0L
    val isNegative = item.balance.amountInCents < 0L
    if (!isNegative && !hasGoal && !hasCreditLimit) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isNegative) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = AppTheme.colors.error,
                    modifier = Modifier.size(14.dp),
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
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (hasGoal) {
            val goalAmount = requireNotNull(targetAmount)
            val ratio = (item.balance.amountInCents.toFloat() / goalAmount.amountInCents.toFloat())
                .coerceIn(0f, 1f)
            val pctInt = (ratio * 100f).roundToInt()
            val formattedCurrent = if (isForeign) {
                CurrencyConverter.formatForeignAmount(item.balance.amountInCents, wallet.currencyCode)
            } else {
                MoneyFormatter.format(item.balance)
            }
            val formattedTarget = if (isForeign) {
                CurrencyConverter.formatForeignAmount(goalAmount.amountInCents, wallet.currencyCode)
            } else {
                MoneyFormatter.format(goalAmount)
            }
            Text(
                text = stringResource(
                    R.string.wallet_goal_progress_format,
                    formattedCurrent,
                    formattedTarget,
                    pctInt,
                ),
                style = AppTheme.typography.labelSmall,
                color = AppTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(AppTheme.shapes.capsule),
                color = AppTheme.colors.sand,
                trackColor = AppTheme.colors.surfaceSunken,
            )
        }

        if (hasCreditLimit) {
            val limit = requireNotNull(wallet.creditLimit)
            val usedDebt = if (item.balance.amountInCents < 0L) -item.balance.amountInCents else 0L
            val available = maxOf(0L, limit.amountInCents - usedDebt)
            Text(
                text = stringResource(
                    R.string.assets_credit_available_format,
                    CurrencyConverter.formatForeignAmount(available, wallet.currencyCode),
                ),
                style = AppTheme.typography.labelSmall,
                color = AppTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
