package com.notepay.core.designsystem.feedback

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.navigation.rememberNotePayReducedMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private data class FeedbackSnackbarVisuals(
    override val message: String,
    override val actionLabel: String?,
    override val duration: SnackbarDuration,
    override val withDismissAction: Boolean,
    val type: FeedbackType,
) : SnackbarVisuals

internal data class FeedbackVisualSpec(
    val icon: ImageVector,
    val tint: Color,
)

internal fun FeedbackType.feedbackIcon(): ImageVector = when (this) {
    FeedbackType.Success -> Icons.Rounded.CheckCircle
    FeedbackType.Error -> Icons.Rounded.ErrorOutline
    FeedbackType.Warning -> Icons.Rounded.WarningAmber
    FeedbackType.Info -> Icons.Rounded.Info
    FeedbackType.Pending -> Icons.Rounded.Sync
    FeedbackType.Offline -> Icons.Rounded.CloudOff
}

@Composable
internal fun FeedbackType.toVisualSpec(): FeedbackVisualSpec {
    val colors = AppTheme.colors
    val tint = when (this) {
        FeedbackType.Success -> colors.success
        FeedbackType.Error -> colors.error
        FeedbackType.Warning, FeedbackType.Pending, FeedbackType.Offline -> colors.warning
        FeedbackType.Info -> colors.textSecondary
    }
    return FeedbackVisualSpec(icon = feedbackIcon(), tint = tint)
}

@Composable
internal fun FeedbackStatusIcon(
    type: FeedbackType,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    val iconModifier = if (type == FeedbackType.Pending && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "feedback_pending_sync")
        val rotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
            ),
            label = "feedback_pending_rotation",
        )
        modifier.graphicsLayer { rotationZ = rotation }
    } else {
        modifier
    }

    Icon(
        imageVector = type.feedbackIcon(),
        contentDescription = null,
        modifier = iconModifier,
        tint = tint,
    )
}

internal fun Modifier.feedbackSnackbarInsets(bottomPadding: Dp = 0.dp): Modifier =
    this.imePadding().navigationBarsPadding().padding(bottom = bottomPadding)

@Composable
internal fun BoxScope.FeedbackSnackbarOverlay(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
) {
    FeedbackSnackbarHost(
        hostState = hostState,
        modifier = modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .feedbackSnackbarInsets(bottomPadding),
    )
}

internal class LatestFeedbackDispatcher(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    private var activeJob: Job? = null

    fun show(feedback: UiFeedback) {
        activeJob?.cancel()
        hostState.currentSnackbarData?.dismiss()
        activeJob = scope.launch {
            hostState.showFeedback(feedback)
        }
    }

    fun dismiss() {
        activeJob?.cancel()
        activeJob = null
        hostState.currentSnackbarData?.dismiss()
    }
}

@Composable
internal fun rememberLatestFeedbackDispatcher(
    hostState: SnackbarHostState,
): LatestFeedbackDispatcher {
    val scope = rememberCoroutineScope()
    return remember(hostState, scope) {
        LatestFeedbackDispatcher(hostState = hostState, scope = scope)
    }
}

internal suspend fun SnackbarHostState.showFeedback(feedback: UiFeedback): Boolean {
    val duration = feedback.duration.resolveSnackbarDuration(hasAction = feedback.actionLabel != null)
    val result = showSnackbar(
        FeedbackSnackbarVisuals(
            message = feedback.message,
            actionLabel = feedback.actionLabel,
            duration = duration,
            withDismissAction = duration == SnackbarDuration.Indefinite,
            type = feedback.type,
        ),
    )
    val actionPerformed = result == SnackbarResult.ActionPerformed
    if (actionPerformed) feedback.onAction?.invoke()
    return actionPerformed
}

@Composable
internal fun FeedbackSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
) {
    SnackbarHost(hostState, modifier) { data ->
        val visuals = data.visuals
        val colors = AppTheme.colors
        val feedbackType = (visuals as? FeedbackSnackbarVisuals)?.type ?: FeedbackType.Info
        val visual = feedbackType.toVisualSpec()
        val shape = AppTheme.shapes.corner16
        val actionLabel = visuals.actionLabel
        val actionOnNewLine = LocalDensity.current.fontScale >= 1.45f

        Surface(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 8.dp)
                .iosCardShadow(shape, elevated = false)
                .semantics { liveRegion = LiveRegionMode.Polite },
            shape = shape,
            color = colors.surface,
            contentColor = colors.textPrimary,
            border = BorderStroke(0.5.dp, colors.hairline),
            shadowElevation = 0.dp,
        ) {
            if (actionOnNewLine && actionLabel != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FeedbackStatusIcon(
                            type = feedbackType,
                            tint = visual.tint,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = visuals.message,
                            style = AppTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (visuals.withDismissAction) {
                            IconButton(
                                onClick = data::dismiss,
                                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.action_close),
                                    tint = colors.textSecondary,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = data::performAction,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Text(
                                text = actionLabel,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(
                            start = 16.dp,
                            end = if (actionLabel != null || visuals.withDismissAction) 8.dp else 16.dp,
                            top = 6.dp,
                            bottom = 6.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeedbackStatusIcon(
                        type = feedbackType,
                        tint = visual.tint,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = visuals.message,
                        style = AppTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (actionLabel != null) {
                        TextButton(
                            onClick = data::performAction,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Text(
                                text = actionLabel,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    if (visuals.withDismissAction) {
                        IconButton(
                            onClick = data::dismiss,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.action_close),
                                tint = colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
