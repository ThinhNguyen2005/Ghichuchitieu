package com.notepay.feature.stats.main.component

import com.notepay.core.designsystem.theme.AppTheme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Nét vòng chưa chọn / đang chọn, dùng chung cho cả Canvas và vùng chạm. */
private val RingStrokeWidth = 32.dp
private val RingSelectedStrokeWidth = 40.dp

/** Lát mỏng hơn ngưỡng này coi như 0% để không chiếm điểm chạm ở biên. */
private const val MinSweepEpsilon = 0.01f

@Composable
internal fun AllocationChartContent(
    breakdown: List<CategoryBreakdownItem>,
    total: Money,
    showAmounts: Boolean,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
) {
    // Donut và legend phải cùng một thứ tự → sắp xếp một lần rồi dùng chung.
    val items = remember(breakdown) { breakdown.sortedByDescending { it.amount.amountInCents } }
    // Chỉ một nguồn sự thật cho lựa chọn: selectedCategory do màn hình cha giữ.
    val selectedItem = items.find { it.category == selectedCategory }
    // MaterialTheme không đọc được bên trong DrawScope → hoist token ra ngoài Canvas.
    val emptyRingColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val chartDescription = if (items.isEmpty()) {
        stringResource(R.string.stats_allocation_no_data)
    } else {
        val shareLabels = ArrayList<String>(5)
        for (item in items.take(5)) {
            shareLabels.add("${item.category.localizedName()} ${(item.percentage * 100).roundToInt()}%")
        }
        stringResource(R.string.stats_allocation_chart_cd_format, shareLabels.joinToString(", "))
    }
    // pointerInput không khởi động lại khi tham số đổi → luôn dùng tham chiếu mới nhất.
    val currentSelection by rememberUpdatedState(selectedCategory)
    val currentOnSelect by rememberUpdatedState(onCategorySelected)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Không cố định chiều cao: legend hiện đủ danh mục nên thẻ cao theo nội dung.
            .heightIn(min = 208.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1.05f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            // Tâm co theo đường kính vòng, trừ đúng nét dày nhất để không đè lên vòng.
            val ringDiameter = maxWidth * .76f
            val centerDiameter = (ringDiameter - RingSelectedStrokeWidth).coerceAtLeast(0.dp)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = chartDescription }
                    .pointerInput(items) {
                        detectTapGestures { offset ->
                            // Chạm ngoài vòng hoặc vào giữa vòng: giữ nguyên lựa chọn.
                            val tapped = findAllocationItem(
                                tap = offset,
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                                ringStrokePx = RingSelectedStrokeWidth.toPx(),
                                breakdown = items,
                            ) ?: return@detectTapGestures
                            currentOnSelect(
                                if (tapped.category == currentSelection) null else tapped.category,
                            )
                        }
                    },
            ) {
                val diameter = min(size.width, size.height) * .76f
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                var startAngle = 270f
                if (items.isEmpty()) {
                    drawArc(
                        color = emptyRingColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = RingStrokeWidth.toPx(), cap = StrokeCap.Round),
                    )
                } else {
                    items.forEach { item ->
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
                                width = if (isSelected) RingSelectedStrokeWidth.toPx() else RingStrokeWidth.toPx(),
                                cap = StrokeCap.Butt,
                            ),
                        )
                        startAngle += sweep
                    }
                }
            }
            AllocationCenter(
                item = selectedItem,
                total = total,
                showAmounts = showAmounts,
                isEmpty = items.isEmpty(),
                modifier = Modifier.size(centerDiameter),
            )
        }
        Column(
            Modifier
                .weight(.95f)
                .padding(start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (items.isEmpty()) {
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
                )
            } else {
                // Hiện đủ danh mục: lát bánh nào cũng có dòng tương ứng để đọc và chọn.
                items.forEach { item ->
                    val selected = item.category == selectedCategory
                    AllocationLegendRow(
                        item = item,
                        selected = selected,
                        onClick = { currentOnSelect(if (selected) null else item.category) },
                    )
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
    modifier: Modifier = Modifier,
) {
    val tint = item?.let { Color(it.category.colorArgb) } ?: MaterialTheme.colorScheme.primary
    val bg = if (isEmpty) {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
    } else if (item != null) {
        tint.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)
    }

    Surface(modifier = modifier, shape = CircleShape, color = bg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (isEmpty) {
                    stringResource(R.string.stats_no_data)
                } else {
                    item?.category?.localizedName() ?: stringResource(R.string.stats_total_allocation)
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (item != null) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (showAmounts) {
                    MoneyFormatter.format(item?.amount ?: total)
                } else {
                    stringResource(R.string.stats_amounts_mask)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Một dòng trong chú giải phân bổ: chấm màu nhận diện, tên danh mục, tỷ trọng và dấu chọn.
 * Vùng chạm tối thiểu 48dp; trạng thái chọn có thêm icon nên không chỉ dựa vào màu.
 */
@Composable
private fun AllocationLegendRow(
    item: CategoryBreakdownItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = Color(item.category.colorArgb)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.corner12)
            .selectable(selected = selected, role = Role.Button, onClick = onClick),
        shape = AppTheme.shapes.corner12,
        color = color.copy(alpha = if (selected) .18f else .08f),
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = item.category.localizedName(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.percent_format,
                    (item.percentage * 100).roundToInt(),
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
            // Giữ chỗ cho dấu chọn để chữ không nhảy khi đổi trạng thái.
            Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
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
    budget: Money?,
    showAmounts: Boolean,
    trendWindow: TrendWindow,
    onTrendWindowSelected: (TrendWindow) -> Unit,
    onPointClick: (MonthlyTrendPoint) -> Unit,
) {
    val chartColor = if (metric == StatsMetric.CHI_TIEU) AppTheme.colors.error else AppTheme.colors.success
    val values = points.map { point ->
        if (metric == StatsMetric.CHI_TIEU) point.expense.amountInCents else point.income.amountInCents
    }
    val averageAmount = Money(StatsChartCalculator.average(values))
    val forecastDisplayState = StatsChartCalculator.forecastDisplayState(
        isExpense = metric == StatsMetric.CHI_TIEU,
        isSelectedMonthCurrent = isSelectedMonthCurrent,
        actualAmountInCents = values.lastOrNull() ?: 0L,
        forecast = forecast,
        budgetInCents = budget?.amountInCents,
    )
    val hasForecast = forecastDisplayState != ForecastDisplayState.HIDDEN
    val axisScale = StatsChartCalculator.trendAxisScale(values, if (hasForecast) forecast else null)
    // Ẩn số tiền thì trục Y cũng phải ẩn, nếu không vẫn đọc được quy mô tiền qua nhãn trục.
    val hiddenAmount = stringResource(R.string.stats_amounts_hidden)
    // Lưới Y tinh gọn: chỉ 2 mốc (đỉnh và 50% đỉnh) với nhãn tiền viết gọn.
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
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)
    // 12 cột thì giãn nhịp nhãn tháng để "T1..T12" không chồng lên nhau.
    val xLabelStride = if (points.size > 6) 2 else 1
    val barHorizontalPadding = if (points.size > 6) 4.dp else 8.dp
    val barShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)

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
            text = if (showAmounts) MoneyFormatter.format(averageAmount) else hiddenAmount,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TrendWindowSelector(
                selected = trendWindow,
                onSelected = onTrendWindowSelected,
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                        val forecastAmountInCents = forecast?.amountInCents
                        val forecastFraction = if (isCurrent && hasForecast && forecastAmountInCents != null) {
                            (forecastAmountInCents.toFloat() / axisScale.topInCents).coerceIn(0f, 1f)
                        } else {
                            null
                        }
                        // Vùng mờ chỉ có nghĩa khi thực tế còn nằm dưới mốc dự báo.
                        val showsForecastZone = forecastAmountInCents != null &&
                            forecastFraction != null &&
                            pointValueInCents < forecastAmountInCents
                        // Chú giải đã ẩn thì cũng không vẽ mốc dự báo trên cột.
                        val forecastMarkerFraction = if (isCurrent && hasForecast) {
                            StatsChartCalculator.forecastMarkerFraction(
                                actualAmountInCents = pointValueInCents,
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
                                hiddenAmount
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
                                if (showsForecastZone) {
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
                    // Chưa có ngân sách để so: dự báo chỉ là vùng mờ trung tính.
                    ForecastDisplayState.PROJECTION,
                    ForecastDisplayState.PASSED,
                    ForecastDisplayState.WITHIN_BUDGET -> Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(chartColor.copy(alpha = 0.25f)),
                    )
                    // Chỉ dự báo vượt ngân sách thật mới dùng ngôn ngữ cảnh báo.
                    ForecastDisplayState.OVER_BUDGET -> Icon(
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
                            ForecastDisplayState.PASSED -> R.string.stats_forecast_passed
                            ForecastDisplayState.WITHIN_BUDGET -> R.string.stats_forecast_within_budget
                            ForecastDisplayState.OVER_BUDGET -> R.string.stats_forecast_over_budget
                            ForecastDisplayState.HIDDEN -> R.string.stats_forecast_legend
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (forecastDisplayState == ForecastDisplayState.OVER_BUDGET) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

/**
 * Bộ chọn khung thời gian xu hướng (3T / 6T / 12T) bằng segmented buttons của Material 3.
 */
@Composable
private fun TrendWindowSelector(
    selected: TrendWindow,
    onSelected: (TrendWindow) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = TrendWindow.entries.map { window ->
        window to stringResource(R.string.stats_trend_window_short_format, window.months)
    }
    StatsSegmentedButtonRow(
        options = options,
        selected = selected,
        onSelected = onSelected,
        modifier = modifier,
    )
}

/**
 * Xác định lát bánh tại điểm chạm.
 *
 * Vùng chạm trùng với nét vẽ dày nhất và góc được tính liên tục từ 0° tại đỉnh vòng,
 * nên lát chiếm 100% khớp toàn vòng và lát 0% không nhận nhầm điểm chạm ở biên.
 */
private fun findAllocationItem(
    tap: Offset,
    width: Float,
    height: Float,
    ringStrokePx: Float,
    breakdown: List<CategoryBreakdownItem>,
): CategoryBreakdownItem? {
    if (breakdown.isEmpty()) return null
    val diameter = min(width, height) * .76f
    val radius = diameter / 2f
    val dx = tap.x - width / 2f
    val dy = tap.y - height / 2f
    val distance = sqrt(dx * dx + dy * dy)
    if (distance < radius - ringStrokePx / 2f || distance > radius + ringStrokePx / 2f) return null

    // Vòng bắt đầu ở 270° (đỉnh) và chạy theo chiều kim đồng hồ; quy về [0, 360).
    val angleFromStart = (((atan2(dy, dx) * 180f / PI.toFloat()) - 270f) % 360f + 360f) % 360f
    var sweepStart = 0f
    breakdown.forEach { item ->
        val sweep = item.percentage * 360f
        if (sweep > MinSweepEpsilon && angleFromStart < sweepStart + sweep) return item
        sweepStart += sweep
    }
    return null
}
