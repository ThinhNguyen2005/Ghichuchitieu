package com.notepay.feature.onboarding.steps

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.NotePayTheme
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.core.platform.notification.NotificationHelper
import com.notepay.feature.onboarding.OnboardingUiState
import com.notepay.feature.onboarding.components.OnboardingInkStageCard
import com.notepay.feature.onboarding.components.OnboardingStepHeader
import java.util.Locale

@Composable
fun Step3ReminderScreen(
    uiState: OnboardingUiState,
    onToggleReminder: (Boolean) -> Unit,
    onSelectTime: (Int, Int) -> Unit,
    onNotificationDenied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberNotePayReducedMotion()
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) onToggleReminder(true) else onNotificationDenied()
    }

    val toggle: (Boolean) -> Unit = { enabled ->
        when {
            !enabled -> onToggleReminder(false)
            NotificationHelper.hasNotificationPermission(context) -> onToggleReminder(true)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> onToggleReminder(true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OnboardingStepHeader(
            eyebrow = stringResource(R.string.onboarding_step3_eyebrow),
            title = stringResource(R.string.onboarding_reminder_title),
            subtitle = stringResource(R.string.onboarding_reminder_subtitle),
        )

        Step3InkReminderPreview(uiState = uiState, reducedMotion = reducedMotion)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = AppTheme.shapes.corner24,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = cardBorder,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp),
                ) {
                    Surface(
                        shape = AppTheme.shapes.corner12,
                        color = if (uiState.reminderEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            AppTheme.colors.surfaceSunken
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.reminderEnabled) {
                                    Icons.Rounded.NotificationsActive
                                } else {
                                    Icons.Rounded.NotificationsOff
                                },
                                contentDescription = null,
                                tint = if (uiState.reminderEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    AppTheme.colors.textSecondary
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_reminder_toggle),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.textPrimary,
                        )
                        Text(
                            text = stringResource(R.string.onboarding_reminder_toggle_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary,
                        )
                    }

                    Switch(checked = uiState.reminderEnabled, onCheckedChange = toggle)
                }

                if (uiState.notificationDenied) {
                    Surface(
                        shape = AppTheme.shapes.corner16,
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = AppTheme.colors.warning,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.onboarding_reminder_permission_denied),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.reminderEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.onboarding_reminder_time_label),
                            style = AppTheme.typography.eyebrow,
                            color = AppTheme.colors.textSecondary,
                        )

                        OnboardingUiState.REMINDER_PRESETS.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { (hour, minute) ->
                                    val selected = uiState.reminderHour == hour &&
                                        uiState.reminderMinute == minute
                                    val pillBg by animateColorAsState(
                                        targetValue = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            AppTheme.colors.surfaceSunken
                                        },
                                        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 180),
                                        label = "ReminderTimePillBg",
                                    )
                                    Surface(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectTime(hour, minute)
                                        },
                                        shape = AppTheme.shapes.capsule,
                                        color = pillBg,
                                        border = BorderStroke(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                            },
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = String.format(Locale.US, "%02d:%02d", hour, minute),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontFeatureSettings = "tnum",
                                                ),
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun Step3InkReminderPreview(
    uiState: OnboardingUiState,
    reducedMotion: Boolean,
) {
    val timeLabel = if (uiState.reminderEnabled) {
        String.format(Locale.US, "%02d:%02d", uiState.reminderHour, uiState.reminderMinute)
    } else {
        stringResource(R.string.onboarding_reminder_off_label)
    }

    OnboardingInkStageCard {
        Surface(
            shape = AppTheme.shapes.corner20,
            color = AppTheme.colors.inkRaised,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = AppTheme.shapes.corner12,
                    color = if (uiState.reminderEnabled) {
                        AppTheme.colors.sand
                    } else {
                        AppTheme.colors.ink
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (uiState.reminderEnabled) {
                                Icons.Rounded.NotificationsActive
                            } else {
                                Icons.Rounded.NotificationsOff
                            },
                            contentDescription = null,
                            tint = if (uiState.reminderEnabled) {
                                AppTheme.colors.onSand
                            } else {
                                AppTheme.colors.onInkMuted
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.onInkMuted,
                        )
                        AnimatedContent(
                            targetState = timeLabel,
                            transitionSpec = {
                                if (reducedMotion) {
                                    EnterTransition.None togetherWith ExitTransition.None
                                } else {
                                    fadeIn(tween(160)) togetherWith fadeOut(tween(110))
                                }
                            },
                            label = "ReminderTimeBadge",
                        ) { targetTime ->
                            Text(
                                text = targetTime,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFeatureSettings = "tnum",
                                ),
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.reminderEnabled) {
                                    AppTheme.colors.sand
                                } else {
                                    AppTheme.colors.onInkMuted
                                },
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.onboarding_reminder_preview_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.onInk,
                    )

                    Text(
                        text = stringResource(
                            if (uiState.reminderEnabled) {
                                R.string.onboarding_reminder_preview_body
                            } else {
                                R.string.onboarding_reminder_preview_off_body
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.onInkMuted,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryBadge(text = uiState.currencyCode)
            SummaryBadge(
                text = uiState.walletName.trim().ifEmpty { stringResource(R.string.icon_label_cash) },
                modifier = Modifier.weight(1f, fill = false),
            )
            SummaryBadge(text = timeLabel)
        }
    }
}

@Composable
private fun SummaryBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AppTheme.shapes.capsule,
        color = AppTheme.colors.inkRaised,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = AppTheme.colors.onInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Step3ReminderPreview() {
    NotePayTheme {
        Step3ReminderScreen(
            uiState = OnboardingUiState(),
            onToggleReminder = {},
            onSelectTime = { _, _ -> },
            onNotificationDenied = {},
        )
    }
}
