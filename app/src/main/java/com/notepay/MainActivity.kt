package com.notepay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.ui.MainViewModel
import com.notepay.ui.navigation.NotePayNavHost
import com.notepay.ui.navigation.Route
import com.notepay.ui.theme.NotePayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        viewModel.handleIncomingIntent(intent)

        splashScreen.setKeepOnScreenCondition {
            viewModel.startDestination.value == null
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
            val glassEnabled by viewModel.liquidGlassEnabled.collectAsStateWithLifecycle()
            val targetRoute by viewModel.pendingRoute.collectAsStateWithLifecycle()
            val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()

            if (startDestination != null) {
                NotePayTheme(
                    themeMode = themeMode,
                    themeColor = themeColor,
                ) {
                    NotePayNavHost(
                        startDestination = startDestination ?: Route.Home.path,
                        liquidGlassEnabled = glassEnabled,
                        pendingRoute = targetRoute,
                        onRouteHandled = viewModel::onRouteHandled,
                        onCompleteWelcome = viewModel::completeWelcome,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIncomingIntent(intent)
    }
}
