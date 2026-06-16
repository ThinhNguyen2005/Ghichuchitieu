package com.notepay.ui.feature.home

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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.text.style.TextAlign
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

data class NotificationUserAppInfo(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable?
)

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

private fun getInstalledUserApps(context: Context): List<NotificationUserAppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    return pm.queryIntentActivities(intent, 0).map { resolveInfo ->
        val packageName = resolveInfo.activityInfo.packageName
        val label = resolveInfo.loadLabel(pm).toString()
        val icon = try {
            resolveInfo.loadIcon(pm)
        } catch (e: Exception) {
            null
        }
        NotificationUserAppInfo(packageName, label, icon)
    }.distinctBy { it.packageName }.sortedBy { it.label }
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

    var userApps by remember { mutableStateOf<List<NotificationUserAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

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

    LaunchedEffect(context) {
        userApps = withContext(Dispatchers.IO) {
            getInstalledUserApps(context)
        }
        isLoadingApps = false
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
    var showPackageDropdown by remember { mutableStateOf(false) }

    var showCustomColorDialog by remember { mutableStateOf(false) }
    var customColorHexInput by remember { mutableStateOf(ThemeManager.customColorHex) }
    var customColorError by remember { mutableStateOf<String?>(null) }

    var filterState by remember { mutableStateOf(0) } // 0 = Tất cả, 1 = Đã loại trừ, 2 = Chưa loại trừ
    var appSearchQuery by remember { mutableStateOf("") }

    val bankPackages = remember { KnownBankApps.packages }
    val allBankPackages = remember(bankPackages, customBankApps) {
        bankPackages + customBankApps.map { it.packageName }
    }
    val userAppsWithoutBanks = remember(userApps, allBankPackages) {
        userApps.filter { it.packageName !in allBankPackages }
    }

    val filteredApps = remember(appSearchQuery, userAppsWithoutBanks, filterState, settings.excludedPackages) {
        val baseList = userAppsWithoutBanks.filter {
            appSearchQuery.isBlank() ||
                    it.label.contains(appSearchQuery, ignoreCase = true) ||
                    it.packageName.contains(appSearchQuery, ignoreCase = true)
        }

        when (filterState) {
            1 -> baseList.filter { it.packageName in settings.excludedPackages }
            2 -> baseList.filter { it.packageName !in settings.excludedPackages }
            else -> baseList
        }
    }

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
                    shape = RoundedCornerShape(16.dp)
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
                                    // Mở trang thông tin ứng dụng để cấu hình pin thủ công
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
                    shape = RoundedCornerShape(16.dp)
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
                                            .clip(CircleShape)
                                            .background(
                                                brush = if (isSystem) autoGradient else SolidColor(color),
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant,
                                                shape = CircleShape
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
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
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
                    shape = RoundedCornerShape(16.dp)
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
                    shape = RoundedCornerShape(16.dp)
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
                                        .clip(RoundedCornerShape(8.dp))
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
                                                    .clip(RoundedCornerShape(8.dp))
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

            // 5. Ứng dụng loại trừ
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Ứng dụng loại trừ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Chọn các ứng dụng thông thường mà bạn muốn NotePay hoàn toàn bỏ qua thông báo (ví dụ: Chat, Game).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = appSearchQuery,
                            onValueChange = { appSearchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp)),
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingIcon = {
                                if (appSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        playHaptic()
                                        appSearchQuery = ""
                                    }) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Xóa tìm kiếm",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                            placeholder = {
                                Text(
                                    "Tìm kiếm ứng dụng...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                            ),
                        )

                        // Bộ lọc Tab nhanh
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val filters = listOf("Tất cả", "Đã loại trừ", "Chưa loại trừ")
                            filters.forEachIndexed { index, label ->
                                val isSelected = filterState == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerLow
                                        )
                                        .clickable {
                                            playHaptic()
                                            filterState = index
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Danh sách kết quả ứng dụng
                        if (isLoadingApps) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (filteredApps.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Không tìm thấy ứng dụng phù hợp.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            // Hiển thị tối đa các app đã lọc
                            filteredApps.forEach { app ->
                                val isExcluded = settings.excludedPackages.contains(app.packageName)
                                val imageBitmap = remember(app.icon) {
                                    app.icon?.toBitmap()?.asImageBitmap()
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            playHaptic()
                                            val current = settings.excludedPackages
                                            val updated = if (!isExcluded) {
                                                current + app.packageName
                                            } else {
                                                current - app.packageName
                                            }
                                            viewModel.setExcludedPackages(updated)
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
                                        if (imageBitmap != null) {
                                            Image(
                                                bitmap = imageBitmap,
                                                contentDescription = app.label,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                                contentDescription = app.label,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = app.label,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = app.packageName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isExcluded,
                                        onCheckedChange = { checked ->
                                            playHaptic()
                                            val current = settings.excludedPackages
                                            val updated = if (checked) {
                                                current + app.packageName
                                            } else {
                                                current - app.packageName
                                            }
                                            viewModel.setExcludedPackages(updated)
                                        }
                                    )
                                }
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
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Thêm ứng dụng ngân hàng", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Nhập thông tin hoặc chọn nhanh ứng dụng tài chính từ các app đã cài trên máy của bạn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Gợi ý app cài trên máy bằng LazyRow
                    if (userAppsWithoutBanks.isNotEmpty()) {
                        Text(
                            "Chọn nhanh từ các app đã cài:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(userAppsWithoutBanks) { app ->
                                val appIcon = remember(app.icon) {
                                    app.icon?.toBitmap()?.asImageBitmap()
                                }
                                val isSelected = customAppPackage == app.packageName

                                Card(
                                    onClick = {
                                        playHaptic()
                                        customAppPackage = app.packageName
                                        customAppLabel = app.label
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.width(80.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .fillMaxWidth()
                                    ) {
                                        if (appIcon != null) {
                                            Image(
                                                bitmap = appIcon,
                                                contentDescription = app.label,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                                contentDescription = app.label,
                                                modifier = Modifier.size(36.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customAppPackage,
                            onValueChange = {
                                customAppPackage = it
                                showPackageDropdown = true
                            },
                            label = { Text("Tên gói (Package Name)") },
                            placeholder = { Text("vd: com.example.banking") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    playHaptic()
                                    showPackageDropdown = !showPackageDropdown
                                }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Chọn ứng dụng")
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = showPackageDropdown && userAppsWithoutBanks.isNotEmpty(),
                            onDismissRequest = { showPackageDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .heightIn(max = 240.dp)
                        ) {
                            val filteredDropdownApps = userAppsWithoutBanks.filter {
                                customAppPackage.isBlank() ||
                                        it.label.contains(customAppPackage, ignoreCase = true) ||
                                        it.packageName.contains(customAppPackage, ignoreCase = true)
                            }
                            if (filteredDropdownApps.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Không tìm thấy app cài đặt nào") },
                                    onClick = { showPackageDropdown = false }
                                )
                            } else {
                                filteredDropdownApps.forEach { app ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(app.label, fontWeight = FontWeight.Bold)
                                                Text(
                                                    app.packageName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            playHaptic()
                                            customAppPackage = app.packageName
                                            customAppLabel = app.label
                                            showPackageDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customAppLabel,
                        onValueChange = { customAppLabel = it },
                        label = { Text("Tên hiển thị (Label)") },
                        placeholder = { Text("vd: Ngân hàng của tôi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                                .clip(CircleShape)
                                .background(previewColor)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
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
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isPresetSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                            )
                                            .border(
                                                width = if (isPresetSelected) 2.dp else 1.dp,
                                                color = if (isPresetSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(8.dp)
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
                                                    .clip(CircleShape)
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
