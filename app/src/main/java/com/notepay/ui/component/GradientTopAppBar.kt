package com.notepay.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent
    )
) {
    val isLightTheme = !isSystemInDarkTheme()
    val topBarColor = if (isLightTheme) Color(0xFFFAFAFA) else Color(0xFF121212)
    val gradientBrush = remember(isLightTheme) {
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
