package com.notepay.ui.feature.stats

import com.notepay.ui.theme.AppTheme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.ui.util.MoneyFormatter
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal data class TrendAxisScale(
    val topInCents: Long,
    val labels: List<Float>,
    val unitLabel: String,
)

internal object StatsChartCalculations {
    fun average(values: List<Long>): Long = if (values.isEmpty()) 0L else values.sum() / values.size

    fun trendAxisScale(actualValues: List<Long>, forecast: Money?): TrendAxisScale {
        val maxValCents = max(
            1L,
            max(actualValues.maxOrNull() ?: 0L, forecast?.amountInCents ?: 0L),
        )
        // Note: Money.amountInCents is stored in cents (1 VND = 100 cents).
        // Therefore, 1.000.000 VND = 100.000.000 cents.
        val isMillions = maxValCents >= 100_000_000L
        val unitDivisor = if (isMillions) 100_000_000f else 100_000f // Divisor to get units in Triệu or Nghìn VND
        val unitLabel = if (isMillions) "Triệu" else "Nghìn"

        val valInUnits = maxValCents.toFloat() / unitDivisor

        val (step, intervals) = when {
            valInUnits <= 1f -> 0.2f to 5
            valInUnits <= 2.5f -> 0.5f to 5
            valInUnits <= 5f -> 1f to 5
            valInUnits <= 10f -> 2f to 5
            valInUnits <= 20f -> 5f to 4
            valInUnits <= 50f -> 10f to 5
            valInUnits <= 60f -> 10f to 6
            valInUnits <= 100f -> 20f to 5
            valInUnits <= 200f -> 50f to 4
            valInUnits <= 500f -> 100f to 5
            valInUnits <= 1000f -> 200f to 5
            else -> {
                val rawStep = valInUnits / 4f
                val exp = kotlin.math.floor(kotlin.math.log10(rawStep.toDouble())).toFloat()
                val base = Math.pow(10.0, exp.toDouble()).toFloat()
                val stepVal = Math.ceil((rawStep / base).toDouble()).toFloat() * base
                val count = Math.ceil((valInUnits / stepVal).toDouble()).toInt().coerceIn(3, 6)
                stepVal to count
            }
        }

        val topInUnits = step * intervals
        val topInCents = (topInUnits * unitDivisor).toLong().coerceAtLeast(maxValCents)
        val labels = (intervals downTo 0).map { it * step }

        return TrendAxisScale(
            topInCents = topInCents,
            labels = labels,
            unitLabel = unitLabel,
        )
    }

    fun percentageChange(current: Long, previous: Long): Float? = when {
        previous == 0L -> null
        else -> ((current - previous).toDouble() / previous * 100).toFloat()
    }

    fun shouldShowForecast(metric: StatsMetric, isSelectedMonthCurrent: Boolean, forecast: Money?): Boolean {
        return metric == StatsMetric.CHI_TIEU && isSelectedMonthCurrent && forecast != null && forecast.amountInCents > 0L
    }
}

@Composable
internal fun AllocationChartContent(
    breakdown: List<CategoryBreakdownItem>,
    total: Money,
    showAmounts: Boolean,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
) {
    var centerItem by remember(selectedCategory, breakdown) {
        mutableStateOf(breakdown.find { it.category == selectedCategory })
    }
    val legendItems = breakdown.take(5)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1.05f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(breakdown) {
                        detectTapGestures { offset ->
                            val tapped = findAllocationItem(offset, size.width.toFloat(), size.height.toFloat(), 34.dp.toPx(), breakdown)
                            centerItem = if (tapped == centerItem) null else tapped
                            onCategorySelected(centerItem?.category)
                        }
                    },
            ) {
                val diameter = min(size.width, size.height) * .76f
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                var startAngle = 270f
                if (breakdown.isEmpty()) {
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.25f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round),
                    )
                } else {
                    breakdown.forEach { item ->
                        val sweep = item.percentage * 360f
                        val isSelected = item.category == selectedCategory
                        drawArc(
                            color = Color(item.category.colorArgb),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(
                                width = if (isSelected) 40.dp.toPx() else 32.dp.toPx(),
                                cap = StrokeCap.Butt,
                            ),
                        )
                        startAngle += sweep
                    }
                }
            }
            AllocationCenter(
                item = centerItem ?: breakdown.find { it.category == selectedCategory },
                total = total,
                showAmounts = showAmounts,
                isEmpty = breakdown.isEmpty(),
            )
        }
        Column(
            Modifier
                .weight(.95f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            if (legendItems.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.stats_allocation_no_data),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    legendItems.forEach { item ->
                        val selected = item.category == selectedCategory
                        val color = Color(item.category.colorArgb)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppTheme.shapes.corner12)
                                .clickable { onCategorySelected(if (selected) null else item.category) },
                            shape = AppTheme.shapes.corner12,
                            color = color.copy(alpha = if (selected) .18f else .08f),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(color),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${item.category.displayName} — ${(item.percentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllocationCenter(
    item: CategoryBreakdownItem?,
    total: Money,
    showAmounts: Boolean,
    isEmpty: Boolean = false,
) {
    val tint = item?.let { Color(it.category.colorArgb) } ?: MaterialTheme.colorScheme.primary
    val bg = if (isEmpty) {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
    } else if (item != null) {
        tint.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)
    }

    Surface(
        shape = CircleShape,
        color = bg,
    ) {
        Column(
            modifier = Modifier
                .size(102.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (isEmpty) stringResource(R.string.stats_no_data) else (item?.category?.displayName ?: stringResource(R.string.stats_total_allocation)),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isEmpty) MaterialTheme.colorScheme.onSurfaceVariant else if (item != null) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (showAmounts) MoneyFormatter.format(item?.amount ?: total) else "••••••",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item != null) {
                Spacer(Modifier.height(3.dp))
                Surface(
                    shape = AppTheme.shapes.capsule,
                    color = tint.copy(alpha = 0.18f),
                ) {
                    Text(
                        text = "${(item.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tint,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun TrendChartContent(
    points: List<MonthlyTrendPoint>,
    metric: StatsMetric,
    isSelectedMonthCurrent: Boolean,
    forecast: Money?,
    showAmounts: Boolean,
    onPointClick: (MonthlyTrendPoint) -> Unit,
) {
    val chartColor = if (metric == StatsMetric.CHI_TIEU) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val values = points.map { point ->
        if (metric == StatsMetric.CHI_TIEU) point.expense.amountInCents else point.income.amountInCents
    }
    val hasForecast = StatsChartCalculations.shouldShowForecast(metric, isSelectedMonthCurrent, forecast)
    val axisScale = StatsChartCalculations.trendAxisScale(values, if (hasForecast) forecast else null)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        // 1. Top Header Row: Unit Label & Colored Dot Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "(${axisScale.unitLabel})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(chartColor),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (metric == StatsMetric.CHI_TIEU) stringResource(R.string.stats_expense) else stringResource(R.string.stats_income),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(10.dp))

        // 2. Chart Grid & Bars Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            // Y-axis labels column
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                axisScale.labels.forEach { value ->
                    val labelText = if (axisScale.unitLabel == "Triệu") "%.1f".format(value) else "%.0f".format(value)
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Chart Canvas & Bars Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                // Horizontal grid lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val linesCount = axisScale.labels.size
                    repeat(linesCount) { index ->
                        val y = size.height * index / (linesCount - 1).coerceAtLeast(1)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }

                // Columns/Bars
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    points.forEachIndexed { index, point ->
                        val valueFraction by animateFloatAsState(
                            (values.getOrElse(index) { 0L }.toFloat() / axisScale.topInCents).coerceIn(0f, 1f),
                            label = "trend-bar$index",
                        )
                        val isCurrent = index == points.lastIndex
                        val forecastFraction = if (isCurrent && hasForecast && forecast != null) {
                            (forecast.amountInCents.toFloat() / axisScale.topInCents).coerceIn(0f, 1f)
                        } else {
                            null
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .clickable { onPointClick(point) },
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(.48f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                // Faded projection column (Vùng mờ dự đoán tiêu - chạm đỉnh forecast)
                                if (forecastFraction != null && forecastFraction > valueFraction) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(forecastFraction)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(chartColor.copy(alpha = 0.25f)),
                                    )
                                }
                                // Solid actual column (Cột thực tế)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(valueFraction)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(chartColor),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Dedicated X-axis month labels row (below chart grid, aligned under bars)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 42.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            points.forEachIndexed { index, point ->
                val isCurrent = index == points.lastIndex
                Text(
                    text = if (isCurrent && isSelectedMonthCurrent) stringResource(R.string.stats_current_month) else stringResource(R.string.stats_month_label_format, point.month),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 4. Forecast legend row (below X-axis month labels, with proper vertical margin)
        if (hasForecast) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.padding(start = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(chartColor.copy(alpha = 0.25f)),
                )
                Text(
                    text = stringResource(R.string.stats_forecast_legend),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun findAllocationItem(tap: Offset, width: Float, height: Float, strokePx: Float, breakdown: List<CategoryBreakdownItem>): CategoryBreakdownItem? {
    if (breakdown.isEmpty()) return null
    val diameter = min(width, height) * .76f
    val radius = diameter / 2f
    val center = Offset(width / 2f, height / 2f)
    val dx = tap.x - center.x
    val dy = tap.y - center.y
    if (sqrt(dx * dx + dy * dy) !in (radius - strokePx * .75f)..(radius + strokePx * .75f)) return null
    val angle = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
    var start = 270f
    breakdown.forEach { item ->
        val sweep = item.percentage * 360f
        val end = (start + sweep) % 360f
        if (if (start <= end) angle in start..end else angle >= start || angle <= end) return item
        start = end
    }
    return null
}
