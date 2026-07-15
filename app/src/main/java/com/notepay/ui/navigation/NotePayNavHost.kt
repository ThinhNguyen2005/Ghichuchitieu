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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.notepay.ui.feature.backup.BackupRestoreScreen

// Backdrop Liquid Glass imports
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.backdrop.shadow.InnerShadow
import com.notepay.ui.component.GlassDropBox
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.notepay.ui.navigation.utils.liquidPopClick
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.sign
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import com.notepay.ui.navigation.utils.DampedDragAnimation
import com.notepay.ui.navigation.utils.InteractiveHighlight
import com.notepay.ui.navigation.utils.inspectDragGestures

// Thêm các thư viện cần dùng cho giao diện tùy biến mới
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
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

private val LocalLiquidBottomTabScale = staticCompositionLocalOf { { 1f } }

@Composable
private fun RowScope.NotePayBottomTabItem(
    tab: BottomTab,
    tabLabel: String,
    isSourceActiveRow: Boolean,
    onClick: () -> Unit,
    isInteractive: Boolean = true
) {
    val scale = LocalLiquidBottomTabScale.current
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .then(
                if (isInteractive) Modifier.clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick
                ) else Modifier
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isSourceActiveRow) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = tabLabel,
            tint = if (isSourceActiveRow) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tabLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isSourceActiveRow) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSourceActiveRow) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePayNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val systemBackground = MaterialTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(systemBackground)
        drawContent()
    }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                NavHost(
                navController = navController,
                startDestination = Route.Home.path,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Route.Home.path) {
                    HomeScreen(
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
                            navController.navigate(Route.TransactionDetail(txId).path)
                        }
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
                            navController.navigate(Route.TransactionDetail(txId).path)
                        },
                        onFeedback = ::showFeedback,
                    )
                }
                composable(Route.Stats.path) {
                    StatsScreen(
                        onAddTransaction = { navController.navigate(Route.AddTransaction.path) },
                        onTransactionClick = { txId ->
                            navController.navigate(Route.TransactionDetail(txId).path)
                        },
                        onConfigureLocalModel = {
                            navController.navigate(Route.NotificationSettings.path)
                        },
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
            }

            if (isMainTab) {
                val haptic = LocalHapticFeedback.current
                val navTabs = remember { bottomTabs.filter { it.route != Route.AddDummy } }
                val primaryColor = MaterialTheme.colorScheme.primary
                val isLightTheme = !isSystemInDarkTheme()
                val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f) else Color(0xFF121212).copy(0.4f)
                val density = LocalDensity.current
                val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

                val selectedIndex = remember(currentRoute) {
                    when (currentRoute?.split("?")?.firstOrNull()) {
                        Route.Home.path -> 0
                        Route.TransactionList.path -> 1
                        Route.Stats.path -> 2
                        Route.BillSplit.path -> 3
                        else -> 0
                    }
                }

                val tabsBackdrop = rememberLayerBackdrop()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = navigationBarOffset.roundToInt()
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left: Tabs Bar
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val tabsCount = navTabs.size
                        val tabWidth = with(density) {
                            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
                        }
                        val widthPx = constraints.maxWidth.toFloat()

                        val offsetAnimation = remember { Animatable(0f) }
                        val panelOffset by remember(density, widthPx) {
                            derivedStateOf {
                                val fraction = (offsetAnimation.value / widthPx).fastCoerceIn(-1f, 1f)
                                with(density) {
                                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                                }
                            }
                        }

                        val dampedDragAnimation: DampedDragAnimation = remember(coroutineScope, tabWidth) {
                            DampedDragAnimation(
                                animationScope = coroutineScope,
                                initialValue = selectedIndex.toFloat(),
                                valueRange = 0f..(tabsCount - 1).toFloat(),
                                visibilityThreshold = 0.001f,
                                initialScale = 1f,
                                pressedScale = 78f / 56f,
                                onDragStarted = {},
                                onDragStopped = {
                                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    navController.navigate(navTabs[targetIndex].route.path) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    coroutineScope.launch {
                                        offsetAnimation.animateTo(
                                            0f,
                                            spring(1f, 300f, 0.5f)
                                        )
                                    }
                                },
                                onDrag = { size, dragAmount ->
                                    updateValue(
                                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                                    )
                                    coroutineScope.launch {
                                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                                    }
                                }
                            )
                        }

                        LaunchedEffect(selectedIndex) {
                            if (selectedIndex.toFloat() != dampedDragAnimation.targetValue) {
                                dampedDragAnimation.animateToValue(selectedIndex.toFloat())
                            }
                        }

                        val interactiveHighlight = remember(coroutineScope, tabWidth) {
                            InteractiveHighlight(
                                animationScope = coroutineScope,
                                position = { size, offset ->
                                    Offset(
                                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                                        size.height / 2f
                                    )
                                }
                            )
                        }

                        // Row 1: Bottom Layer - Main visible bar
                        Row(
                            Modifier
                                .graphicsLayer {
                                    translationX = panelOffset
                                }
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { CircleShape },
                                    effects = {
                                        vibrancy()
                                        blur(8f.dp.toPx())
                                        lens(24f.dp.toPx(), 24f.dp.toPx())
                                    },
                                    layerBlock = {
                                        val progress = dampedDragAnimation.pressProgress
                                        val scale = androidx.compose.ui.util.lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                    onDrawSurface = { drawRect(containerColor) }
                                )
                                .then(interactiveHighlight.modifier)
                                .height(64f.dp)
                                .fillMaxWidth()
                                .padding(4f.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            navTabs.forEach { tab ->
                                NotePayBottomTabItem(
                                    tab = tab,
                                    tabLabel = tabLabels[bottomTabs.indexOf(tab)],
                                    isSourceActiveRow = false,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val targetIndex = navTabs.indexOf(tab)
                                        dampedDragAnimation.animateToValue(targetIndex.toFloat())
                                        navController.navigate(tab.route.path) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }

                        // Row 2: Target active row source (invisible backdrop source)
                        CompositionLocalProvider(
                            LocalLiquidBottomTabScale provides {
                                androidx.compose.ui.util.lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
                            }
                        ) {
                            Row(
                                Modifier
                                    .clearAndSetSemantics {}
                                    .alpha(0f)
                                    .layerBackdrop(tabsBackdrop)
                                    .graphicsLayer {
                                        translationX = panelOffset
                                    }
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { CircleShape },
                                        effects = {
                                            val progress = dampedDragAnimation.pressProgress
                                            vibrancy()
                                            blur(8f.dp.toPx())
                                            lens(
                                                24f.dp.toPx() * progress,
                                                24f.dp.toPx() * progress
                                            )
                                        },
                                        highlight = {
                                            val progress = dampedDragAnimation.pressProgress
                                            Highlight.Default.copy(alpha = progress)
                                        },
                                        onDrawSurface = { drawRect(containerColor) }
                                    )
                                    .then(interactiveHighlight.modifier)
                                    .height(56f.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 4f.dp)
                                    .graphicsLayer(colorFilter = ColorFilter.tint(primaryColor)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                navTabs.forEach { tab ->
                                    NotePayBottomTabItem(
                                        tab = tab,
                                        tabLabel = tabLabels[bottomTabs.indexOf(tab)],
                                        isSourceActiveRow = true,
                                        onClick = {},
                                        isInteractive = false
                                    )
                                }
                            }
                        }

                        // Box 3: Sliding Highlight Indicator
                        Box(
                            Modifier
                                .padding(horizontal = 4f.dp)
                                .graphicsLayer {
                                    translationX =
                                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                                }
                                .then(interactiveHighlight.gestureModifier)
                                .then(dampedDragAnimation.modifier)
                                .drawBackdrop(
                                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                                    shape = { CircleShape },
                                    effects = {
                                        val progress = dampedDragAnimation.pressProgress
                                        lens(
                                            10f.dp.toPx() * progress,
                                            14f.dp.toPx() * progress,
                                            chromaticAberration = true
                                        )
                                    },
                                    highlight = {
                                        val progress = dampedDragAnimation.pressProgress
                                        Highlight.Default.copy(alpha = progress)
                                    },
                                    shadow = {
                                        val progress = dampedDragAnimation.pressProgress
                                        Shadow(alpha = progress)
                                    },
                                    innerShadow = {
                                        val progress = dampedDragAnimation.pressProgress
                                        InnerShadow(
                                            radius = 8f.dp * progress,
                                            alpha = progress
                                        )
                                    },
                                    layerBlock = {
                                        scaleX = dampedDragAnimation.scaleX
                                        scaleY = dampedDragAnimation.scaleY
                                        val velocity = dampedDragAnimation.velocity / 10f
                                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                                    },
                                    onDrawSurface = {
                                        val progress = dampedDragAnimation.pressProgress
                                        drawRect(
                                            if (isLightTheme) Color.Black.copy(0.1f)
                                            else Color.White.copy(0.1f),
                                            alpha = 1f - progress
                                        )
                                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                                    }
                                )
                                .height(56f.dp)
                                .width(with(density) { tabWidth.toDp() })
                        )
                    }

                    // Right: Add Button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                clip = false
                            )
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { CircleShape },
                                effects = {
                                    vibrancy()
                                    blur(20.dp.toPx())
                                    lens(
                                        refractionHeight = 8.dp.toPx(),
                                        refractionAmount = 16.dp.toPx()
                                    )
                                },
                                onDrawSurface = {
                                    // Nền kính mờ tối + tint primary nhẹ nhàng
                                    drawRect(Color.Black.copy(alpha = 0.3f))
                                    drawRect(primaryColor.copy(alpha = 0.25f))
                                }
                            )
                            .liquidPopClick {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

            // Render Glass Drop Box (inline popup sheet)
            GlassDropBox(
                backdrop = backdrop,
                visible = showQuickAddSheet,
                onDismissRequest = { showQuickAddSheet = false }
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

@Composable
private fun QuickAddOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    val isLightTheme = !androidx.compose.foundation.isSystemInDarkTheme()
    val cardBgColor = if (isLightTheme) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)
    val borderStrokeColor = if (isLightTheme) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f)
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBgColor)
            .border(1.dp, borderStrokeColor, shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
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
                    color = if (isLightTheme) Color(0xFF1A1A1A) else Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLightTheme) Color(0xFF757575) else Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
