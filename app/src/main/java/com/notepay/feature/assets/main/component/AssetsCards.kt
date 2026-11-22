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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notepay.R
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
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
    if (isOther) AppTheme.colors.onInkMuted else WalletUiHelper.getColor(colorKey.orEmpty())

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
    val reducedMotion = rememberNotePayReducedMotion()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner28,
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.ink,
        ),
        border = if (!AppTheme.colors.isLight) {
            BorderStroke(1.dp, AppTheme.colors.inkBorder)
        } else {
            null
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.spaceExtraLarge),
        ) {
            // Dòng trên: nhãn "Tổng tài sản ròng" + badge biến động trong kỳ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.assets_total_balance),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.onInkMuted,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))

                if (changePercentage != null && changePercentage != 0f) {
                    val isPositive = changePercentage >= 0f
                    val trendColor = if (isPositive) AppTheme.colors.onInkIncome else AppTheme.colors.onInkExpense
                    Surface(
                        shape = AppTheme.shapes.capsule,
                        color = trendColor.copy(alpha = 0.18f),
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
                            val locale = LocalLocale.current.platformLocale
                            val formattedPct = String.format(locale, "%.1f", abs(changePercentage))
                            Text(
                                text = stringResource(
                                    if (isPositive) R.string.percent_change_positive else R.string.percent_change_negative,
                                    formattedPct,
                                ),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = trendColor,
                            )
                        }
                    }
                } else if (changePercentage != null) {
                    Surface(
                        shape = AppTheme.shapes.capsule,
                        color = AppTheme.colors.onInk.copy(alpha = 0.10f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = AppTheme.dimensions.spaceSmall, vertical = 3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = null,
                                tint = AppTheme.colors.onInkMuted,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceExtraSmall))
                            Text(
                                text = stringResource(R.string.badge_stable),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppTheme.colors.onInkMuted,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))

            // Số tiền Hero Net Worth
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
                    color = AppTheme.colors.onInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // UX: Xử lý cảnh báo số âm dưới số tiền hero
            if (totalNetWorth.amountInCents < 0L) {
                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = AppTheme.colors.onInkExpense,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                    Text(
                        text = stringResource(R.string.assets_negative_balance_hint),
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.onInkExpense,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceMedium))

            // 2 Thẻ Tóm tắt: Tổng Tài sản vs Tổng Nợ phải trả
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceMedium),
            ) {
                // Thẻ Tổng Tài sản
                Surface(
                    shape = AppTheme.shapes.corner16,
                    color = AppTheme.colors.inkRaised,
                    modifier = Modifier.weight(1f),
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
                                    .background(AppTheme.colors.onInkIncome),
                            )
                            Text(
                                text = stringResource(R.string.assets_total_assets_label),
                                style = AppTheme.typography.labelSmall,
                                color = AppTheme.colors.onInkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = MoneyFormatter.format(totalAssetsVnd),
                            style = AppTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.onInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.assets_wallets_count_format, assetCount, stringResource(R.string.wallet_fallback)),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.onInkMuted,
                            maxLines = 1,
                        )
                    }
                }

                // Thẻ Tổng Nợ phải trả
                Surface(
                    shape = AppTheme.shapes.corner16,
                    color = AppTheme.colors.inkRaised,
                    modifier = Modifier.weight(1f),
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
                                    .background(AppTheme.colors.onInkExpense),
                            )
                            Text(
                                text = stringResource(R.string.assets_total_liabilities_label),
                                style = AppTheme.typography.labelSmall,
                                color = AppTheme.colors.onInkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = MoneyFormatter.format(totalLiabilitiesVnd),
                            style = AppTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.onInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.assets_wallets_count_format, liabilityCount, stringResource(R.string.wallet_type_loan)),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.onInkMuted,
                            maxLines = 1,
                        )
                    }
                }
            }

            // Dòng trạng thái tỷ giá ngoại tệ (nếu có ví ngoại tệ)
            if (hasForeignCurrency) {
                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceMedium))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        tint = AppTheme.colors.onInkMuted,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.assets_rates_updated_format,
                            ratesLastUpdated ?: stringResource(R.string.assets_rate_today),
                        ),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onInkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))

            // Bộ chuyển đổi Cơ cấu Tài sản / Nợ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (allocationMode == AllocationMode.ASSET) {
                        stringResource(R.string.assets_positive_allocation_title)
                    } else {
                        stringResource(R.string.assets_view_allocation_liability)
                    },
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.onInkMuted,
                )

                Row(
                    modifier = Modifier
                        .clip(AppTheme.shapes.capsule)
                        .background(AppTheme.colors.inkRaised)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val isAssetSelected = allocationMode == AllocationMode.ASSET
                    Box(
                        modifier = Modifier
                            .clip(AppTheme.shapes.capsule)
                            .background(if (isAssetSelected) AppTheme.colors.sand else Color.Transparent)
                            .clickable { onAllocationModeChanged(AllocationMode.ASSET) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.assets_view_allocation_asset),
                            style = AppTheme.typography.labelSmall,
                            fontWeight = if (isAssetSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAssetSelected) AppTheme.colors.onSand else AppTheme.colors.onInkMuted,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(AppTheme.shapes.capsule)
                            .background(if (!isAssetSelected) AppTheme.colors.sand else Color.Transparent)
                            .clickable { onAllocationModeChanged(AllocationMode.LIABILITY) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.assets_view_allocation_liability),
                            style = AppTheme.typography.labelSmall,
                            fontWeight = if (!isAssetSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isAssetSelected) AppTheme.colors.onSand else AppTheme.colors.onInkMuted,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceMedium))

            // Thanh phân bổ và chú giải
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
                                color = AppTheme.colors.onInkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.percent_format, pct),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.onInk,
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
                    color = AppTheme.colors.onInkMuted,
                )
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceExtraLarge))

            // Hai Action Buttons (Chuyển tiền & Thêm ví)
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
                        containerColor = AppTheme.colors.sand,
                        contentColor = AppTheme.colors.onSand,
                        disabledContainerColor = AppTheme.colors.inkRaised,
                        disabledContentColor = AppTheme.colors.onInkMuted,
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
                    contentPadding = PaddingValues(
                        horizontal = AppTheme.dimensions.spaceMedium,
                        vertical = AppTheme.dimensions.spaceMedium,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.inkRaised,
                        contentColor = AppTheme.colors.onInk,
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
        BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.35f))
    } else {
        BorderStroke(1.dp, AppTheme.colors.separator)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
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
                    )
                }

                Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceMedium))

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = wallet.name,
                        style = AppTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Cảnh báo số âm / thấu chi
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
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Nhãn loại tài khoản
                        Surface(
                            shape = AppTheme.shapes.corner12,
                            color = colorValue.copy(alpha = 0.10f),
                        ) {
                            Text(
                                text = stringResource(WalletUiHelper.getWalletTypeTitleRes(wallet.type)),
                                style = AppTheme.typography.labelSmall,
                                color = colorValue,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (wallet.isShared) {
                            Spacer(modifier = Modifier.width(6.dp))
                            RoleBadge(role = wallet.sharedRole ?: SharedWalletRole.VIEWER)
                        }

                        if (wallet.purpose != WalletPurpose.PERSONAL && !wallet.isShared) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = AppTheme.shapes.corner12,
                                color = AppTheme.colors.sand.copy(alpha = 0.35f),
                            ) {
                                Text(
                                    text = stringResource(WalletUiHelper.getPurposeTitleRes(wallet.purpose)),
                                    style = AppTheme.typography.labelSmall,
                                    color = AppTheme.colors.onSand,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        if (!wallet.includeInTotalBalance) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = AppTheme.shapes.corner12,
                                color = AppTheme.colors.surfaceSunken,
                            ) {
                                Text(
                                    text = stringResource(R.string.wallet_excluded_from_total_badge),
                                    style = AppTheme.typography.labelSmall,
                                    color = AppTheme.colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))

                        if (wallet.isActive) {
                            Surface(
                                shape = AppTheme.shapes.corner12,
                                color = AppTheme.colors.primary.copy(alpha = 0.15f),
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
                                            .background(AppTheme.colors.primary),
                                    )
                                    Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceExtraSmall))
                                    Text(
                                        text = stringResource(R.string.wallet_badge_active),
                                        style = AppTheme.typography.labelMedium,
                                        color = AppTheme.colors.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                            Text(
                                text = stringResource(R.string.bullet_separator),
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary,
                            )
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                        }

                        Text(
                            text = stringResource(R.string.wallet_tx_count, item.transactionCount),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
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
                if (isForeign) {
                    Text(
                        text = stringResource(R.string.assets_approx_vnd_format, MoneyFormatter.format(item.convertedBalanceVnd)),
                        style = AppTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.textSecondary,
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Text(
                    text = primaryBalanceText,
                    style = AppTheme.typography.amountCard.copy(fontSize = 18.sp),
                    color = if (item.balance.amountInCents < 0L) AppTheme.colors.error else AppTheme.colors.textPrimary,
                )
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (hasIncome) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.semantics(mergeDescendants = true) {},
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                contentDescription = stringResource(R.string.home_income_month),
                                tint = AppTheme.colors.success,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceExtraSmall))
                            Text(
                                text = stringResource(
                                    R.string.amount_positive_format,
                                    MoneyFormatter.format(item.monthlyIncome),
                                ),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.success,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (hasExpense) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.semantics(mergeDescendants = true) {},
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                                contentDescription = stringResource(R.string.home_expense_month),
                                tint = AppTheme.colors.error,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceExtraSmall))
                            Text(
                                text = stringResource(
                                    R.string.amount_negative_format,
                                    MoneyFormatter.format(item.monthlyExpense),
                                ),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (canShowMembers) {
                        OutlinedButton(
                            onClick = { onManageMembers() },
                            shape = AppTheme.shapes.capsule,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Group,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.shared_wallet_members_action),
                                style = AppTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (canPayCard) {
                        OutlinedButton(
                            onClick = { onPayCard() },
                            shape = AppTheme.shapes.capsule,
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.assets_card_action_pay_card),
                                style = AppTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (canAdjustBalance) {
                        OutlinedButton(
                            onClick = { onAdjustBalance() },
                            shape = AppTheme.shapes.capsule,
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.assets_card_action_adjust_balance),
                                style = AppTheme.typography.labelSmall,
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
                                tint = AppTheme.colors.primary,
                            )
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                            Text(
                                text = stringResource(R.string.set_as_active_wallet),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
