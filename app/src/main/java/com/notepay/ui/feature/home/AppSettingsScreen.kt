package com.notepay.ui.feature.home

import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.feature.autocapture.autoCaptureSettingsItem
import com.notepay.platform.LiquidGlassBlockReason
import com.notepay.platform.OsCompatHelper
import com.notepay.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
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

    fun playHaptic() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        playHaptic()
                        onBack()
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // 1. Màu sắc chủ đề (Theme Color)
            item {
                ThemeSettingsCard(onPlayHaptic = ::playHaptic)
            }

            // 2. Chụp tự động thông báo
            autoCaptureSettingsItem()

            // 3. Trí tuệ nhân tạo (AI Engine)
            item {
                AiEngineSettingsCard(
                    isGeminiNanoAvailable = isGeminiNanoAvailable,
                    apiKey = geminiApiKey,
                    cloudAiEnabled = cloudAiEnabled,
                    smartReceiptAiEnabled = smartReceiptAiEnabled,
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
                )
            }

            // 4. Giao diện & Hiệu ứng Liquid Glass
            item {
                val glassCompatibility = OsCompatHelper.liquidGlassCompatibility(
                    isHardwareAccelerated = view.isHardwareAccelerated,
                )
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

                SettingCard {
                    SettingRowWithIcon(
                        icon = Icons.Rounded.BlurOn,
                        title = stringResource(R.string.navigation_glass_title),
                        description = "${glassCompatibility.deviceDescription} • $glassStatus",
                        trailingContent = {
                            Switch(
                                checked = liquidGlassEnabled && glassCompatibility.isSupported,
                                onCheckedChange = {
                                    playHaptic()
                                    viewModel.setLiquidGlassEnabled(it)
                                },
                                enabled = glassCompatibility.isSupported
                            )
                        }
                    )
                }
            }

            // 5. Nhắc nhở ghi chép mỗi tối
            item {
                SettingCard {
                    SettingRowWithIcon(
                        icon = Icons.Rounded.NotificationsActive,
                        title = stringResource(R.string.settings_daily_reminder_title),
                        description = if (dailyReminderEnabled) {
                            stringResource(R.string.settings_daily_reminder_enabled)
                        } else {
                            stringResource(R.string.settings_daily_reminder_disabled)
                        },
                        trailingContent = {
                            Switch(
                                checked = dailyReminderEnabled,
                                onCheckedChange = {
                                    playHaptic()
                                    viewModel.setDailyReminderEnabled(context, it)
                                }
                            )
                        }
                    )
                }
            }

            // 6. Sao lưu & Khôi phục dữ liệu
            item {
                SettingCard(
                    onClick = {
                        playHaptic()
                        onNavigateToBackupRestore()
                    }
                ) {
                    SettingRowWithIcon(
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
 * Thẻ bọc Setting chuẩn phong cách với viền mỏng và góc bo 20dp đồng bộ.
 */
@Composable
private fun SettingCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
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

/**
 * Hàng cài đặt chuẩn phong cách AddWalletScreen:
 * - Icon tròn 40dp bên trái có background mờ
 * - Tiêu đề SemiBold + mô tả phụ
 * - Trailing content bên phải (Switch / Chevron / Action)
 */
@Composable
private fun SettingRowWithIcon(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconBackground: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    trailingContent: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailingContent()
    }
}

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
) {
    var keyInput by remember(apiKey) { mutableStateOf(apiKey.orEmpty()) }
    var showPassword by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Result<String>?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val isLocalFlavor = com.notepay.BuildConfig.FLAVOR == "local"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        shape = AppTheme.shapes.corner20
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header AI Engine
            SettingRowWithIcon(
                icon = Icons.Rounded.AutoAwesome,
                title = stringResource(R.string.settings_ai_engine_title),
                description = stringResource(
                    if (isLocalFlavor) R.string.settings_ai_engine_subtitle_local
                    else R.string.settings_ai_engine_subtitle
                ),
                trailingContent = {}
            )

            // On-device Nano Status Badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                shape = AppTheme.shapes.corner12,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isGeminiNanoAvailable) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                        contentDescription = null,
                        tint = if (isGeminiNanoAvailable) Color(0xFF1B7F4F) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(
                            if (isGeminiNanoAvailable) R.string.settings_ai_gemini_nano_ready
                            else R.string.settings_ai_gemini_nano_unsupported
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isGeminiNanoAvailable) Color(0xFF1B7F4F) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isGeminiNanoAvailable) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }

            if (isLocalFlavor) {
                // Local Privacy-First Notice Card đồng bộ như notice VietQR
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    shape = AppTheme.shapes.corner14,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
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
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = stringResource(R.string.settings_ai_local_offline_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            } else {
                // API Key Input Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.settings_ai_api_key_label),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = onOpenKeyPage,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                stringResource(R.string.settings_ai_api_key_get_free),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = it
                            testResult = null
                        },
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (keyInput.isNotEmpty()) {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = stringResource(
                                                if (showPassword) R.string.content_description_hide_password
                                                else R.string.content_description_show_password
                                            ),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(onClick = {
                                        keyInput = ""
                                        testResult = null
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Clear,
                                            contentDescription = stringResource(R.string.content_description_clear_text),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    IconButton(onClick = {
                                        clipboardManager.getText()?.text?.let { keyInput = it.trim() }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentPaste,
                                            contentDescription = stringResource(R.string.content_description_paste),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )

                    // Two Balanced Action Buttons (46dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                .height(46.dp),
                            shape = AppTheme.shapes.corner12,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.settings_ai_api_key_testing),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.NetworkCheck,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.settings_ai_api_key_test),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onSaveApiKey(keyInput.trim().takeIf { it.isNotBlank() })
                            },
                            enabled = keyInput.trim() != (apiKey ?: ""),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = AppTheme.shapes.corner12
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.settings_ai_api_key_save),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Test result feedback
                    testResult?.let { res ->
                        Surface(
                            color = if (res.isSuccess) Color(0xFF1B7F4F).copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                            shape = AppTheme.shapes.corner10,
                            border = BorderStroke(
                                1.dp,
                                if (res.isSuccess) Color(0xFF1B7F4F).copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
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

                // Switches với Icon tròn 40dp đồng bộ
                SettingRowWithIcon(
                    icon = Icons.Rounded.CloudQueue,
                    title = stringResource(R.string.settings_ai_cloud_toggle_title),
                    description = stringResource(R.string.settings_ai_cloud_toggle_desc),
                    trailingContent = {
                        Switch(
                            checked = cloudAiEnabled,
                            onCheckedChange = onToggleCloudAi
                        )
                    }
                )

                SettingRowWithIcon(
                    icon = Icons.Rounded.ReceiptLong,
                    title = stringResource(R.string.settings_ai_receipt_toggle_title),
                    description = stringResource(R.string.settings_ai_receipt_toggle_desc),
                    trailingContent = {
                        Switch(
                            checked = smartReceiptAiEnabled,
                            onCheckedChange = onToggleSmartReceiptAi
                        )
                    }
                )

                // Privacy Notice Card đồng bộ
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    shape = AppTheme.shapes.corner14,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
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
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.settings_ai_privacy_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSettingsCard(
    onPlayHaptic: () -> Unit,
) {
    val context = LocalContext.current
    val currentTheme = com.notepay.ui.theme.ThemeManager.currentThemeColor

    val themeOptions = remember {
        listOf(
            Triple("ios", R.string.theme_ios, Color(0xFF1C1C1E)),
            Triple("dynamic", R.string.theme_dynamic_color, Color(0xFF6750A4)),
        )
    }

    SettingCard {
        SettingRowWithIcon(
            icon = Icons.Rounded.Palette,
            title = stringResource(R.string.settings_theme_title),
            description = stringResource(R.string.settings_theme_description),
            trailingContent = {}
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(themeOptions, key = { it.first }) { (key, labelRes, color) ->
                val isSelected = currentTheme == key
                val isDynamicOption = key == "dynamic"
                val isIosOption = key == "ios"

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onPlayHaptic()
                        com.notepay.ui.theme.ThemeManager.updateThemeColor(context, key)
                    },
                    label = {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        when {
                            isDynamicOption -> Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF6750A4)
                            )
                            isIosOption -> Icon(
                                imageVector = Icons.Rounded.PhoneIphone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1C1C1E)
                            )
                            else -> Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

