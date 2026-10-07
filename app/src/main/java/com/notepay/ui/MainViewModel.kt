package com.notepay.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.domain.model.AuthUser
import com.notepay.domain.repository.AuthRepository
import com.notepay.platform.widget.WidgetConstants
import com.notepay.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cấp Activity cho [com.notepay.MainActivity].
 *
 * Đảm bảo:
 * 1. Lưu trữ và bảo toàn [pendingRoute] trước các biến cố vòng đời (xoay màn hình, recreate).
 * 2. Cung cấp trạng thái reactive duy nhất cho Theme (themeMode, themeColor, liquidGlassEnabled)
 *    từ AppSettingsDataStore (Single Source of Truth), loại bỏ hoàn toàn SharedPreferences tĩnh.
 * 3. Xác định [startDestination] không chớp nháy (no-flicker) bằng cách đồng bộ với SplashScreen.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    val startDestination: StateFlow<String?> = combine(
        appSettingsDataStore.hasSeenWelcome,
        authRepository.observeAuthState()
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

    val authUser: StateFlow<AuthUser?> = authRepository.observeAuthState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = authRepository.currentUser,
    )

    fun completeWelcome() {
        viewModelScope.launch {
            appSettingsDataStore.setHasSeenWelcome(true)
        }
    }

    val themeMode: StateFlow<String> = appSettingsDataStore.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "system",
    )

    val themeColor: StateFlow<String> = appSettingsDataStore.themeColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "ios",
    )

    val liquidGlassEnabled: StateFlow<Boolean> = appSettingsDataStore.liquidGlassEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

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
