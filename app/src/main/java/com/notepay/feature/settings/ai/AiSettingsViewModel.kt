package com.notepay.ui.feature.settings.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.ai.CloudGeminiAdvisor
import com.notepay.ai.GeminiNanoBudgetAdvisor
import com.notepay.data.preferences.AiSettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ApiKeyTestState {
    data object Idle : ApiKeyTestState
    data object Testing : ApiKeyTestState
    data class Success(val message: String) : ApiKeyTestState
    data class Failure(val error: String) : ApiKeyTestState
}

data class AiSettingsUiState(
    val apiKey: String? = null,
    val cloudAiEnabled: Boolean = true,
    val cloudModelName: String = AiSettingsDataStore.DEFAULT_MODEL,
    val smartReceiptAiEnabled: Boolean = true,
    val isGeminiNanoAvailable: Boolean = false,
)

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val aiSettingsDataStore: AiSettingsDataStore,
    private val cloudGeminiAdvisor: CloudGeminiAdvisor,
    private val geminiNanoAdvisor: GeminiNanoBudgetAdvisor,
) : ViewModel() {

    private val _testState = MutableStateFlow<ApiKeyTestState>(ApiKeyTestState.Idle)
    val testState: StateFlow<ApiKeyTestState> = _testState.asStateFlow()

    private val isGeminiNanoAvailableFlow = flow {
        emit(geminiNanoAdvisor.isGeminiNanoAvailable())
    }

    val uiState: StateFlow<AiSettingsUiState> = combine(
        aiSettingsDataStore.geminiApiKey,
        aiSettingsDataStore.cloudAiEnabled,
        aiSettingsDataStore.cloudModelName,
        aiSettingsDataStore.smartReceiptAiEnabled,
        isGeminiNanoAvailableFlow,
    ) { key, cloudEnabled, model, smartReceipt, nanoAvailable ->
        AiSettingsUiState(
            apiKey = key,
            cloudAiEnabled = cloudEnabled,
            cloudModelName = model,
            smartReceiptAiEnabled = smartReceipt,
            isGeminiNanoAvailable = nanoAvailable,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AiSettingsUiState(),
    )

    fun setApiKey(key: String?) {
        viewModelScope.launch {
            aiSettingsDataStore.setGeminiApiKey(key)
            _testState.value = ApiKeyTestState.Idle
        }
    }

    fun setCloudAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            aiSettingsDataStore.setCloudAiEnabled(enabled)
        }
    }

    fun setCloudModelName(model: String) {
        viewModelScope.launch {
            aiSettingsDataStore.setCloudModelName(model)
        }
    }

    fun setSmartReceiptAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            aiSettingsDataStore.setSmartReceiptAiEnabled(enabled)
        }
    }

    fun testApiKey(key: String) {
        if (key.isBlank()) {
            _testState.value = ApiKeyTestState.Failure("empty")
            return
        }
        viewModelScope.launch {
            _testState.value = ApiKeyTestState.Testing
            val result = cloudGeminiAdvisor.testConnection(key)
            result.fold(
                onSuccess = { msg ->
                    _testState.value = ApiKeyTestState.Success(msg)
                },
                onFailure = { err ->
                    _testState.value = ApiKeyTestState.Failure(err.message ?: "")
                }
            )
        }
    }

    fun resetTestState() {
        _testState.value = ApiKeyTestState.Idle
    }
}
