package com.notepay.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.zIndex
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.notepay.R
import com.notepay.ui.component.FloatingAddButton
import com.notepay.ui.navigation.utils.DampedDragAnimation
import com.notepay.ui.navigation.utils.InteractiveHighlight
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

internal data class BottomTab(
    val route: Route,
    @param:StringRes val labelRes: Int,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
)

internal val bottomTabs = listOf(
    BottomTab(Route.Home, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    BottomTab(Route.Stats, R.string.nav_report, Icons.Outlined.Analytics, Icons.Filled.Analytics),
    BottomTab(
        Route.Assets,
        R.string.nav_assets,
        Icons.Outlined.AccountBalanceWallet,
        Icons.Filled.AccountBalanceWallet,
    ),
    BottomTab(Route.Utilities, R.string.nav_more, Icons.Outlined.MoreHoriz, Icons.Filled.MoreHoriz),
)

internal fun isMainTabRoute(route: String?): Boolean {
    if (route == null) return false
    return bottomTabs.any { tab ->
        route == tab.route.path || route.startsWith("${tab.route.path}?")
    }
}

private val LocalLiquidBottomTabScale = staticCompositionLocalOf { { 1f } }

@Composable
private fun RowScope.NotePayBottomTabItem(
    tab: BottomTab,
    tabLabel: String,
    isSourceActiveRow: Boolean,
    onClick: () -> Unit,
    isInteractive: Boolean = true,
    reducedMotion: Boolean = false,
) {
    val scale = LocalLiquidBottomTabScale.current
    val selectionProgress by animateFloatAsState(
        targetValue = if (isSourceActiveRow) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(NotePayMotion.tabIndicatorDurationMillis),
        label = "bottom tab selection",
    )
    val activeColor by animateColorAsState(
        targetValue = if (isSourceActiveRow) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = if (reducedMotion) snap() else tween(NotePayMotion.tabIndicatorDurationMillis),
        label = "bottom tab color",
    )
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .then(
                if (isInteractive) Modifier.clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick
                ) else Modifier
            )
            .fillMaxHeight()
            .weight(1f)
            .semantics { selected = isSourceActiveRow }
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                val contentScale = 0.96f + (0.04f * selectionProgress)
                scaleX = contentScale
                scaleY = contentScale
            },
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = if (isSourceActiveRow) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tabLabel,
                tint = activeColor,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tabLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isSourceActiveRow) FontWeight.Bold else FontWeight.Normal,
                ),
                color = activeColor,
            )
        }
    }
}

@Composable
fun BoxScope.NotePayBottomBar(
    currentRoute: String?,
    showQuickAddSheet: Boolean,
    useNavigationGlass: Boolean,
    reducedMotion: Boolean,
    backdrop: Backdrop,
    onTabSelected: (Route) -> Unit,
    onToggleQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
    navigationBarOffsetProvider: () -> Float = { 0f },
    selectedTabIndex: Int? = null,
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val navTabs = remember { bottomTabs }
    val tabLabels = bottomTabs.map { tab -> stringResource(tab.labelRes) }
    val fabContentDesc = stringResource(
        if (showQuickAddSheet) R.string.quick_add_close_menu else R.string.quick_add_open_menu
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val isLightTheme = !isSystemInDarkTheme()
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.65f) else Color(0xFF121212).copy(0.7f)
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    val selectedIndex = selectedTabIndex ?: remember(currentRoute) {
        rootTabIndexForRoute(currentRoute) ?: 0
    }

    val tabsBackdrop = rememberLayerBackdrop()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(2f)
            .alpha(if (showQuickAddSheet) 0.92f else 1f)
            .align(Alignment.BottomCenter)
            .graphicsLayer {
                translationY = navigationBarOffsetProvider()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!useNavigationGlass) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                val tabWidthPx = constraints.maxWidth.toFloat() / navTabs.size
                val tabWidth = with(density) { tabWidthPx.toDp() }
                val indicatorOffset = remember {
                    Animatable(
                        tabIndicatorOffsetPx(
                            selectedIndex = selectedIndex,
                            tabCount = navTabs.size,
                            tabWidthPx = tabWidthPx,
                        )
                    )
                }

                LaunchedEffect(selectedIndex, tabWidthPx, reducedMotion) {
                    val targetOffset = tabIndicatorOffsetPx(
                        selectedIndex = selectedIndex,
                        tabCount = navTabs.size,
                        tabWidthPx = tabWidthPx,
                    )
                    if (reducedMotion) {
                        indicatorOffset.snapTo(targetOffset)
                    } else {
                        indicatorOffset.animateTo(
                            targetValue = targetOffset,
                            animationSpec = tween(NotePayMotion.tabIndicatorDurationMillis),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(tabWidth)
                        .graphicsLayer {
                            translationX = indicatorOffset.value
                        }
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            CircleShape,
                        ),
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    navTabs.forEachIndexed { index, tab ->
                        NotePayBottomTabItem(
                            tab = tab,
                            tabLabel = tabLabels[bottomTabs.indexOf(tab)],
                            isSourceActiveRow = index == selectedIndex,
                            reducedMotion = reducedMotion,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab.route)
                            },
                        )
                    }
                }
            }
        } else {
            // Left: Tabs Bar
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val tabsCount = navTabs.size
                val tabWidth = with(density) {
                    (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
                }
                val widthPx = constraints.maxWidth.toFloat()

                val offsetAnimation = remember { Animatable(0f) }
                val panelOffset by remember(density, widthPx) {
                    derivedStateOf {
                        val fraction = (offsetAnimation.value / widthPx).fastCoerceIn(-1f, 1f)
                        with(density) {
                            4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                        }
                    }
                }

                val dampedDragAnimation: DampedDragAnimation = remember(
                    coroutineScope,
                    tabWidth,
                    reducedMotion,
                ) {
                    DampedDragAnimation(
                        animationScope = coroutineScope,
                        initialValue = selectedIndex.toFloat(),
                        valueRange = 0f..(tabsCount - 1).toFloat(),
                        visibilityThreshold = 0.001f,
                        initialScale = 1f,
                        pressedScale = 78f / 56f,
                        reducedMotion = reducedMotion,
                        onDragStarted = {},
                        onDragStopped = {
                            val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(navTabs[targetIndex].route)
                            coroutineScope.launch {
                                if (reducedMotion) {
                                    offsetAnimation.snapTo(0f)
                                } else {
                                    offsetAnimation.animateTo(
                                        0f,
                                        spring(1f, 300f, 0.5f)
                                    )
                                }
                            }
                        },
                        onDrag = { _, dragAmount ->
                            updateValue(
                                (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                                    .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                            )
                            coroutineScope.launch {
                                offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                            }
                        }
                    )
                }

                LaunchedEffect(selectedIndex) {
                    if (selectedIndex.toFloat() != dampedDragAnimation.targetValue) {
                        dampedDragAnimation.animateToValue(selectedIndex.toFloat())
                    }
                }

                val interactiveHighlight = remember(coroutineScope, tabWidth, reducedMotion) {
                    InteractiveHighlight(
                        animationScope = coroutineScope,
                        reducedMotion = reducedMotion,
                        position = { size, _ ->
                            Offset(
                                if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                                else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                                size.height / 2f
                            )
                        }
                    )
                }

                // Row 1: Bottom Layer - Main visible bar
                Row(
                    Modifier
                        .graphicsLayer {
                            translationX = panelOffset
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                                lens(8.dp.toPx(), 8f.dp.toPx())
                            },
                            layerBlock = {
                                val progress = dampedDragAnimation.pressProgress
                                val scale = androidx.compose.ui.util.lerp(
                                    1f,
                                    1f + 16f.dp.toPx() / size.width,
                                    progress
                                )
                                scaleX = scale
                                scaleY = scale
                            },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        .then(interactiveHighlight.modifier)
                        .height(64f.dp)
                        .fillMaxWidth()
                        .padding(4f.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navTabs.forEach { tab ->
                        NotePayBottomTabItem(
                            tab = tab,
                            tabLabel = tabLabels[bottomTabs.indexOf(tab)],
                            isSourceActiveRow = false,
                            reducedMotion = reducedMotion,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val targetIndex = navTabs.indexOf(tab)
                                dampedDragAnimation.animateToValue(targetIndex.toFloat())
                                onTabSelected(tab.route)
                            }
                        )
                    }
                }

                // Row 2: Capture labels and content backdrop only
                CompositionLocalProvider(
                    LocalLiquidBottomTabScale provides {
                        androidx.compose.ui.util.lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
                    }
                ) {
                    Row(
                        Modifier
                            .clearAndSetSemantics {}
                            .alpha(0f)
                            .layerBackdrop(tabsBackdrop)
                            .graphicsLayer {
                                translationX = panelOffset
                            }
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { CircleShape },
                                effects = {
                                    val progress = dampedDragAnimation.pressProgress
                                    vibrancy()
                                    blur(8f.dp.toPx())
                                    lens(
                                        24f.dp.toPx() * progress,
                                        24f.dp.toPx() * progress
                                    )
                                },
                                highlight = {
                                    val progress = dampedDragAnimation.pressProgress
                                    Highlight.Default.copy(alpha = progress)
                                },
                                onDrawSurface = { drawRect(containerColor) }
                            )
                            .then(interactiveHighlight.modifier)
                            .height(56f.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 4f.dp)
                            .graphicsLayer(colorFilter = ColorFilter.tint(primaryColor)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navTabs.forEach { tab ->
                            NotePayBottomTabItem(
                                tab = tab,
                                tabLabel = tabLabels[bottomTabs.indexOf(tab)],
                                isSourceActiveRow = true,
                                onClick = {},
                                isInteractive = false,
                                reducedMotion = reducedMotion,
                            )
                        }
                    }
                }

                // Box 3: Sliding Highlight Indicator
                Box(
                    Modifier
                        .padding(horizontal = 4f.dp)
                        .graphicsLayer {
                            translationX =
                                if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                                else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                        }
                        .then(interactiveHighlight.gestureModifier)
                        .then(dampedDragAnimation.modifier)
                        .drawBackdrop(
                            backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                            shape = { CircleShape },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                lens(
                                    10f.dp.toPx() * progress,
                                    14f.dp.toPx() * progress,
                                    chromaticAberration = true
                                )
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Default.copy(alpha = progress)
                            },
                            shadow = {
                                val progress = dampedDragAnimation.pressProgress
                                Shadow(alpha = progress)
                            },
                            innerShadow = {
                                val progress = dampedDragAnimation.pressProgress
                                InnerShadow(
                                    radius = 8f.dp * progress,
                                    alpha = progress
                                )
                            },
                            layerBlock = {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(
                                    if (isLightTheme) Color.Black.copy(0.1f)
                                    else Color.White.copy(0.1f),
                                    alpha = 1f - progress
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            }
                        )
                        .height(56f.dp)
                        .width(with(density) { tabWidth.toDp() })
                )
            }
        }

        FloatingAddButton(
            backdrop = backdrop,
            expanded = showQuickAddSheet,
            contentDescription = fabContentDesc,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleQuickAdd()
            },
        )
    }
}

@Deprecated(
    message = "Use navigationBarOffsetProvider lambda instead to avoid recomposition",
    replaceWith = ReplaceWith(
        "NotePayBottomBar(currentRoute, showQuickAddSheet, useNavigationGlass, reducedMotion, backdrop, onTabSelected, onToggleQuickAdd, modifier, { navigationBarOffset })"
    )
)
@Composable
fun BoxScope.NotePayBottomBar(
    currentRoute: String?,
    navigationBarOffset: Float,
    showQuickAddSheet: Boolean,
    useNavigationGlass: Boolean,
    reducedMotion: Boolean,
    backdrop: Backdrop,
    onTabSelected: (Route) -> Unit,
    onToggleQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NotePayBottomBar(
        currentRoute = currentRoute,
        showQuickAddSheet = showQuickAddSheet,
        useNavigationGlass = useNavigationGlass,
        reducedMotion = reducedMotion,
        backdrop = backdrop,
        onTabSelected = onTabSelected,
        onToggleQuickAdd = onToggleQuickAdd,
        modifier = modifier,
        navigationBarOffsetProvider = { navigationBarOffset },
    )
}

