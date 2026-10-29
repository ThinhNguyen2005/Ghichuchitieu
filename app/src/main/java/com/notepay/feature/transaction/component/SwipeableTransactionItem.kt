package com.notepay.feature.transaction.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.notepay.core.designsystem.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Bề rộng vùng hành động lộ ra sau khi vuốt — cũng là vùng chạm của nút (>= 48dp). */
private val ActionWidth = 96.dp

/** Tỉ lệ bề rộng hành động để phát rung báo "đã nắm được thao tác". */
private const val RevealHapticFraction = 0.5f

/**
 * Bọc một dòng giao dịch để vuốt ngang lộ nút hành động dạng "dính" (sticky reveal):
 * - Vuốt sang trái: lộ nút **Xóa**, dừng lại ở trạng thái mở cho tới khi người dùng chạm.
 * - Vuốt sang phải: lộ nút **Sửa** với cơ chế tương tự.
 * - Chạm vào dòng khi đang mở: đóng lại thay vì mở chi tiết.
 *
 * Bề mặt hành động luôn được compose sẵn phía sau dòng, nên không có State nào bị đọc
 * trong từng khung hình kéo: vị trí chỉ đọc trong layout phase qua `Modifier.offset { }`.
 *
 * @param shape bo góc của cả khối (dùng `RectangleShape` khi dòng nằm trong một thẻ nhóm).
 * @param content nội dung dòng; mặc định là [TransactionItem] kiểu danh sách giao dịch.
 */
@Composable
fun SwipeableTransactionItem(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    walletName: String = "",
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    hapticEnabled: Boolean = true,
    isSelected: Boolean = false,
    shape: Shape = AppTheme.shapes.corner16,
    content: @Composable () -> Unit = {
        TransactionItem(
            transaction = transaction,
            walletName = walletName,
            onClick = onClick,
            onLongClick = onDelete,
            isSelected = isSelected,
            modifier = Modifier.fillMaxWidth(),
        )
    },
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val editAction = onEdit
    val deleteAction = onDelete

    val actionWidthPx = with(density) { ActionWidth.toPx() }
    val hapticThresholdPx = actionWidthPx * RevealHapticFraction
    val minOffset = if (deleteAction != null) -actionWidthPx else 0f
    val maxOffset = if (editAction != null) actionWidthPx else 0f

    val offsetX = remember { Animatable(0f) }
    var isOpen by remember { mutableStateOf(false) }
    var dragStartOffset by remember { mutableFloatStateOf(0f) }
    var dragAccumulated by remember { mutableFloatStateOf(0f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    suspend fun settleTo(target: Float) {
        offsetX.animateTo(
            targetValue = target,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 520f),
        )
        isOpen = target != 0f
        hasTriggeredHaptic = false
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        // Vùng hành động nằm sau dòng; chỉ phần không bị dòng che mới nhìn thấy và chạm được.
        if (editAction != null) {
            RevealAction(
                label = stringResource(R.string.action_edit),
                icon = Icons.Rounded.Edit,
                containerColor = AppTheme.colors.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                alignment = Alignment.CenterStart,
                onAction = {
                    coroutineScope.launch { settleTo(0f) }
                    editAction()
                },
            )
        }
        if (deleteAction != null) {
            RevealAction(
                label = stringResource(R.string.action_delete),
                icon = Icons.Rounded.Delete,
                containerColor = AppTheme.colors.error,
                contentColor = MaterialTheme.colorScheme.onError,
                alignment = Alignment.CenterEnd,
                onAction = {
                    coroutineScope.launch { settleTo(0f) }
                    deleteAction()
                },
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(actionWidthPx, minOffset, maxOffset) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            coroutineScope.launch { offsetX.stop() }
                            dragStartOffset = offsetX.value
                            dragAccumulated = 0f
                            hasTriggeredHaptic = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulated += dragAmount
                            val target = (dragStartOffset + dragAccumulated)
                                .coerceIn(minOffset, maxOffset)

                            val pastThreshold = abs(target) >= hapticThresholdPx
                            if (pastThreshold && !hasTriggeredHaptic) {
                                if (hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                hasTriggeredHaptic = true
                            } else if (!pastThreshold && hasTriggeredHaptic) {
                                hasTriggeredHaptic = false
                            }

                            coroutineScope.launch { offsetX.snapTo(target) }
                        },
                        onDragEnd = {
                            val settledOffset = when {
                                offsetX.value <= -hapticThresholdPx -> minOffset
                                offsetX.value >= hapticThresholdPx -> maxOffset
                                else -> 0f
                            }
                            coroutineScope.launch { settleTo(settledOffset) }
                        },
                        onDragCancel = {
                            coroutineScope.launch { settleTo(0f) }
                        },
                    )
                }
        ) {
            content()

            // Đang mở: chạm vào dòng để đóng, tránh mở nhầm chi tiết giao dịch.
            if (isOpen) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = stringResource(R.string.action_close),
                            onClick = { coroutineScope.launch { settleTo(0f) } },
                        )
                )
            }
        }
    }
}

/** Một nút hành động nằm sát một cạnh của dòng, chỉ lộ ra khi dòng bị kéo. */
@Composable
private fun BoxScope.RevealAction(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    alignment: Alignment,
    onAction: () -> Unit,
) {
    Box(
        modifier = Modifier.matchParentSize(),
        contentAlignment = alignment,
    ) {
        Column(
            modifier = Modifier
                .width(ActionWidth)
                .fillMaxHeight()
                .background(containerColor)
                .clickable(onClickLabel = label, onClick = onAction),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}
