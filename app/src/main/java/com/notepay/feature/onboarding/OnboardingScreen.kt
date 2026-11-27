package com.notepay.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.notepay.R
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.NotePayTheme
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.feature.onboarding.components.OnboardingPrimaryButton
import com.notepay.feature.onboarding.components.OnboardingStepProgressBar
import com.notepay.feature.onboarding.components.OnboardingTopBackdrop
import com.notepay.feature.onboarding.steps.Step1CurrencyScreen
import com.notepay.feature.onboarding.steps.Step2WalletScreen
import com.notepay.feature.onboarding.steps.Step3ReminderScreen
import kotlin.math.absoluteValue

/**
 * Onboarding v4 stateless — nhận [uiState], phát [onAction].
 */
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberNotePayReducedMotion()
    val currentStep = uiState.currentStep

    LaunchedEffect(currentStep, reducedMotion) {
        val target = currentStep.stepIndex
        if (pagerState.currentPage != target) {
            if (reducedMotion) {
                pagerState.scrollToPage(target)
            } else {
                pagerState.animateScrollToPage(target)
            }
        }
    }

    BackHandler(enabled = currentStep.previousStep != null && !uiState.isSaving) {
        currentStep.previousStep?.let { onAction(OnboardingAction.NavigateToStep(it)) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        OnboardingTopBackdrop(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.38f),
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                OnboardingHeader(
                    currentStep = currentStep,
                    isSaving = uiState.isSaving,
                    onBack = {
                        currentStep.previousStep?.let { onAction(OnboardingAction.NavigateToStep(it)) }
                    },
                    onSkip = { onAction(OnboardingAction.Skip) },
                )
            },
            bottomBar = {
                OnboardingFooter(
                    currentStep = currentStep,
                    enabled = uiState.canContinue,
                    isSaving = uiState.isSaving,
                    saveError = uiState.saveError,
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
                            horizontal = AppTheme.dimensions.spaceMedium,
                            vertical = AppTheme.dimensions.spaceMedium,
                        ),
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) { page ->
                val pageModifier = if (reducedMotion) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val pageOffset = (
                                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                ).absoluteValue.coerceIn(0f, 1f)
                            val scale = lerp(start = 0.96f, stop = 1f, fraction = 1f - pageOffset)
                            scaleX = scale
                            scaleY = scale
                            alpha = lerp(start = 0.72f, stop = 1f, fraction = 1f - pageOffset)
                        }
                }
                when (page) {
                    0 -> Step1CurrencyScreen(
                        uiState = uiState,
                        onSelectCurrency = { onAction(OnboardingAction.SelectCurrency(it)) },
                        onSelectLanguage = { onAction(OnboardingAction.SelectLanguage(it)) },
                        modifier = pageModifier,
                    )
                    1 -> Step2WalletScreen(
                        uiState = uiState,
                        onNameChange = { onAction(OnboardingAction.UpdateWalletName(it)) },
                        onSelectIcon = { onAction(OnboardingAction.SelectWalletIcon(it)) },
                        modifier = pageModifier,
                    )
                    2 -> Step3ReminderScreen(
                        uiState = uiState,
                        onToggleReminder = { onAction(OnboardingAction.ToggleReminder(it)) },
                        onSelectTime = { hour, minute ->
                            onAction(OnboardingAction.UpdateReminderTime(hour, minute))
                        },
                        onNotificationDenied = { onAction(OnboardingAction.ReportNotificationDenied) },
                        modifier = pageModifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeader(
    currentStep: OnboardingStep,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(60.dp)
            .padding(horizontal = 12.dp),
    ) {
        if (currentStep.previousStep != null) {
            IconButton(
                onClick = onBack,
                enabled = !isSaving,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = AppTheme.colors.textPrimary,
                )
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp),
            ) {
                Surface(
                    shape = AppTheme.shapes.corner12,
                    color = AppTheme.colors.sand,
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_brand_mark),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        OnboardingStepProgressBar(currentStep = currentStep)

        TextButton(
            onClick = onSkip,
            enabled = !isSaving,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_action_skip),
                style = MaterialTheme.typography.labelLarge,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun OnboardingFooter(
    currentStep: OnboardingStep,
    enabled: Boolean,
    isSaving: Boolean,
    saveError: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier,
    ) {
        if (saveError) {
            Surface(
                shape = AppTheme.shapes.corner16,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.onboarding_save_error_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        val isLastStep = currentStep == OnboardingStep.REMINDER
        OnboardingPrimaryButton(
            text = if (isLastStep) {
                stringResource(R.string.onboarding_action_start)
            } else {
                stringResource(R.string.onboarding_action_continue)
            },
            onClick = onContinue,
            enabled = enabled,
            isLoading = isSaving,
            trailingIcon = if (isLastStep) {
                Icons.Rounded.Check
            } else {
                Icons.AutoMirrored.Rounded.ArrowForward
            },
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
