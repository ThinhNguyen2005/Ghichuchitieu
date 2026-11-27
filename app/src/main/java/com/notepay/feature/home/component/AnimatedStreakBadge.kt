package com.notepay.feature.home.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.StreakBlazeFlame
import com.notepay.core.designsystem.theme.StreakFlameCore
import com.notepay.core.designsystem.theme.StreakInfernoDarkFlame
import com.notepay.core.designsystem.theme.StreakInfernoFlame
import com.notepay.core.designsystem.theme.StreakSparkFlame
import com.notepay.core.navigation.rememberNotePayHaptic
import com.notepay.core.navigation.rememberNotePayReducedMotion
import kotlinx.coroutines.launch

internal enum class StreakTier {
    SPARK,
    BLAZE,
    INFERNO,
}

internal fun streakTierFor(days: Int): StreakTier = when {
    days >= 7 -> StreakTier.INFERNO
    days >= 3 -> StreakTier.BLAZE
    else -> StreakTier.SPARK
}

private data class StreakMotionValues(
    val scaleY: State<Float>,
    val tiltDeg: State<Float>,
    val emberPhase: State<Float>,
    val shimmerProgress: State<Float>,
)

@Composable
private fun rememberStreakMotionValues(
    tier: StreakTier,
    reducedMotion: Boolean,
): StreakMotionValues {
    if (reducedMotion) {
        return remember {
            StreakMotionValues(
                scaleY = mutableFloatStateOf(1f),
                tiltDeg = mutableFloatStateOf(0f),
                emberPhase = mutableFloatStateOf(0f),
                shimmerProgress = mutableFloatStateOf(-1f),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "streak-flame")
    val durationMs = when (tier) {
        StreakTier.SPARK -> 1400
        StreakTier.BLAZE -> 1050
        StreakTier.INFERNO -> 850
    }
    val scaleY = transition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (tier == StreakTier.INFERNO) 1.11f else 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "streak-scale-y",
    )
    val tiltDeg = transition.animateFloat(
        initialValue = -2.8f,
        targetValue = 2.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs + 220, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "streak-tilt",
    )
    val emberPhase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "streak-ember",
    )
    val shimmerProgress = transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "streak-shimmer",
    )
    return StreakMotionValues(
        scaleY = scaleY,
        tiltDeg = tiltDeg,
        emberPhase = emberPhase,
        shimmerProgress = shimmerProgress,
    )
}

@Composable
private fun streakFlameTint(tier: StreakTier): Color {
    val isDark = !AppTheme.colors.isLight
    return when (tier) {
        StreakTier.SPARK -> StreakSparkFlame
        StreakTier.BLAZE -> StreakBlazeFlame
        StreakTier.INFERNO -> if (isDark) StreakInfernoDarkFlame else StreakInfernoFlame
    }
}

/**
 * Animated TopBar streak pill with tiered flame pulse, draw-phase embers/shimmer,
 * spring tap bounce, and reduced-motion fallback.
 */
@Composable
internal fun AnimatedStreakBadge(
    streakDays: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tier = streakTierFor(streakDays)
    val reducedMotion = rememberNotePayReducedMotion()
    val performHaptic = rememberNotePayHaptic()
    val coroutineScope = rememberCoroutineScope()
    val tapScale = remember { Animatable(1f) }
    val motion = rememberStreakMotionValues(tier = tier, reducedMotion = reducedMotion)
    val flameColor = streakFlameTint(tier)
    val pillShape = AppTheme.shapes.capsule
    val surfaceColor = AppTheme.colors.surface.copy(alpha = if (AppTheme.colors.isLight) 0.92f else 0.86f)
    val borderColor = if (tier == StreakTier.INFERNO) {
        flameColor.copy(alpha = 0.45f)
    } else {
        AppTheme.colors.hairline
    }
    val daysLabel = pluralStringResource(R.plurals.home_streak_days, streakDays, streakDays)

    Surface(
        onClick = {
            performHaptic(HapticFeedbackType.TextHandleMove)
            if (!reducedMotion) {
                coroutineScope.launch {
                    tapScale.snapTo(0.90f)
                    tapScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
            }
            onClick()
        },
        shape = pillShape,
        color = surfaceColor,
        contentColor = AppTheme.colors.textPrimary,
        border = BorderStroke(if (tier == StreakTier.INFERNO) 1.dp else 0.5.dp, borderColor),
        modifier = modifier
            .defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget)
            .graphicsLayer {
                val scale = tapScale.value
                scaleX = scale
                scaleY = scale
            }
            .iosCardShadow(shape = pillShape, elevated = false)
            .semantics(mergeDescendants = true) {},
    ) {
        val shimmerModifier = if (tier == StreakTier.INFERNO && !reducedMotion) {
            Modifier.drawWithCache {
                val shimmerWidth = size.width * 0.35f
                val brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        StreakFlameCore.copy(alpha = 0.16f),
                        Color.Transparent,
                    ),
                    start = Offset.Zero,
                    end = Offset(shimmerWidth, size.height),
                )
                onDrawWithContent {
                    drawContent()
                    val progress = motion.shimmerProgress.value
                    if (progress in -0.35f..1.45f) {
                        translate(left = progress * (size.width + shimmerWidth) - shimmerWidth) {
                            drawRect(brush = brush)
                        }
                    }
                }
            }
        } else {
            Modifier
        }

        Row(
            modifier = shimmerModifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            AnimatedFlameMark(
                tier = tier,
                flameColor = flameColor,
                motion = motion,
                reducedMotion = reducedMotion,
                iconSize = 18.dp,
            )

            AnimatedContent(
                targetState = daysLabel,
                transitionSpec = {
                    if (reducedMotion) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                    }
                },
                label = "streak-days-text",
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AnimatedFlameMark(
    tier: StreakTier,
    flameColor: Color,
    motion: StreakMotionValues,
    reducedMotion: Boolean,
    iconSize: Dp,
) {
    val boxSize = iconSize + 6.dp
    Box(
        modifier = Modifier
            .size(boxSize)
            .drawBehind {
                val glowRadius = size.minDimension * if (tier == StreakTier.INFERNO) 0.56f else 0.48f
                val glowAlpha = if (reducedMotion) 0.14f else 0.12f + (motion.scaleY.value - 0.95f) * 0.8f
                drawCircle(
                    color = flameColor.copy(alpha = glowAlpha.coerceIn(0.08f, 0.26f)),
                    radius = glowRadius,
                    center = Offset(size.width * 0.5f, size.height * 0.58f),
                )
                if (!reducedMotion && tier != StreakTier.SPARK) {
                    val phase = motion.emberPhase.value
                    val emberY1 = size.height * (0.45f - phase * 0.42f)
                    val emberAlpha1 = (1f - phase) * 0.75f
                    drawCircle(
                        color = StreakFlameCore.copy(alpha = emberAlpha1),
                        radius = size.minDimension * 0.065f,
                        center = Offset(size.width * 0.30f, emberY1),
                    )
                    val phase2 = (phase + 0.5f) % 1f
                    val emberY2 = size.height * (0.42f - phase2 * 0.38f)
                    val emberAlpha2 = (1f - phase2) * 0.65f
                    drawCircle(
                        color = flameColor.copy(alpha = emberAlpha2),
                        radius = size.minDimension * 0.055f,
                        center = Offset(size.width * 0.72f, emberY2),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Whatshot,
            contentDescription = stringResource(R.string.home_streak_cd),
            tint = flameColor,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleY = motion.scaleY.value
                    rotationZ = motion.tiltDeg.value
                    transformOrigin = TransformOrigin(0.5f, 0.85f)
                },
        )
        if (tier != StreakTier.SPARK) {
            Icon(
                imageVector = Icons.Rounded.Whatshot,
                contentDescription = null,
                tint = StreakFlameCore.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(iconSize * 0.56f)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .graphicsLayer {
                        scaleY = motion.scaleY.value
                        transformOrigin = TransformOrigin(0.5f, 0.9f)
                    },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StreakStatusSheet(
    streakDays: Int,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tier = streakTierFor(streakDays)
    val reducedMotion = rememberNotePayReducedMotion()
    val motion = rememberStreakMotionValues(tier = tier, reducedMotion = reducedMotion)
    val flameColor = streakFlameTint(tier)

    val tierTitleRes = when (tier) {
        StreakTier.SPARK -> R.string.home_streak_tier_spark
        StreakTier.BLAZE -> R.string.home_streak_tier_blaze
        StreakTier.INFERNO -> R.string.home_streak_tier_inferno
    }
    val tierDescRes = when (tier) {
        StreakTier.SPARK -> R.string.home_streak_desc_spark
        StreakTier.BLAZE -> R.string.home_streak_desc_blaze
        StreakTier.INFERNO -> R.string.home_streak_desc_inferno
    }
    val milestoneText = when {
        streakDays < 3 -> stringResource(R.string.home_streak_next_milestone, 3 - streakDays, 3)
        streakDays < 7 -> stringResource(R.string.home_streak_next_milestone, 7 - streakDays, 7)
        else -> stringResource(R.string.home_streak_max_milestone)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AppTheme.shapes.sheetTop,
        containerColor = AppTheme.colors.surface,
        contentColor = AppTheme.colors.textPrimary,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = AppTheme.colors.hairline)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_streak_sheet_title),
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )

            Surface(
                shape = CircleShape,
                color = flameColor.copy(alpha = 0.14f),
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedFlameMark(
                        tier = tier,
                        flameColor = flameColor,
                        motion = motion,
                        reducedMotion = reducedMotion,
                        iconSize = 44.dp,
                    )
                }
            }

            Text(
                text = pluralStringResource(R.plurals.home_streak_days, streakDays, streakDays),
                style = AppTheme.typography.displaySerif.copy(fontFeatureSettings = "tnum"),
                color = AppTheme.colors.textPrimary,
            )

            Surface(
                shape = AppTheme.shapes.capsule,
                color = flameColor.copy(alpha = 0.14f),
            ) {
                Text(
                    text = stringResource(tierTitleRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = flameColor,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }

            Text(
                text = stringResource(tierDescRes),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Surface(
                shape = AppTheme.shapes.corner16,
                color = AppTheme.colors.surfaceSunken,
                border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = milestoneText,
                    style = MaterialTheme.typography.labelLarge,
                    color = AppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            OutlinedButton(
                onClick = onDismiss,
                shape = AppTheme.shapes.capsule,
                border = BorderStroke(1.dp, AppTheme.colors.borderStrong),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = AppTheme.colors.surfaceRaised,
                    contentColor = AppTheme.colors.textPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
            ) {
                Text(
                    text = stringResource(R.string.action_close),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
