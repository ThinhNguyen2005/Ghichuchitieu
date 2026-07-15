package com.notepay.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GradientBottomActionBar(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = 16.dp,
        top = 20.dp,
        end = 16.dp,
        bottom = 10.dp,
    ),
    content: @Composable () -> Unit,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val bottomBarColor = if (isLightTheme) Color(0xFFFAFAFA) else Color(0xFF121212)
    val gradientBrush = remember(isLightTheme) {
        Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.35f to bottomBarColor.copy(alpha = 0.55f),
            1.0f to bottomBarColor.copy(alpha = 0.88f),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(gradientBrush)
            .navigationBarsPadding()
            .padding(contentPadding),
    ) {
        content()
    }
}
