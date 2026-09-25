package com.notepay.ui.feature.home

import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.feature.autocapture.autoCaptureSettingsItem
import com.notepay.platform.LiquidGlassBlockReason
import com.notepay.platform.OsCompatHelper
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.theme.NotePayTheme
import com.notepay.ui.theme.ThemeManager

/**
 * State thuần túy cho màn hình Cài đặt (SettingsUiState).
 */
data class SettingsUiState(
    val themeMode: String = "system",
    val geminiApiKey: String? = null,
    val cloudAiEnabled: Boolean = true,
    val smartReceiptAiEnabled: Boolean = true,
    val isGeminiNanoAvailable: Boolean = false,
    val liquidGlassEnabled: Boolean = false,
    val isLiquidGlassSupported: Boolean = true,
    val liquidGlassStatusText: String = "",
    val dailyReminderEnabled: Boolean = false,
    val budgetAlertsEnabled: Boolean = true,
    val weeklyDigestEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 30,
)

/**
 * Entry-point kết nối ViewModel và Navigation.
 */
@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
    onNavigateToBackupRestore: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val cloudAiEnabled by viewModel.cloudAiEnabled.collectAsStateWithLifecycle()
    val smartReceiptAiEnabled by viewModel.smartReceiptAiEnabled.collectAsStateWithLifecycle()
    val isGeminiNanoAvailable by viewModel.isGeminiNanoAvailable.collectAsStateWithLifecycle()
    val liquidGlassEnabled by viewModel.liquidGlassEnabled.collectAsStateWithLifecycle()
    val dailyReminderEnabled by viewModel.dailyReminderEnabled.collectAsStateWithLifecycle()
    val budgetAlertsEnabled by viewModel.budgetAlertsEnabled.collectAsStateWithLifecycle()
    val weeklyDigestEnabled by viewModel.weeklyDigestEnabled.collectAsStateWithLifecycle()
    val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()

    val glassCompatibility = remember(view.isHardwareAccelerated) {
        OsCompatHelper.liquidGlassCompatibility(
            isHardwareAccelerated = view.isHardwareAccelerated,
        )
    }
    val glassStatus = when (glassCompatibility.blockReason) {
        null -> if (liquidGlassEnabled) {
            stringResource(R.string.navigation_glass_enabled)
        } else {
            stringResource(R.string.navigation_glass_disabled)
        }
        LiquidGlassBlockReason.ANDROID_VERSION ->
            stringResource(R.string.navigation_glass_block_android)
        LiquidGlassBlockReason.HARDWARE_ACCELERATION ->
            stringResource(R.string.navigation_glass_block_hardware)
    }

    val uiState = SettingsUiState(
        themeMode = ThemeManager.themeMode,
        geminiApiKey = geminiApiKey,
        cloudAiEnabled = cloudAiEnabled,
        smartReceiptAiEnabled = smartReceiptAiEnabled,
        isGeminiNanoAvailable = isGeminiNanoAvailable,
        liquidGlassEnabled = liquidGlassEnabled,
        isLiquidGlassSupported = glassCompatibility.isSupported,
        liquidGlassStatusText = "${glassCompatibility.deviceDescription} • $glassStatus",
        dailyReminderEnabled = dailyReminderEnabled,
        budgetAlertsEnabled = budgetAlertsEnabled,
        weeklyDigestEnabled = weeklyDigestEnabled,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
    )

    fun playHaptic() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    SettingsContent(
        uiState = uiState,
        onBack = {
            playHaptic()
            onBack()
        },
        onThemeModeSelected = { mode ->
            playHaptic()
            ThemeManager.updateThemeMode(context, mode)
        },
        onSaveApiKey = { key ->
            playHaptic()
            viewModel.setGeminiApiKey(key)
        },
        onToggleCloudAi = { enabled ->
            playHaptic()
            viewModel.setCloudAiEnabled(enabled)
        },
        onToggleSmartReceiptAi = { enabled ->
            playHaptic()
            viewModel.setSmartReceiptAiEnabled(enabled)
        },
        onTestApiKey = { key, callback ->
            playHaptic()
            viewModel.testGeminiApiKey(key, callback)
        },
        onOpenKeyPage = {
            playHaptic()
            context.startActivity(
                Intent(Intent.ACTION_VIEW, "https://aistudio.google.com/app/apikey".toUri())
            )
        },
        onToggleLiquidGlass = { enabled ->
            playHaptic()
            viewModel.setLiquidGlassEnabled(enabled)
        },
        onToggleDailyReminder = { enabled ->
            playHaptic()
            viewModel.setDailyReminderEnabled(context, enabled)
        },
        onToggleBudgetAlerts = { enabled ->
            playHaptic()
            viewModel.setBudgetAlertsEnabled(enabled)
        },
        onToggleWeeklyDigest = { enabled ->
            playHaptic()
            viewModel.setWeeklyDigestEnabled(context, enabled)
        },
        onUpdateReminderTime = { hour, minute ->
            playHaptic()
            viewModel.updateReminderTime(context, hour, minute)
        },
        onNavigateToBackupRestore = {
            playHaptic()
            onNavigateToBackupRestore()
        }
    )
}

/**
 * Giao diện stateless của màn hình Cài đặt, tuân thủ chặt chẽ Material 3 Expressive UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onThemeModeSelected: (String) -> Unit,
    onSaveApiKey: (String?) -> Unit,
    onToggleCloudAi: (Boolean) -> Unit,
    onToggleSmartReceiptAi: (Boolean) -> Unit,
    onTestApiKey: (String, (Result<String>) -> Unit) -> Unit,
    onOpenKeyPage: () -> Unit,
    onToggleLiquidGlass: (Boolean) -> Unit,
    onToggleDailyReminder: (Boolean) -> Unit,
    onToggleBudgetAlerts: (Boolean) -> Unit = {},
    onToggleWeeklyDigest: (Boolean) -> Unit = {},
    onUpdateReminderTime: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToBackupRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_settings_title),
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
            // Khu vực 1: Chọn Chế độ Giao diện (Theme Selector với SingleChoiceSegmentedButtonRow)
            item {
                ThemeSelectorCard(
                    selectedMode = uiState.themeMode,
                    onThemeModeSelected = onThemeModeSelected
                )
            }

            // Flavor feature: Chụp tự động thông báo
            autoCaptureSettingsItem()

            // Khu vực 2, 3, 4: Trí tuệ nhân tạo (AI Engine & API Key)
            item {
                AiEngineSettingsCard(
                    isGeminiNanoAvailable = uiState.isGeminiNanoAvailable,
                    apiKey = uiState.geminiApiKey,
                    cloudAiEnabled = uiState.cloudAiEnabled,
                    smartReceiptAiEnabled = uiState.smartReceiptAiEnabled,
                    onSaveApiKey = onSaveApiKey,
                    onToggleCloudAi = onToggleCloudAi,
                    onToggleSmartReceiptAi = onToggleSmartReceiptAi,
                    onTestApiKey = onTestApiKey,
                    onOpenKeyPage = onOpenKeyPage,
                )
            }

            // Khu vực 5: Hiệu ứng Liquid Glass & Nhắc nhở ghi chép (ListItem + Switch Thumb Icon)
            item {
                SettingCard {
                    SettingListItem(
                        icon = Icons.Rounded.BlurOn,
                        title = stringResource(R.string.navigation_glass_title),
                        description = uiState.liquidGlassStatusText,
                        trailingContent = {
                            Switch(
                                checked = uiState.liquidGlassEnabled && uiState.isLiquidGlassSupported,
                                onCheckedChange = onToggleLiquidGlass,
                                enabled = uiState.isLiquidGlassSupported,
                                thumbContent = if (uiState.liquidGlassEnabled && uiState.isLiquidGlassSupported) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                } else null
                            )
                        }
                    )

                }
            }

            // Khu vực: Thông báo & Lời nhắc thông minh
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
                                checked = uiState.budgetAlertsEnabled,
                                onCheckedChange = onToggleBudgetAlerts,
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
                        title = stringResource(R.string.settings_notif_weekly_digest_title),
                        description = stringResource(R.string.settings_notif_weekly_digest_desc),
                        trailingContent = {
                            Switch(
                                checked = uiState.weeklyDigestEnabled,
                                onCheckedChange = onToggleWeeklyDigest,
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
                        title = stringResource(R.string.settings_notif_daily_reminder_title),
                        description = stringResource(R.string.settings_notif_daily_reminder_desc),
                        trailingContent = {
                            Switch(
                                checked = uiState.dailyReminderEnabled,
                                onCheckedChange = onToggleDailyReminder,
                            )
                        }
                    )

                    if (uiState.dailyReminderEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // Giờ nhắc nhở
                        SettingListItem(
                            icon = Icons.Rounded.CheckCircle,
                            title = stringResource(R.string.settings_notif_reminder_time_title),
                            description = "%02d:%02d".format(uiState.reminderHour, uiState.reminderMinute),
                            onClick = {
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hour, minute -> onUpdateReminderTime(hour, minute) },
                                    uiState.reminderHour,
                                    uiState.reminderMinute,
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

            // Khu vực 6: Sao lưu & Khôi phục dữ liệu
            item {
                SettingCard(onClick = onNavigateToBackupRestore) {
                    SettingListItem(
                        icon = Icons.Rounded.CloudSync,
                        title = stringResource(R.string.settings_backup_restore_title),
                        description = stringResource(R.string.settings_backup_restore_description),
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

/**
 * Khu vực 1: Theme Selector Card chuẩn Material 3 với SingleChoiceSegmentedButtonRow
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectorCard(
    selectedMode: String,
    onThemeModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingCard(modifier = modifier) {
        SettingHeader(
            icon = Icons.Rounded.Palette,
            title = stringResource(R.string.settings_theme_mode_title),
            description = stringResource(R.string.settings_theme_mode_desc)
        )

        val options = remember {
            listOf(
                Triple("light", R.string.theme_light, Icons.Rounded.LightMode),
                Triple("dark", R.string.theme_dark, Icons.Rounded.DarkMode),
                Triple("system", R.string.theme_system, Icons.Rounded.BrightnessAuto)
            )
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEachIndexed { index, (mode, labelRes, icon) ->
                val isSelected = selectedMode == mode
                SegmentedButton(
                    selected = isSelected,
                    onClick = { onThemeModeSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = isSelected) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    }
}

/**
 * Khu vực 2, 3, 4, 5: Thẻ cấu hình AI Engine & Gemini API Key
 */
@Composable
private fun AiEngineSettingsCard(
    isGeminiNanoAvailable: Boolean,
    apiKey: String?,
    cloudAiEnabled: Boolean,
    smartReceiptAiEnabled: Boolean,
    onSaveApiKey: (String?) -> Unit,
    onToggleCloudAi: (Boolean) -> Unit,
    onToggleSmartReceiptAi: (Boolean) -> Unit,
    onTestApiKey: (String, (Result<String>) -> Unit) -> Unit,
    onOpenKeyPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var keyInput by remember(apiKey) { mutableStateOf(apiKey.orEmpty()) }
    var showPassword by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Result<String>?>(null) }
    val isLocalFlavor = com.notepay.BuildConfig.FLAVOR == "local"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shape = AppTheme.shapes.corner20
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header AI Engine
            SettingHeader(
                icon = Icons.Rounded.AutoAwesome,
                title = stringResource(R.string.settings_ai_engine_title),
                description = stringResource(
                    if (isLocalFlavor) R.string.settings_ai_engine_subtitle_local
                    else R.string.settings_ai_engine_subtitle
                )
            )

            // Khu vực 4: Thông báo Gemini Nano Status Banner (Warning / Assist Banner)
            val bannerContainerColor = if (isGeminiNanoAvailable) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            }
            val bannerContentColor = if (isGeminiNanoAvailable) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            }
            val bannerBorderColor = if (isGeminiNanoAvailable) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = bannerContainerColor,
                shape = AppTheme.shapes.corner14,
                border = BorderStroke(1.dp, bannerBorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isGeminiNanoAvailable) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = if (isGeminiNanoAvailable) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(
                            if (isGeminiNanoAvailable) R.string.settings_ai_gemini_nano_ready
                            else R.string.settings_ai_gemini_nano_unsupported
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = bannerContentColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (isLocalFlavor) {
                // Local Privacy Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = AppTheme.shapes.corner14,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_ai_local_offline_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_ai_local_offline_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Khu vực 2: Khung nhập API Key (OutlinedTextField + supportingText link + Single Visibility Toggle)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = it
                            testResult = null
                        },
                        label = { Text(stringResource(R.string.settings_ai_api_key_label)) },
                        placeholder = { Text(stringResource(R.string.settings_ai_api_key_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = AppTheme.shapes.corner14,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            // Dọn dẹp trailing icons: Chỉ giữ 1 icon bật/tắt hiển thị mật khẩu
                            IconButton(
                                onClick = { showPassword = !showPassword },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                            ) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = stringResource(
                                        if (showPassword) R.string.content_description_hide_password
                                        else R.string.content_description_show_password
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        supportingText = {
                            // Đặt liên kết lấy API Key làm supportingText phía dưới khung nhập
                            Row(
                                modifier = Modifier
                                    .clickable { onOpenKeyPage() }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = stringResource(R.string.settings_ai_api_key_get_free),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    )

                    // Khu vực 3: Phân cấp Nút (Button Hierarchy: OutlinedButton vs Primary Filled Button)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Nút phụ (Secondary Action): Kiểm tra kết nối
                        OutlinedButton(
                            onClick = {
                                val candidate = keyInput.trim().ifBlank { apiKey.orEmpty() }
                                if (candidate.isNotBlank()) {
                                    isTesting = true
                                    testResult = null
                                    onTestApiKey(candidate) { res ->
                                        isTesting = false
                                        testResult = res
                                    }
                                }
                            },
                            enabled = !isTesting && (keyInput.isNotBlank() || !apiKey.isNullOrBlank()),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            shape = AppTheme.shapes.corner12,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.settings_ai_api_key_testing),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.NetworkCheck,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.settings_ai_api_key_test),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Nút chính (Primary Action): Lưu Key
                        Button(
                            onClick = {
                                onSaveApiKey(keyInput.trim().takeIf { it.isNotBlank() })
                            },
                            enabled = keyInput.trim() != (apiKey ?: ""),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            shape = AppTheme.shapes.corner12,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.settings_ai_api_key_save),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Phản hồi kết quả test
                    testResult?.let { res ->
                        Surface(
                            color = if (res.isSuccess) Color(0xFF1B7F4F).copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = AppTheme.shapes.corner10,
                            border = BorderStroke(
                                1.dp,
                                if (res.isSuccess) Color(0xFF1B7F4F).copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (res.isSuccess) {
                                    stringResource(R.string.settings_ai_api_key_test_success, res.getOrNull().orEmpty())
                                } else {
                                    stringResource(R.string.settings_ai_api_key_test_failed, res.exceptionOrNull()?.message.orEmpty())
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (res.isSuccess) Color(0xFF1B7F4F) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Khu vực 5: Switches với ListItem và Switch M3 Thumb Icon
                SettingListItem(
                    icon = Icons.Rounded.CloudQueue,
                    title = stringResource(R.string.settings_ai_cloud_toggle_title),
                    description = stringResource(R.string.settings_ai_cloud_toggle_desc),
                    trailingContent = {
                        Switch(
                            checked = cloudAiEnabled,
                            onCheckedChange = onToggleCloudAi,
                            thumbContent = if (cloudAiEnabled) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            } else null
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                SettingListItem(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = stringResource(R.string.settings_ai_receipt_toggle_title),
                    description = stringResource(R.string.settings_ai_receipt_toggle_desc),
                    trailingContent = {
                        Switch(
                            checked = smartReceiptAiEnabled,
                            onCheckedChange = onToggleSmartReceiptAi,
                            thumbContent = if (smartReceiptAiEnabled) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            } else null
                        )
                    }
                )

                // Privacy Notice Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = AppTheme.shapes.corner14,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.settings_ai_privacy_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tiêu đề thẻ setting với Icon tròn 40dp
 */
@Composable
private fun SettingHeader(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Tiêu đề nhóm cài đặt
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

/**
 * Mục danh sách cấu hình (ListItem chuẩn Material 3) kết hợp Switch hoặc Action
 */
@Composable
private fun SettingListItem(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable () -> Unit,
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = trailingContent
    )
}

/**
 * Thẻ bọc Setting chuẩn Material 3:
 * Phân tầng màu bề mặt (surfaceContainer) và bo góc mềm mại 20dp.
 */
@Composable
fun SettingCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shape = AppTheme.shapes.corner20,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable { onClick() } else Modifier
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

// ==========================================
// Preview Components
// ==========================================

@Preview(name = "Settings Screen - Light Mode", showBackground = true)
@Composable
private fun SettingsScreenPreviewLight() {
    NotePayTheme(darkTheme = false) {
        SettingsContent(
            uiState = SettingsUiState(
                themeMode = "system",
                geminiApiKey = "AIzaSyFakeKeyForPreview",
                isGeminiNanoAvailable = false,
                liquidGlassEnabled = true,
                isLiquidGlassSupported = true,
                liquidGlassStatusText = "Qualcomm Snapdragon • Đã kích hoạt hiệu ứng kính",
                dailyReminderEnabled = true
            ),
            onBack = {},
            onThemeModeSelected = {},
            onSaveApiKey = {},
            onToggleCloudAi = {},
            onToggleSmartReceiptAi = {},
            onTestApiKey = { _, _ -> },
            onOpenKeyPage = {},
            onToggleLiquidGlass = {},
            onToggleDailyReminder = {},
            onNavigateToBackupRestore = {}
        )
    }
}

@Preview(name = "Settings Screen - Dark Mode", showBackground = true)
@Composable
private fun SettingsScreenPreviewDark() {
    NotePayTheme(darkTheme = true) {
        SettingsContent(
            uiState = SettingsUiState(
                themeMode = "dark",
                geminiApiKey = null,
                isGeminiNanoAvailable = true,
                liquidGlassEnabled = false,
                isLiquidGlassSupported = true,
                liquidGlassStatusText = "Google Tensor G4 • Đang tắt",
                dailyReminderEnabled = false
            ),
            onBack = {},
            onThemeModeSelected = {},
            onSaveApiKey = {},
            onToggleCloudAi = {},
            onToggleSmartReceiptAi = {},
            onTestApiKey = { _, _ -> },
            onOpenKeyPage = {},
            onToggleLiquidGlass = {},
            onToggleDailyReminder = {},
            onNavigateToBackupRestore = {}
        )
    }
}

/**
 * Alias tên SettingsScreen hỗ trợ gọi linh hoạt theo quy chuẩn Screen naming convention.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToBackupRestore: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    AppSettingsScreen(
        onBack = onBack,
        onNavigateToBackupRestore = onNavigateToBackupRestore,
        viewModel = viewModel
    )
}
