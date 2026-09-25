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
import com.notepay.ui.theme.NotePayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        viewModel.handleIncomingIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
            val glassEnabled by viewModel.liquidGlassEnabled.collectAsStateWithLifecycle()
            val targetRoute by viewModel.pendingRoute.collectAsStateWithLifecycle()

            NotePayTheme(
                themeMode = themeMode,
                themeColor = themeColor,
            ) {
                NotePayNavHost(
                    liquidGlassEnabled = glassEnabled,
                    pendingRoute = targetRoute,
                    onRouteHandled = viewModel::onRouteHandled,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIncomingIntent(intent)
    }
}
