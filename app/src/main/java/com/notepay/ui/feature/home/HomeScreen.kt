package com.notepay.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.model.Money
import com.notepay.ui.component.BalanceCard
import com.notepay.ui.component.EmptyState
import com.notepay.ui.component.EmptyStateWithAction
import com.notepay.ui.component.KpiRow
import com.notepay.ui.component.TransactionItem
import com.notepay.ui.theme.NotePayTheme

import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Payments
import com.notepay.domain.model.Wallet
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.rounded.Check
import com.notepay.ui.theme.ThemeManager


import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.IconButton
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.core.graphics.drawable.toBitmap
import com.notepay.data.preferences.KnownBankApps
import com.notepay.data.preferences.KnownBankApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.feature.home.BudgetProjection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onSeeAll: () -> Unit,
    onAddWallet: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    navigationBarOffset: Float = 0f,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showWalletSwitcher by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isListenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isListenerEnabled = isNotificationListenerEnabled(context)
                com.notepay.service.NotePayNotificationListenerService.heal(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToReminders) {
                        BadgedBox(
                            badge = {
                                if (state.dueRemindersCount > 0) {
                                    Badge {
                                        Text(state.dueRemindersCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (state.dueRemindersCount > 0) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                                contentDescription = "Nhắc nhở gia hạn"
                            )
                        }
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Cài đặt thông báo"
                        )
                    }
                }
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(layoutDirection)
                ),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = padding.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                if (state.wallets.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        EmptyStateWithAction(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            title = "Chưa có ví nào",
                            description = "Tạo ví để bắt đầu ghi chép giao dịch và quản lý chi tiêu.",
                            actionLabel = "Tạo ví",
                            onClick = { showWalletSwitcher = true },
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        BalanceCard(
                            wallet = state.activeWallet,
                            balance = state.currentBalance,
                            onClick = { showWalletSwitcher = true },
                            monthlyExpense = state.monthlyExpense,
                        )
                        val budgetProjection = state.budgetProjection
                        val activeWallet = state.activeWallet
                        if (budgetProjection != null && activeWallet?.budgetLimit != null) {
                            BudgetAnalysisCard(
                                projection = budgetProjection,
                                budgetLimit = activeWallet.budgetLimit
                            )
                        }
                    }
                }
            }
            if (!isListenerEnabled) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                            try {
                                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tự động ghi chép giao dịch",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Nhấp vào đây để cấp quyền đọc thông báo ngân hàng và Momo (Local Parse).",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "💡 Mẹo: Nếu công tắc bị xám màu (Cài đặt bị hạn chế), hãy vào Thông tin ứng dụng NotePay -> Bấm dấu 3 chấm góc trên bên phải -> Chọn \"Cho phép cài đặt bị hạn chế\" rồi thử lại.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Đang tự động ghi chép",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                                Text(
                                    text = "Sẵn sàng đọc thông báo giao dịch.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                )
                            }
                            if (com.notepay.BuildConfig.DEBUG) {
                                TextButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            androidx.core.content.ContextCompat.checkSelfPermission(
                                                context,
                                                android.Manifest.permission.POST_NOTIFICATIONS
                                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                        ) {
                                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            simulateTpBankNotification(context)
                                        }
                                    }
                                ) {
                                    Text("Gửi test")
                                }
                            }
                        }
                    }
                }
            }
            item {
                KpiRow(
                    income = state.monthlyIncome,
                    expense = state.monthlyExpense,
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Giao dịch gần đây", style = MaterialTheme.typography.titleMedium)
                    androidx.compose.material3.TextButton(onClick = onSeeAll) { Text("Xem tất cả") }
                }
            }
            if (state.recentTransactions.isEmpty()) {
                item { EmptyState(message = "Chưa có giao dịch nào. Nhấn Thêm để bắt đầu.") }
            } else {
                items(state.recentTransactions, key = { it.id }) { tx ->
                    val walletName = state.wallets.find { it.id == tx.walletId }?.name ?: ""
                    TransactionItem(
                        transaction = tx,
                        walletName = walletName,
                        onClick = { onTransactionClick(tx.id) },
                    )
                }
            }
            item { Spacer(Modifier.height(160.dp)) }
        }
    }

    if (showWalletSwitcher) {
        AlertDialog(
            onDismissRequest = { showWalletSwitcher = false },
            title = { Text("Chọn ví tài chính") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.wallets) { wallet ->
                        val isSelected = wallet.id == state.activeWallet?.id
                        val iconVector = when (wallet.iconKey) {
                            Wallet.ICON_CASH -> Icons.Rounded.Payments
                            Wallet.ICON_BANK -> Icons.Rounded.AccountBalance
                            "momo" -> Icons.Rounded.AccountBalanceWallet
                            Wallet.ICON_CARD -> Icons.Rounded.CreditCard
                            else -> Icons.Rounded.Payments
                        }
                        val tintColor = when (wallet.colorKey) {
                            Wallet.COLOR_PRIMARY -> MaterialTheme.colorScheme.primary
                            Wallet.COLOR_SECONDARY -> MaterialTheme.colorScheme.secondary
                            Wallet.COLOR_TERTIARY -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    viewModel.selectWallet(wallet.id)
                                    showWalletSwitcher = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = tintColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = wallet.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showWalletSwitcher = false
                                    onAddWallet()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Thêm ví mới",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWalletSwitcher = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false },
            viewModel = viewModel
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NotePayTheme {
        Column(Modifier.padding(16.dp)) {
            BalanceCard(
                wallet = Wallet(id = 1L, name = "Tiền mặt", initialBalance = Money(1_500_000_00), iconKey = "cash", colorKey = "primary"),
                balance = Money(1_500_000_00)
            )
            Spacer(Modifier.height(16.dp))
            KpiRow(income = Money(5_000_000_00), expense = Money(3_500_000_00))
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(context.packageName)
}

private fun simulateTpBankNotification(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channelId = "tpbank_simulation_channel"
    val channelName = "Giả lập TPBank (Debug)"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(
            channelId,
            channelName,
            android.app.NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    val title = "TPBank Mobile"

    val inboxStyle = androidx.core.app.NotificationCompat.InboxStyle()
        .addLine("(TPBank): 14/06/26;06:25")
        .addLine("TK: xxxx5539020")
        .addLine("PS:-30.000VND")
        .addLine("SD: 410.054VND")
        .addLine("SD KHA DUNG: 410.054VND")
        .addLine("ND: NAP TIEN VI MOMO - 0945553902")
        .addLine("- 133366724699")
        .addLine("SO GD: 661TTMB261662918")

    val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
        .setContentTitle(title)
        .setContentText("(TPBank): 14/06/26;06:25...")
        .setStyle(inboxStyle)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
        .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .build()

    manager.notify(1001, notification)
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

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    viewModel: HomeViewModel,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var userApps by remember { mutableStateOf<List<UserAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    LaunchedEffect(context) {
        userApps = withContext(Dispatchers.IO) {
            getInstalledUserApps(context)
        }
        isLoadingApps = false
    }

    val installedBankApps = remember(context) {
        KnownBankApps.displayApps.filter { app ->
            isBankAppInstalled(context, app.packageName)
        }
    }

    var filterState by remember { mutableStateOf(0) } // 0 = Tất cả, 1 = Đã loại trừ, 2 = Chưa loại trừ
    var appSearchQuery by remember { mutableStateOf("") }

    val bankPackages = remember { KnownBankApps.packages }
    val userAppsWithoutBanks = remember(userApps, bankPackages) {
        userApps.filter { it.packageName !in bankPackages }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Cấu hình đọc thông báo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 0. Màu sắc ứng dụng
                item {
                    Text(
                        "Màu sắc chủ đạo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val themeOptions = listOf(
                        "system" to Color.Gray,
                        "green" to Color(0xFF1B7F4F),
                        "blue" to Color(0xFF1976D2),
                        "red" to Color(0xFFC2185B),
                        "orange" to Color(0xFFE65100),
                        "teal" to Color(0xFF00796B),
                        "gold" to Color(0xFF8A6600),
                        "brown" to Color(0xFF8D4F38),
                        "gray" to Color(0xFF566066)
                    )
                    
                    val autoGradient = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1B7F4F),
                            Color(0xFF1976D2),
                            Color(0xFFC2185B)
                        )
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(themeOptions) { (key, color) ->
                            val isSelected = ThemeManager.currentThemeColor == key
                            val isSystem = key == "system"
                            
                            Box(
                                modifier = Modifier
                                    .size(48.dp),
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
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.outlineVariant,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            ThemeManager.updateThemeColor(context, key)
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 16.dp))
                }

                // 1. Tự động ghi chép giao dịch
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Tự động ghi chép giao dịch",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Bật/tắt đọc thông báo giao dịch ngân hàng và ví điện tử.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoCaptureEnabled,
                            onCheckedChange = { viewModel.setAutoCaptureEnabled(it) }
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 16.dp))
                }

                // 2. Danh sách ngân hàng
                item {
                    Text(
                        "Danh sách ngân hàng",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Bật/tắt nhận diện thông báo từ các app ngân hàng đã cài đặt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (installedBankApps.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Text(
                                "Không phát hiện ứng dụng ngân hàng nào được cài đặt.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(installedBankApps) { app ->
                        val isEnabled = settings.enabledPackages.contains(app.packageName)
                        val iconDrawable = remember(app.packageName) {
                            getInstalledBankAppIcon(context, app.packageName)
                        }
                        val imageBitmap = remember(iconDrawable) {
                            iconDrawable?.toBitmap()?.asImageBitmap()
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
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
                                        imageVector = Icons.Rounded.AccountBalance,
                                        contentDescription = app.label,
                                        tint = MaterialTheme.colorScheme.primary,
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
                                checked = isEnabled,
                                onCheckedChange = { viewModel.setPackageEnabled(app.packageName, it) }
                            )
                        }
                    }
                }

                // 3. Ứng dụng loại trừ
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Ứng dụng loại trừ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Chọn các ứng dụng bạn muốn hoàn toàn loại trừ, không phân tích thông báo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
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
                                IconButton(onClick = { appSearchQuery = "" }) {
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
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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
                                    .clickable { filterState = index }
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
                }

                // 4. Danh sách ứng dụng loại trừ
                if (isLoadingApps) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (filteredApps.isEmpty()) {
                    item {
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
                    }
                } else {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isExcluded = settings.excludedPackages.contains(app.packageName)
                        val imageBitmap = remember(app.icon) {
                            app.icon?.toBitmap()?.asImageBitmap()
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", fontWeight = FontWeight.Bold)
            }
        }
    )
}

private data class UserAppInfo(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable?
)

private fun getInstalledUserApps(context: Context): List<UserAppInfo> {
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
        UserAppInfo(packageName, label, icon)
    }.distinctBy { it.packageName }.sortedBy { it.label }
}

@Composable
private fun CircularBudgetRing(
    percentage: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(percentage) {
        animationProgress.animateTo(
            targetValue = percentage,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            
            drawArc(
                color = color.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = animationProgress.value * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun BudgetAnalysisCard(
    projection: BudgetProjection,
    budgetLimit: Money,
    modifier: Modifier = Modifier
) {
    val spentPercentage = projection.spentPercentage
    val progressColor = when {
        spentPercentage < 0.70f -> Color(0xFF4CAF50)
        spentPercentage < 0.90f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    val adviceMessage = when {
        spentPercentage >= 1.0f -> {
            val overspent = projection.spentThisWallet.amountInCents - budgetLimit.amountInCents
            "🚨 Bạn đã tiêu quá hạn mức ví ${MoneyFormatter.format(Money(overspent))}!"
        }
        projection.isProjectedToExceed -> {
            "⚠️ Dự báo sẽ vượt hạn mức vào ngày ${projection.exhaustionDateLabel}. Bạn nên giảm chi tiêu xuống dưới ${MoneyFormatter.format(projection.safeDailyLimit)}/ngày để giữ an toàn."
        }
        else -> {
            "💚 Chi tiêu trong tầm kiểm soát. Dự kiến tiêu dùng cuối tháng là ${MoneyFormatter.format(projection.projectedSpend)} (Dưới hạn mức)."
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = progressColor.copy(alpha = 0.08f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = progressColor.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularBudgetRing(
                    percentage = spentPercentage,
                    color = progressColor,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = "${(spentPercentage * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dự báo ngân sách",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = adviceMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}