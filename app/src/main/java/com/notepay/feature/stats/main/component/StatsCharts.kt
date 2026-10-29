package com.notepay.feature.stats.main.component

import com.notepay.core.designsystem.theme.AppTheme

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.analytics.ForecastDisplayState
import com.notepay.domain.analytics.StatsChartCalculator
import com.notepay.domain.model.Category
import com.notepay.domain.money.Money
import com.notepay.feature.stats.main.CategoryBreakdownItem
import com.notepay.feature.stats.main.MonthlyTrendPoint
import com.notepay.feature.stats.main.TrendWindow
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.util.localizedName
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

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
    // MaterialTheme không đọc được bên trong DrawScope → hoist token ra ngoài Canvas.
    val emptyRingColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val chartDescription = if (breakdown.isEmpty()) {
        stringResource(R.string.stats_allocation_chart_empty_cd)
    } else {
        val shares = legendItems.map { item ->
            "${item.category.localizedName()} ${(item.percentage * 100).toInt()}%"
        }
        stringResource(R.string.stats_allocation_chart_cd_format, shares.joinToString(", "))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(208.dp)
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
                    .semantics { contentDescription = chartDescription }
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
                        color = emptyRingColor,
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
                    Spacer(Modifier.height(8.dp))
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
                                .selectable(
                                    selected = selected,
                                    role = Role.Button,
                                    onClick = { onCategorySelected(if (selected) null else item.category) },
                                ),
                            shape = AppTheme.shapes.corner12,
                            color = color.copy(alpha = if (selected) .18f else .08f),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(color),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${item.category.localizedName()} — ${(item.percentage * 100).toInt()}%",
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
                .size(104.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (isEmpty) stringResource(R.string.stats_no_data) else (item?.category?.localizedName() ?: stringResource(R.string.stats_total_allocation)),
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
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = AppTheme.shapes.capsule,
                    color = tint.copy(alpha = 0.18f),
                ) {
                    Text(
                        text = "${(item.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tint,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
    trendWindow: TrendWindow,
    onTrendWindowSelected: (TrendWindow) -> Unit,
    onPointClick: (MonthlyTrendPoint) -> Unit,
) {
    val chartColor = if (metric == StatsMetric.CHI_TIEU) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val values = points.map { point ->
        if (metric == StatsMetric.CHI_TIEU) point.expense.amountInCents else point.income.amountInCents
    }
    val averageAmount = Money(StatsChartCalculator.average(values))
    val forecastDisplayState = StatsChartCalculator.forecastDisplayState(
        isExpense = metric == StatsMetric.CHI_TIEU,
        isSelectedMonthCurrent = isSelectedMonthCurrent,
        actualAmountInCents = values.lastOrNull() ?: 0L,
        forecast = forecast,
    )
    val hasForecast = forecastDisplayState != ForecastDisplayState.HIDDEN
    val axisScale = StatsChartCalculator.trendAxisScale(values, if (hasForecast) forecast else null)
    // Lưới Y tinh gọn: chỉ 2 mốc (đỉnh và 50% đỉnh) với nhãn tiền viết gọn.
    val topAxisLabel = MoneyFormatter.formatCompactVietnamese(Money(axisScale.topInCents))
    val middleAxisLabel = MoneyFormatter.formatCompactVietnamese(Money(axisScale.topInCents / 2))
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)
    // 12 cột thì giãn nhịp nhãn tháng để "T1..T12" không chồng lên nhau.
    val xLabelStride = if (points.size > 6) 2 else 1
    val barHorizontalPadding = if (points.size > 6) 4.dp else 8.dp
    val barShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    val barContentDescriptionHidden = stringResource(R.string.stats_amounts_hidden)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        // 1. Mức chi/thu trung bình của cả giai đoạn đang hiển thị
        Text(
            text = stringResource(R.string.stats_trend_average_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (showAmounts) MoneyFormatter.format(averageAmount) else barContentDescriptionHidden,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(12.dp))

        // 2. Chọn khoảng thời gian (3T / 6T / 12T) + chú giải màu
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TrendWindowSelector(
                selected = trendWindow,
                onSelected = onTrendWindowSelected,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
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
        }

        Spacer(Modifier.height(12.dp))

        // 3. Lưới + cột: nhãn Y neo đúng mốc đỉnh (100%) và giữa (50%)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight(),
            ) {
                Text(
                    text = topAxisLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.TopEnd),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = middleAxisLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Chart Canvas & Bars Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                // 2 đường gióng ngang: mốc đỉnh và mốc 50% đỉnh
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val middleY = size.height / 2f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, middleY),
                        end = Offset(size.width, middleY),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                // Columns/Bars
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    points.forEachIndexed { index, point ->
                        val pointValueInCents = values.getOrElse(index) { 0L }
                        val valueFraction by animateFloatAsState(
                            (pointValueInCents.toFloat() / axisScale.topInCents).coerceIn(0f, 1f),
                            label = "trend-bar$index",
                        )
                        val isCurrent = index == points.lastIndex
                        val forecastFraction = if (isCurrent && hasForecast && forecast != null) {
                            (forecast.amountInCents.toFloat() / axisScale.topInCents).coerceIn(0f, 1f)
                        } else {
                            null
                        }
                        val forecastMarkerFraction = if (isCurrent) {
                            StatsChartCalculator.forecastMarkerFraction(
                                state = forecastDisplayState,
                                forecast = forecast,
                                axisTopInCents = axisScale.topInCents,
                            )
                        } else {
                            null
                        }

                        val pointDescription = stringResource(
                            R.string.stats_trend_point_cd_format,
                            point.month,
                            if (showAmounts) {
                                MoneyFormatter.format(Money(pointValueInCents))
                            } else {
                                barContentDescriptionHidden
                            },
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = barHorizontalPadding)
                                .clip(barShape)
                                .semantics(mergeDescendants = true) {
                                    contentDescription = pointDescription
                                    role = Role.Button
                                }
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
                                if (forecastDisplayState == ForecastDisplayState.PROJECTION && forecastFraction != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(forecastFraction)
                                            .clip(barShape)
                                            .background(chartColor.copy(alpha = 0.25f)),
                                    )
                                }
                                // Solid actual column (Cột thực tế)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(valueFraction)
                                        .clip(barShape)
                                        .background(chartColor),
                                )
                                if (forecastMarkerFraction != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(forecastMarkerFraction),
                                        contentAlignment = Alignment.TopCenter,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(.82f)
                                                .height(3.dp)
                                                .clip(CircleShape)
                                                .background(chartColor.copy(alpha = 0.85f)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Nhãn tháng dưới chân cột (12 tháng thì giãn nhịp để không chồng chữ)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            points.forEachIndexed { index, point ->
                val isCurrent = index == points.lastIndex && isSelectedMonthCurrent
                val showLabel = (points.lastIndex - index) % xLabelStride == 0
                Text(
                    text = if (showLabel) stringResource(R.string.stats_month_label_format, point.month) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }
        }

        // 5. Forecast legend row (below X-axis month labels, with proper vertical margin)
        if (hasForecast) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (forecastDisplayState) {
                    ForecastDisplayState.PROJECTION -> Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(chartColor.copy(alpha = 0.25f)),
                    )
                    ForecastDisplayState.REACHED -> Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = chartColor,
                        modifier = Modifier.size(16.dp),
                    )
                    ForecastDisplayState.EXCEEDED -> Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    ForecastDisplayState.HIDDEN -> Unit
                }
                Text(
                    text = stringResource(
                        when (forecastDisplayState) {
                            ForecastDisplayState.PROJECTION -> R.string.stats_forecast_legend
                            ForecastDisplayState.REACHED -> R.string.stats_forecast_reached
                            ForecastDisplayState.EXCEEDED -> R.string.stats_forecast_exceeded
                            ForecastDisplayState.HIDDEN -> R.string.stats_forecast_legend
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Chip chọn khung thời gian xu hướng (3T / 6T / 12T).
 * Mỗi chip cao tối thiểu 48dp và đọc rõ số tháng qua contentDescription.
 */
@Composable
private fun TrendWindowSelector(
    selected: TrendWindow,
    onSelected: (TrendWindow) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TrendWindow.entries.forEach { window ->
            val isSelected = window == selected
            val windowDescription = stringResource(R.string.stats_trend_window_cd_format, window.months)
            Surface(
                modifier = Modifier
                    .clip(AppTheme.shapes.capsule)
                    .semantics { contentDescription = windowDescription }
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onSelected(window) },
                    ),
                shape = AppTheme.shapes.capsule,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.stats_trend_window_short_format, window.months),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
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
