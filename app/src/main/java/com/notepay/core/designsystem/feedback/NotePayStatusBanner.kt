package com.notepay.core.designsystem.feedback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.navigation.NotePayAnimatedVisibility

/**
 * Shared inline status banner for persistent non-transient states (offline, syncing, warning,
 * retryable inline errors) styled per DESIGN.md v5.0.0.
 */
@Composable
internal fun NotePayStatusBanner(
    message: String,
    type: FeedbackType,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    useSoftTint: Boolean = true,
    liveRegionMode: LiveRegionMode = LiveRegionMode.Polite,
) {
    NotePayAnimatedVisibility(
        visible = visible,
        modifier = modifier,
    ) {
        val colors = AppTheme.colors
        val visual = type.toVisualSpec()
        val shape = AppTheme.shapes.corner16
        val containerColor = if (useSoftTint) {
            visual.tint.copy(alpha = if (colors.isLight) 0.10f else 0.16f).compositeOver(colors.surface)
        } else {
            colors.surface
        }
        val borderColor = if (useSoftTint) visual.tint.copy(alpha = 0.28f) else colors.hairline
        val hasAction = actionLabel != null && onAction != null
        val stackAction = hasAction && LocalDensity.current.fontScale >= 1.45f

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .iosCardShadow(shape, elevated = false)
                .semantics { liveRegion = liveRegionMode },
            shape = shape,
            color = containerColor,
            contentColor = colors.textPrimary,
            border = BorderStroke(0.5.dp, borderColor),
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = if (hasAction && !stackAction) 6.dp else 12.dp,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FeedbackStatusIcon(
                        type = type,
                        tint = visual.tint,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = message,
                        style = AppTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (hasAction && !stackAction) {
                        TextButton(
                            onClick = onAction,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Text(
                                text = actionLabel,
                                color = visual.tint,
                                style = AppTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                if (hasAction && stackAction) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = onAction,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Text(
                                text = actionLabel,
                                color = visual.tint,
                                style = AppTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}
