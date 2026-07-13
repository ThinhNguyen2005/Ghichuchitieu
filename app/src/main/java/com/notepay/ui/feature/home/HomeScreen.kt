package com.notepay.ui.feature.home

import com.notepay.ui.theme.AppTheme

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
import com.notepay.ui.component.GradientTopAppBar
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
import com.notepay.ui.component.EmptyStateWithAction
import com.notepay.ui.util.WalletUiHelper
import com.notepay.ui.component.KpiRow
import com.notepay.ui.component.TransactionItem
import com.notepay.ui.theme.NotePayTheme

import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.rounded.ArrowDropDown
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
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.width
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
    onEditWallet: (Long) -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    navigationBarOffset: Float = 0f,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showWalletSwitcher by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Resolve string resources trước khi dùng trong non-Composable scopes.
    val notifPermissionTitle = stringResource(R.string.home_notif_permission_title)
    val notifPermissionDesc = stringResource(R.string.home_notif_permission_desc)
    val notifPermissionTip = stringResource(R.string.home_notif_permission_tip)
    val batteryTitle = stringResource(R.string.home_battery_title)
    val batteryDesc = stringResource(R.string.home_battery_desc)
    val batterySetupLabel = stringResource(R.string.action_setup)
    val recentLabel = stringResource(R.string.home_recent_transactions)
    val seeAllLabel = stringResource(R.string.action_see_all)
    val emptyTx = stringResource(R.string.home_empty_transactions)
    val chooseWallet = stringResource(R.string.home_choose_wallet)
    val closeLabel = stringResource(R.string.action_close)
    val editWalletLabel = stringResource(R.string.home_edit_wallet)
    val addNewWalletLabel = stringResource(R.string.home_add_new_wallet)
    val reminderCd = stringResource(R.string.home_reminder_cd)
    val notifSettingsCd = stringResource(R.string.home_notification_settings_cd)
    val emptyWalletTitle = stringResource(R.string.home_empty_wallet_title)
    val emptyWalletDesc = stringResource(R.string.home_empty_wallet_desc)
    val createWalletLabel = stringResource(R.string.home_create_wallet)
    val budgetTitle = stringResource(R.string.budget_title)
    val overspentFmt = stringResource(R.string.budget_overspent)
    val willExceedFmt = stringResource(R.string.budget_will_exceed)
    val safeFmt = stringResource(R.string.budget_safe)
    var isListenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var isBatteryOptimizationsIgnored by remember {
        mutableStateOf(
            (context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager)
                .isIgnoringBatteryOptimizations(context.packageName)
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isListenerEnabled = isNotificationListenerEnabled(context)
                isBatteryOptimizationsIgnored = (context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager)
                    .isIgnoringBatteryOptimizations(context.packageName)
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
            GradientTopAppBar(
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
                                contentDescription = reminderCd
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToNotificationSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = notifSettingsCd
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

        if (state.wallets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateWithAction(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = emptyWalletTitle,
                    description = emptyWalletDesc,
                    actionLabel = createWalletLabel,
                    onClick = { showWalletSwitcher = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
            return@Scaffold
        }

        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = padding.calculateStartPadding(layoutDirection),
                    end = padding.calculateEndPadding(layoutDirection)
                ),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = padding.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BalanceCard(
                        wallet = state.activeWallet,
                        balance = state.currentBalance,
                        onClick = { showWalletSwitcher = true },
                        onEditWallet = onEditWallet,
                        monthlyExpense = state.monthlyExpense,
                    )
                    val budgetProjection = state.budgetProjection
                    val activeWallet = state.activeWallet
                    if (budgetProjection != null && activeWallet?.budgetLimit != null) {
                        BudgetAnalysisCard(
                            projection = budgetProjection,
                            budgetLimit = activeWallet.budgetLimit,
                            title = budgetTitle,
                            overspentFormat = overspentFmt,
                            willExceedFormat = willExceedFmt,
                            safeFormat = safeFmt,
                        )
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
                        shape = AppTheme.shapes.corner16,
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
                                    text = notifPermissionTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = notifPermissionDesc,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = notifPermissionTip,
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
                if (!isBatteryOptimizationsIgnored) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            shape = AppTheme.shapes.corner16
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BatteryAlert,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = batteryTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = batteryDesc,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            try {
                                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                context.startActivity(intent)
                                            } catch (ex: Exception) {
                                                val intent = Intent(Settings.ACTION_SETTINGS)
                                                context.startActivity(intent)
                                            }
                                        }
                                    }
                                ) {
                                    Text(batterySetupLabel)
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
                    Text(recentLabel, style = MaterialTheme.typography.titleMedium)
                    androidx.compose.material3.TextButton(onClick = onSeeAll) { Text(seeAllLabel) }
                }
            }
            if (state.recentTransactions.isEmpty()) {
                item { EmptyStateWithAction(title = emptyTx) }
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
            title = { Text(chooseWallet) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.wallets) { wallet ->
                        val isSelected = wallet.id == state.activeWallet?.id
                        val iconVector = WalletUiHelper.getIcon(wallet.iconKey)
                        val tintColor = WalletUiHelper.getColor(wallet.colorKey)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppTheme.shapes.corner12)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    viewModel.selectWallet(wallet.id)
                                    showWalletSwitcher = false
                                }
                                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = tintColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = wallet.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    showWalletSwitcher = false
                                    onEditWallet(wallet.id)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = editWalletLabel,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
                                .clip(AppTheme.shapes.corner12)
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
                                text = addNewWalletLabel,
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
                    Text(closeLabel)
                }
            }
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
    title: String,
    overspentFormat: String,
    willExceedFormat: String,
    safeFormat: String,
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
            overspentFormat.format(MoneyFormatter.format(Money(overspent)))
        }
        projection.isProjectedToExceed -> {
            willExceedFormat.format(
                projection.exhaustionDateLabel,
                MoneyFormatter.format(projection.safeDailyLimit)
            )
        }
        else -> {
            safeFormat.format(MoneyFormatter.format(projection.projectedSpend))
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = progressColor.copy(alpha = 0.08f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = AppTheme.shapes.corner24,
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
                    text = title,
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