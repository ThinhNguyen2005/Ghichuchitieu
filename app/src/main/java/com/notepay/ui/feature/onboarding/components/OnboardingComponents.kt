package com.notepay.ui.feature.onboarding.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.notepay.ui.feature.onboarding.OnboardingStep
import com.notepay.ui.theme.AppTheme

/** Nút CTA chính, đủ 48dp touch target, có phản hồi nhấn nhẹ. */
@Composable
fun OnboardingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "OnboardingButtonPressScale"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = AppTheme.shapes.capsule,
        color = AppTheme.colors.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/** Chấm tiến trình 3 bước. */
@Composable
fun ProgressDotsIndicator(
    currentStep: OnboardingStep,
    totalSteps: Int = 3,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceSmall),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        repeat(totalSteps) { index ->
            val isSelected = currentStep.stepIndex == index
            val isPassed = currentStep.stepIndex > index

            val width by animateDpAsState(
                targetValue = if (isSelected) 20.dp else AppTheme.dimensions.spaceSmall,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                label = "DotWidth"
            )

            val color by animateColorAsState(
                targetValue = when {
                    isSelected || isPassed -> AppTheme.colors.primary
                    else -> AppTheme.colors.borderStrong
                },
                animationSpec = tween(durationMillis = 200),
                label = "DotColor"
            )

            Box(
                modifier = Modifier
                    .height(AppTheme.dimensions.spaceSmall)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** Hàng chọn 1 trong N — dùng cho tiền tệ, ngôn ngữ, icon ví, giờ nhắc. */
@Composable
fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            AppTheme.colors.surfaceSunken
        },
        animationSpec = tween(durationMillis = 180),
        label = "ChoiceRowBackground"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = AppTheme.shapes.row,
        color = background,
        border = if (selected) BorderStroke(1.dp, AppTheme.colors.primary) else null,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.dimensions.spaceMedium, vertical = 14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.textPrimary
            )

            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.primary)
                )
            }
        }
    }
}

/** Ô chọn icon ví. */
@Composable
fun IconChoice(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            AppTheme.colors.surfaceSunken
        },
        animationSpec = tween(durationMillis = 180),
        label = "IconChoiceBackground"
    )

    Surface(
        shape = AppTheme.shapes.row,
        color = background,
        border = if (selected) BorderStroke(1.dp, AppTheme.colors.primary) else null,
        modifier = modifier
            .size(56.dp)
            .clip(AppTheme.shapes.row)
            .clickable(role = Role.RadioButton) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
