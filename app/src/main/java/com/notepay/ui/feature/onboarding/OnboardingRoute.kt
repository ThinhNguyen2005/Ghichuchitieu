package com.notepay.ui.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Route stateful: nối [OnboardingViewModel] với [OnboardingScreen] stateless.
 * Điều hướng chỉ xảy ra một lần khi ViewModel báo đã lưu xong toàn bộ dữ liệu.
 */
@Composable
fun OnboardingRoute(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) onComplete()
    }

    OnboardingScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}
