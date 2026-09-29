package com.notepay.ui.feature.settings.appearance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.platform.LocaleHelper
import com.notepay.platform.OsCompatHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppearanceLanguageUiState(
    val themeMode: String = "system",
    val themeColor: String = "ledger",
    val liquidGlassEnabled: Boolean = false,
    val isLiquidGlassSupported: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val appLanguage: String = "system",
)

@HiltViewModel
class AppearanceLanguageViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    val uiState: StateFlow<AppearanceLanguageUiState> = combine(
        appSettingsDataStore.themeMode,
        appSettingsDataStore.themeColor,
        appSettingsDataStore.liquidGlassEnabled,
        appSettingsDataStore.hapticFeedbackEnabled,
        appSettingsDataStore.appLanguage,
    ) { mode, color, glassEnabled, hapticEnabled, lang ->
        AppearanceLanguageUiState(
            themeMode = mode,
            themeColor = color,
            liquidGlassEnabled = glassEnabled,
            isLiquidGlassSupported = OsCompatHelper.supportsLiquidGlass(),
            hapticFeedbackEnabled = hapticEnabled,
            appLanguage = lang,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppearanceLanguageUiState(
            isLiquidGlassSupported = OsCompatHelper.supportsLiquidGlass(),
        ),
    )

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            appSettingsDataStore.setThemeMode(mode)
        }
    }

    fun setThemeColor(color: String) {
        viewModelScope.launch {
            appSettingsDataStore.setThemeColor(color)
        }
    }

    fun setLiquidGlassEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setLiquidGlassEnabled(enabled)
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setHapticFeedbackEnabled(enabled)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            appSettingsDataStore.setAppLanguage(language)
            LocaleHelper.applyLocale(context, language)
        }
    }
}

