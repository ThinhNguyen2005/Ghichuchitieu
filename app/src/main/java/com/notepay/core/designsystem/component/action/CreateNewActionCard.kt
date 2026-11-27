package com.notepay.core.designsystem.component.action

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.isAppDarkTheme
import com.notepay.core.navigation.rememberNotePayReducedMotion

@Composable
fun CreateNewActionCard(
    visible: Boolean,
    index: Int,
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    val transitionState = remember { MutableTransitionState(if (reducedMotion) visible else false) }
    transitionState.targetState = visible
    val transition = rememberTransition(transitionState = transitionState, label = "createAction$index")
    val alpha by transition.animateFloat(
        transitionSpec = {
            if (reducedMotion) snap()
            else tween(durationMillis = if (targetState) 150 else 100, delayMillis = if (targetState) 30 + index * 30 else 0)
        },
        label = "actionAlpha",
    ) { if (it) 1f else 0f }
    val translationY by transition.animateFloat(
        transitionSpec = {
            if (reducedMotion) snap()
            else tween(durationMillis = if (targetState) 170 else 100, delayMillis = if (targetState) 30 + index * 30 else 0)
        },
        label = "actionTranslation",
    ) { if (it) 0f else 14f }
    val shape = AppTheme.shapes.corner20
    val actionTranslationPx = with(LocalDensity.current) { 14.dp.toPx() }
    val iconContainerAlpha = if (isAppDarkTheme()) 0.24f else 0.14f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translationY / 14f * actionTranslationPx
            }
            .clip(shape)
            .background(AppTheme.colors.surfaceSunken)
            .border(
                width = 0.5.dp,
                color = AppTheme.colors.hairline,
                shape = shape,
            )
            .semantics(mergeDescendants = true) { role = Role.Button }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(accentColor.copy(alpha = iconContainerAlpha), AppTheme.shapes.corner16),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accentColor)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = AppTheme.colors.textSecondary,
        )
    }
}
