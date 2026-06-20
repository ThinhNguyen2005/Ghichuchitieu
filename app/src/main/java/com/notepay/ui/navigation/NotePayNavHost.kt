package com.notepay.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.notepay.ui.component.CradleShape
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notepay.R
import com.notepay.ui.feature.addtransaction.AddTransactionScreen
import com.notepay.ui.feature.addtransaction.EditTransactionScreen
import com.notepay.ui.feature.billsplit.BillSplitScreen
import com.notepay.ui.feature.billsplit.DebtorDetailScreen
import com.notepay.ui.feature.detail.TransactionDetailScreen
import com.notepay.ui.feature.home.HomeScreen
import com.notepay.ui.feature.home.NotificationSettingsScreen
import com.notepay.ui.feature.list.TransactionListScreen
import com.notepay.ui.feature.stats.StatsScreen
import com.notepay.ui.feature.subscription.SubscriptionScreen
import com.notepay.ui.feature.wallet.AddWalletScreen
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.feedback.FeedbackDuration
import com.notepay.ui.feature.utilities.UtilitiesScreen

// Thêm các thư viện cần dùng cho giao diện tùy biến mới
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.outlined.Analytics

private data class BottomTab(
    val route: Route,
    @androidx.annotation.StringRes val labelRes: Int,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomTabs = listOf(
    BottomTab(Route.Home, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    BottomTab(Route.TransactionList, R.string.nav_transactions,
        Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
    BottomTab(Route.AddDummy, R.string.nav_add, Icons.Rounded.Add, Icons.Rounded.Add),
    BottomTab(Route.Stats, R.string.nav_stats,
        Icons.Outlined.Analytics, Icons.Filled.Analytics),
    BottomTab(Route.BillSplit, R.string.nav_bill_split,
        Icons.AutoMirrored.Outlined.CallSplit, Icons.AutoMirrored.Filled.CallSplit),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePayNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isMainTab = currentRoute != null && bottomTabs.any { tab ->
        currentRoute == tab.route.path || currentRoute.startsWith("${tab.route.path}?")
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var showQuickAddSheet by remember { mutableStateOf(false) }

    // Resolve string resources một lần để dùng cho cả bottom nav và FAB tooltip.
    val tabLabels = bottomTabs.map { tab -> stringResource(tab.labelRes) }
    val quickAddTitle = stringResource(R.string.quick_add_title)
    val quickAddExpenseTitle = stringResource(R.string.quick_add_expense_title)
    val quickAddExpenseDesc = stringResource(R.string.quick_add_expense_desc)
    val quickAddBillSplitTitle = stringResource(R.string.quick_add_bill_split_title)
    val quickAddBillSplitDesc = stringResource(R.string.quick_add_bill_split_desc)
    val quickAddSubscriptionTitle = stringResource(R.string.quick_add_subscription_title)
    val quickAddSubscriptionDesc = stringResource(R.string.quick_add_subscription_desc)
    val fabContentDesc = stringResource(R.string.nav_add)

    // Chiều cao thanh điều hướng gồm 76dp (Bar) + 28dp (bottom padding) = 104dp.
    val barHeightPx = with(LocalDensity.current) { 104.dp.toPx() }
    var navigationBarOffset by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentRoute) {
        navigationBarOffset = 0f
    }

    val nestedScrollConnection = remember(barHeightPx, isMainTab) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isMainTab) {
                    val delta = available.y
                    navigationBarOffset = (navigationBarOffset - delta).coerceIn(0f, barHeightPx)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (isMainTab) {
                    val targetOffset = if (navigationBarOffset > barHeightPx / 2f) barHeightPx else 0f
                    coroutineScope.launch {
                        Animatable(navigationBarOffset).animateTo(
                            targetValue = targetOffset,
                            animationSpec = tween(durationMillis = 200)
                        ) {
                            navigationBarOffset = this.value
                        }
                    }
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    suspend fun showFeedback(feedback: UiFeedback): Boolean {
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(
            message = feedback.message,
            actionLabel = feedback.actionLabel,
            duration = when (feedback.duration) {
                FeedbackDuration.Short -> SnackbarDuration.Short
                FeedbackDuration.Long -> SnackbarDuration.Long
                FeedbackDuration.Indefinite -> SnackbarDuration.Indefinite
            },
        )
        return result == SnackbarResult.ActionPerformed
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 104.dp)
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) { padding ->        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Route.Home.path,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Route.Home.path) {
                    HomeScreen(
                        onAddTransaction = { navController.navigate(Route.AddTransaction.path) },
                        onSeeAll = {
                            navController.navigate(Route.TransactionList.path) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onAddWallet = { navController.navigate(Route.AddWallet.path) },
                        onEditWallet = { walletId -> navController.navigate(Route.EditWallet(walletId).path) },
                        onNavigateToReminders = { navController.navigate(Route.Subscription.path) },
                        onNavigateToNotificationSettings = { navController.navigate(Route.NotificationSettings.path) },
                        onTransactionClick = { txId ->
                            navController.navigate(Route.EditTransaction(txId).path)
                        },
                        navigationBarOffset = navigationBarOffset
                    )
                }
                composable(Route.AddTransaction.path) {
                    AddTransactionScreen(
                        onSaved = {
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = Route.EditTransaction.ROUTE,
                    arguments = listOf(navArgument(Route.EditTransaction.ARG_ID) { type = NavType.LongType }),
                ) {
                    EditTransactionScreen(
                        onSaved = { feedback ->
                            navController.popBackStack()
                            showFeedback(feedback)
                        },
                        onBack = { navController.popBackStack() },
                        onFeedback = ::showFeedback,
                    )
                }
                composable(
                    route = Route.TransactionDetail.ROUTE,
                    arguments = listOf(navArgument(Route.TransactionDetail.ARG_ID) { type = NavType.LongType }),
                ) {
                    TransactionDetailScreen(
                        onBack = { navController.popBackStack() },
                        onEdit = { id -> navController.navigate(Route.EditTransaction(id).path) },
                        onCreateBillSplit = {
                            navController.navigate(Route.BillSplit.path) {
                                launchSingleTop = true
                            }
                        },
                        onCreateSubscription = { _, _ ->
                            navController.navigate(Route.Subscription.path) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(Route.TransactionList.path) {
                    TransactionListScreen(
                        onTransactionClick = { txId ->
                            navController.navigate(Route.EditTransaction(txId).path)
                        },
                        onFeedback = ::showFeedback,
                    )
                }
                composable(Route.Stats.path) {
                    StatsScreen(
                        onAddTransaction = { navController.navigate(Route.AddTransaction.path) },
                    )
                }
                composable(Route.AddWallet.path) {
                    AddWalletScreen(
                        onSaved = { feedback ->
                            navController.popBackStack()
                            showFeedback(feedback)
                        },
                        onBack = { navController.popBackStack() },
                        onFeedback = ::showFeedback,
                    )
                }
                composable(
                    route = Route.EditWallet.ROUTE,
                    arguments = listOf(
                        navArgument(Route.EditWallet.ARG_ID) { type = NavType.LongType }
                    )
                ) {
                    AddWalletScreen(
                        onSaved = { feedback ->
                            navController.popBackStack()
                            showFeedback(feedback)
                        },
                        onBack = { navController.popBackStack() },
                        onFeedback = ::showFeedback,
                    )
                }
                composable(
                    route = "bill-split?showCreate={showCreate}",
                    arguments = listOf(navArgument("showCreate") { type = NavType.BoolType; defaultValue = false })
                ) { backStackEntry ->
                    val arguments = backStackEntry.arguments
                    val showCreateArg = arguments?.getBoolean("showCreate") ?: false
                    val isHandled = backStackEntry.savedStateHandle.get<Boolean>("showCreateHandled") ?: false

                    if (showCreateArg && !isHandled) {
                        backStackEntry.savedStateHandle["showCreate"] = true
                        backStackEntry.savedStateHandle["showCreateHandled"] = true
                    } else if (!showCreateArg) {
                        backStackEntry.savedStateHandle["showCreateHandled"] = false
                    }

                    val showCreateFlow = backStackEntry.savedStateHandle.getStateFlow("showCreate", false)
                    val showCreate by showCreateFlow.collectAsState()
                    BillSplitScreen(
                        onDebtorClick = { debtorName ->
                            navController.navigate(Route.DebtorDetail(debtorName).path)
                        },
                        onFeedback = ::showFeedback,
                        navigationBarOffset = navigationBarOffset,
                        initialShowCreate = showCreate,
                        onClearShowCreate = {
                            backStackEntry.savedStateHandle["showCreate"] = false
                        }
                    )
                }
                composable(
                    route = Route.DebtorDetail.ROUTE,
                    arguments = listOf(navArgument(Route.DebtorDetail.ARG_NAME) { type = NavType.StringType }),
                ) { backStackEntry ->
                    val debtorName = backStackEntry.arguments?.getString(Route.DebtorDetail.ARG_NAME).orEmpty()
                    DebtorDetailScreen(
                        debtorName = debtorName,
                        onBack = { navController.popBackStack() },
                        onFeedback = ::showFeedback,
                    )
                }
                composable(
                    route = "subscription?showCreate={showCreate}",
                    arguments = listOf(navArgument("showCreate") { type = NavType.BoolType; defaultValue = false })
                ) { backStackEntry ->
                    val arguments = backStackEntry.arguments
                    val showCreateArg = arguments?.getBoolean("showCreate") ?: false
                    val isHandled = backStackEntry.savedStateHandle.get<Boolean>("showCreateHandled") ?: false

                    if (showCreateArg && !isHandled) {
                        backStackEntry.savedStateHandle["showCreate"] = true
                        backStackEntry.savedStateHandle["showCreateHandled"] = true
                    } else if (!showCreateArg) {
                        backStackEntry.savedStateHandle["showCreateHandled"] = false
                    }

                    val showCreateFlow = backStackEntry.savedStateHandle.getStateFlow("showCreate", false)
                    val showCreate by showCreateFlow.collectAsState()
                    SubscriptionScreen(
                        navigationBarOffset = navigationBarOffset,
                        initialShowCreate = showCreate,
                        onBack = { navController.popBackStack() },
                        onClearShowCreate = {
                            backStackEntry.savedStateHandle["showCreate"] = false
                        }
                    )
                }
                composable(Route.Utilities.path) {
                    UtilitiesScreen(
                        onNavigateToBillSplit = {
                            navController.navigate(Route.BillSplit.path) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToSubscription = {
                            navController.navigate(Route.Subscription.path) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(Route.NotificationSettings.path) {
                    NotificationSettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            if (isMainTab) {
                val BottomBarShape = remember { CradleShape() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = navigationBarOffset.roundToInt()
                            )
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 16.dp,
                                shape = BottomBarShape,
                                clip = false
                            ),
                        shape = BottomBarShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Left tabs (Home & TransactionList)
                                bottomTabs.take(2).forEachIndexed { index, tab ->
                                    val tabLabel = tabLabels[index]
                                    val isSelected = currentRoute != null && currentRoute.split("?").firstOrNull() == tab.route.path
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable(
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                navController.navigate(tab.route.path) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Top horizontal indicator bar
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .width(40.dp)
                                                    .height(3.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)
                                                    )
                                                    .align(Alignment.TopCenter)
                                            )
                                        }

                                        // Icon & Label Column
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxHeight().padding(top = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tabLabel,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = tabLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                
                                // Center spacer for the cutout "cradle"
                                Spacer(modifier = Modifier.weight(0.8f))
                                
                                // Right tabs (Stats & BillSplit)
                                bottomTabs.takeLast(2).forEachIndexed { index, tab ->
                                    val realIndex = bottomTabs.size - 2 + index
                                    val tabLabel = tabLabels[realIndex]
                                    val isSelected = currentRoute != null && currentRoute.split("?").firstOrNull() == tab.route.path
                                    val shouldColorTab = isSelected
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable(
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                navController.navigate(tab.route.path) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Top horizontal indicator bar
                                        if (shouldColorTab) {
                                            Box(
                                                modifier = Modifier
                                                    .width(40.dp)
                                                    .height(3.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)
                                                    )
                                                    .align(Alignment.TopCenter)
                                            )
                                        }

                                        // Icon & Label Column
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxHeight().padding(top = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tabLabel,
                                                tint = if (shouldColorTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = tabLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = if (shouldColorTab) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (shouldColorTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.navigationBarsPadding())
                        }
                    }
                    
                    // Floating Center Creator Add Button
                    Box(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .offset(y = (-24).dp)
                            .size(64.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                showQuickAddSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = fabContentDesc,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Render Bottom Sheet
            if (showQuickAddSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showQuickAddSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = quickAddTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Option 1: Thêm chi tiêu
                        QuickAddOption(
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            title = quickAddExpenseTitle,
                            description = quickAddExpenseDesc,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = {
                                showQuickAddSheet = false
                                navController.navigate(Route.AddTransaction.path)
                            }
                        )

                        // Option 2: Chia hóa đơn
                        QuickAddOption(
                            icon = Icons.AutoMirrored.Outlined.CallSplit,
                            title = quickAddBillSplitTitle,
                            description = quickAddBillSplitDesc,
                            color = MaterialTheme.colorScheme.tertiary,
                            onClick = {
                                showQuickAddSheet = false
                                val isAlreadyOnBillSplit = currentRoute?.startsWith("bill-split") == true
                                if (isAlreadyOnBillSplit) {
                                    navController.currentBackStackEntry?.savedStateHandle?.set("showCreate", true)
                                } else {
                                    navController.navigate("bill-split?showCreate=true") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    try {
                                        navController.getBackStackEntry("bill-split?showCreate={showCreate}")
                                            .savedStateHandle["showCreate"] = true
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            }
                        )

                        // Option 3: Hóa đơn định kỳ
                        QuickAddOption(
                            icon = Icons.Outlined.Notifications,
                            title = quickAddSubscriptionTitle,
                            description = quickAddSubscriptionDesc,
                            color = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                showQuickAddSheet = false
                                val isAlreadyOnSubscription = currentRoute?.startsWith("subscription") == true
                                if (isAlreadyOnSubscription) {
                                    navController.currentBackStackEntry?.savedStateHandle?.set("showCreate", true)
                                } else {
                                    navController.navigate("subscription?showCreate=true") {
                                        launchSingleTop = true
                                    }
                                    try {
                                        navController.getBackStackEntry("subscription?showCreate={showCreate}")
                                            .savedStateHandle["showCreate"] = true
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}