package com.notepay.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `name` là tên file DataStore trên máy. Trước đây phản hồi lời khuyên được lưu trực tiếp bằng
 * SharedPreferences ("notepay_ai_feedback") ngay trong StatsViewModel; chuyển sang DataStore để
 * ViewModel không còn chạm tầng lưu trữ. Đổi giá trị này sẽ làm mất lịch sử bình chọn đã có,
 * chỉ thực hiện khi app chưa phát hành.
 */
private val Context.advisorFeedbackDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "advisor_feedback",
)

/**
 * Lưu điểm phản hồi của người dùng cho từng lời khuyên tài chính (id -> 1 thích / -1 không thích),
 * dùng làm tín hiệu xếp hạng lại gợi ý trong [com.notepay.domain.analytics.StatsInsightsCalculator].
 */
@Singleton
class AdvisorFeedbackStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.applicationContext.advisorFeedbackDataStore

    val feedbacks: Flow<Map<String, Int>> = dataStore.data.map { preferences ->
        buildMap {
            for ((key, value) in preferences.asMap()) {
                if (value is Int) put(key.name, value)
            }
        }
    }

    suspend fun setFeedback(adviceId: String, score: Int) {
        dataStore.edit { preferences ->
            preferences[intPreferencesKey(adviceId)] = score
        }
    }
}
