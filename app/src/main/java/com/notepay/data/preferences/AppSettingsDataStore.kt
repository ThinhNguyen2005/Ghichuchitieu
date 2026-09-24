package com.notepay.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.notepay.platform.OsCompatHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "notepay_app_settings")

@Singleton
class AppSettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val dataStore = context.appSettingsDataStore

    companion object {
        val KEY_LIQUID_GLASS_ENABLED = booleanPreferencesKey("liquid_glass_enabled")
        val KEY_DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val KEY_DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val KEY_DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")

        val KEY_CURRENCY_CODE = stringPreferencesKey("currency_code")
        val KEY_CURRENCY_SYMBOL_POSITION = stringPreferencesKey("currency_symbol_position")
        val KEY_CURRENCY_THOUSAND_SEPARATOR = stringPreferencesKey("currency_thousand_separator")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")

        fun walletBackgroundKey(walletId: Long) = stringPreferencesKey("wallet_bg_$walletId")
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

    fun observeWalletBackground(walletId: Long): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[walletBackgroundKey(walletId)]
        }
    }

    suspend fun setWalletBackground(walletId: Long, uriString: String?) {
        dataStore.edit { preferences ->
            val key = walletBackgroundKey(walletId)
            if (uriString == null) {
                preferences.remove(key)
            } else {
                preferences[key] = uriString
            }
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
}
