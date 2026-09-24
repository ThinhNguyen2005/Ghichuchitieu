package com.notepay.ui.feature.settings.appearance

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.platform.OsCompatHelper
import com.notepay.ui.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class AppearanceLanguageUiState(
    val themeMode: String = "system",
    val themeColor: String = "ios",
    val liquidGlassEnabled: Boolean = false,
    val isLiquidGlassSupported: Boolean = true,
    val appLanguage: String = "system",
)

@HiltViewModel
class AppearanceLanguageViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
) : ViewModel() {

    val uiState: StateFlow<AppearanceLanguageUiState> = combine(
        appSettingsDataStore.liquidGlassEnabled,
        appSettingsDataStore.appLanguage,
    ) { glassEnabled, lang ->
        AppearanceLanguageUiState(
            themeMode = ThemeManager.themeMode,
            themeColor = ThemeManager.currentThemeColor,
            liquidGlassEnabled = glassEnabled,
            isLiquidGlassSupported = OsCompatHelper.supportsLiquidGlass(),
            appLanguage = lang,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppearanceLanguageUiState(
            themeMode = ThemeManager.themeMode,
            themeColor = ThemeManager.currentThemeColor,
            isLiquidGlassSupported = OsCompatHelper.supportsLiquidGlass(),
        ),
    )

    fun setThemeMode(context: Context, mode: String) {
        ThemeManager.updateThemeMode(context, mode)
    }

    fun setThemeColor(context: Context, color: String) {
        ThemeManager.updateThemeColor(context, color)
    }

    fun setLiquidGlassEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setLiquidGlassEnabled(enabled)
        }
    }

    fun setLanguage(context: Context, language: String) {
        viewModelScope.launch {
            appSettingsDataStore.setAppLanguage(language)
            applyLocale(context, language)
        }
    }

    private fun applyLocale(context: Context, language: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val localeList = when (language) {
                "vi" -> LocaleList.forLanguageTags("vi")
                "en" -> LocaleList.forLanguageTags("en")
                else -> LocaleList.getEmptyLocaleList()
            }
            localeManager?.applicationLocales = localeList
        } else {
            val locale = when (language) {
                "vi" -> Locale.forLanguageTag("vi")
                "en" -> Locale.forLanguageTag("en")
                else -> Locale.getDefault()
            }
            Locale.setDefault(locale)
            @Suppress("DEPRECATION")
            val config = context.resources.configuration
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}
