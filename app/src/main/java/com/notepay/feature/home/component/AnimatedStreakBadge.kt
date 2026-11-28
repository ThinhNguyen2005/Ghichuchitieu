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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
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

private val FlameWhiteCore = Color(0xFFFFF8DC)

private data class StreakMotionValues(
    val dancePhase: State<Float>,
    val pulse: State<Float>,
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
                dancePhase = mutableFloatStateOf(0.15f),
                pulse = mutableFloatStateOf(0.5f),
                emberPhase = mutableFloatStateOf(0f),
                shimmerProgress = mutableFloatStateOf(-1f),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "streak-flame")
    val cycleMs = when (tier) {
        StreakTier.SPARK -> 1800
        StreakTier.BLAZE -> 1450
        StreakTier.INFERNO -> 1150
    }
    val dancePhase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "streak-dance-phase",
    )
    val pulse = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (cycleMs * 0.65f).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "streak-pulse",
    )
    val emberPhase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "streak-ember",
    )
    val shimmerProgress = transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "streak-shimmer",
    )
    return StreakMotionValues(
        dancePhase = dancePhase,
        pulse = pulse,
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
 * Animated TopBar streak capsule with a multi-layered custom Bézier flame,
 * warm luminous emblem well, bounded shimmer sweep, and tactile spring feedback.
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
    val isLight = AppTheme.colors.isLight
    val baseSurface = AppTheme.colors.surface.copy(alpha = if (isLight) 0.95f else 0.90f)
    val daysLabel = pluralStringResource(R.plurals.home_streak_days, streakDays, streakDays)
    val streakCd = stringResource(R.string.home_streak_cd)

    val pillBorderBrush = remember(flameColor, isLight, tier) {
        val leftAlpha = when (tier) {
            StreakTier.SPARK -> if (isLight) 0.38f else 0.45f
            StreakTier.BLAZE -> if (isLight) 0.50f else 0.58f
            StreakTier.INFERNO -> if (isLight) 0.65f else 0.75f
        }
        val rightAlpha = if (isLight) 0.16f else 0.22f
        Brush.horizontalGradient(
            colors = listOf(
                flameColor.copy(alpha = leftAlpha),
                StreakFlameCore.copy(alpha = rightAlpha),
            ),
        )
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget)
            .graphicsLayer {
                val scale = tapScale.value
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = {
                performHaptic(HapticFeedbackType.TextHandleMove)
                if (!reducedMotion) {
                    coroutineScope.launch {
                        tapScale.snapTo(0.92f)
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
            color = baseSurface,
            contentColor = AppTheme.colors.textPrimary,
            border = BorderStroke(1.dp, pillBorderBrush),
            modifier = Modifier
                .iosCardShadow(shape = pillShape, elevated = false)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$streakCd: $daysLabel"
                },
        ) {
            Row(
                modifier = Modifier
                    .drawWithCache {
                        val washBrush = Brush.horizontalGradient(
                            colors = listOf(
                                flameColor.copy(alpha = if (isLight) 0.16f else 0.22f),
                                StreakFlameCore.copy(alpha = if (isLight) 0.06f else 0.08f),
                                Color.Transparent,
                            ),
                            startX = 0f,
                            endX = size.width * 0.85f,
                        )
                        val shimmerWidth = size.width * 0.42f
                        val shimmerBrush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                StreakFlameCore.copy(alpha = if (isLight) 0.20f else 0.16f),
                                Color.Transparent,
                            ),
                            start = Offset.Zero,
                            end = Offset(shimmerWidth, size.height),
                        )
                        onDrawWithContent {
                            drawRect(brush = washBrush)
                            drawContent()
                            if (tier != StreakTier.SPARK && !reducedMotion) {
                                val progress = motion.shimmerProgress.value
                                if (progress in -0.4f..1.4f) {
                                    val left = progress * (size.width + shimmerWidth) - shimmerWidth
                                    translate(left = left) {
                                        drawRect(
                                            brush = shimmerBrush,
                                            size = Size(shimmerWidth, size.height),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    .padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                // Dedicated circular flame medallion well
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    StreakFlameCore.copy(alpha = if (isLight) 0.28f else 0.32f),
                                    flameColor.copy(alpha = if (isLight) 0.14f else 0.20f),
                                ),
                            ),
                        )
                        .border(
                            width = 0.75.dp,
                            color = flameColor.copy(alpha = if (isLight) 0.32f else 0.42f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedFlameMark(
                        tier = tier,
                        flameColor = flameColor,
                        motion = motion,
                        reducedMotion = reducedMotion,
                        iconSize = 20.dp,
                    )
                }

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
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Custom 3-layer organic Bézier flame drawn entirely in the Draw phase.
 * Reuses cached [Path] instances across frames so zero allocations occur during animation.
 */
@Composable
private fun AnimatedFlameMark(
    tier: StreakTier,
    flameColor: Color,
    motion: StreakMotionValues,
    reducedMotion: Boolean,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    val outerPath = remember { Path() }
    val midPath = remember { Path() }
    val corePath = remember { Path() }

    val baseOuterColor = when (tier) {
        StreakTier.SPARK -> Color(0xFFE84A00)
        StreakTier.BLAZE -> Color(0xFFD92B00)
        StreakTier.INFERNO -> Color(0xFFB81400)
    }

    Canvas(
        modifier = modifier.size(iconSize),
    ) {
        val w = size.width
        val h = size.height
        val phaseAngle = (motion.dancePhase.value * 2f * PI).toFloat()
        val pulseVal = motion.pulse.value

        val sway = if (reducedMotion) 0f else sin(phaseAngle)
        val secondarySway = if (reducedMotion) 0f else cos(phaseAngle * 1.35f)
        val heightPulse = if (reducedMotion) 0f else (pulseVal - 0.5f) * 0.055f

        val haloRadius = (w * 0.48f) * (1f + heightPulse * 0.8f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    flameColor.copy(alpha = 0.26f),
                    StreakFlameCore.copy(alpha = 0.12f),
                    Color.Transparent,
                ),
                center = Offset(w * 0.5f, h * 0.56f),
                radius = haloRadius,
            ),
            radius = haloRadius,
            center = Offset(w * 0.5f, h * 0.56f),
        )

        if (!reducedMotion) {
            drawRisingEmbers(
                tier = tier,
                emberPhase = motion.emberPhase.value,
                flameColor = flameColor,
            )
        }

        drawFlameLayers(
            outerPath = outerPath,
            midPath = midPath,
            corePath = corePath,
            sway = sway,
            secondarySway = secondarySway,
            heightPulse = heightPulse,
            flameColor = flameColor,
            baseOuterColor = baseOuterColor,
        )
    }
}

@Suppress("LongParameterList")
private fun DrawScope.drawFlameLayers(
    outerPath: Path,
    midPath: Path,
    corePath: Path,
    sway: Float,
    secondarySway: Float,
    heightPulse: Float,
    flameColor: Color,
    baseOuterColor: Color,
) {
    val w = size.width
    val h = size.height
    val baseX = w * 0.50f
    val baseY = h * 0.92f
    val mainTipX = w * (0.48f + sway * 0.055f)
    val mainTipY = h * (0.07f - heightPulse)

    buildOuterFlamePath(
        path = outerPath,
        w = w,
        h = h,
        baseX = baseX,
        baseY = baseY,
        mainTipX = mainTipX,
        mainTipY = mainTipY,
        sway = sway,
        secondarySway = secondarySway,
        heightPulse = heightPulse,
    )
    drawPath(
        path = outerPath,
        brush = Brush.verticalGradient(
            colors = listOf(baseOuterColor, flameColor, baseOuterColor),
            startY = mainTipY,
            endY = baseY,
        ),
    )

    val midTipX = w * (0.49f - secondarySway * 0.04f)
    val midTipY = h * (0.24f - heightPulse * 0.65f)
    val midBaseY = h * 0.89f
    buildMidFlamePath(
        path = midPath,
        w = w,
        h = h,
        baseX = baseX,
        midBaseY = midBaseY,
        midTipX = midTipX,
        midTipY = midTipY,
        sway = sway,
        secondarySway = secondarySway,
    )
    drawPath(
        path = midPath,
        brush = Brush.verticalGradient(
            colors = listOf(StreakFlameCore, Color(0xFFFF9E00), flameColor),
            startY = midTipY,
            endY = midBaseY,
        ),
    )

    val coreTipX = w * (0.50f + sway * 0.025f)
    val coreTipY = h * (0.48f - heightPulse * 0.45f)
    val coreBaseY = h * 0.87f
    corePath.reset()
    corePath.moveTo(baseX, coreBaseY)
    corePath.cubicTo(w * 0.35f, h * 0.85f, w * 0.36f, h * 0.63f, coreTipX, coreTipY)
    corePath.cubicTo(w * 0.64f, h * 0.63f, w * 0.65f, h * 0.85f, baseX, coreBaseY)
    corePath.close()

    drawPath(
        path = corePath,
        brush = Brush.verticalGradient(
            colors = listOf(FlameWhiteCore, Color.White, StreakFlameCore),
            startY = coreTipY,
            endY = coreBaseY,
        ),
    )
}

@Suppress("LongParameterList")
private fun buildOuterFlamePath(
    path: Path,
    w: Float,
    h: Float,
    baseX: Float,
    baseY: Float,
    mainTipX: Float,
    mainTipY: Float,
    sway: Float,
    secondarySway: Float,
    heightPulse: Float,
) {
    val leftTongueX = w * (0.19f + secondarySway * 0.025f)
    val leftTongueY = h * (0.37f + heightPulse * 0.35f)
    val rightTongueX = w * (0.81f + secondarySway * 0.03f)
    val rightTongueY = h * (0.27f - heightPulse * 0.35f)

    path.reset()
    path.moveTo(baseX, baseY)
    path.cubicTo(w * 0.20f, h * 0.91f, w * 0.09f, h * 0.64f, leftTongueX, leftTongueY)
    path.cubicTo(w * 0.22f, h * 0.47f, w * 0.30f, h * 0.50f, w * (0.33f + sway * 0.02f), h * 0.43f)
    path.cubicTo(w * (0.36f + sway * 0.03f), h * 0.27f, w * (0.41f + sway * 0.045f), h * 0.16f, mainTipX, mainTipY)
    path.cubicTo(w * (0.58f + sway * 0.04f), h * 0.18f, w * (0.63f + sway * 0.025f), h * 0.35f, w * (0.66f + secondarySway * 0.02f), h * 0.43f)
    path.cubicTo(w * 0.70f, h * 0.39f, w * 0.76f, h * 0.33f, rightTongueX, rightTongueY)
    path.cubicTo(w * 0.92f, h * 0.54f, w * 0.82f, h * 0.91f, baseX, baseY)
    path.close()
}

@Suppress("LongParameterList")
private fun buildMidFlamePath(
    path: Path,
    w: Float,
    h: Float,
    baseX: Float,
    midBaseY: Float,
    midTipX: Float,
    midTipY: Float,
    sway: Float,
    secondarySway: Float,
) {
    path.reset()
    path.moveTo(baseX, midBaseY)
    path.cubicTo(w * 0.27f, h * 0.87f, w * 0.22f, h * 0.58f, midTipX, midTipY)
    path.cubicTo(w * 0.58f, h * 0.36f, w * 0.60f, h * 0.49f, w * (0.64f + sway * 0.02f), h * 0.52f)
    path.cubicTo(w * 0.69f, h * 0.47f, w * 0.73f, h * 0.43f, w * (0.74f + secondarySway * 0.02f), h * 0.46f)
    path.cubicTo(w * 0.80f, h * 0.65f, w * 0.73f, h * 0.87f, baseX, midBaseY)
    path.close()
}

private fun DrawScope.drawRisingEmbers(
    tier: StreakTier,
    emberPhase: Float,
    flameColor: Color,
) {
    val w = size.width
    val h = size.height
    val count = when (tier) {
        StreakTier.SPARK -> 1
        StreakTier.BLAZE -> 2
        StreakTier.INFERNO -> 3
    }
    for (i in 0 until count) {
        val localPhase = (emberPhase + i * (1f / count)) % 1f
        // Smooth bell curve alpha: fades in at 0, peaks at 0.4, fades out smoothly to 0 at 1.0
        val alpha = sin(localPhase * PI).toFloat().coerceIn(0f, 1f) * 0.85f
        val xAnchor = when (i) {
            0 -> 0.26f
            1 -> 0.74f
            else -> 0.50f
        }
        val driftX = sin((localPhase * 2f * PI + i).toFloat()) * (w * 0.04f)
        val emberX = w * xAnchor + driftX
        val emberY = h * (0.46f - localPhase * 0.36f)
        val radius = size.minDimension * (0.055f * (1f - localPhase * 0.35f))
        val color = if (i % 2 == 0) StreakFlameCore else flameColor
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius,
            center = Offset(emberX, emberY),
        )
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
    val isLight = AppTheme.colors.isLight

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.home_streak_sheet_title),
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )

            // Concentric luminous flame medallion
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .drawWithCache {
                        val outerGlow = Brush.radialGradient(
                            colors = listOf(
                                StreakFlameCore.copy(alpha = if (isLight) 0.30f else 0.35f),
                                flameColor.copy(alpha = if (isLight) 0.14f else 0.18f),
                                Color.Transparent,
                            ),
                        )
                        val ringStroke = Stroke(width = 1.25.dp.toPx())
                        onDrawBehind {
                            drawCircle(brush = outerGlow)
                            drawCircle(
                                color = flameColor.copy(alpha = 0.35f),
                                radius = size.minDimension * 0.42f,
                                style = ringStroke,
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedFlameMark(
                    tier = tier,
                    flameColor = flameColor,
                    motion = motion,
                    reducedMotion = reducedMotion,
                    iconSize = 52.dp,
                )
            }

            Text(
                text = pluralStringResource(R.plurals.home_streak_days, streakDays, streakDays),
                style = AppTheme.typography.displaySerif.copy(fontFeatureSettings = "tnum"),
                color = AppTheme.colors.textPrimary,
            )

            Surface(
                shape = AppTheme.shapes.capsule,
                color = flameColor.copy(alpha = if (isLight) 0.14f else 0.20f),
                border = BorderStroke(0.75.dp, flameColor.copy(alpha = 0.40f)),
            ) {
                Text(
                    text = stringResource(tierTitleRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = flameColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            Text(
                text = stringResource(tierDescRes),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            StreakMilestoneProgressCard(
                streakDays = streakDays,
                flameColor = flameColor,
                milestoneText = milestoneText,
            )

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

@Composable
private fun StreakMilestoneProgressCard(
    streakDays: Int,
    flameColor: Color,
    milestoneText: String,
) {
    val progressFraction = (streakDays.coerceIn(0, 7) / 7f)
    val trackColor = AppTheme.colors.hairline
    val milestones = listOf(
        1 to R.string.home_streak_tier_spark,
        3 to R.string.home_streak_tier_blaze,
        7 to R.string.home_streak_tier_inferno,
    )

    Surface(
        shape = AppTheme.shapes.corner20,
        color = AppTheme.colors.surfaceSunken,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                milestones.forEach { (targetDays, labelRes) ->
                    val reached = streakDays >= targetDays
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    if (reached) flameColor else AppTheme.colors.surface,
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (reached) flameColor else AppTheme.colors.hairline,
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (reached) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp),
                                )
                            } else {
                                Text(
                                    text = targetDays.toString(),
                                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.textSecondary,
                                )
                            }
                        }
                        Text(
                            text = pluralStringResource(R.plurals.home_streak_days, targetDays, targetDays),
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = if (reached) FontWeight.Bold else FontWeight.Medium,
                            color = if (reached) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (reached) flameColor else AppTheme.colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Smooth rounded gradient progress bar (0..7 days)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            ) {
                val corner = CornerRadius(size.height / 2f, size.height / 2f)
                drawRoundRect(
                    color = trackColor,
                    cornerRadius = corner,
                )
                val fillWidth = (size.width * progressFraction).coerceAtLeast(size.height)
                if (streakDays > 0) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                StreakFlameCore,
                                flameColor,
                            ),
                            startX = 0f,
                            endX = fillWidth,
                        ),
                        size = Size(fillWidth, size.height),
                        cornerRadius = corner,
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = milestoneText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
