package com.notepay.ui.feature.home

import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.ai.LocalModelInstallStatus
import com.notepay.ai.LocalModelState
import com.notepay.ui.theme.AppTheme

private const val DEFAULT_LOCAL_MODEL_PAGE = "https://github.com/google-ai-edge/LiteRT-LM#supported-models-and-performance"

private fun formatModelSize(sizeBytes: Long): String {
    if (sizeBytes <= 0L) return "không rõ dung lượng"
    val mb = sizeBytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        "%.1f GB".format(mb / 1024.0)
    } else {
        "%.0f MB".format(mb)
    }
}

@Composable
private fun LocalAiModelSettingsCard(
    localModel: LocalModelState,
    onOpenModelPage: () -> Unit,
    onPickModel: () -> Unit,
    onRemoveModel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
        ),
        shape = AppTheme.shapes.corner16
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AI cục bộ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Dùng khi Gemini Nano không khả dụng. NotePay chỉ nhập file đã tải, xử lý offline hoàn toàn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = when (localModel.status) {
                            LocalModelInstallStatus.READY -> Icons.Rounded.CheckCircle
                            LocalModelInstallStatus.IMPORTING -> Icons.Rounded.Downloading
                            LocalModelInstallStatus.ERROR -> Icons.Rounded.Error
                            LocalModelInstallStatus.NOT_INSTALLED -> Icons.Rounded.CloudOff
                        },
                        contentDescription = null,
                        tint = when (localModel.status) {
                            LocalModelInstallStatus.READY -> Color(0xFF1B7F4F)
                            LocalModelInstallStatus.ERROR -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (localModel.status) {
                                LocalModelInstallStatus.READY ->
                                    localModel.displayName ?: "Mô hình AI cục bộ"
                                LocalModelInstallStatus.IMPORTING -> "Đang cài mô hình"
                                LocalModelInstallStatus.ERROR -> "Chưa cài được mô hình"
                                LocalModelInstallStatus.NOT_INSTALLED -> "Chưa có mô hình"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                localModel.status == LocalModelInstallStatus.READY ->
                                    "Đã lưu trong bộ nhớ riêng · ${formatModelSize(localModel.sizeBytes)}"
                                localModel.status == LocalModelInstallStatus.IMPORTING ->
                                    "Đang sao chép vào bộ nhớ riêng của ứng dụng"
                                localModel.message != null -> localModel.message.orEmpty()
                                else -> "Chỉ chọn tệp .litertlm từ danh sách tương thích chính thức."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (localModel.status == LocalModelInstallStatus.IMPORTING) {
                val progress = localModel.progress
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenModelPage,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Trang tải")
                }
                Button(
                    onClick = onPickModel,
                    enabled = localModel.status != LocalModelInstallStatus.IMPORTING,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (localModel.status == LocalModelInstallStatus.READY) "Đổi model" else "Chọn file")
                }
            }

            if (localModel.status == LocalModelInstallStatus.READY) {
                TextButton(
                    onClick = onRemoveModel,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Xóa mô hình khỏi máy")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    onNavigateToBackupRestore: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val localModel by viewModel.localModel.collectAsStateWithLifecycle()

    val modelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importLocalAiModel(uri)
        }
    }

    fun playHaptic() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt ứng dụng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        playHaptic()
                        onBack()
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
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
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Màu sắc chủ đề (Theme Color)
            item {
                ThemeSettingsCard(onPlayHaptic = ::playHaptic)
            }

            // 2. Mô hình AI cục bộ
            item {
                LocalAiModelSettingsCard(
                    localModel = localModel,
                    onOpenModelPage = {
                        playHaptic()
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(DEFAULT_LOCAL_MODEL_PAGE)
                            )
                        )
                    },
                    onPickModel = {
                        playHaptic()
                        modelPicker.launch(arrayOf("application/octet-stream", "application/zip", "*/*"))
                    },
                    onRemoveModel = {
                        playHaptic()
                        viewModel.removeLocalAiModel()
                    },
                )
            }

            // 3. Sao lưu & Khôi phục dữ liệu
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            playHaptic()
                            onNavigateToBackupRestore()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = AppTheme.shapes.corner16,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_backup_restore_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.settings_backup_restore_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSettingsCard(
    onPlayHaptic: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = AppTheme.shapes.corner16
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Giao diện & Màu sắc",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Tùy chỉnh tông màu chính cho ứng dụng NotePay.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
