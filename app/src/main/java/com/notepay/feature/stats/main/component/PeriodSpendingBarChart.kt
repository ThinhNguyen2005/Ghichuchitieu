package com.notepay.feature.stats.main.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.localizedName
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.domain.analytics.PeriodBarBucketKind
import com.notepay.domain.analytics.StatsChartCalculator
import com.notepay.domain.analytics.StatsRange
import com.notepay.domain.model.Category
import com.notepay.domain.money.Money
import com.notepay.feature.stats.main.PeriodBarPoint
import com.notepay.feature.stats.main.tabularNums

private const val InteractiveBarLimit = 12
private const val DimmedBarAlpha = 0.42f
private const val DefaultBarAlpha = 0.88f

@Composable
internal fun PeriodSpendingBarChart(
    bars: List<PeriodBarPoint>,
    metric: StatsMetric,
    periodRange: StatsRange?,
    selectedCategory: Category?,
    showAmounts: Boolean,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember(bars, metric, selectedCategory) { mutableStateOf<Int?>(null) }
    val validSelectedIndex = selectedIndex?.takeIf { it in bars.indices }

    val sectionTitle = periodBarTitle(
        bars = bars,
        metric = metric,
        periodRange = periodRange,
        selectedCategory = selectedCategory,
    )

    if (bars.isEmpty() || bars.all { it.amount.amountInCents == 0L }) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.corner16)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.stats_period_bar_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    val barColor = if (metric == StatsMetric.CHI_TIEU) AppTheme.colors.error else AppTheme.colors.success
    val values = remember(bars) { bars.map { it.amount.amountInCents } }
    val axisScale = remember(values) { StatsChartCalculator.trendAxisScale(values, forecast = null) }
    val hiddenAmount = stringResource(R.string.stats_amounts_hidden)
    val topAxisLabel = if (showAmounts) {
        MoneyFormatter.formatCompactVietnamese(Money(axisScale.topInCents))
    } else {
        hiddenAmount
    }
    val middleAxisLabel = if (showAmounts) {
        MoneyFormatter.formatCompactVietnamese(Money(axisScale.topInCents / 2))
    } else {
        hiddenAmount
    }

    val peakBar = remember(bars) { bars.maxByOrNull { it.amount.amountInCents } }
    val peakText = if (showAmounts && peakBar != null) {
        MoneyFormatter.formatCompactVietnamese(peakBar.amount)
    } else {
        hiddenAmount
    }
    val averageText = if (showAmounts) {
        MoneyFormatter.formatCompactVietnamese(Money(StatsChartCalculator.average(values)))
    } else {
        hiddenAmount
    }

    val selectedBar = validSelectedIndex?.let { bars[it] }
    val selectedBarLabel = selectedBar?.let { formatFullBucketLabel(it) }
    val clearSelectionLabel = stringResource(R.string.stats_period_bar_clear_selection)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (selectedBar != null && selectedBarLabel != null) {
                val readout = "$selectedBarLabel · ${MoneyFormatter.format(selectedBar.amount, showAmounts)}"
                Surface(
                    shape = AppTheme.shapes.capsule,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(AppTheme.shapes.capsule)
                        .semantics {
                            role = Role.Button
                            onClick(label = clearSelectionLabel, action = null)
                        }
                        .clickable { selectedIndex = null },
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = readout,
                            style = MaterialTheme.typography.labelMedium.tabularNums,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.stats_period_bar_peak_format, peakText),
                    style = MaterialTheme.typography.labelMedium.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        val reducedMotion = rememberNotePayReducedMotion()
        val animSpec = if (reducedMotion) snap<Float>() else tween<Float>(durationMillis = 220)
        val isCompactDense = bars.size > InteractiveBarLimit
        val barShape = RoundedCornerShape(
            topStart = if (isCompactDense) 4.dp else 8.dp,
            topEnd = if (isCompactDense) 4.dp else 8.dp,
        )
        val chartSummaryCd = stringResource(
            R.string.stats_period_bar_chart_cd_format,
            sectionTitle,
            peakText,
            averageText,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .padding(end = 6.dp),
            ) {
                Text(
                    text = topAxisLabel,
                    style = MaterialTheme.typography.labelSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.TopEnd),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = middleAxisLabel,
                    style = MaterialTheme.typography.labelSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val plotModifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .let { base ->
                    if (!isCompactDense) {
                        base
                    } else {
                        base
                            .semantics { contentDescription = chartSummaryCd }
                            .pointerInput(bars) {
                                detectTapGestures { offset ->
                                    if (size.width <= 0 || bars.isEmpty()) return@detectTapGestures
                                    val idx = ((offset.x / size.width.toFloat()) * bars.size)
                                        .toInt()
                                        .coerceIn(0, bars.lastIndex)
                                    selectedIndex = if (selectedIndex == idx) null else idx
                                }
                            }
                            .pointerInput(bars) {
                                detectHorizontalDragGestures { change, _ ->
                                    if (size.width <= 0 || bars.isEmpty()) return@detectHorizontalDragGestures
                                    change.consume()
                                    val idx = ((change.position.x / size.width.toFloat()) * bars.size)
                                        .toInt()
                                        .coerceIn(0, bars.lastIndex)
                                    selectedIndex = idx
                                }
                            }
                    }
                }

            Box(modifier = plotModifier) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 1.dp.toPx()
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = strokePx,
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = strokePx,
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, size.height - strokePx / 2f),
                        end = Offset(size.width, size.height - strokePx / 2f),
                        strokeWidth = strokePx,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(
                        when {
                            bars.size <= 7 -> 8.dp
                            bars.size <= InteractiveBarLimit -> 4.dp
                            else -> 2.dp
                        },
                    ),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    bars.forEachIndexed { index, point ->
                        val targetFraction = if (axisScale.topInCents > 0L) {
                            (point.amount.amountInCents.toFloat() / axisScale.topInCents).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        val valueFraction by animateFloatAsState(
                            targetValue = targetFraction,
                            animationSpec = animSpec,
                            label = "period-bar-${point.key}",
                        )
                        val isBarSelected = validSelectedIndex == index
                        val barAlpha = when {
                            validSelectedIndex == null -> if (point.isCurrentBucket) 1f else DefaultBarAlpha
                            isBarSelected -> 1f
                            else -> DimmedBarAlpha
                        }
                        val fillWidthFraction = when {
                            bars.size <= 7 -> 0.56f
                            bars.size <= InteractiveBarLimit -> 0.72f
                            else -> 0.85f
                        }

                        if (!isCompactDense) {
                            val pointLabel = formatFullBucketLabel(point)
                            val pointAmount = MoneyFormatter.format(point.amount, showAmounts)
                            val pointCd = stringResource(
                                R.string.stats_period_bar_point_cd_format,
                                pointLabel,
                                pointAmount,
                            )
                            val stateDesc = stringResource(
                                if (isBarSelected) {
                                    R.string.stats_allocation_selected
                                } else {
                                    R.string.stats_allocation_unselected
                                },
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .heightIn(min = 48.dp)
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = pointCd
                                        role = Role.Button
                                        stateDescription = stateDesc
                                    }
                                    .clickable {
                                        selectedIndex = if (selectedIndex == index) null else index
                                    },
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fillWidthFraction)
                                        .fillMaxHeight(valueFraction)
                                        .clip(barShape)
                                        .background(barColor.copy(alpha = barAlpha)),
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fillWidthFraction)
                                        .fillMaxHeight(valueFraction)
                                        .clip(barShape)
                                        .background(barColor.copy(alpha = barAlpha)),
                                )
                            }
                        }
                    }
                }
            }
        }

        PeriodBarXAxisLabels(
            bars = bars,
            selectedIndex = validSelectedIndex,
        )
    }
}

@Composable
private fun PeriodBarXAxisLabels(
    bars: List<PeriodBarPoint>,
    selectedIndex: Int?,
) {
    val fontScale = LocalDensity.current.fontScale
    val isSingleColumnLabel = bars.size <= 7 && fontScale < 1.7f

    if (isSingleColumnLabel) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp, top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bars.forEachIndexed { index, point ->
                val highlighted = index == selectedIndex || (selectedIndex == null && point.isCurrentBucket)
                Text(
                    text = formatShortAxisLabel(point),
                    style = MaterialTheme.typography.labelMedium.tabularNums,
                    color = if (highlighted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    } else {
        val stride = when {
            bars.size <= 7 -> 2
            bars.size <= InteractiveBarLimit -> if (fontScale >= 1.5f) 3 else 2
            else -> if (fontScale >= 1.5f) 7 else 5
        }
        val visibleIndices = bars.indices.filter { index ->
            index == 0 ||
                index == bars.lastIndex ||
                (index % stride == 0 && bars.lastIndex - index >= stride / 2 + 1)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp, top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            visibleIndices.forEach { index ->
                val point = bars[index]
                val highlighted = index == selectedIndex || (selectedIndex == null && point.isCurrentBucket)
                Text(
                    text = formatShortAxisLabel(point),
                    style = MaterialTheme.typography.labelMedium.tabularNums,
                    color = if (highlighted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun periodBarTitle(
    bars: List<PeriodBarPoint>,
    metric: StatsMetric,
    periodRange: StatsRange?,
    selectedCategory: Category?,
): String {
    val isExpense = metric == StatsMetric.CHI_TIEU
    val bucketKind = bars.firstOrNull()?.bucketKind
    val titleRes = when {
        bucketKind == PeriodBarBucketKind.YEAR ->
            if (isExpense) R.string.stats_period_bar_yearly_expense else R.string.stats_period_bar_yearly_income
        bucketKind == PeriodBarBucketKind.MONTH || periodRange == StatsRange.YEAR ->
            if (isExpense) R.string.stats_period_bar_monthly_expense else R.string.stats_period_bar_monthly_income
        else ->
            if (isExpense) R.string.stats_period_bar_daily_expense else R.string.stats_period_bar_daily_income
    }
    val baseTitle = stringResource(titleRes)
    return if (selectedCategory != null) {
        "$baseTitle • ${selectedCategory.localizedName()}"
    } else {
        baseTitle
    }
}

@Composable
private fun formatShortAxisLabel(point: PeriodBarPoint): String = when (point.bucketKind) {
    PeriodBarBucketKind.WEEKDAY -> weekdayLabel(point.indexValue)
    PeriodBarBucketKind.DAY_OF_MONTH -> point.indexValue.toString()
    PeriodBarBucketKind.MONTH -> stringResource(R.string.stats_month_label_format, point.indexValue)
    PeriodBarBucketKind.YEAR -> point.indexValue.toString()
    PeriodBarBucketKind.DATE_SHORT -> stringResource(
        R.string.stats_period_bar_date_short_format,
        point.indexValue,
        point.monthValue ?: 1,
    )
}

@Composable
private fun formatFullBucketLabel(point: PeriodBarPoint): String = when (point.bucketKind) {
    PeriodBarBucketKind.WEEKDAY -> weekdayLabel(point.indexValue)
    PeriodBarBucketKind.DAY_OF_MONTH,
    PeriodBarBucketKind.DATE_SHORT -> stringResource(
        R.string.stats_period_bar_day_of_month_format,
        point.indexValue,
        point.monthValue ?: 1,
    )
    PeriodBarBucketKind.MONTH -> stringResource(R.string.stats_month_label_format, point.indexValue)
    PeriodBarBucketKind.YEAR -> point.indexValue.toString()
}

@Composable
private fun weekdayLabel(isoDayNumber: Int): String = when (isoDayNumber) {
    1 -> stringResource(R.string.calendar_weekday_monday)
    2 -> stringResource(R.string.calendar_weekday_tuesday)
    3 -> stringResource(R.string.calendar_weekday_wednesday)
    4 -> stringResource(R.string.calendar_weekday_thursday)
    5 -> stringResource(R.string.calendar_weekday_friday)
    6 -> stringResource(R.string.calendar_weekday_saturday)
    else -> stringResource(R.string.calendar_weekday_sunday)
}
