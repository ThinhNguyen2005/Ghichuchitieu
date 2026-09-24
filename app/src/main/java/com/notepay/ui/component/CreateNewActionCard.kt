package com.notepay.ui.component

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.dp
import com.notepay.ui.theme.AppTheme

@Composable
fun ColumnScope.CreateNewActionCard(
    visible: Boolean,
    index: Int,
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val transitionState = remember { MutableTransitionState(visible) }
    transitionState.targetState = visible
    val transition = rememberTransition(transitionState = transitionState, label = "createAction$index")
    val alpha by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = if (targetState) 150 else 100, delayMillis = if (targetState) 35 + index * 35 else 0)
        },
        label = "actionAlpha",
    ) { if (it) 1f else 0f }
    val translationY by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = if (targetState) 170 else 100, delayMillis = if (targetState) 35 + index * 35 else 0)
        },
        label = "actionTranslation",
    ) { if (it) 0f else 14f }
    val shape = AppTheme.shapes.corner20
    val actionTranslationPx = with(LocalDensity.current) { 14.dp.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.64f),
                shape = shape,
            )
            .semantics(mergeDescendants = true) { role = Role.Button }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .then(
                Modifier.graphicsLayer {
                    this.alpha = alpha
                    this.translationY = translationY / 14f * actionTranslationPx
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(accentColor.copy(alpha = if (com.notepay.ui.theme.isAppDarkTheme()) 0.26f else 0.14f), AppTheme.shapes.corner14),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accentColor)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
