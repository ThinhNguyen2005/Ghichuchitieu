package com.notepay.ui.feature.home

import com.notepay.ui.theme.AppTheme

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.data.preferences.KnownBankApp
import com.notepay.data.preferences.KnownBankApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.Build
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.app.NotificationCompat
import com.notepay.ai.LocalModelInstallStatus
import com.notepay.R
import com.notepay.ai.LocalModelState

data class BankAppUiModel(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val isInstalled: Boolean,
    val isCustom: Boolean
)

private const val DEFAULT_LOCAL_MODEL_PAGE = "https://huggingface.co/litert-community/Qwen3-0.6B-int4/tree/main"

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
                        "Dùng khi Gemini Nano không khả dụng. NotePay chỉ nhập file đã tải, không xin quyền Internet.",
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
                                else -> "Khuyến nghị: Qwen3 0.6B INT4 bản .litertlm cho máy tầm trung."
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

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(context.packageName)
}

private fun isBankAppInstalled(context: Context, primaryPackageName: String): Boolean {
    val packagesToCheck = KnownBankApps.equivalentPackages[primaryPackageName] ?: listOf(primaryPackageName)
    val pm = context.packageManager
    for (pkg in packagesToCheck) {
        try {
            pm.getPackageInfo(pkg, 0)
            return true
        } catch (e: Exception) {
            // ignore
        }
    }
    return false
}

private fun getInstalledBankAppIcon(context: Context, primaryPackageName: String): android.graphics.drawable.Drawable? {
    val packagesToCheck = KnownBankApps.equivalentPackages[primaryPackageName] ?: listOf(primaryPackageName)
    val pm = context.packageManager
    for (pkg in packagesToCheck) {
        try {
            return pm.getApplicationIcon(pkg)
        } catch (e: Exception) {
            // ignore
        }
    }
    return null
}

private fun simulateTpBankNotification(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "tpbank_simulation_channel"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Giả lập TPBank (Debug)", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
    }
    val inboxStyle = NotificationCompat.InboxStyle()
        .addLine("(TPBank): 14/06/26;06:25")
        .addLine("TK: xxxx5539020")
        .addLine("PS:-30.000VND")
        .addLine("SD: 410.054VND")
        .addLine("SD KHA DUNG: 410.054VND")
        .addLine("ND: NAP TIEN VI MOMO - 0945553902")
        .addLine("- 133366724699")
        .addLine("SO GD: 661TTMB261662918")
    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("TPBank Mobile")
        .setContentText("(TPBank): 14/06/26;06:25...")
        .setStyle(inboxStyle)
        .setSmallIcon(com.notepay.R.drawable.ic_stat_notepay)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    manager.notify(1001, notification)
}

private fun simulateMomoNotification(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "momo_simulation_channel"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Giả lập MoMo (Debug)", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("MoMo")
        .setContentText("+50.000đ Nguyễn Văn A chuyển tiền. Số dư: 1.250.000đ. Lúc 14:30 17/06/2026")
        .setSmallIcon(com.notepay.R.drawable.ic_stat_notepay)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    manager.notify(1002, notification)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val localModel by viewModel.localModel.collectAsStateWithLifecycle()
    val modelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::importLocalAiModel)
    }

    var isListenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var isBatteryOptimizationsIgnored by remember {
        mutableStateOf(
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .isIgnoringBatteryOptimizations(context.packageName)
        )
    }

    // Refresh permission states when returning to screen
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isListenerEnabled = isNotificationListenerEnabled(context)
                isBatteryOptimizationsIgnored = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .isIgnoringBatteryOptimizations(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val customBankApps = remember(settings.customBankApps) {
        settings.customBankApps.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 2) {
                KnownBankApp(parts[0], parts[1])
            } else {
                null
            }
        }
    }

    var bankAppUiList by remember { mutableStateOf<List<BankAppUiModel>>(emptyList()) }
    var isLoadingBankApps by remember { mutableStateOf(true) }

    LaunchedEffect(context, customBankApps) {
        isLoadingBankApps = true
        bankAppUiList = withContext(Dispatchers.IO) {
            val defaultApps = KnownBankApps.displayApps
            val allAppsToCheck = (defaultApps + customBankApps).distinctBy { it.packageName }
            allAppsToCheck.mapNotNull { app ->
                val isInstalled = isBankAppInstalled(context, app.packageName)
                if (isInstalled) {
                    val iconDrawable = getInstalledBankAppIcon(context, app.packageName)
                    val iconBitmap = iconDrawable?.toBitmap()?.asImageBitmap()
                    val isCustom = customBankApps.any { it.packageName == app.packageName }
                    BankAppUiModel(
                        packageName = app.packageName,
                        label = app.label,
                        icon = iconBitmap,
                        isInstalled = true,
                        isCustom = isCustom
                    )
                } else if (customBankApps.any { it.packageName == app.packageName }) {
                    val isCustom = true
                    val iconDrawable = getInstalledBankAppIcon(context, app.packageName)
                    val iconBitmap = iconDrawable?.toBitmap()?.asImageBitmap()
                    BankAppUiModel(
                        packageName = app.packageName,
                        label = app.label,
                        icon = iconBitmap,
                        isInstalled = false,
                        isCustom = isCustom
                    )
                } else {
                    null
                }
            }
        }
        isLoadingBankApps = false
    }

    val sortedBankApps = remember(bankAppUiList, settings.enabledPackages) {
        bankAppUiList.sortedWith(
            compareByDescending<BankAppUiModel> { KnownBankApps.isSupported(it.packageName) }
                .thenByDescending { settings.enabledPackages.contains(it.packageName) }
                .thenBy { it.label }
        )
    }

    val changeAutoCapture: (Boolean) -> Unit = { enabled ->
        if (enabled && !isListenerEnabled) {
            // Persist the user's intent first. Once Android grants listener access,
            // the service can start without requiring a second tap on this switch.
            viewModel.setAutoCaptureEnabled(true)
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } else {
            viewModel.setAutoCaptureEnabled(enabled)
        }
    }

    var showAddCustomAppDialog by remember { mutableStateOf(false) }
    var customAppLabel by remember { mutableStateOf("") }
    var customAppPackage by remember { mutableStateOf("") }

    fun playHaptic() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cấu hình đọc thông báo", fontWeight = FontWeight.Bold) },
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
            // 1. Quyền hệ thống & Trạng thái chạy ngầm
            item {
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
                        Text(
                            "Quyền hệ thống & Chạy ngầm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Quyền truy cập thông báo
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playHaptic()
                                    if (!isListenerEnabled) {
                                        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                        context.startActivity(intent)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Quyền truy cập thông báo", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (isListenerEnabled) "Đã cấp quyền" else "Chưa được cấp quyền (Nhấn để bật)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isListenerEnabled) Color(0xFF1B7F4F) else MaterialTheme.colorScheme.error
                                )
                            }
                            Icon(
                                imageVector = if (isListenerEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                contentDescription = null,
                                tint = if (isListenerEnabled) Color(0xFF1B7F4F) else MaterialTheme.colorScheme.error
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Quyền tối ưu hóa pin
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playHaptic()
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Chế độ pin chạy ngầm", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (isBatteryOptimizationsIgnored) "Đang cấu hình: Không hạn chế" else "Đang cấu hình: Tối ưu hóa (Nhấn để chuyển sang Không hạn chế)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isBatteryOptimizationsIgnored) Color(0xFF1B7F4F) else MaterialTheme.colorScheme.error
                                )
                            }
                            Icon(
                                imageVector = if (isBatteryOptimizationsIgnored) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                contentDescription = null,
                                tint = if (isBatteryOptimizationsIgnored) Color(0xFF1B7F4F) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

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
            // 2. Tự động ghi chép giao dịch
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = AppTheme.shapes.corner16
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playHaptic()
                                changeAutoCapture(!settings.autoCaptureEnabled)
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Nhận diện giao dịch ngân hàng",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                when {
                                    !settings.autoCaptureEnabled -> "Đang tắt — NotePay không xử lý thông báo ngân hàng."
                                    !isListenerEnabled -> "Đang tạm dừng — cần cấp quyền truy cập thông báo."
                                    else -> "Đang bật — hiện chỉ tự động nhận diện TPBank."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoCaptureEnabled,
                            onCheckedChange = {
                                playHaptic()
                                changeAutoCapture(it)
                            }
                        )
                    }
                }
            }

            // 3. Danh sách ngân hàng
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = AppTheme.shapes.corner16
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Nhận diện ngân hàng",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    "TPBank đã hỗ trợ",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Text(
                            "TPBank đã được kiểm thử. Các ngân hàng và ví khác chỉ được hiển thị để nhận biết, chưa tham gia tự động ghi giao dịch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (isLoadingBankApps) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        } else if (sortedBankApps.isEmpty()) {
                            Text(
                                "Không phát hiện ứng dụng ngân hàng nào được cài đặt.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            sortedBankApps.forEach { app ->
                                val isSupported = KnownBankApps.isSupported(app.packageName)
                                val isEnabled = isSupported && settings.enabledPackages.contains(app.packageName)
                                val canToggle = isSupported && settings.autoCaptureEnabled

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AppTheme.shapes.corner8)
                                        .clickable(enabled = canToggle) {
                                            playHaptic()
                                            viewModel.setPackageEnabled(app.packageName, !isEnabled)
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (app.icon != null) {
                                            Image(
                                                bitmap = app.icon,
                                                contentDescription = app.label,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(AppTheme.shapes.corner8)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.AccountBalance,
                                                contentDescription = app.label,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(40.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = app.label,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = when {
                                                    !isSupported -> "Chưa khả dụng"
                                                    !settings.autoCaptureEnabled -> "Tạm dừng theo cài đặt chung"
                                                    isEnabled -> "Đang nhận diện"
                                                    else -> "Đã tắt cho TPBank"
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = when {
                                                    !isSupported -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    isEnabled && settings.autoCaptureEnabled -> Color(0xFF1B7F4F)
                                                    else -> MaterialTheme.colorScheme.error
                                                },
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = app.packageName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (app.isCustom) {
                                            IconButton(
                                                onClick = {
                                                    playHaptic()
                                                    viewModel.removeCustomBankApp(app.packageName)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Delete,
                                                    contentDescription = "Xóa app thủ công",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = isEnabled,
                                            enabled = canToggle,
                                            onCheckedChange = {
                                                playHaptic()
                                                viewModel.setPackageEnabled(app.packageName, it)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (com.notepay.BuildConfig.DEBUG) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = AppTheme.shapes.corner16
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header vùng Debug
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Science,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Kiểm thử thông báo (DEBUG)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Text(
                                text = "Gửi thông báo giả lập để kiểm tra khả năng đọc và phân tích giao dịch từ các ngân hàng hệ thống.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // --- TPBank — Đã kiểm thử ---
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp) // Khoảng cách nhỏ giữa icon và chữ
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Đã xác minh",
                                            tint = MaterialTheme.colorScheme.primary, // Dùng màu Primary chuẩn M3 đại diện cho Success/Verified
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "TPBank",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Đã kiểm thử — InboxStyle đa dòng",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        playHaptic()
                                        simulateTpBankNotification(context)
                                    },
                                    enabled = settings.autoCaptureEnabled &&
                                        isListenerEnabled &&
                                        settings.enabledPackages.contains(KnownBankApps.TPBANK_PACKAGE),
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Gửi test", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            // MoMo parser is not enabled until its real-device format is verified.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.HourglassDisabled,
                                            contentDescription = "Chưa khả dụng",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "MoMo",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Chưa khả dụng — không tự động ghi giao dịch",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        playHaptic()
                                        simulateMomoNotification(context)
                                    },
                                    enabled = false,
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Sắp hỗ trợ", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // --- Khu vực Cảnh báo (Thay thế emoji bằng Row + Icon chuyên dụng) ---
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = "Cảnh báo",
                                    tint = MaterialTheme.colorScheme.error, // Đồng bộ màu lỗi/cảnh báo hệ thống
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Ngoài TPBank, mọi ngân hàng và ví điện tử đều bị chặn ở tầng xử lý cho đến khi được kiểm thử chính xác.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCustomAppDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddCustomAppDialog = false
                customAppLabel = ""
                customAppPackage = ""
            },
            // M3 đề xuất bo góc lớn (28.dp) cho Dialog để tạo cảm giác ôm sát, hiện đại
            shape = RoundedCornerShape(28.dp),
            // M3 sử dụng thuộc tính icon riêng biệt nằm phía trên tiêu đề
            icon = {
                Icon(
                    imageVector = Icons.Rounded.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Thêm ứng dụng ngân hàng",
                    style = MaterialTheme.typography.headlineSmall, // Font style chuẩn M3
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Theo dõi thông báo biến độ số dư từ thông báo ngân hàng, hoạt động local 100% dữ liệu không ra khỏi máy của bạn..",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // --- Ô NHẬP: TÊN GÓI (PACKAGE NAME) ---
                    OutlinedTextField(
                        value = customAppPackage,
                        onValueChange = { customAppPackage = it },
                        label = { Text("Tên gói ứng dụng") },
                        placeholder = { Text("vd: com.VCB") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        // Bo góc 16.dp chuẩn cấu trúc Shape M3 cho ô nhập liệu
                        shape = AppTheme.shapes.corner16,
                        // Icon AppShortcut: Biểu tượng một app nhỏ nằm trong điện thoại, cực chuẩn cho "Package Name"
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.AppShortcut,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Unspecified, autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next,platformImeOptions = null, showKeyboardOnFocus = null,hintLocales = null)
                    )

                    // --- Ô NHẬP: TÊN ỨNG DỤNG NGÂN HÀNG ---
                    OutlinedTextField(
                        value = customAppLabel,
                        onValueChange = { customAppLabel = it },
                        label = { Text("Tên ứng dụng ngân hàng") },
                        placeholder = { Text("vd: Vietcombank") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppTheme.shapes.corner16,
                        // Icon Label của Material 3 có đường nét bo tròn mềm mại hơn bản cũ
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Label,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        playHaptic()
                        if (customAppPackage.isNotBlank() && customAppLabel.isNotBlank()) {
                            viewModel.addCustomBankApp(customAppPackage.trim(), customAppLabel.trim())
                            showAddCustomAppDialog = false
                            customAppLabel = ""
                            customAppPackage = ""
                        }
                    }
                ) {
                    Text("Thêm mới", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        playHaptic()
                        showAddCustomAppDialog = false
                        customAppLabel = ""
                        customAppPackage = ""
                    }
                ) {
                    Text("Đóng")
                }
            }
        )
    }
}
