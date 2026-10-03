package com.notepay.ui.feature.settings.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.feature.autocapture.autoCaptureSettingsItem
import com.notepay.ui.component.SectionHeader
import com.notepay.ui.component.SettingCard
import com.notepay.ui.component.SettingListItem
import com.notepay.ui.feature.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    
    val dailyReminderEnabled by viewModel.dailyReminderEnabled.collectAsStateWithLifecycle()
    val budgetAlertsEnabled by viewModel.budgetAlertsEnabled.collectAsStateWithLifecycle()
    val weeklyDigestEnabled by viewModel.weeklyDigestEnabled.collectAsStateWithLifecycle()
    val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.utilities_reminder_title), // Will change/verify later
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // Flavor feature: Chụp tự động thông báo
            autoCaptureSettingsItem()

            item {
                SectionHeader(title = stringResource(R.string.settings_notification_section_title))
            }
            
            item {
                SettingCard {
                    // Cảnh báo ngân sách
                    SettingListItem(
                        icon = Icons.Rounded.Warning,
                        title = stringResource(R.string.settings_notif_budget_alerts_title),
                        description = stringResource(R.string.settings_notif_budget_alerts_desc),
                        trailingContent = {
                            Switch(
                                checked = budgetAlertsEnabled,
                                onCheckedChange = { viewModel.setBudgetAlertsEnabled(it) },
                            )
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Tổng kết tuần
                    SettingListItem(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        title = stringResource(R.string.notif_channel_weekly_digest),
                        description = stringResource(R.string.settings_notif_weekly_digest_desc),
                        trailingContent = {
                            Switch(
                                checked = weeklyDigestEnabled,
                                onCheckedChange = { viewModel.setWeeklyDigestEnabled(context, it) },
                            )
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Nhắc nhở hàng ngày & Streak
                    SettingListItem(
                        icon = Icons.Rounded.NotificationsActive,
                        title = stringResource(R.string.notif_channel_daily_reminders),
                        description = stringResource(R.string.settings_notif_daily_reminder_desc),
                        trailingContent = {
                            Switch(
                                checked = dailyReminderEnabled,
                                onCheckedChange = { viewModel.setDailyReminderEnabled(context, it) },
                            )
                        }
                    )

                    if (dailyReminderEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // Giờ nhắc nhở
                        SettingListItem(
                            icon = Icons.Rounded.CheckCircle,
                            title = stringResource(R.string.settings_notif_reminder_time_title),
                            description = "%02d:%02d".format(reminderHour, reminderMinute),
                            onClick = {
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hour, minute -> viewModel.updateReminderTime(context, hour, minute) },
                                    reminderHour,
                                    reminderMinute,
                                    true,
                                ).show()
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
