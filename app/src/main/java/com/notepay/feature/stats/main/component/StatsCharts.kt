package com.notepay.feature.stats.main.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.localizedName
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.domain.model.Category
import com.notepay.feature.stats.main.CategoryBreakdownItem
import com.notepay.feature.stats.main.tabularNums
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

/** Nét vòng chưa chọn / đang chọn / halo và vùng chạm tối thiểu cho donut chart. */
private val RingStrokeWidth = 22.dp
private val RingSelectedStrokeWidth = 26.dp
private val RingSelectedHaloStrokeWidth = 32.dp
private val MinHitStrokeWidth = 44.dp
private const val RingDiameterRatio = 0.80f
private const val SliceGapDegrees = 2f
private const val UnselectedSliceAlpha = 0.32f
private const val SelectedHaloAlpha = 0.22f

/** Lát mỏng hơn ngưỡng này coi như 0% để không chiếm điểm chạm ở biên. */
private const val MinSweepEpsilon = 0.01f

private data class AnimatedAllocationSlice(
    val item: CategoryBreakdownItem,
    val strokeDp: Float,
    val alpha: Float,
    val isSelected: Boolean,
)

internal fun formatSharePercentValue(percentage: Float): String {
    val clamped = if (percentage.isFinite()) percentage.coerceIn(0f, 1f) else 0f
    return when {
        clamped >= 0.9995f -> "100"
        clamped > 0f && clamped < 0.0005f -> "<0.1"
        else -> String.format(Locale.US, "%.1f", clamped * 100f)
    }
}

internal fun formatSharePercent(percentage: Float): String =
    "${formatSharePercentValue(percentage)}%"

@Composable
internal fun AllocationChartContent(
    breakdown: List<CategoryBreakdownItem>,
    showAmounts: Boolean,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
) {
    // Donut và danh sách accordion bên dưới phải cùng một thứ tự → sắp xếp theo số tiền giảm dần.
    val items = remember(breakdown) { breakdown.sortedByDescending { it.amount.amountInCents } }
    // Chỉ một nguồn sự thật cho lựa chọn: selectedCategory do màn hình cha giữ.
    val selectedItem = items.find { it.category == selectedCategory }
    val topItem = items.firstOrNull()
    // MaterialTheme không đọc được bên trong DrawScope → hoist token ra ngoài Canvas.
    val emptyRingColor = AppTheme.colors.hairline
    val reducedMotion = rememberNotePayReducedMotion()
    val sliceAnimSpec = if (reducedMotion) snap<Float>() else tween<Float>(durationMillis = 220)
    val animatedSlices = items.map { item ->
        key(item.category.id) {
            val isSelected = item.category == selectedCategory
            val strokeDp by animateFloatAsState(
                targetValue = if (isSelected) RingSelectedStrokeWidth.value else RingStrokeWidth.value,
                animationSpec = sliceAnimSpec,
                label = "allocation-stroke-${item.category.id}",
            )
            val alpha by animateFloatAsState(
                targetValue = if (selectedCategory == null || isSelected) 1f else UnselectedSliceAlpha,
                animationSpec = sliceAnimSpec,
                label = "allocation-alpha-${item.category.id}",
            )
            AnimatedAllocationSlice(
                item = item,
                strokeDp = strokeDp,
                alpha = alpha,
                isSelected = isSelected,
            )
        }
    }
    val chartDescription = if (items.isEmpty()) {
        stringResource(R.string.stats_allocation_no_data)
    } else {
        val shareLabels = ArrayList<String>(5)
        for (item in items.take(5)) {
            shareLabels.add("${item.category.localizedName()} ${formatSharePercent(item.percentage)}")
        }
        stringResource(R.string.stats_allocation_chart_cd_format, shareLabels.joinToString(", "))
    }
    // pointerInput không khởi động lại khi tham số đổi → luôn dùng tham chiếu mới nhất.
    val currentSelection by rememberUpdatedState(selectedCategory)
    val currentOnSelect by rememberUpdatedState(onCategorySelected)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier.size(204.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Tâm co theo đường kính vòng, trừ nét chọn + khoảng thoáng 8dp để không đè lên vòng.
            val ringDiameter = maxWidth * RingDiameterRatio
            val centerDiameter = (ringDiameter - RingSelectedStrokeWidth - 8.dp).coerceAtLeast(0.dp)
            val centerHoleRadiusPx = with(LocalDensity.current) { centerDiameter.toPx() / 2f }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = chartDescription }
                    .pointerInput(items, centerHoleRadiusPx) {
                        detectTapGestures { offset ->
                            // Chạm ngoài vòng hoặc vào giữa vòng: giữ nguyên lựa chọn.
                            val tapped = findAllocationItem(
                                tap = offset,
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                                ringStrokePx = maxOf(RingSelectedStrokeWidth.toPx(), MinHitStrokeWidth.toPx()),
                                centerHoleRadiusPx = centerHoleRadiusPx,
                                breakdown = items,
                            ) ?: return@detectTapGestures
                            currentOnSelect(
                                if (tapped.category == currentSelection) null else tapped.category,
                            )
                        }
                    },
            ) {
                val diameter = min(size.width, size.height) * RingDiameterRatio
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                if (animatedSlices.isEmpty()) {
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
                    val gapDegrees = if (animatedSlices.size > 1) SliceGapDegrees else 0f
                    var startAngle = 270f
                    var selectedArc: Triple<AnimatedAllocationSlice, Float, Float>? = null
                    animatedSlices.forEach { slice ->
                        val fullSweep = slice.item.percentage * 360f
                        val drawSweep = if (gapDegrees > 0f && fullSweep > gapDegrees * 1.5f) {
                            fullSweep - gapDegrees
                        } else {
                            fullSweep
                        }
                        val drawStart = startAngle + (fullSweep - drawSweep) / 2f
                        startAngle += fullSweep
                        if (slice.isSelected) {
                            selectedArc = Triple(slice, drawStart, drawSweep)
                        } else {
                            drawArc(
                                color = Color(slice.item.category.colorArgb).copy(alpha = slice.alpha),
                                startAngle = drawStart,
                                sweepAngle = drawSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(
                                    width = slice.strokeDp.dp.toPx(),
                                    cap = StrokeCap.Butt,
                                ),
                            )
                        }
                    }
                    selectedArc?.let { (slice, drawStart, drawSweep) ->
                        val sliceColor = Color(slice.item.category.colorArgb)
                        drawArc(
                            color = sliceColor.copy(alpha = SelectedHaloAlpha),
                            startAngle = drawStart,
                            sweepAngle = drawSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(
                                width = RingSelectedHaloStrokeWidth.toPx(),
                                cap = StrokeCap.Butt,
                            ),
                        )
                        drawArc(
                            color = sliceColor.copy(alpha = slice.alpha),
                            startAngle = drawStart,
                            sweepAngle = drawSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(
                                width = slice.strokeDp.dp.toPx(),
                                cap = StrokeCap.Butt,
                            ),
                        )
                    }
                }
            }
            AllocationCenter(
                item = selectedItem,
                topItem = topItem,
                showAmounts = showAmounts,
                isEmpty = items.isEmpty(),
                modifier = Modifier.size(centerDiameter),
            )
        }
    }
}

@Composable
private fun AllocationCenter(
    item: CategoryBreakdownItem?,
    topItem: CategoryBreakdownItem?,
    showAmounts: Boolean,
    isEmpty: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val displayItem = item ?: topItem
    val tint = displayItem?.let { Color(it.category.colorArgb) } ?: AppTheme.colors.primary
    val bg = when {
        isEmpty -> AppTheme.colors.surfaceSunken.copy(alpha = 0.7f)
        item != null -> tint.copy(alpha = 0.12f)
        else -> AppTheme.colors.surfaceSunken
    }
    val borderStroke = if (!isEmpty && item != null) {
        BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    } else {
        null
    }

    Surface(
        modifier = modifier.clearAndSetSemantics {},
        shape = CircleShape,
        color = bg,
        border = borderStroke,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (!isEmpty && displayItem != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(tint),
                    )
                    Text(
                        text = displayItem.category.localizedName(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatSharePercent(displayItem.percentage),
                    style = MaterialTheme.typography.titleMedium.tabularNums,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (showAmounts) {
                        MoneyFormatter.format(displayItem.amount)
                    } else {
                        stringResource(R.string.stats_amounts_mask)
                    },
                    style = MaterialTheme.typography.labelSmall.tabularNums,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = stringResource(R.string.stats_no_data),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
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
    centerHoleRadiusPx: Float,
    breakdown: List<CategoryBreakdownItem>,
): CategoryBreakdownItem? {
    if (breakdown.isEmpty()) return null
    val diameter = min(width, height) * RingDiameterRatio
    val radius = diameter / 2f
    val dx = tap.x - width / 2f
    val dy = tap.y - height / 2f
    val distance = sqrt(dx * dx + dy * dy)
    val innerLimit = maxOf(radius - ringStrokePx / 2f, centerHoleRadiusPx)
    if (distance < innerLimit || distance > radius + ringStrokePx / 2f) return null

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
