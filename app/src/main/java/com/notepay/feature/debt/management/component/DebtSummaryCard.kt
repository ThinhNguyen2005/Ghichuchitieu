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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.money.Money
import com.notepay.domain.usecase.debt.DebtSummary
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.NotePayTheme
import com.notepay.core.designsystem.formatter.MoneyFormatter

/**
 * Thẻ tổng quan nợ v4 Sand & Ink:
 * - Nền thẻ ink bo 28dp phẳng, viền 1px ở dark mode.
 * - Icon handshake với nền inkRaised và tint sand.
 * - Cần thu dùng màu [AppTheme.colors.onInkIncome], Cần trả dùng màu [AppTheme.colors.onInkExpense].
 * - Thanh tỉ lệ thu/trả: đoạn 1 [AppTheme.colors.sand], đoạn 2 [AppTheme.colors.graphite].
 * - Bỏ tương tác click khi không truyền [onClick].
 */
@Composable
fun DebtSummaryCard(
    summary: DebtSummary?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val isLight = AppTheme.colors.isLight
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = AppTheme.shapes.corner28,
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.ink
        ),
        border = if (!isLight) BorderStroke(1.dp, AppTheme.colors.inkBorder) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Handshake Icon + Title + Net Balance + Optional Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(AppTheme.shapes.capsule)
                        .background(AppTheme.colors.inkRaised),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Handshake,
                        contentDescription = null,
                        tint = AppTheme.colors.sand,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.debt_summary_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.onInk
                    )
                    if (summary != null) {
                        Text(
                            text = stringResource(
                                R.string.debt_summary_net,
                                MoneyFormatter.format(summary.netBalance)
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = if (summary.netBalance.amountInCents >= 0) {
                                AppTheme.colors.onInkIncome
                            } else {
                                AppTheme.colors.onInkExpense
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = AppTheme.colors.onInkMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Thanh tỉ lệ thu/trả (đoạn 1 sand, đoạn 2 graphite)
            val collectCents = summary?.totalToCollect?.amountInCents?.coerceAtLeast(0L) ?: 0L
            val payCents = summary?.totalToPay?.amountInCents?.coerceAtLeast(0L) ?: 0L
            val totalCents = collectCents + payCents

            if (totalCents > 0L) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(AppTheme.shapes.corner12)
                        .background(AppTheme.colors.inkRaised)
                ) {
                    if (collectCents > 0L) {
                        Box(
                            modifier = Modifier
                                .weight(collectCents.toFloat())
                                .fillMaxHeight()
                                .background(AppTheme.colors.sand)
                        )
                    }
                    if (payCents > 0L) {
                        Box(
                            modifier = Modifier
                                .weight(payCents.toFloat())
                                .fillMaxHeight()
                                .background(AppTheme.colors.graphite)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(AppTheme.shapes.corner12)
                        .background(AppTheme.colors.inkRaised)
                )
            }

            // Numbers: Cần thu (onInkIncome) & Cần trả (onInkExpense)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cần thu
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(AppTheme.shapes.corner16)
                        .background(AppTheme.colors.inkRaised)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = AppTheme.colors.onInkIncome,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.debt_collect),
                                style = MaterialTheme.typography.labelMedium,
                                color = AppTheme.colors.onInkMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = summary?.let { MoneyFormatter.format(it.totalToCollect) }
                                ?: MoneyFormatter.format(Money.ZERO),
                            style = AppTheme.typography.amountCard,
                            color = AppTheme.colors.onInkIncome
                        )
                    }
                }

                // Cần trả
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(AppTheme.shapes.corner16)
                        .background(AppTheme.colors.inkRaised)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                                contentDescription = null,
                                tint = AppTheme.colors.onInkExpense,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.debt_tab_borrow),
                                style = MaterialTheme.typography.labelMedium,
                                color = AppTheme.colors.onInkMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = summary?.let { MoneyFormatter.format(it.totalToPay) }
                                ?: MoneyFormatter.format(Money.ZERO),
                            style = AppTheme.typography.amountCard,
                            color = AppTheme.colors.onInkExpense
                        )
                    }
                }
            }
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
