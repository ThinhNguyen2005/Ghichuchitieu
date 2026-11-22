package com.notepay.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.preferencesDataStore
import com.notepay.core.platform.OsCompatHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notepay_app_settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(context, "notepay_settings")
        )
    }
)

@Singleton
class AppSettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val dataStore = context.appSettingsDataStore

    companion object {
        val KEY_LIQUID_GLASS_ENABLED = booleanPreferencesKey("liquid_glass_enabled")
        val KEY_HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val KEY_DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val KEY_DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val KEY_DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")

        val KEY_BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
        val KEY_WEEKLY_DIGEST_ENABLED = booleanPreferencesKey("weekly_digest_enabled")
        val KEY_LAST_NOTIFIED_BUDGET_MONTH = stringPreferencesKey("last_notified_budget_month")
        val KEY_LAST_NOTIFIED_BUDGET_THRESHOLD = intPreferencesKey("last_notified_budget_threshold")

        val KEY_CURRENCY_CODE = stringPreferencesKey("currency_code")
        val KEY_CURRENCY_SYMBOL_POSITION = stringPreferencesKey("currency_symbol_position")
        val KEY_CURRENCY_THOUSAND_SEPARATOR = stringPreferencesKey("currency_thousand_separator")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_THEME_COLOR = stringPreferencesKey("theme_color")
        val KEY_HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        val KEY_HOME_SMART_TIPS_DISMISSED = booleanPreferencesKey("home_smart_tips_dismissed")
        val KEY_HOME_SMART_TIP_ROTATION = intPreferencesKey("home_smart_tip_rotation")
        val KEY_HOME_BACKGROUND_PRESET = stringPreferencesKey("home_background_preset")
        val KEY_HOME_BACKGROUND_IMAGE_PATH = stringPreferencesKey("home_background_image_path")
        val KEY_HOME_BACKGROUND_BLUR_ENABLED = booleanPreferencesKey("home_background_blur_enabled")
        val KEY_HOME_BALANCE_HIDDEN = booleanPreferencesKey("home_balance_hidden")
    }

    val hasSeenWelcome: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HAS_SEEN_WELCOME] ?: false
    }

    suspend fun setHasSeenWelcome(hasSeen: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HAS_SEEN_WELCOME] = hasSeen
        }
    }

    val budgetAlertsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_BUDGET_ALERTS_ENABLED] ?: true
    }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_BUDGET_ALERTS_ENABLED] = enabled
        }
    }

    val weeklyDigestEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_WEEKLY_DIGEST_ENABLED] ?: true
    }

    suspend fun setWeeklyDigestEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_WEEKLY_DIGEST_ENABLED] = enabled
        }
    }

    val lastNotifiedBudgetMonth: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_NOTIFIED_BUDGET_MONTH]
    }

    val lastNotifiedBudgetThreshold: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_NOTIFIED_BUDGET_THRESHOLD] ?: 0
    }

    suspend fun recordBudgetAlertNotification(monthKey: String, threshold: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_NOTIFIED_BUDGET_MONTH] = monthKey
            preferences[KEY_LAST_NOTIFIED_BUDGET_THRESHOLD] = threshold
        }
    }

    /**
     * Trạng thái bật/tắt hiệu ứng Liquid Glass.
     * Mặc định bật khi thiết bị hỗ trợ Liquid Glass trên thanh điều hướng.
     */
    val liquidGlassEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        val defaultVal = OsCompatHelper.supportsLiquidGlass()
        preferences[KEY_LIQUID_GLASS_ENABLED] ?: defaultVal
    }

    suspend fun setLiquidGlassEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_LIQUID_GLASS_ENABLED] = enabled
        }
    }

    val hapticFeedbackEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HAPTIC_FEEDBACK_ENABLED] ?: true
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    val dailyReminderEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_DAILY_REMINDER_ENABLED] ?: true
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_REMINDER_ENABLED] = enabled
        }
    }

    val reminderHour: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_DAILY_REMINDER_HOUR] ?: 20
    }

    val reminderMinute: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_DAILY_REMINDER_MINUTE] ?: 30
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_REMINDER_HOUR] = hour
            preferences[KEY_DAILY_REMINDER_MINUTE] = minute
        }
    }

    val currencyCode: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_CURRENCY_CODE] ?: "VND"
    }

    suspend fun setCurrencyCode(code: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CURRENCY_CODE] = code
        }
    }

    val currencySymbolPosition: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_CURRENCY_SYMBOL_POSITION] ?: "after"
    }

    suspend fun setCurrencySymbolPosition(position: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CURRENCY_SYMBOL_POSITION] = position
        }
    }

    val currencyThousandSeparator: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_CURRENCY_THOUSAND_SEPARATOR] ?: "dot"
    }

    suspend fun setCurrencyThousandSeparator(separator: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CURRENCY_THOUSAND_SEPARATOR] = separator
        }
    }

    val appLanguage: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_APP_LANGUAGE] ?: "system"
    }

    suspend fun setAppLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_LANGUAGE] = language
        }
    }

    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "system"
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    val themeColor: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_THEME_COLOR] ?: "sand"
    }

    suspend fun setThemeColor(color: String) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_COLOR] = color
        }
    }

    val homeSmartTipsDismissed: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HOME_SMART_TIPS_DISMISSED] ?: false
    }

    suspend fun setHomeSmartTipsDismissed(dismissed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HOME_SMART_TIPS_DISMISSED] = dismissed
        }
    }

    val homeSmartTipRotation: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_HOME_SMART_TIP_ROTATION] ?: 0
    }

    suspend fun incrementHomeSmartTipRotation() {
        dataStore.edit { preferences ->
            val current = preferences[KEY_HOME_SMART_TIP_ROTATION] ?: 0
            preferences[KEY_HOME_SMART_TIP_ROTATION] = current + 1
        }
    }

    val homeBackgroundPreset: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_HOME_BACKGROUND_PRESET] ?: "warm_silk"
    }

    suspend fun setHomeBackgroundPreset(presetId: String) {
        dataStore.edit { preferences ->
            preferences[KEY_HOME_BACKGROUND_PRESET] = presetId
        }
    }

    val homeBackgroundImagePath: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_HOME_BACKGROUND_IMAGE_PATH]
    }

    suspend fun setHomeBackgroundCustomImage(path: String?) {
        dataStore.edit { preferences ->
            if (path != null) {
                preferences[KEY_HOME_BACKGROUND_IMAGE_PATH] = path
                preferences[KEY_HOME_BACKGROUND_PRESET] = "custom"
            } else {
                preferences.remove(KEY_HOME_BACKGROUND_IMAGE_PATH)
                if (preferences[KEY_HOME_BACKGROUND_PRESET] == "custom") {
                    preferences[KEY_HOME_BACKGROUND_PRESET] = "warm_silk"
                }
            }
        }
    }

    val homeBackgroundBlurEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HOME_BACKGROUND_BLUR_ENABLED] ?: false
    }

    suspend fun setHomeBackgroundBlurEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HOME_BACKGROUND_BLUR_ENABLED] = enabled
        }
    }

    val homeBalanceHidden: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HOME_BALANCE_HIDDEN] ?: false
    }

    suspend fun setHomeBalanceHidden(hidden: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HOME_BALANCE_HIDDEN] = hidden
        }
    }
}
