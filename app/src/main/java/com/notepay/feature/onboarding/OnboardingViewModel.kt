package com.notepay.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.domain.model.Wallet
import com.notepay.domain.repository.WalletRepository
import com.notepay.core.platform.LocaleHelper
import com.notepay.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Onboarding 3 bước: tiền tệ & ngôn ngữ → ví → nhắc nhở.
 *
 * Lưu ý thiết kế:
 * - **Ngôn ngữ áp ngay ở bước 1** (không đợi `Complete`), để bước 2/3 hiển thị đúng ngôn ngữ
 *   và giá trị đã vào DataStore trước khi hoàn tất.
 * - **Ví seed bất đồng bộ**: `SeedCallback` insert ví mặc định trong `onCreate` của Room bằng
 *   coroutine riêng, nên lần đọc đầu có thể thấy `null`. [complete] chờ tối đa 1s rồi fallback
 *   sang [Wallet.default] để tránh tạo ví thứ hai hoặc NPE.
 * - `Complete` là cửa duy nhất ghi dữ liệu: tiền tệ → ví → nhắc nhở → cờ hoàn tất.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val appSettingsDataStore: AppSettingsDataStore,
    @param:ApplicationContext private val context: Context
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
                        walletIconKey = wallet?.iconKey ?: Wallet.ICON_CASH
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // DataStore/Room chưa sẵn sàng — vẫn phải có tên ví để bước 2 không bị khoá.
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
                appSettingsDataStore.setAppLanguage(code)
                LocaleHelper.applyLocale(context, code)
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
                // ① Chờ SeedCallback commit ví mặc định (Room tạo DB bất đồng bộ).
                val seeded = withTimeoutOrNull(1_000) {
                    walletRepository.observeActive().first { it != null }
                }

                // ② Tái dùng ví đã có; chỉ tạo mới khi chắc chắn chưa có ví nào.
                val existing = seeded ?: walletRepository.observeActive().first()
                val target = (existing ?: Wallet.default()).copy(
                    name = snapshot.walletName.trim(),
                    iconKey = snapshot.walletIconKey,
                    isActive = true
                )
                val walletId = walletRepository.upsert(target)
                walletRepository.setActive(if (target.id != 0L) target.id else walletId)

                // ③ Tiền tệ (ngôn ngữ đã lưu ở bước 1).
                appSettingsDataStore.setCurrencyCode(snapshot.currencyCode)

                // ④ Nhắc nhở.
                appSettingsDataStore.setDailyReminderEnabled(snapshot.reminderEnabled)
                appSettingsDataStore.setReminderTime(snapshot.reminderHour, snapshot.reminderMinute)
                if (snapshot.reminderEnabled) {
                    ReminderScheduler.scheduleDailyReminder(
                        context,
                        targetHour = snapshot.reminderHour,
                        targetMinute = snapshot.reminderMinute
                    )
                } else {
                    ReminderScheduler.cancelDailyReminder(context)
                }

                // ⑤ Cờ hoàn tất — ghi cuối cùng để state luôn nhất quán.
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
