package com.notepay.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.feature.onboarding.components.OnboardingPrimaryButton
import com.notepay.feature.onboarding.components.ProgressDotsIndicator
import com.notepay.feature.onboarding.steps.Step1CurrencyScreen
import com.notepay.feature.onboarding.steps.Step2WalletScreen
import com.notepay.feature.onboarding.steps.Step3ReminderScreen
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.NotePayTheme

/**
 * Onboarding stateless — nhận [uiState], phát [onAction].
 * Pager chỉ là cơ chế render; ViewModel là nguồn sự thật cho bước hiện tại.
 */
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val haptic = LocalHapticFeedback.current
    val currentStep = uiState.currentStep

    LaunchedEffect(currentStep) {
        val target = currentStep.stepIndex
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    // Phím Back hệ thống lùi bước thay vì thoát app (trừ bước đầu).
    BackHandler(enabled = currentStep.previousStep != null) {
        currentStep.previousStep?.let { onAction(OnboardingAction.NavigateToStep(it)) }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            OnboardingHeader(
                showBack = currentStep.previousStep != null,
                onBack = {
                    currentStep.previousStep?.let { onAction(OnboardingAction.NavigateToStep(it)) }
                },
                onSkip = { onAction(OnboardingAction.Skip) }
            )
        },
        bottomBar = {
            OnboardingFooter(
                currentStep = currentStep,
                enabled = uiState.canContinue && !uiState.isSaving,
                onContinue = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val next = currentStep.nextStep
                    if (next != null) {
                        onAction(OnboardingAction.NavigateToStep(next))
                    } else {
                        onAction(OnboardingAction.Complete)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = AppTheme.dimensions.spaceLarge,
                        vertical = AppTheme.dimensions.spaceMedium
                    )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> Step1CurrencyScreen(
                    uiState = uiState,
                    onSelectCurrency = { onAction(OnboardingAction.SelectCurrency(it)) },
                    onSelectLanguage = { onAction(OnboardingAction.SelectLanguage(it)) }
                )
                1 -> Step2WalletScreen(
                    uiState = uiState,
                    onNameChange = { onAction(OnboardingAction.UpdateWalletName(it)) },
                    onSelectIcon = { onAction(OnboardingAction.SelectWalletIcon(it)) }
                )
                2 -> Step3ReminderScreen(
                    uiState = uiState,
                    onToggleReminder = { onAction(OnboardingAction.ToggleReminder(it)) },
                    onSelectTime = { hour, minute ->
                        onAction(OnboardingAction.UpdateReminderTime(hour, minute))
                    },
                    onNotificationDenied = { onAction(OnboardingAction.ReportNotificationDenied) }
                )
            }
        }
    }
}

@Composable
private fun OnboardingHeader(
    showBack: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = AppTheme.dimensions.spaceSmall)
    ) {
        if (showBack) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = AppTheme.colors.textPrimary
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }

        TextButton(onClick = onSkip) {
            Text(
                text = stringResource(R.string.onboarding_action_skip),
                style = MaterialTheme.typography.labelLarge,
                color = AppTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun OnboardingFooter(
    currentStep: OnboardingStep,
    enabled: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceMedium),
        modifier = modifier
    ) {
        ProgressDotsIndicator(currentStep = currentStep)

        OnboardingPrimaryButton(
            text = if (currentStep == OnboardingStep.REMINDER) {
                stringResource(R.string.onboarding_action_start)
            } else {
                stringResource(R.string.onboarding_action_continue)
            },
            onClick = onContinue,
            enabled = enabled
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    NotePayTheme {
        OnboardingScreen(uiState = OnboardingUiState(), onAction = {})
    }
}
