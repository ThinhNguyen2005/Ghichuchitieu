package com.notepay.feature.debt.management.component

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
import com.notepay.core.designsystem.theme.NotePayTheme
import com.notepay.domain.money.Money
import com.notepay.domain.usecase.debt.DebtSummary

/**
 * Debt overview card v5 (Luminous iOS & Atmospheric Canvas):
 * Uses a luminous gradient elevated hero surface when [isHero] = true, and a clean surface card
 * with soft iOS shadow when [isHero] = false.
 */
@Composable
fun DebtSummaryCard(
    summary: DebtSummary?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isHero: Boolean = onClick == null,
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
    val cardBorder = if (isHero) {
        BorderStroke(1.dp, if (isLight) HomeHeroCardLightRim else HomeHeroCardDarkRim)
    } else {
        BorderStroke(0.5.dp, colors.hairline)
    }

    val iconBgColor = if (isHero) colors.sand.copy(alpha = if (isLight) 0.42f else 0.24f) else colors.surfaceSunken
    val iconTint = colors.textPrimary
    val titleColor = colors.textPrimary
    val mutedColor = colors.textSecondary
    val collectColor = colors.success
    val payColor = colors.error
    val tileAlpha = if (isLight) 0.10f else 0.16f
    val ratioBarBg = colors.surfaceSunken

    val clickLabel = stringResource(R.string.debt_nav_title)
    val baseModifier = modifier
        .fillMaxWidth()
        .iosCardShadow(shape = cardShape, elevated = isHero)
        .semantics(mergeDescendants = true) {}
    val cardModifier = if (onClick != null) {
        baseModifier
            .clip(cardShape)
            .clickable(
                role = Role.Button,
                onClickLabel = clickLabel,
                onClick = onClick,
            )
    } else {
        baseModifier
    }

    Card(
        modifier = cardModifier,
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isHero) Color.Transparent else colors.surface,
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isHero) Modifier.background(heroGradient) else Modifier)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(AppTheme.shapes.capsule)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Handshake,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.debt_summary_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                    )
                    if (summary != null) {
                        Text(
                            text = stringResource(
                                R.string.debt_summary_net,
                                MoneyFormatter.format(summary.netBalance),
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFeatureSettings = "tnum",
                            ),
                            color = if (summary.netBalance.amountInCents >= 0) {
                                collectColor
                            } else {
                                payColor
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = mutedColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            val collectCents = summary?.totalToCollect?.amountInCents?.coerceAtLeast(0L) ?: 0L
            val payCents = summary?.totalToPay?.amountInCents?.coerceAtLeast(0L) ?: 0L
            val totalCents = collectCents + payCents

            if (totalCents > 0L) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(AppTheme.shapes.corner12)
                        .background(ratioBarBg),
                ) {
                    if (collectCents > 0L) {
                        Box(
                            modifier = Modifier
                                .weight(collectCents.toFloat())
                                .fillMaxHeight()
                                .background(collectColor),
                        )
                    }
                    if (payCents > 0L) {
                        Box(
                            modifier = Modifier
                                .weight(payCents.toFloat())
                                .fillMaxHeight()
                                .background(payColor),
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(AppTheme.shapes.corner12)
                        .background(ratioBarBg),
                )
            }

            val collectAmountText = summary?.let { MoneyFormatter.format(it.totalToCollect) }
                ?: MoneyFormatter.format(Money.ZERO)
            val payAmountText = summary?.let { MoneyFormatter.format(it.totalToPay) }
                ?: MoneyFormatter.format(Money.ZERO)

            if (isLargeFont) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DebtSummaryMetricTile(
                        label = stringResource(R.string.debt_collect),
                        amountText = collectAmountText,
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        accentColor = collectColor,
                        containerAlpha = tileAlpha,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DebtSummaryMetricTile(
                        label = stringResource(R.string.debt_tab_borrow),
                        amountText = payAmountText,
                        icon = Icons.AutoMirrored.Rounded.TrendingDown,
                        accentColor = payColor,
                        containerAlpha = tileAlpha,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DebtSummaryMetricTile(
                        label = stringResource(R.string.debt_collect),
                        amountText = collectAmountText,
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        accentColor = collectColor,
                        containerAlpha = tileAlpha,
                        modifier = Modifier.weight(1f),
                    )
                    DebtSummaryMetricTile(
                        label = stringResource(R.string.debt_tab_borrow),
                        amountText = payAmountText,
                        icon = Icons.AutoMirrored.Rounded.TrendingDown,
                        accentColor = payColor,
                        containerAlpha = tileAlpha,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtSummaryMetricTile(
    label: String,
    amountText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    containerAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.corner16,
        color = accentColor.copy(alpha = containerAlpha),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTheme.colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = amountText,
                style = AppTheme.typography.amountCard,
                color = accentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ==== PREVIEWS ====

private val sampleSummary = DebtSummary(
    totalToCollect = Money(15_000_000_00),
    totalToPay = Money(5_000_000_00),
    netBalance = Money(10_000_000_00),
    activeDebtsCount = 4,
    overdueCount = 1,
    dueTodayCount = 0,
)

@Preview(name = "Light", showBackground = true)
@Composable
private fun DebtSummaryCardPreviewLight() {
    NotePayTheme(darkTheme = false) {
        DebtSummaryCard(
            summary = sampleSummary,
            modifier = Modifier.padding(16.dp),
            onClick = {},
        )
    }
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun DebtSummaryCardPreviewDark() {
    NotePayTheme(darkTheme = true) {
        DebtSummaryCard(
            summary = sampleSummary,
            modifier = Modifier.padding(16.dp),
            onClick = {},
        )
    }
}

@Preview(name = "FontScale 1.3", fontScale = 1.3f, showBackground = true)
@Composable
private fun DebtSummaryCardPreviewFont13() {
    NotePayTheme(darkTheme = false) {
        DebtSummaryCard(
            summary = sampleSummary,
            modifier = Modifier.padding(16.dp),
            onClick = {},
        )
    }
}

@Preview(name = "FontScale 2.0", fontScale = 2.0f, showBackground = true)
@Composable
private fun DebtSummaryCardPreviewFont20() {
    NotePayTheme(darkTheme = false) {
        DebtSummaryCard(
            summary = sampleSummary,
            modifier = Modifier.padding(16.dp),
            onClick = {},
        )
    }
}
