package com.notepay.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Transaction
import com.notepay.ui.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Component bao bọc TransactionItem cho phép vuốt trực quan bám sát ngón tay (Direct Manipulation):
 * - Vuốt sang trái: Kích hoạt Xóa (Nền đỏ + Icon Thùng rác).
 * - Vuốt sang phải: Kích hoạt Sửa (Nền xanh/cam + Icon Bút chì).
 * - Vật lý đàn hồi (Rubber-band physics) khi kéo quá ngưỡng.
 * - Rung Haptic khi chạm mốc hành động.
 * - Khả năng ngắt quãng (Interruptible): chạm vào là dừng animation lò xo ngay tức khắc.
 */
@Composable
fun SwipeableTransactionItem(
    transaction: Transaction,
    walletName: String = "",
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val actionThresholdPx = with(density) { 88.dp.toPx() }
    val maxDragPx = with(density) { 160.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.corner16)
    ) {
        val currentOffset = offsetX.value

        // 1. Nền thao tác phía sau (Action Background)
        if (currentOffset != 0f) {
            val isSwipingLeft = currentOffset < 0
            val absOffset = abs(currentOffset)
            val progress = (absOffset / actionThresholdPx).coerceIn(0f, 1.5f)
            val iconScale = (0.6f + progress * 0.4f).coerceIn(0.6f, 1.2f)

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(AppTheme.shapes.corner16)
                    .background(
                        if (isSwipingLeft) {
                            if (absOffset >= actionThresholdPx) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.errorContainer
                        } else {
                            if (absOffset >= actionThresholdPx) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = if (isSwipingLeft) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.scale(iconScale)
                ) {
                    if (isSwipingLeft) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (absOffset >= actionThresholdPx) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = if (absOffset >= actionThresholdPx) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            tint = if (absOffset >= actionThresholdPx) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.action_edit),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (absOffset >= actionThresholdPx) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // 2. Nội dung Item chính phía trước (bám theo ngón tay 1:1)
        Box(
            modifier = Modifier
                .offset { IntOffset(currentOffset.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            coroutineScope.launch {
                                offsetX.stop() // Ngắt animation tức thì nếu đang nảy
                            }
                            hasTriggeredHaptic = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val target = offsetX.value + dragAmount
                                val absTarget = abs(target)
                                
                                // Áp dụng lực cản cao su (Rubber-band physics) khi kéo quá mức
                                val dampedTarget = if (absTarget > actionThresholdPx) {
                                    val overdrag = absTarget - actionThresholdPx
                                    val dampedOverdrag = overdrag * 0.35f
                                    (actionThresholdPx + dampedOverdrag).coerceAtMost(maxDragPx) * target.sign
                                } else {
                                    target
                                }

                                // Kích hoạt rung haptic 1 lần khi vượt qua mốc snap
                                if (abs(dampedTarget) >= actionThresholdPx && !hasTriggeredHaptic) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    hasTriggeredHaptic = true
                                } else if (abs(dampedTarget) < actionThresholdPx && hasTriggeredHaptic) {
                                    hasTriggeredHaptic = false
                                }

                                offsetX.snapTo(dampedTarget)
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                val finalOffset = offsetX.value
                                val triggered = abs(finalOffset) >= actionThresholdPx

                                if (triggered) {
                                    if (finalOffset < 0) {
                                        onDelete?.invoke()
                                    } else {
                                        onEdit?.invoke()
                                    }
                                }

                                // Bung lò xo trở về vị trí ban đầu mượt mà
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                hasTriggeredHaptic = false
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                                hasTriggeredHaptic = false
                            }
                        }
                    )
                }
        ) {
            TransactionItem(
                transaction = transaction,
                walletName = walletName,
                onClick = onClick,
                onLongClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
