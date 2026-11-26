package com.notepay.feature.settings.appearance

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.core.platform.LocaleHelper
import com.notepay.core.platform.OsCompatHelper
import com.notepay.data.datastore.AppSettingsDataStore
import com.notepay.data.local.HomeBackgroundStorage
import com.notepay.feature.home.HomeBackgroundPreset
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
    val themeColor: String = "sand",
    val liquidGlassEnabled: Boolean = false,
    val isLiquidGlassSupported: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val appLanguage: String = "system",
    val backgroundPreset: HomeBackgroundPreset = HomeBackgroundPreset.WARM_SILK,
    val customBackgroundPath: String? = null,
    val backgroundBlurEnabled: Boolean = false,
    val isSmartTipsDismissed: Boolean = false,
)

private data class HomeCustomizationPrefs(
    val backgroundPreset: HomeBackgroundPreset,
    val customBackgroundPath: String?,
    val backgroundBlurEnabled: Boolean,
    val isSmartTipsDismissed: Boolean,
)

@HiltViewModel
class AppearanceLanguageViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val basePrefsFlow = combine(
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
            appLanguage = LocaleHelper.normalizeLanguageCode(lang),
        )
    }

    private val homePrefsFlow = combine(
        appSettingsDataStore.homeBackgroundPreset,
        appSettingsDataStore.homeBackgroundImagePath,
        appSettingsDataStore.homeBackgroundBlurEnabled,
        appSettingsDataStore.homeSmartTipsDismissed,
    ) { presetId, imagePath, blurEnabled, smartTipsDismissed ->
        val rawPreset = HomeBackgroundPreset.fromId(presetId)
        val resolvedPreset = if (rawPreset == HomeBackgroundPreset.CUSTOM && imagePath.isNullOrBlank()) {
            HomeBackgroundPreset.WARM_SILK
        } else {
            rawPreset
        }
        HomeCustomizationPrefs(
            backgroundPreset = resolvedPreset,
            customBackgroundPath = imagePath,
            backgroundBlurEnabled = blurEnabled,
            isSmartTipsDismissed = smartTipsDismissed,
        )
    }

    val uiState: StateFlow<AppearanceLanguageUiState> = combine(
        basePrefsFlow,
        homePrefsFlow,
    ) { base, home ->
        base.copy(
            backgroundPreset = home.backgroundPreset,
            customBackgroundPath = home.customBackgroundPath,
            backgroundBlurEnabled = home.backgroundBlurEnabled,
            isSmartTipsDismissed = home.isSmartTipsDismissed,
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
            val normalized = LocaleHelper.normalizeLanguageCode(language)
            appSettingsDataStore.setAppLanguage(normalized)
            LocaleHelper.applyLocale(context, normalized)
        }
    }

    fun selectBackgroundPreset(preset: HomeBackgroundPreset) {
        viewModelScope.launch {
            appSettingsDataStore.setHomeBackgroundPreset(preset.id)
        }
    }

    fun setBackgroundBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setHomeBackgroundBlurEnabled(enabled)
        }
    }

    fun setSmartInsightsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setHomeSmartTipsDismissed(!enabled)
        }
    }

    fun applyCustomBackground(
        uri: Uri,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            val previousPath = uiState.value.customBackgroundPath
            val saved = HomeBackgroundStorage.saveFromUri(context, uri)
            saved.onSuccess { newPath ->
                appSettingsDataStore.setHomeBackgroundCustomImage(newPath)
                if (previousPath != null && previousPath != newPath) {
                    HomeBackgroundStorage.deleteCustomBackground(previousPath)
                }
                onResult(true)
            }.onFailure {
                onResult(false)
            }
        }
    }

    fun clearCustomBackground() {
        viewModelScope.launch {
            val previousPath = uiState.value.customBackgroundPath
            appSettingsDataStore.setHomeBackgroundCustomImage(null)
            HomeBackgroundStorage.deleteCustomBackground(previousPath)
        }
    }
}

