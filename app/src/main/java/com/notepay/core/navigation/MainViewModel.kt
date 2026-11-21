package com.notepay.core.navigation

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.core.designsystem.formatter.MoneyFormatConfig
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.platform.LocaleHelper
import com.notepay.core.platform.widget.WidgetConstants
import com.notepay.data.datastore.AppSettingsDataStore
import com.notepay.data.remote.CurrencyRateRepository
import com.notepay.domain.model.AuthUser
import com.notepay.domain.model.CurrencyConverter
import com.notepay.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cấp Activity cho [com.notepay.MainActivity].
 *
 * Đảm bảo:
 * 1. Lưu trữ và bảo toàn [pendingRoute] trước các biến cố vòng đời (xoay màn hình, recreate).
 * 2. Chờ cấu hình appearance từ DataStore trước khi rời splash screen.
 *    từ AppSettingsDataStore (Single Source of Truth), loại bỏ hoàn toàn SharedPreferences tĩnh.
 * 3. Xác định [startDestination] không chớp nháy (no-flicker) bằng cách đồng bộ với SplashScreen.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
    private val authRepository: AuthRepository,
    private val currencyRateRepository: CurrencyRateRepository,
) : ViewModel() {

    private val _ratesSnapshot = MutableStateFlow(currencyRateRepository.offlineFallbackSnapshot())

    data class InitialAppearance(
        val themeMode: String,
        val themeColor: String,
        val appLanguage: String,
    )

    val initialAppearance: StateFlow<InitialAppearance?> = combine(
        appSettingsDataStore.themeMode,
        appSettingsDataStore.themeColor,
        appSettingsDataStore.appLanguage,
    ) { mode, color, language ->
        InitialAppearance(
            themeMode = mode,
            themeColor = color,
            appLanguage = LocaleHelper.normalizeLanguageCode(language),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    private val authStateFlow = authRepository.observeAuthState().shareIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        replay = 1,
    )

    val startDestination: StateFlow<String?> = combine(
        appSettingsDataStore.hasSeenWelcome,
        authStateFlow,
    ) { hasSeenWelcome, user ->
        if (!hasSeenWelcome && user == null) {
            Route.Welcome.path
        } else {
            Route.Home.path
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val authUser: StateFlow<AuthUser?> = authStateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun completeWelcome() {
        viewModelScope.launch {
            appSettingsDataStore.setHasSeenWelcome(true)
        }
    }

    val liquidGlassEnabled: StateFlow<Boolean> = appSettingsDataStore.liquidGlassEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    val hapticFeedbackEnabled: StateFlow<Boolean> = appSettingsDataStore.hapticFeedbackEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            currencyRateRepository.readCachedSnapshot()?.let { cached ->
                _ratesSnapshot.value = cached
            }
            _ratesSnapshot.value = currencyRateRepository.getVndRatesOrFallback()
        }
        viewModelScope.launch {
            combine(
                appSettingsDataStore.currencyCode,
                appSettingsDataStore.currencySymbolPosition,
                appSettingsDataStore.currencyThousandSeparator,
                _ratesSnapshot,
            ) { code, position, separator, snapshot ->
                MoneyFormatConfig(
                    currencyCode = code,
                    symbolPosition = position,
                    thousandSeparator = separator,
                    ratePerVnd = CurrencyConverter.ratePerVnd(code, snapshot.rates),
                )
            }.collect { cfg ->
                MoneyFormatter.updateConfig(
                    currencyCode = cfg.currencyCode,
                    symbolPosition = cfg.symbolPosition,
                    thousandSeparator = cfg.thousandSeparator,
                    ratePerVnd = cfg.ratePerVnd,
                )
            }
        }
    }

    fun handleIncomingIntent(intent: Intent?) {
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

    fun onRouteHandled() {
        _pendingRoute.value = null
    }
}
