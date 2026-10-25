package com.notepay.ui.feature.settings.notification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Trạng thái + hành động của màn Cài đặt thông báo.
 *
 * Trước đây các flow này nằm trong `HomeViewModel` dù trang Home không dùng tới, khiến
 * màn Cài đặt phụ thuộc vào ViewModel của tab Home. Chuyển về đúng màn sở hữu giúp
 * mỗi ViewModel chỉ còn một lý do để thay đổi.
 *
 * [context] được inject dưới dạng Application Context để lên lịch WorkManager mà
 * không giữ tham chiếu Activity.
 */
@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    val dailyReminderEnabled = appSettingsDataStore.dailyReminderEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    val budgetAlertsEnabled = appSettingsDataStore.budgetAlertsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    val weeklyDigestEnabled = appSettingsDataStore.weeklyDigestEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    val reminderHour = appSettingsDataStore.reminderHour.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 20,
    )

    val reminderMinute = appSettingsDataStore.reminderMinute.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 30,
    )

    fun setDailyReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setDailyReminderEnabled(enabled)
            if (enabled) {
                ReminderScheduler.scheduleDailyReminder(
                    context,
                    targetHour = reminderHour.value,
                    targetMinute = reminderMinute.value,
                )
            } else {
                ReminderScheduler.cancelDailyReminder(context)
            }
        }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setBudgetAlertsEnabled(enabled)
        }
    }

    fun setWeeklyDigestEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setWeeklyDigestEnabled(enabled)
            if (enabled) {
                ReminderScheduler.scheduleWeeklyDigest(context)
            } else {
                ReminderScheduler.cancelWeeklyDigest(context)
            }
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            appSettingsDataStore.setReminderTime(hour, minute)
            if (dailyReminderEnabled.value) {
                ReminderScheduler.scheduleDailyReminder(context, hour, minute)
            }
        }
    }
}
