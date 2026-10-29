package com.notepay.core.designsystem.component.bar

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    // Màu gốc của dải gradient. Đặt bằng màu container thật của thanh khi nó không nằm
    // trên nền `colorScheme.background` (ví dụ pane chi tiết của bố cục hai pane).
    topBarColor: Color = MaterialTheme.colorScheme.background,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent
    )
) {
    val gradientBrush = remember(topBarColor) {
        Brush.verticalGradient(
            0.0f to topBarColor,
            0.6f to topBarColor,
            0.85f to topBarColor.copy(alpha = 0.5f),
            1.0f to Color.Transparent
        )
    }

    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = windowInsets,
        colors = colors,
        modifier = modifier.background(gradientBrush)
    )
}
