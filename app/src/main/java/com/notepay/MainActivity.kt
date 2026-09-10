package com.notepay

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.data.preferences.AppSettingsDataStore
import javax.inject.Inject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.notepay.ui.navigation.NotePayNavHost
import com.notepay.ui.theme.NotePayTheme
import com.notepay.ui.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var appSettings: AppSettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        ThemeManager.initialize(this)

        setContent {
            NotePayTheme {
                val glassEnabled by appSettings.liquidGlassEnabled
                    .collectAsStateWithLifecycle(false)

                NotePayNavHost(
                    liquidGlassEnabled = glassEnabled
                )
            }
        }
    }


}
