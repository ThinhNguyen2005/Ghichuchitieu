package com.notepay.ui.feature.settings.ai

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.data.preferences.AiSettingsDataStore
import com.notepay.ui.theme.AppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: AiSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val testState by viewModel.testState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    var apiKeyInput by remember { mutableStateOf("") }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var showApiKeySheet by remember { mutableStateOf(false) }
    var showModelPickerSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.apiKey) {
        if (apiKeyInput.isEmpty() && !uiState.apiKey.isNullOrBlank()) {
            apiKeyInput = uiState.apiKey.orEmpty()
        }
    }

    if (showApiKeySheet) {
        ApiKeyBottomSheet(
            apiKeyInput = apiKeyInput,
            isApiKeyVisible = isApiKeyVisible,
            testState = testState,
            onApiKeyChange = {
                apiKeyInput = it
                viewModel.setApiKey(it)
            },
            onToggleVisibility = { isApiKeyVisible = !isApiKeyVisible },
            onPasteFromClipboard = {
                scope.launch {
                    val clip = clipboard.getClipEntry()
                        ?.clipData
                        ?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)
                        ?.coerceToText(context)
                        ?.toString()
                        .orEmpty()
                    if (clip.isNotBlank()) {
                        apiKeyInput = clip
                        viewModel.setApiKey(clip)
                    }
                }
            },
            onClearKey = {
                apiKeyInput = ""
                viewModel.setApiKey("")
            },
            onTestKey = { viewModel.testApiKey(apiKeyInput) },
            onDismissRequest = {
                viewModel.resetTestState()
                showApiKeySheet = false
            },
        )
    }

    if (showModelPickerSheet) {
        ModelPickerBottomSheet(
            selectedModel = uiState.cloudModelName,
            onModelSelect = { viewModel.setCloudModelName(it) },
            onDismissRequest = { showModelPickerSheet = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ai_settings_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 40.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header: AI Engine Overview Card (Phong cách UserProfileHeaderCard của Utilities)
            item {
                AiEngineHeaderCard(
                    isNanoAvailable = uiState.isGeminiNanoAvailable,
                )
            }

            // Section 1: CÔNG CỤ AI
            item {
                SectionHeader(title = stringResource(R.string.ai_section_cloud))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AiSettingRow(
                            icon = Icons.Rounded.Cloud,
                            iconTint = Color(0xFF007AFF),
                            title = stringResource(R.string.ai_cloud_toggle_title),
                            subtitle = stringResource(R.string.ai_cloud_toggle_desc),
                            trailing = {
                                Switch(
                                    checked = uiState.cloudAiEnabled,
                                    onCheckedChange = { viewModel.setCloudAiEnabled(it) },
                                )
                            },
                        )
                        ItemDivider()
                        AiSettingRow(
                            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                            iconTint = Color(0xFFFF9500),
                            title = stringResource(R.string.ai_smart_receipt_title),
                            subtitle = stringResource(R.string.ai_smart_receipt_desc),
                            trailing = {
                                Switch(
                                    checked = uiState.smartReceiptAiEnabled,
                                    onCheckedChange = { viewModel.setSmartReceiptAiEnabled(it) },
                                )
                            },
                        )
                    }
                }
            }

            // Section 2: MÔ HÌNH & KẾT NỐI
            item {
                SectionHeader(title = stringResource(R.string.ai_model_selection_title))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AiSettingRow(
                            icon = Icons.Rounded.Psychology,
                            iconTint = Color(0xFF5856D6),
                            title = stringResource(R.string.ai_model_selection_title),
                            subtitle = uiState.cloudModelName,
                            onClick = { showModelPickerSheet = true },
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                        )
                        ItemDivider()
                        val maskedKey = if (uiState.apiKey.isNullOrBlank()) {
                            stringResource(R.string.ai_api_key_hint)
                        } else {
                            "••••••••••••••••"
                        }
                        AiSettingRow(
                            icon = Icons.Rounded.Key,
                            iconTint = Color(0xFF34C759),
                            title = stringResource(R.string.ai_api_key_label),
                            subtitle = maskedKey,
                            onClick = { showApiKeySheet = true },
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                        )
                    }
                }
            }

            // Section 3: BẢO MẬT & QUYỀN RIÊNG TƯ
            item {
                SectionHeader(title = stringResource(R.string.ai_section_privacy))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ai_section_privacy),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.ai_privacy_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.25,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiEngineHeaderCard(
    isNanoAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5856D6).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF5856D6),
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ai_nano_banner_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isNanoAvailable) Color(0xFF34C759) else Color(0xFFFF9500)),
                    )
                    Text(
                        text = stringResource(
                            if (isNanoAvailable) R.string.ai_nano_banner_ready
                            else R.string.ai_nano_banner_unsupported
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = if (isNanoAvailable) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                contentDescription = null,
                tint = if (isNanoAvailable) Color(0xFF34C759) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

@Composable
private fun AiSettingRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        trailing()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeyBottomSheet(
    apiKeyInput: String,
    isApiKeyVisible: Boolean,
    testState: ApiKeyTestState,
    onApiKeyChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onPasteFromClipboard: () -> Unit,
    onClearKey: () -> Unit,
    onTestKey: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.ai_api_key_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = stringResource(R.string.action_done),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = onApiKeyChange,
                label = { Text(stringResource(R.string.ai_api_key_label)) },
                placeholder = { Text(stringResource(R.string.ai_api_key_hint)) },
                visualTransformation = if (isApiKeyVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Rounded.VisibilityOff
                                else Icons.Rounded.Visibility,
                                contentDescription = null,
                            )
                        }
                        if (apiKeyInput.isNotEmpty()) {
                            IconButton(onClick = onClearKey) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = stringResource(R.string.action_delete),
                                )
                            }
                        } else {
                            IconButton(onClick = onPasteFromClipboard) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = stringResource(R.string.ai_api_key_paste),
                                )
                            }
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Clickable Link to AI Studio
            Row(
                modifier = Modifier
                    .clickable {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://aistudio.google.com/app/apikey".toUri()),
                        )
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.ai_api_key_supporting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }

            // Test Connection Button
            OutlinedButton(
                onClick = onTestKey,
                enabled = testState !is ApiKeyTestState.Testing && apiKeyInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
            ) {
                if (testState is ApiKeyTestState.Testing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_ai_api_key_testing))
                } else {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_ai_api_key_test))
                }
            }

            // Test feedback banner
            when (testState) {
                is ApiKeyTestState.Success -> {
                    Surface(
                        color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                        shape = AppTheme.shapes.corner12,
                        border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.ai_test_success),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                is ApiKeyTestState.Failure -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        shape = AppTheme.shapes.corner12,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = if (testState.error == "empty") stringResource(R.string.ai_test_empty_key)
                                else stringResource(R.string.ai_test_failure),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                else -> Unit
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerBottomSheet(
    selectedModel: String,
    onModelSelect: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.ai_model_selection_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            AiSettingsDataStore.AVAILABLE_MODELS.forEach { model ->
                val isSelected = model == selectedModel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppTheme.shapes.corner12)
                        .clickable {
                            onModelSelect(model)
                            onDismissRequest()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = model,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
