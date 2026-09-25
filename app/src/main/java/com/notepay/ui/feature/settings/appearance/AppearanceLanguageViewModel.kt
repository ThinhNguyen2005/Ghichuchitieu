package com.notepay.ui.feature.settings.appearance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.platform.LocaleHelper
import com.notepay.platform.OsCompatHelper
import com.notepay.ui.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
            LocaleHelper.applyLocale(context, language)
        }
    }
}
