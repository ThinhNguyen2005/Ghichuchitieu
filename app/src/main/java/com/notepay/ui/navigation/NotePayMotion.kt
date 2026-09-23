package com.notepay.ui.navigation

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object NotePayMotion {
    const val tabIndicatorDurationMillis = 220
    const val contentDurationMillis = 220
    const val contentFadeOutDurationMillis = 140
    const val contentFadeDelayMillis = 40
    const val navigationBarSettleDurationMillis = 200
    val contentTranslation: Dp = 12.dp
}

private val rootTabRoutes = listOf(
    Route.Home.path,
    Route.Stats.path,
    Route.Assets.path,
    Route.Utilities.path,
)

internal fun rootTabIndexForRoute(route: String?): Int? {
    val baseRoute = route?.substringBefore('?') ?: return null
    return rootTabRoutes.indexOf(baseRoute).takeIf { it >= 0 }
}

internal fun tabTransitionDirection(initialRoute: String?, targetRoute: String?): Int? {
    val initialIndex = rootTabIndexForRoute(initialRoute) ?: return null
    val targetIndex = rootTabIndexForRoute(targetRoute) ?: return null
    return (targetIndex - initialIndex).sign.takeIf { it != 0 }
}

internal fun tabIndicatorOffsetPx(
    selectedIndex: Int,
    tabCount: Int,
    tabWidthPx: Float,
): Float {
    if (tabCount <= 0) return 0f
    return selectedIndex.coerceIn(0, tabCount - 1) * tabWidthPx
}

@Composable
internal fun rememberNotePayReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

private val Int.sign: Int
    get() = when {
        this < 0 -> -1
        this > 0 -> 1
        else -> 0
    }
