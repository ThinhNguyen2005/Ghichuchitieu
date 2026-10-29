package com.notepay.ui.feature.onboarding.steps

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.notepay.R
import com.notepay.platform.notification.NotificationHelper
import com.notepay.ui.feature.onboarding.OnboardingUiState
import com.notepay.ui.feature.onboarding.components.ChoiceRow
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.theme.NotePayTheme

@Composable
fun Step3ReminderScreen(
    uiState: OnboardingUiState,
    onToggleReminder: (Boolean) -> Unit,
    onSelectTime: (Int, Int) -> Unit,
    onNotificationDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
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
            .padding(
                start = AppTheme.dimensions.spaceLarge,
                end = AppTheme.dimensions.spaceLarge,
                top = AppTheme.dimensions.spaceLarge,
                bottom = AppTheme.dimensions.spaceExtraLarge
            )
    ) {
        Text(
            text = stringResource(R.string.onboarding_reminder_title),
            style = MaterialTheme.typography.headlineLarge,
            color = AppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
        Text(
            text = stringResource(R.string.onboarding_reminder_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.onboarding_reminder_toggle),
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.textPrimary
            )
            Switch(checked = uiState.reminderEnabled, onCheckedChange = toggle)
        }

        if (uiState.notificationDenied) {
            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
            Text(
                text = stringResource(R.string.onboarding_reminder_permission_denied),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.warning
            )
        }

        if (uiState.reminderEnabled) {
            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))
            Text(
                text = stringResource(R.string.onboarding_reminder_time_label),
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.colors.textMuted
            )
            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))

            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceSmall)) {
                OnboardingUiState.REMINDER_PRESETS.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceSmall)) {
                        row.forEach { (hour, minute) ->
                            ChoiceRow(
                                label = String.format("%02d:%02d", hour, minute),
                                selected = uiState.reminderHour == hour &&
                                    uiState.reminderMinute == minute,
                                onClick = { onSelectTime(hour, minute) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
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
            onNotificationDenied = {}
        )
    }
}
