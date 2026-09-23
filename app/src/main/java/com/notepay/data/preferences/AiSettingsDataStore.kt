package com.notepay.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "notepay_ai_settings")

@Singleton
class AiSettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val dataStore = context.aiSettingsDataStore

    companion object {
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_CLOUD_AI_ENABLED = booleanPreferencesKey("cloud_ai_enabled")
        val KEY_CLOUD_MODEL_NAME = stringPreferencesKey("cloud_model_name")
        val KEY_SMART_RECEIPT_AI_ENABLED = booleanPreferencesKey("smart_receipt_ai_enabled")

        const val DEFAULT_MODEL = "gemini-3.8-flash"

        val AVAILABLE_MODELS = listOf(
            "gemini-3.8-flash",
            "gemini-3.6-flash",
            "gemini-3.1-flash-lite",
            "gemini-3-flash-preview",
        )
    }

    val geminiApiKey: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_GEMINI_API_KEY]?.takeIf { it.isNotBlank() }
    }

    val cloudAiEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_CLOUD_AI_ENABLED] ?: true
    }

    val cloudModelName: Flow<String> = dataStore.data.map { preferences ->
        val saved = preferences[KEY_CLOUD_MODEL_NAME]?.trim()
        if (saved.isNullOrBlank() || !AVAILABLE_MODELS.contains(saved)) {
            DEFAULT_MODEL
        } else {
            saved
        }
    }

    val smartReceiptAiEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SMART_RECEIPT_AI_ENABLED] ?: true
    }

    suspend fun setGeminiApiKey(apiKey: String?) {
        dataStore.edit { preferences ->
            val trimmed = apiKey?.trim()
            if (trimmed.isNullOrBlank()) {
                preferences.remove(KEY_GEMINI_API_KEY)
            } else {
                preferences[KEY_GEMINI_API_KEY] = trimmed
            }
        }
    }

    suspend fun setCloudAiEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_CLOUD_AI_ENABLED] = enabled
        }
    }

    suspend fun setCloudModelName(model: String) {
        dataStore.edit { preferences ->
            val trimmed = model.trim()
            preferences[KEY_CLOUD_MODEL_NAME] = if (trimmed.isBlank() || !AVAILABLE_MODELS.contains(trimmed)) {
                DEFAULT_MODEL
            } else {
                trimmed
            }
        }
    }

    suspend fun setSmartReceiptAiEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SMART_RECEIPT_AI_ENABLED] = enabled
        }
    }
}
