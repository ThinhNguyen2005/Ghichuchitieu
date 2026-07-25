package com.notepay.ui.component

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/** A short-lived glass panel anchored above NotePay's floating navigation. */
@Composable
fun BoxScope.CreateNewGlassPanel(
    backdrop: Backdrop,
    expanded: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onHidden: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(Boolean) -> Unit,
) {
    val visibility = remember { MutableTransitionState(false) }
    LaunchedEffect(expanded) { visibility.targetState = expanded }
    LaunchedEffect(visibility.currentState, visibility.isIdle) {
        if (!visibility.currentState && visibility.isIdle) onHidden()
    }

    if (visibility.currentState || visibility.targetState) {
        val transition = updateTransition(visibility, label = "createNewPanel")
        val panelAlpha by transition.animateFloat(
            transitionSpec = { tween(durationMillis = if (targetState) 150 else 120) },
            label = "panelAlpha",
        ) { if (it) 1f else 0f }
        val scrimAlpha by transition.animateFloat(
            transitionSpec = { tween(durationMillis = 180) },
            label = "scrimAlpha",
        ) { if (it) 0.10f else 0f }
        val translationProgress by transition.animateFloat(
            transitionSpec = { spring(dampingRatio = 0.84f, stiffness = 420f) },
            label = "panelTranslation",
        ) { if (it) 0f else 1f }
        val darkTheme = isSystemInDarkTheme()
        val shape = RoundedCornerShape(32.dp)
        val panelSurface = if (darkTheme) {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )

            Column(
                modifier = modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 16.dp, end = 16.dp, bottom = 88.dp)
                    .navigationBarsPadding()
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .semantics { paneTitle = title }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            vibrancy()
                            blur(8.dp.toPx())
                        },
                        layerBlock = {
                            alpha = panelAlpha
                            translationY = 36.dp.toPx() * translationProgress
                        },
                        onDrawSurface = { drawRect(panelSurface) },
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                content(expanded)
            }
        }
    }
}

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
    val transition = updateTransition(visible, label = "createAction$index")
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
    val shape = RoundedCornerShape(20.dp)
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
                .background(accentColor.copy(alpha = if (isSystemInDarkTheme()) 0.26f else 0.14f), RoundedCornerShape(14.dp)),
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

@Composable
fun FloatingAddButton(
    backdrop: Backdrop,
    expanded: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val transition = updateTransition(expanded, label = "floatingAdd")
    val iconRotation by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.86f, stiffness = 500f) },
        label = "addIconRotation",
    ) { if (it) 45f else 0f }
    val expandedLift by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.86f, stiffness = 500f) },
        label = "addLift",
    ) { if (it) 0.96f else 1f }
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        label = "addPressScale",
    )
    val shape = CircleShape
    val darkTheme = isSystemInDarkTheme()
    val buttonSurface = if (darkTheme) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    }

    Box(
        modifier = modifier
            .size(64.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(6.dp.toPx())
                    if (supportsLiquidLens()) lens(6.dp.toPx(), 12.dp.toPx())
                },
                layerBlock = {
                    val scale = pressScale * expandedLift
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = { drawRect(buttonSurface) },
            )
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(30.dp)
                .graphicsLayer { rotationZ = iconRotation },
        )
    }
}
