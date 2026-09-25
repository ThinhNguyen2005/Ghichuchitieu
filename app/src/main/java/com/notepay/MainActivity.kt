package com.notepay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.platform.widget.WidgetConstants
import com.notepay.ui.navigation.NotePayNavHost
import com.notepay.ui.navigation.Route
import com.notepay.ui.theme.NotePayTheme
import com.notepay.ui.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var appSettings: AppSettingsDataStore

    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute = _pendingRoute.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        ThemeManager.initialize(this)

        setContent {
            NotePayTheme {
                val glassEnabled by appSettings.liquidGlassEnabled
                    .collectAsStateWithLifecycle(false)
                val targetRoute by pendingRoute.collectAsStateWithLifecycle(null)

                NotePayNavHost(
                    liquidGlassEnabled = glassEnabled,
                    pendingRoute = targetRoute,
                    onRouteHandled = { _pendingRoute.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val navigateTo = intent?.getStringExtra(WidgetConstants.EXTRA_NAVIGATE_TO)
            ?: if (intent?.action == WidgetConstants.ACTION_QUICK_ADD) {
                Route.AddTransaction.path
            } else {
                null
            }
        if (navigateTo != null) {
            _pendingRoute.value = navigateTo
        }
    }
}
