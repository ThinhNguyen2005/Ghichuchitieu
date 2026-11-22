package com.notepay.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.platform.LocaleHelper
import com.notepay.data.datastore.AppSettingsDataStore
import com.notepay.data.remote.CurrencyRateRepository
import com.notepay.domain.model.CurrencyConverter
import com.notepay.domain.model.Wallet
import com.notepay.domain.repository.WalletRepository
import com.notepay.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Onboarding 3 bước: tiền tệ & ngôn ngữ → ví → nhắc nhở.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val currencyRateRepository: CurrencyRateRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        hydrate()
    }

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.SelectCurrency -> selectCurrency(action.code)
            is OnboardingAction.SelectLanguage -> selectLanguage(action.code)
            is OnboardingAction.UpdateWalletName -> updateWalletName(action.name)
            is OnboardingAction.SelectWalletIcon ->
                _uiState.update { it.copy(walletIconKey = action.iconKey) }
            is OnboardingAction.ToggleReminder ->
                _uiState.update {
                    it.copy(reminderEnabled = action.enabled, notificationDenied = false)
                }
            is OnboardingAction.UpdateReminderTime -> _uiState.update {
                it.copy(reminderHour = action.hour, reminderMinute = action.minute)
            }
            OnboardingAction.ReportNotificationDenied -> _uiState.update {
                it.copy(reminderEnabled = false, notificationDenied = true)
            }
            is OnboardingAction.NavigateToStep ->
                _uiState.update { it.copy(currentStep = action.step) }
            OnboardingAction.Complete -> complete()
            OnboardingAction.Skip -> complete()
        }
    }

    private fun hydrate() {
        viewModelScope.launch {
            try {
                val savedCurrency = appSettingsDataStore.currencyCode.first()
                val savedLanguage = appSettingsDataStore.appLanguage.first()
                val savedReminderEnabled = appSettingsDataStore.dailyReminderEnabled.first()
                val savedHour = appSettingsDataStore.reminderHour.first()
                val savedMinute = appSettingsDataStore.reminderMinute.first()
                val wallet = walletRepository.observeActive().first()

                _uiState.update { state ->
                    state.copy(
                        currencyCode = savedCurrency,
                        languageCode = LocaleHelper.normalizeLanguageCode(savedLanguage),
                        reminderEnabled = savedReminderEnabled,
                        reminderHour = savedHour,
                        reminderMinute = savedMinute,
                        walletName = wallet?.name ?: Wallet.default().name,
                        walletIconKey = wallet?.iconKey ?: Wallet.ICON_CASH,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(walletName = Wallet.default().name) }
            }
        }
    }

    private fun selectCurrency(code: String) {
        _uiState.update { it.copy(currencyCode = code) }
    }

    /** Áp ngôn ngữ ngay: ghi DataStore rồi đổi locale để bước sau hiển thị đúng. */
    private fun selectLanguage(code: String) {
        _uiState.update { it.copy(languageCode = code) }
        viewModelScope.launch {
            try {
                val normalized = LocaleHelper.normalizeLanguageCode(code)
                appSettingsDataStore.setAppLanguage(normalized)
                LocaleHelper.applyLocale(context, normalized)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Không chặn onboarding nếu đổi ngôn ngữ thất bại.
            }
        }
    }

    private fun updateWalletName(name: String) {
        _uiState.update { it.copy(walletName = name.take(Wallet.MAX_NAME_LENGTH)) }
    }

    private fun complete() {
        val snapshot = _uiState.value
        if (snapshot.isSaving || snapshot.walletName.isBlank()) return

        _uiState.update { it.copy(isSaving = true, saveError = false) }

        viewModelScope.launch {
            try {
                val seeded = withTimeoutOrNull(1_000) {
                    walletRepository.observeActive().first { it != null }
                }

                val existing = seeded ?: walletRepository.observeActive().first()
                val target = (existing ?: Wallet.default()).copy(
                    name = snapshot.walletName.trim(),
                    iconKey = snapshot.walletIconKey,
                    isActive = true,
                )
                val walletId = walletRepository.upsert(target)
                walletRepository.setActive(if (target.id != 0L) target.id else walletId)

                appSettingsDataStore.setCurrencyCode(snapshot.currencyCode)
                val ratesSnapshot = withContext(Dispatchers.IO) {
                    currencyRateRepository.readCachedSnapshot()
                        ?: currencyRateRepository.offlineFallbackSnapshot()
                }
                MoneyFormatter.updateConfig(
                    currencyCode = snapshot.currencyCode,
                    ratePerVnd = CurrencyConverter.ratePerVnd(snapshot.currencyCode, ratesSnapshot.rates),
                )

                appSettingsDataStore.setDailyReminderEnabled(snapshot.reminderEnabled)
                appSettingsDataStore.setReminderTime(snapshot.reminderHour, snapshot.reminderMinute)
                if (snapshot.reminderEnabled) {
                    ReminderScheduler.scheduleDailyReminder(
                        context,
                        targetHour = snapshot.reminderHour,
                        targetMinute = snapshot.reminderMinute,
                    )
                } else {
                    ReminderScheduler.cancelDailyReminder(context)
                }

                appSettingsDataStore.setHasSeenWelcome(true)
                _uiState.update { it.copy(isSaving = false, isCompleted = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = true) }
            }
        }
    }
}
