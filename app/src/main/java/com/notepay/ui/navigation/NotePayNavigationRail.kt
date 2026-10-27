package com.notepay.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.notepay.ui.adaptive.NotePayAdaptive
import com.notepay.ui.theme.isAppDarkTheme

/*
 * Chiều cao item 56dp = đúng spec NavigationRailItem của Material 3 (icon 24dp + label
 * 10sp vẫn vừa khi font scale 2.0×). Nhờ vậy pill chỉ cao 260dp thay vì phủ hết chiều
 * dọc màn hình tablet, tránh việc `Backdrop` phải blur/lens toàn bộ chiều cao.
 * Mọi giá trị bên dưới là bội số của 4, nên tổng cao: 2×12 + 4×56 + 3×4 = 260dp nằm
 * đúng 4dp grid.
 */
private val RailItemHeight = 56.dp
private val RailItemSpacing = 4.dp
// Lề ngang giữ 8dp để bề rộng item còn 56dp — nhãn 10sp không bị ellipsis sớm.
private val RailPaddingHorizontal = 8.dp
// Lề dọc nới lên 12dp để tổng chiều cao chia hết cho 4.
private val RailPaddingVertical = 12.dp
private val RailStartPadding = 12.dp

/** Bề rộng pill; phần còn lại của [NotePayAdaptive.navigationRailInset] là lề và khoảng thở. */
private val RailWidth = NotePayAdaptive.navigationRailInset - 24.dp
private val RailShape: Shape = RoundedCornerShape(RailWidth / 2f)

@Composable
fun BoxScope.NotePayNavigationRail(
    currentRoute: String?,
    selectedTabIndex: Int?,
    useNavigationGlass: Boolean,
    reducedMotion: Boolean,
    backdrop: Backdrop,
    onTabSelected: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val navTabs = remember { bottomTabs }
    val labels = bottomTabs.map { tab -> stringResource(tab.labelRes) }
    val selectedIndex = selectedTabIndex ?: remember(currentRoute) {
        rootTabIndexForRoute(currentRoute) ?: 0
    }
    val isLightTheme = !isAppDarkTheme()
    val containerColor = if (isLightTheme) {
        Color.White.copy(alpha = 0.28f)
    } else {
        Color(0xFF1A1A1A).copy(alpha = 0.32f)
    }
    val primaryColor = MaterialTheme.colorScheme.primary

    // Đọc state bên trong graphicsLayer để việc trượt chỉ tốn draw phase (không recompose).
    val capsuleOffset = animateDpAsState(
        targetValue = (RailItemHeight + RailItemSpacing) * selectedIndex,
        animationSpec = if (reducedMotion) snap() else tween(NotePayMotion.tabIndicatorDurationMillis),
        label = "rail selection",
    )

    Box(
        modifier = modifier
            .align(Alignment.CenterStart)
            .zIndex(2f)
            .padding(start = RailStartPadding)
            .width(RailWidth)
            .railSurface(
                useGlass = useNavigationGlass,
                backdrop = backdrop,
                shape = RailShape,
                containerColor = containerColor,
                highlightAlpha = if (isLightTheme) 0.55f else 0.40f,
                innerShadowAlpha = if (isLightTheme) 0.12f else 0.25f,
            )
            .padding(horizontal = RailPaddingHorizontal, vertical = RailPaddingVertical),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(RailItemHeight)
                .graphicsLayer {
                    translationY = capsuleOffset.value.toPx()
                }
                .clip(RailShape)
                .background(primaryColor.copy(alpha = 0.14f)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(RailItemSpacing)) {
            navTabs.forEachIndexed { index, tab ->
                NavigationRailTabItem(
                    tab = tab,
                    label = labels[index],
                    isSelected = index == selectedIndex,
                    reducedMotion = reducedMotion,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(tab.route)
                    },
                )
            }
        }
    }
}

@Composable
private fun NavigationRailTabItem(
    tab: BottomTab,
    label: String,
    isSelected: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = if (reducedMotion) snap() else tween(NotePayMotion.tabIndicatorDurationMillis),
        label = "rail item color",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(RailItemHeight)
            .clip(RailShape)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClickLabel = null,
                onClick = onClick,
            )
            .semantics { selected = isSelected },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Nền pill: kính lỏng khi thiết bị hỗ trợ, còn lại là surface đặc — cùng quy ước với
 * [NotePayBottomBar] để hai thanh điều hướng không lệch phong cách.
 */
@Composable
private fun Modifier.railSurface(
    useGlass: Boolean,
    backdrop: Backdrop,
    shape: Shape,
    containerColor: Color,
    highlightAlpha: Float,
    innerShadowAlpha: Float,
): Modifier {
    if (!useGlass) return clip(shape).background(MaterialTheme.colorScheme.surfaceContainer)
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(10f.dp.toPx())
            lens(16f.dp.toPx(), 16f.dp.toPx())
        },
        highlight = { Highlight.Default.copy(alpha = highlightAlpha) },
        innerShadow = { InnerShadow(radius = 3f.dp, alpha = innerShadowAlpha) },
        onDrawSurface = { drawRect(containerColor) },
    )
}
