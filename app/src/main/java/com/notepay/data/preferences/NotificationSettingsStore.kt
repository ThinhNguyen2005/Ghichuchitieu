package com.notepay.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationSettings(
    /** Hạn mức chi tiêu tháng (VND * 100). 0 = chưa cài, bỏ qua Budget Alert. */
    val monthlyBudgetCents: Long = 0L,
)

private val Context.notificationSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_settings",
)

@Singleton
class NotificationSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.applicationContext.notificationSettingsDataStore

    val settings: Flow<NotificationSettings> = dataStore.data.map { preferences ->
        NotificationSettings(
            monthlyBudgetCents = preferences[Keys.MONTHLY_BUDGET_CENTS] ?: 0L,
        )
    }

    suspend fun setMonthlyBudget(amountCents: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.MONTHLY_BUDGET_CENTS] = amountCents
        }
    }

    private object Keys {
        val MONTHLY_BUDGET_CENTS = longPreferencesKey("monthly_budget_cents")
    }
}
