package com.notepay

import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.ui.navigation.NotePayNavHost
import com.notepay.ui.theme.NotePayTheme
import com.notepay.ui.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var appSettings: AppSettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        ThemeManager.initialize(this)

        setContent {
            val appLanguage by appSettings.appLanguage
                .collectAsStateWithLifecycle("system")

            LaunchedEffect(appLanguage) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val localeManager = getSystemService(LocaleManager::class.java)
                    val localeList = when (appLanguage) {
                        "vi" -> LocaleList.forLanguageTags("vi")
                        "en" -> LocaleList.forLanguageTags("en")
                        else -> LocaleList.getEmptyLocaleList()
                    }
                    if (localeManager?.applicationLocales != localeList) {
                        localeManager?.applicationLocales = localeList
                    }
                } else if (appLanguage != "system") {
                    val targetLocale = Locale.forLanguageTag(appLanguage)
                    if (Locale.getDefault().language != targetLocale.language) {
                        Locale.setDefault(targetLocale)
                        @Suppress("DEPRECATION")
                        val config = resources.configuration
                        config.setLocale(targetLocale)
                        @Suppress("DEPRECATION")
                        resources.updateConfiguration(config, resources.displayMetrics)
                    }
                }
            }

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
