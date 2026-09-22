package com.notepay.ui.component

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop

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
    val transitionState = remember { MutableTransitionState(expanded) }
    transitionState.targetState = expanded
    val transition = rememberTransition(transitionState = transitionState, label = "floatingAdd")
    val iconRotation by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.86f, stiffness = 500f) },
        label = "addIconRotation",
    ) { if (it) 45f else 0f }
    val expandedLift by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.86f, stiffness = 500f) },
        label = "addLift",
    ) { if (it) 0.96f else 1f }
    val pressScale by animateFloatAsState(
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
            .graphicsLayer {
                val scale = pressScale * expandedLift
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(buttonSurface.copy(alpha = 1f))
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
