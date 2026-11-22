package com.notepay.feature.stats.main.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.localizedName
import com.notepay.domain.analytics.CategoryYoYDelta
import com.notepay.domain.analytics.LongTermTrendAnalysis
import com.notepay.domain.analytics.RollingMonthComparisonPoint
import com.notepay.domain.analytics.SeasonalPatternInsight
import com.notepay.domain.analytics.VietnameseSeason
import com.notepay.domain.money.Money
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun LongTermTrendSection(
    analysis: LongTermTrendAnalysis,
    showAmounts: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        border = BorderStroke(1.dp, AppTheme.colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.spaceLarge),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderRow(nextTetDays = analysis.nextTetDays)

            when (analysis) {
                is LongTermTrendAnalysis.InsufficientData -> ColdStartCardContent(
                    analysis = analysis,
                    showAmounts = showAmounts,
                )
                is LongTermTrendAnalysis.Ready -> ReadyTrendContent(
                    analysis = analysis,
                    showAmounts = showAmounts,
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(nextTetDays: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoGraph,
                contentDescription = null,
                tint = AppTheme.colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.stats_long_term_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (nextTetDays != null && nextTetDays in 0..90) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = AppTheme.shapes.capsule,
                color = AppTheme.colors.sand,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Celebration,
                        contentDescription = null,
                        tint = AppTheme.colors.onSand,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.stats_tet_countdown_badge, nextTetDays),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.onSand,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColdStartCardContent(
    analysis: LongTermTrendAnalysis.InsufficientData,
    showAmounts: Boolean,
) {
    val progress = (analysis.monthsAvailable.toFloat() / analysis.monthsRequired.toFloat())
        .coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(
                R.string.stats_cold_start_title,
                analysis.monthsAvailable,
                analysis.monthsRequired,
            ),
            style = AppTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.textPrimary,
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(AppTheme.shapes.capsule),
            color = AppTheme.colors.sand,
            trackColor = AppTheme.colors.surfaceSunken,
        )

        Text(
            text = stringResource(R.string.stats_cold_start_desc),
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textSecondary,
        )

        analysis.threeMonthFallback?.let { fallback ->
            val pctText = fallback.changePercent?.let { pct ->
                val sign = if (pct >= 0f) "+" else "-"
                "$sign${String.format(Locale.ROOT, "%.1f", abs(pct))}%"
            } ?: MoneyFormatter.format(Money(abs(fallback.deltaCents)), showAmounts)

            Surface(
                shape = AppTheme.shapes.corner16,
                color = AppTheme.colors.surfaceSunken,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.stats_cold_start_mom_label, pctText),
                    style = AppTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.colors.textPrimary,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun ReadyTrendContent(
    analysis: LongTermTrendAnalysis.Ready,
    showAmounts: Boolean,
) {
    val yoy = analysis.yoy
    val pct = yoy.expenseChangePercent
    val isHigher = (pct ?: 0f) > 0f || yoy.expenseDeltaCents > 0L
    val badgeColor = if (isHigher) AppTheme.colors.error else AppTheme.colors.success

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.stats_yoy_same_month_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = MoneyFormatter.format(Money(yoy.currentExpenseCents), showAmounts),
                    style = AppTheme.typography.amountCard,
                    color = AppTheme.colors.textPrimary,
                )
            }

            if (pct != null) {
                Surface(
                    shape = AppTheme.shapes.capsule,
                    color = badgeColor.copy(alpha = 0.14f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (isHigher) {
                                Icons.AutoMirrored.Rounded.TrendingUp
                            } else {
                                Icons.AutoMirrored.Rounded.TrendingDown
                            },
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp),
                        )
                        val sign = if (pct >= 0f) "+" else "-"
                        Text(
                            text = "$sign${String.format(Locale.ROOT, "%.1f", abs(pct))}%",
                            style = AppTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.stats_yoy_no_prior_spend),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }

        if (analysis.rolling12Months.isNotEmpty()) {
            TwoYearComparisonLineChart(points = analysis.rolling12Months)
            ChartLegendRow()
        }

        CategoryDeltaSummaryRow(
            rising = yoy.topRisingCategories,
            falling = yoy.topFallingCategories,
            showAmounts = showAmounts,
        )

        analysis.seasonalInsights.forEach { insight ->
            SeasonalInsightCard(
                insight = insight,
                showAmounts = showAmounts,
            )
        }
    }
}

@Composable
private fun TwoYearComparisonLineChart(points: List<RollingMonthComparisonPoint>) {
    val cd = stringResource(R.string.stats_long_term_chart_cd)
    val currentLineColor = AppTheme.colors.ink
    val priorLineColor = AppTheme.colors.textMuted
    val sandAccentColor = AppTheme.colors.sand
    val gridColor = AppTheme.colors.hairline

    val currentPath = remember { Path() }
    val priorPath = remember { Path() }
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) }
    val currentStroke = remember { Stroke(width = 6f, cap = StrokeCap.Round) }
    val priorStroke = remember(dashEffect) { Stroke(width = 4f, cap = StrokeCap.Round, pathEffect = dashEffect) }

    val maxExpense = remember(points) {
        points.maxOfOrNull { maxOf(it.currentYearExpenseCents, it.priorYearExpenseCents) }
            ?.coerceAtLeast(1L) ?: 1L
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .semantics { contentDescription = cd },
    ) {
        val w = size.width
        val h = size.height
        val padY = 12f
        val plotH = (h - padY * 2f).coerceAtLeast(1f)
        val count = points.size
        if (count < 2) return@Canvas

        drawLine(
            color = gridColor,
            start = Offset(0f, h - padY),
            end = Offset(w, h - padY),
            strokeWidth = 2f,
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, padY + plotH / 2f),
            end = Offset(w, padY + plotH / 2f),
            strokeWidth = 1.5f,
        )

        currentPath.reset()
        priorPath.reset()
        val stepX = w / (count - 1).toFloat()

        points.forEachIndexed { idx, pt ->
            val x = idx * stepX
            val yCur = padY + plotH * (1f - (pt.currentYearExpenseCents.toFloat() / maxExpense.toFloat()))
            val yPrev = padY + plotH * (1f - (pt.priorYearExpenseCents.toFloat() / maxExpense.toFloat()))
            if (idx == 0) {
                currentPath.moveTo(x, yCur)
                priorPath.moveTo(x, yPrev)
            } else {
                currentPath.lineTo(x, yCur)
                priorPath.lineTo(x, yPrev)
            }
        }

        drawPath(path = priorPath, color = priorLineColor, style = priorStroke)
        drawPath(path = currentPath, color = currentLineColor, style = currentStroke)

        val lastIdx = points.lastIndex
        val lastY = padY + plotH * (1f - (points[lastIdx].currentYearExpenseCents.toFloat() / maxExpense.toFloat()))
        drawCircle(
            color = sandAccentColor,
            radius = 8f,
            center = Offset(lastIdx * stepX, lastY),
        )
    }
}

@Composable
private fun ChartLegendRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendDot(color = AppTheme.colors.ink, label = stringResource(R.string.stats_chart_this_year))
        LegendDot(color = AppTheme.colors.textMuted, label = stringResource(R.string.stats_chart_last_year))
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun CategoryDeltaSummaryRow(
    rising: List<CategoryYoYDelta>,
    falling: List<CategoryYoYDelta>,
    showAmounts: Boolean,
) {
    if (rising.isEmpty() && falling.isEmpty()) return
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (rising.isNotEmpty()) {
            val risingText = rising.joinToString(", ") { item ->
                "${item.category.localizedName(context)} (+${MoneyFormatter.format(Money(item.deltaCents), showAmounts)})"
            }
            Text(
                text = "${stringResource(R.string.stats_yoy_rising_label)} $risingText",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (falling.isNotEmpty()) {
            val fallingText = falling.joinToString(", ") { item ->
                "${item.category.localizedName(context)} (-${MoneyFormatter.format(Money(abs(item.deltaCents)), showAmounts)})"
            }
            Text(
                text = "${stringResource(R.string.stats_yoy_falling_label)} $fallingText",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.success,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SeasonalInsightCard(
    insight: SeasonalPatternInsight,
    showAmounts: Boolean,
) {
    val context = LocalContext.current
    val seasonTitleRes = when (insight.season) {
        VietnameseSeason.PRE_TET -> R.string.stats_season_pre_tet
        VietnameseSeason.POST_TET -> R.string.stats_season_post_tet
        VietnameseSeason.WEDDING_SEASON -> R.string.stats_season_wedding
        VietnameseSeason.SUMMER_TRAVEL -> R.string.stats_season_summer
    }
    val pctAbs = abs(insight.changePercent).roundToInt()
    val isSpike = insight.changePercent >= 0f
    val formattedMonthly = MoneyFormatter.format(Money(insight.seasonalMonthlyEquivalentCents), showAmounts)

    Surface(
        shape = AppTheme.shapes.corner16,
        color = AppTheme.colors.surfaceSunken,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = AppTheme.shapes.capsule,
                    color = AppTheme.colors.sand,
                ) {
                    Text(
                        text = stringResource(seasonTitleRes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.onSand,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                text = stringResource(
                    if (isSpike) R.string.stats_season_spike_desc else R.string.stats_season_dip_desc,
                    pctAbs,
                    formattedMonthly,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (insight.topCategories.isNotEmpty()) {
                val catsText = insight.topCategories.joinToString(", ") { it.localizedName(context) }
                Text(
                    text = stringResource(R.string.stats_season_top_categories, catsText),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
