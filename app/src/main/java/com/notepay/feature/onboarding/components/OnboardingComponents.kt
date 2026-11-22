package com.notepay.feature.onboarding.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notepay.R
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.isAppDarkTheme
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.feature.onboarding.OnboardingStep

/**
 * Dải nền chuyển sắc mềm đặc trưng v4 "Sand & Ink", dựng hoàn toàn từ token Theme.
 */
@Composable
fun OnboardingTopBackdrop(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val brushes = remember(colors) {
        val isLight = colors.isLight
        val sandWash = Brush.verticalGradient(
            0f to colors.sand.copy(alpha = if (isLight) 0.58f else 0.14f),
            0.5f to colors.clay.copy(alpha = if (isLight) 0.16f else 0.06f),
            1f to Color.Transparent,
        )
        val glow = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isLight) 0.45f else 0.05f),
                Color.Transparent,
            ),
        )
        val fadeToBackground = Brush.verticalGradient(
            0f to Color.Transparent,
            0.6f to Color.Transparent,
            0.9f to colors.background,
            1f to colors.background,
        )
        listOf(sandWash, glow, fadeToBackground)
    }

    var stacked: Modifier = modifier.fillMaxWidth()
    brushes.forEach { brush -> stacked = stacked.background(brush) }
    Box(modifier = stacked)
}

/** Thanh tiến trình 3 phân đoạn pill mượt kèm bộ đếm `01 / 03`. */
@Composable
fun OnboardingStepProgressBar(
    currentStep: OnboardingStep,
    modifier: Modifier = Modifier,
    totalSteps: Int = 3,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(totalSteps) { index ->
                val isActive = currentStep.stepIndex >= index
                val fraction by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0f,
                    animationSpec = tween(durationMillis = if (reducedMotion) 0 else 280),
                    label = "StepTrackFraction",
                )
                val barColor by animateColorAsState(
                    targetValue = if (currentStep.stepIndex == index) {
                        AppTheme.colors.primary
                    } else {
                        AppTheme.colors.primary.copy(alpha = 0.5f)
                    },
                    animationSpec = tween(durationMillis = if (reducedMotion) 0 else 220),
                    label = "StepTrackColor",
                )

                Box(
                    modifier = Modifier
                        .width(if (currentStep.stepIndex == index) 28.dp else 18.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.border.copy(alpha = 0.35f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(CircleShape)
                            .background(barColor),
                    )
                }
            }
        }

        Text(
            text = stringResource(
                R.string.onboarding_step_counter_format,
                currentStep.stepIndex + 1,
                totalSteps,
            ),
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.textSecondary,
        )
    }
}

/** Tiêu đề biên tập v4 (eyebrow + display serif Fraunces + mô tả ngắn). */
@Composable
fun OnboardingStepHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = AppTheme.shapes.capsule,
            color = colors.sand.copy(alpha = if (colors.isLight) 0.55f else 0.16f),
            border = BorderStroke(1.dp, colors.sand.copy(alpha = 0.45f)),
        ) {
            Text(
                text = eyebrow,
                style = AppTheme.typography.eyebrow,
                color = if (colors.isLight) colors.onSand else colors.sand,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }

        Text(
            text = title,
            style = AppTheme.typography.displaySerif.copy(
                fontSize = 28.sp,
                lineHeight = 34.sp,
            ),
            color = colors.textPrimary,
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
    }
}

/** Thẻ Ink tâm điểm duy nhất cho mỗi bước onboarding (có họa tiết vòng tròn cache GPU). */
@Composable
fun OnboardingInkStageCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sandTint = AppTheme.colors.sand.copy(alpha = 0.08f)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner28,
        color = AppTheme.colors.ink,
        contentColor = AppTheme.colors.onInk,
        border = if (isAppDarkTheme()) BorderStroke(1.dp, AppTheme.colors.inkBorder) else null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithCache {
                    val stroke = Stroke(width = 1.5.dp.toPx())
                    val outerRadius = size.minDimension * 0.62f
                    val innerRadius = size.minDimension * 0.36f
                    val center = Offset(x = size.width * 0.92f, y = size.height * 0.16f)
                    onDrawBehind {
                        drawCircle(color = sandTint, radius = outerRadius, center = center, style = stroke)
                        drawCircle(color = sandTint, radius = innerRadius, center = center, style = stroke)
                    }
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

/** Nút CTA chính v4: cao 56dp, bo capsule, có nhấn scale & trạng thái loading. */
@Composable
fun OnboardingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    trailingIcon: ImageVector? = null,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "OnboardingButtonPressScale",
    )

    Surface(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = AppTheme.shapes.capsule,
        color = if (enabled) AppTheme.colors.primary else AppTheme.colors.primary.copy(alpha = 0.45f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
