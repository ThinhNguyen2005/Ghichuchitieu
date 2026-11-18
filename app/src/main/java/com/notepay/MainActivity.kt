package com.notepay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.core.designsystem.adaptive.ProvideWindowLayout
import com.notepay.core.designsystem.theme.NotePayTheme
import com.notepay.core.navigation.MainViewModel
import com.notepay.core.navigation.NotePayNavHost
import com.notepay.core.navigation.Route
import com.notepay.core.navigation.rememberNotePayReducedMotion
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        viewModel.handleIncomingIntent(intent)

        splashScreen.setKeepOnScreenCondition {
            viewModel.startDestination.value == null || viewModel.initialAppearance.value == null
        }

        setContent {
            val appearance by viewModel.initialAppearance.collectAsStateWithLifecycle()
            val glassEnabled by viewModel.liquidGlassEnabled.collectAsStateWithLifecycle()
            val hapticEnabled by viewModel.hapticFeedbackEnabled.collectAsStateWithLifecycle()
            val targetRoute by viewModel.pendingRoute.collectAsStateWithLifecycle()
            val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
            val authUser by viewModel.authUser.collectAsStateWithLifecycle()

            if (startDestination != null && appearance != null) {
                val settings = appearance ?: return@setContent
                val systemDark = isSystemInDarkTheme()
                val darkTheme = when (settings.themeMode) {
                    "light" -> false
                    "dark" -> true
                    else -> systemDark
                }
                val reduceMotion = rememberNotePayReducedMotion()

                DisposableEffect(darkTheme) {
                    val barStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    }
                    enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                    onDispose { }
                }

                ProvideWindowLayout {
                    NotePayTheme(
                        themeMode = settings.themeMode,
                        themeColor = settings.themeColor,
                        appLanguage = settings.appLanguage,
                        reduceMotion = reduceMotion,
                    ) {
                        NotePayNavHost(
                            authUser = authUser,
                            startDestination = startDestination ?: Route.Home.path,
                            liquidGlassEnabled = glassEnabled,
                            hapticFeedbackEnabled = hapticEnabled,
                            pendingRoute = targetRoute,
                            onRouteHandled = viewModel::onRouteHandled,
                            onCompleteWelcome = viewModel::completeWelcome,
                        )
                    }
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
