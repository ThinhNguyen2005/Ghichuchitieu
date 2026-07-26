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

data class BudgetSettings(
    /** Hạn mức chi tiêu tháng (VND * 100). 0 = chưa cài, bỏ qua Budget Alert. */
    val monthlyBudgetCents: Long = 0L,
)

/**
 * `name` là tên file DataStore trên máy. Đổi giá trị này sẽ làm mất hạn mức chi tiêu người
 * dùng đã cài, nên chỉ đổi khi app chưa phát hành. Lần đổi từ "notification_settings" sang
 * "budget_settings" thực hiện trong giai đoạn thử nghiệm, khi chưa có người dùng thật.
 */
private val Context.budgetSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "budget_settings",
)

@Singleton
class BudgetSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.applicationContext.budgetSettingsDataStore

    val settings: Flow<BudgetSettings> = dataStore.data.map { preferences ->
        BudgetSettings(
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
