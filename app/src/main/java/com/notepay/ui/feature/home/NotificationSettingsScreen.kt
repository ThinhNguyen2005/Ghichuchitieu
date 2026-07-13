package com.notepay.ui.feature.home

import com.notepay.ui.theme.AppTheme

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.data.preferences.KnownBankApp
import com.notepay.data.preferences.KnownBankApps
import com.notepay.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.toColorInt
import android.os.Build
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.app.NotificationCompat

data class BankAppUiModel(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val isInstalled: Boolean,
    val isCustom: Boolean
)

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
        .setSmallIcon(android.R.drawable.ic_dialog_info)
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
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    manager.notify(1002, notification)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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
            compareByDescending<BankAppUiModel> { settings.enabledPackages.contains(it.packageName) }
                .thenBy { it.label }
        )
    }

    var showAddCustomAppDialog by remember { mutableStateOf(false) }
    var customAppLabel by remember { mutableStateOf("") }
    var customAppPackage by remember { mutableStateOf("") }

    var showCustomColorDialog by remember { mutableStateOf(false) }
    var customColorHexInput by remember { mutableStateOf(ThemeManager.customColorHex) }
    var customColorError by remember { mutableStateOf<String?>(null) }

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

            // 2. Màu sắc chủ đạo
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
                        Text(
                            "Màu sắc chủ đạo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Chọn màu sắc đại diện chính cho toàn bộ giao diện của ứng dụng.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val themeOptions = remember(ThemeManager.customColorHex) {
                            listOf(
                                "system" to Color.Gray,
                                "green" to Color(0xFF1B7F4F),
                                "blue" to Color(0xFF1976D2),
                                "red" to Color(0xFFC2185B),
                                "orange" to Color(0xFFE65100),
                                "teal" to Color(0xFF00796B),
                                "gold" to Color(0xFF8A6600),
                                "brown" to Color(0xFF8D4F38),
                                "gray" to Color(0xFF566066),
                                "custom" to Color(ThemeManager.customColorHex.toColorInt())
                            )
                        }

                        val autoGradient = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1B7F4F),
                                Color(0xFF1976D2),
                                Color(0xFFC2185B)
                            )
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(themeOptions) { (key, color) ->
                                val isSelected = ThemeManager.currentThemeColor == key
                                val isSystem = key == "system"
                                val isCustom = key == "custom"

                                Box(
                                    modifier = Modifier.size(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(AppTheme.shapes.circle)
                                            .background(
                                                brush = if (isSystem) autoGradient else SolidColor(color),
                                                shape = AppTheme.shapes.circle
                                            )
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant,
                                                shape = AppTheme.shapes.circle
                                            )
                                            .clickable {
                                                playHaptic()
                                                if (isCustom) {
                                                    ThemeManager.updateThemeColor(context, "custom")
                                                    showCustomColorDialog = true
                                                } else {
                                                    ThemeManager.updateThemeColor(context, key)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSystem) {
                                            Text(
                                                text = "A",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        } else if (isCustom) {
                                            Icon(
                                                imageVector = Icons.Rounded.Palette,
                                                contentDescription = "Tùy chỉnh màu",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .align(Alignment.BottomEnd)
                                                .background(MaterialTheme.colorScheme.primary, AppTheme.shapes.circle)
                                                .border(1.5.dp, MaterialTheme.colorScheme.surface, AppTheme.shapes.circle),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Tự động ghi chép giao dịch
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
                                viewModel.setAutoCaptureEnabled(!settings.autoCaptureEnabled)
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Tự động ghi chép giao dịch",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Bật/tắt đọc thông báo giao dịch ngân hàng và ví điện tử tự động.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoCaptureEnabled,
                            onCheckedChange = {
                                playHaptic()
                                viewModel.setAutoCaptureEnabled(it)
                            }
                        )
                    }
                }
            }

            // 4. Danh sách ngân hàng
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
                            TextButton(
                                onClick = {
                                    playHaptic()
                                    showAddCustomAppDialog = true
                                }
                            ) {
                                Text("+ Thêm app thủ công", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Text(
                            "Bật/tắt nhận diện thông báo từ các app ngân hàng đã cài đặt dưới đây.",
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
                                val isEnabled = settings.enabledPackages.contains(app.packageName)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AppTheme.shapes.corner8)
                                        .clickable {
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
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Gửi test", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            // --- MoMo — Đã kiểm thử ---
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
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Đã xác minh",
                                            tint = MaterialTheme.colorScheme.primary,
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
                                        text = "Đã kiểm thử — Push notification format",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        playHaptic()
                                        simulateMomoNotification(context)
                                    },
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Gửi test", style = MaterialTheme.typography.labelMedium)
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
                                    text = "Các ngân hàng khác (VCB, Techcombank, BIDV…) chưa được kiểm thử đầy đủ. Kết quả đọc thông báo có thể không chính xác.",
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

    if (showCustomColorDialog) {
        AlertDialog(
            onDismissRequest = {
                showCustomColorDialog = false
                customColorHexInput = ThemeManager.customColorHex
                customColorError = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Tự chọn màu sắc chủ đạo", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Nhập mã màu HEX hoặc chọn từ các preset màu sắc cao cấp được đề xuất.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Xem trước màu và ô nhập
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val previewColor = remember(customColorHexInput) {
                            try {
                                Color(customColorHexInput.toColorInt())
                            } catch (e: Exception) {
                                Color.Gray
                            }
                        }

                        // Preview Circle
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(AppTheme.shapes.circle)
                                .background(previewColor)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, AppTheme.shapes.circle)
                        )

                        // Hex Input
                        OutlinedTextField(
                            value = customColorHexInput,
                            onValueChange = { input ->
                                val formatted = if (input.startsWith("#")) input else "#$input"
                                customColorHexInput = formatted.take(7)

                                customColorError = if (!formatted.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                                    "Mã màu HEX không hợp lệ (vd: #E91E63)"
                                } else {
                                    null
                                }
                            },
                            label = { Text("Mã màu HEX") },
                            placeholder = { Text("#1B7F4F") },
                            singleLine = true,
                            isError = customColorError != null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (customColorError != null) {
                        Text(
                            text = customColorError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        "Preset màu sắc premium:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val premiumPresets = listOf(
                        "Amethyst" to "#9C27B0",
                        "Cobalt" to "#0047AB",
                        "Emerald" to "#50C878",
                        "Hổ phách" to "#FFBF00",
                        "Coral" to "#FF6F61",
                        "Mint" to "#66CDAA",
                        "Ruby" to "#E0115F",
                        "Sapphire" to "#0F52BA",
                        "Rose" to "#FF007F",
                        "Sky Blue" to "#00B0FF",
                        "Đồng Cỏ" to "#4F7942",
                        "Đất Nung" to "#E2725B"
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 0 until 3) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (j in 0 until 4) {
                                    val idx = i * 4 + j
                                    val (name, hex) = premiumPresets[idx]
                                    val isPresetSelected = customColorHexInput.equals(hex, ignoreCase = true)
                                    val colorObj = Color(hex.toColorInt())

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(AppTheme.shapes.corner8)
                                            .background(
                                                if (isPresetSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                            )
                                            .border(
                                                width = if (isPresetSelected) 2.dp else 1.dp,
                                                color = if (isPresetSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                shape = AppTheme.shapes.corner8
                                            )
                                            .clickable {
                                                playHaptic()
                                                customColorHexInput = hex
                                                customColorError = null
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(AppTheme.shapes.circle)
                                                    .background(colorObj)
                                            )
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        playHaptic()
                        if (customColorError == null && customColorHexInput.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                            ThemeManager.updateCustomColor(context, customColorHexInput)
                            ThemeManager.updateThemeColor(context, "custom")
                            showCustomColorDialog = false
                        }
                    },
                    enabled = customColorError == null && customColorHexInput.isNotBlank()
                ) {
                    Text("Áp dụng", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        playHaptic()
                        showCustomColorDialog = false
                        customColorHexInput = ThemeManager.customColorHex
                        customColorError = null
                    }
                ) {
                    Text("Đóng")
                }
            }
        )
    }
}