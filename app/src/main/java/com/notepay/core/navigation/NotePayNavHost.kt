package com.notepay.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notepay.feature.assets.main.QuickTransferViewModel
import com.notepay.feature.assets.main.sheet.TransferBottomSheet
import com.notepay.feature.transaction.natural.NaturalEntryBottomSheet
import androidx.compose.foundation.pager.rememberPagerState
import androidx.navigation.compose.composable
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.notepay.core.platform.OsCompatHelper
import com.notepay.R
import com.notepay.core.designsystem.adaptive.LocalWindowLayout
import com.notepay.core.designsystem.adaptive.NotePayAdaptive
import com.notepay.core.designsystem.adaptive.usesNavigationRail
import com.notepay.core.designsystem.component.action.CreateNewBottomSheet
import com.notepay.core.designsystem.component.action.QuickAddAction
import com.notepay.core.designsystem.component.button.FloatingAddButton
import com.notepay.core.designsystem.component.effect.LocalNotePayBackdrop
import com.notepay.feature.billsplit.billSplitGraph
import com.notepay.feature.onboarding.OnboardingRoute
import com.notepay.feature.stats.statsScreen
import com.notepay.feature.subscription.subscriptionScreen
import com.notepay.feature.transaction.transactionGraph
import com.notepay.feature.debt.debtGraph
import com.notepay.feature.utilities.utilitiesGraph
import com.notepay.feature.assets.main.walletGraph
import com.notepay.core.designsystem.feedback.FeedbackSnackbarHost
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.feedback.feedbackSnackbarInsets
import com.notepay.core.designsystem.feedback.rememberLatestFeedbackDispatcher
import com.notepay.core.designsystem.feedback.showFeedback
import com.notepay.domain.model.AuthUser
import com.notepay.core.designsystem.util.LocalAuthUser
import com.notepay.feature.transaction.form.TransactionFormSaveResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NotePayNavHost(
    navController: NavHostController = rememberNavController(),
    authUser: AuthUser? = null,
    startDestination: String = Route.Home.path,
    liquidGlassEnabled: Boolean = false,
    hapticFeedbackEnabled: Boolean = true,
    pendingRoute: String? = null,
    onRouteHandled: () -> Unit = {},
    onCompleteWelcome: () -> Unit = {},
) {
    val useNavigationGlass = liquidGlassEnabled && OsCompatHelper.liquidGlassCompatibility(
        isHardwareAccelerated = LocalView.current.isHardwareAccelerated,
    ).isSupported
    // Tablet/foldable: đủ chỗ thì đổi bottom bar thành rail dọc, tắt vuốt ngang của pager.
    val windowLayout = LocalWindowLayout.current
    val useNavigationRail = windowLayout.usesNavigationRail
    val reducedMotion = rememberNotePayReducedMotion()
    val motionDistancePx = with(LocalDensity.current) {
        NotePayMotion.contentTranslation.roundToPx()
    }
    val systemBackground = MaterialTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(systemBackground)
        drawContent()
    }
    val pagerState = rememberPagerState(initialPage = 0) { bottomTabs.size }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isMainTab = isMainTabRoute(currentRoute)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val feedbackDispatcher = rememberLatestFeedbackDispatcher(snackbarHostState)
    var showQuickAddSheet by rememberSaveable { mutableStateOf(false) }
    var showNaturalEntrySheet by rememberSaveable { mutableStateOf(false) }
    var quickAddAction by remember { mutableStateOf<QuickAddAction?>(null) }
    val quickTransferViewModel: QuickTransferViewModel = hiltViewModel()
    val viewTransactionLabel = stringResource(R.string.action_view_transaction)

    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            navController.navigate(pendingRoute) {
                launchSingleTop = true
            }
            onRouteHandled()
        }
    }

    LaunchedEffect(isMainTab) {
        if (!isMainTab) {
            showQuickAddSheet = false
            showNaturalEntrySheet = false
        }
    }

    LaunchedEffect(quickAddAction, isMainTab) {
        val action = quickAddAction ?: return@LaunchedEffect
        quickAddAction = null
        if (!isMainTab) return@LaunchedEffect
        val route = when (action) {
            QuickAddAction.Transaction -> Route.AddTransaction.path
            QuickAddAction.Transfer -> {
                quickTransferViewModel.open()
                null
            }
            QuickAddAction.ScanReceipt -> Route.AddTransaction.withScan()
            QuickAddAction.NaturalEntry -> {
                showNaturalEntrySheet = true
                null
            }
            QuickAddAction.Debt -> Route.DebtManagement.withCreate()
            QuickAddAction.BillSplit -> Route.BillSplit.path
            QuickAddAction.Subscription -> Route.Subscription.path
        }
        if (route != null) navController.navigate(route) { launchSingleTop = true }
    }

    val showFeedback: suspend (UiFeedback) -> Boolean = remember(feedbackDispatcher) {
        { feedback ->
            feedbackDispatcher.show(feedback)
            false
        }
    }

    fun showFeedbackAfterReturn(source: NavBackStackEntry, feedback: UiFeedback) {
        if (navController.currentBackStackEntry != source) return
        val destination = navController.previousBackStackEntry ?: return
        if (!navController.popBackStack()) return
        coroutineScope.launch {
            val state = destination.lifecycle.currentStateFlow.first {
                it == Lifecycle.State.RESUMED || it == Lifecycle.State.DESTROYED
            }
            if (state != Lifecycle.State.RESUMED) return@launch
            feedbackDispatcher.show(feedback)
        }
    }

    fun onTransactionSaved(source: NavBackStackEntry, result: TransactionFormSaveResult) {
        val primary = result.feedback.copy(
            actionLabel = viewTransactionLabel,
            onAction = {
                navController.navigate(Route.TransactionDetail(result.transactionId).path) {
                    launchSingleTop = true
                }
            },
        )
        val followUp = result.followUpWarning
        if (followUp == null) {
            showFeedbackAfterReturn(source, primary)
            return
        }
        if (navController.currentBackStackEntry != source) return
        val destination = navController.previousBackStackEntry ?: return
        if (!navController.popBackStack()) return
        coroutineScope.launch {
            val state = destination.lifecycle.currentStateFlow.first {
                it == Lifecycle.State.RESUMED || it == Lifecycle.State.DESTROYED
            }
            if (state != Lifecycle.State.RESUMED) return@launch
            val actionPerformed = snackbarHostState.showFeedback(primary)
            if (!actionPerformed && destination.lifecycle.currentState == Lifecycle.State.RESUMED) {
                feedbackDispatcher.show(followUp)
            }
        }
    }

    // Quy đổi route gốc -> trang pager. Toạ độ tab chỉ tồn tại trong `rootTabRoutes`
    // (NotePayMotion.kt) nên các nav graph con không phải hardcode 0/1/2/3.
    val navigateToRootTab: (Route) -> Unit = remember(coroutineScope, reducedMotion, pagerState) {
        { route ->
            rootTabIndexForRoute(route.path)?.let { pageIndex ->
                coroutineScope.launch {
                    if (reducedMotion) pagerState.scrollToPage(pageIndex)
                    else pagerState.animateScrollToPage(pageIndex)
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            FeedbackSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.feedbackSnackbarInsets(
                    bottomPadding = if (isMainTab && !useNavigationRail) 104.dp else 16.dp,
                ),
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        CompositionLocalProvider(
            LocalNotePayBackdrop provides backdrop,
            LocalAuthUser provides authUser,
            LocalHapticFeedbackEnabled provides hapticFeedbackEnabled,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (useNavigationGlass) Modifier.layerBackdrop(backdrop) else Modifier)
                ) {
                    SharedTransitionLayout {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier.fillMaxSize(),
                            enterTransition = {
                                notePayForwardEnterTransition(
                                    initialRoute = initialState.destination.route,
                                    targetRoute = targetState.destination.route,
                                    reducedMotion = reducedMotion,
                                    tabDistancePx = motionDistancePx,
                                )
                            },
                            exitTransition = {
                                notePayForwardExitTransition(
                                    initialRoute = initialState.destination.route,
                                    targetRoute = targetState.destination.route,
                                    reducedMotion = reducedMotion,
                                    tabDistancePx = motionDistancePx,
                                )
                            },
                            popEnterTransition = {
                                if (reducedMotion) {
                                    EnterTransition.None
                                } else {
                                    fadeIn(tween(NotePayMotion.contentDurationMillis)) +
                                        scaleIn(
                                            initialScale = 0.95f,
                                            transformOrigin = TransformOrigin(0.5f, 0.5f),
                                            animationSpec = tween(NotePayMotion.contentDurationMillis),
                                        )
                                }
                            },
                            popExitTransition = {
                                if (reducedMotion) {
                                    ExitTransition.None
                                } else {
                                    slideOutHorizontally(
                                        animationSpec = tween(NotePayMotion.contentFadeOutDurationMillis),
                                        targetOffsetX = { motionDistancePx },
                                    ) + fadeOut(tween(NotePayMotion.contentFadeOutDurationMillis))
                                }
                            },
                        ) {
                            composable(Route.Welcome.path) {
                                OnboardingRoute(
                                    onComplete = {
                                        navController.navigate(Route.Home.path) {
                                            popUpTo(navController.graph.id) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                )
                            }
                            composable(Route.Home.path) {
                                MainTabPager(
                                    pagerState = pagerState,
                                    navController = navController,
                                    showFeedback = showFeedback,
                                    reducedMotion = reducedMotion,
                                    userScrollEnabled = !useNavigationRail,
                                    contentStartInset = if (useNavigationRail) {
                                        NotePayAdaptive.navigationRailInset
                                    } else {
                                        0.dp
                                    },
                                )
                            }
                            transactionGraph(navController, showFeedback, ::onTransactionSaved)
                            statsScreen(
                                navController = navController,
                                onNavigateToTab = navigateToRootTab,
                            )
                            billSplitGraph(navController, showFeedback, ::showFeedbackAfterReturn)
                            subscriptionScreen(navController, showFeedback = showFeedback)
                            walletGraph(
                                navController = navController,
                                showFeedback = showFeedback,
                                onWalletSaved = ::showFeedbackAfterReturn,
                                onNavigateToTab = navigateToRootTab,
                            )
                            utilitiesGraph(
                                navController = navController,
                                onFeedback = showFeedback,
                                onNavigateToTab = navigateToRootTab,
                            )
                            debtGraph(
                                navController = navController,
                                showFeedback = showFeedback,
                                onFeedbackAfterReturn = ::showFeedbackAfterReturn,
                            )
                        }
                    }
                }

                if (isMainTab) {
                    if (useNavigationRail) {
                        NotePayNavigationRail(
                            currentRoute = currentRoute,
                            selectedTabIndex = pagerState.targetPage,
                            useNavigationGlass = useNavigationGlass,
                            reducedMotion = reducedMotion,
                            backdrop = backdrop,
                            onTabSelected = navigateToRootTab,
                        )
                        // Rail không có chỗ cho FAB nên nút Thêm nhanh về góc dưới - phải.
                        FloatingAddButton(
                            expanded = showQuickAddSheet,
                            contentDescription = stringResource(R.string.quick_add_open_menu),
                            onClick = { showQuickAddSheet = !showQuickAddSheet },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .zIndex(2f)
                                .padding(end = 24.dp, bottom = 24.dp)
                                .navigationBarsPadding(),
                        )
                    } else {
                        NotePayBottomBar(
                            currentRoute = currentRoute,
                            selectedTabIndex = pagerState.targetPage,
                            showQuickAddSheet = showQuickAddSheet,
                            useNavigationGlass = useNavigationGlass,
                            reducedMotion = reducedMotion,
                            backdrop = backdrop,
                            onTabSelected = navigateToRootTab,
                            onToggleQuickAdd = { showQuickAddSheet = !showQuickAddSheet },
                        )
                    }

                    if (showQuickAddSheet) {
                        CreateNewBottomSheet(
                            onDismissRequest = { showQuickAddSheet = false },
                            onAction = { action ->
                                showQuickAddSheet = false
                                quickAddAction = action
                            },
                        )
                    }

                    if (showNaturalEntrySheet) {
                        NaturalEntryBottomSheet(
                            onDismiss = { showNaturalEntrySheet = false },
                            onFeedback = feedbackDispatcher::show,
                            onOpenAiSettings = {
                                showNaturalEntrySheet = false
                                navController.navigate(Route.AiSettings.path) { launchSingleTop = true }
                            },
                        )
                    }

                    QuickTransferSheetHost(
                        viewModel = quickTransferViewModel,
                        onFeedback = feedbackDispatcher::show,
                    )
                }

            }
        }
    }
}

@Composable
private fun QuickTransferSheetHost(
    viewModel: QuickTransferViewModel,
    onFeedback: (UiFeedback) -> Unit,
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, onFeedback) {
        viewModel.feedback.collect { feedback ->
            onFeedback(feedback)
        }
    }

    if (form.isVisible) {
        TransferBottomSheet(
            form = form,
            wallets = wallets,
            onDismiss = viewModel::close,
            onFromWalletChange = viewModel::onFromWalletChanged,
            onToWalletChange = viewModel::onToWalletChanged,
            onAmountChange = viewModel::onAmountChanged,
            onNoteChange = viewModel::onNoteChanged,
            onConfirm = viewModel::executeTransfer,
        )
    }
}
