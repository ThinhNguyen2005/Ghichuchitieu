package com.notepay.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.pager.rememberPagerState
import androidx.navigation.compose.composable
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.notepay.platform.OsCompatHelper
import com.notepay.R
import com.notepay.ui.adaptive.LocalWindowLayout
import com.notepay.ui.adaptive.NotePayAdaptive
import com.notepay.ui.adaptive.usesNavigationRail
import com.notepay.ui.component.FloatingAddButton
import com.notepay.ui.component.LocalNotePayBackdrop
import com.notepay.ui.feature.billsplit.billSplitGraph
import com.notepay.ui.feature.onboarding.OnboardingRoute
import com.notepay.ui.feature.stats.statsScreen
import com.notepay.ui.feature.subscription.subscriptionScreen
import com.notepay.ui.feature.transaction.transactionGraph
import com.notepay.ui.feature.debt.debtGraph
import com.notepay.ui.feature.utilities.utilitiesGraph
import com.notepay.ui.feature.wallet.walletGraph
import com.notepay.ui.feedback.FeedbackDuration
import com.notepay.ui.feedback.UiFeedback
import com.notepay.domain.model.AuthUser
import com.notepay.ui.util.LocalAuthUser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NotePayNavHost(
    navController: NavHostController = rememberNavController(),
    authUser: AuthUser? = null,
    startDestination: String = Route.Home.path,
    liquidGlassEnabled: Boolean = false,
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
    val barHeightPx = with(LocalDensity.current) { 104.dp.toPx() }
    val navigationBarOffsetState = remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            navController.navigate(pendingRoute) {
                launchSingleTop = true
            }
            onRouteHandled()
        }
    }

    LaunchedEffect(currentRoute, pagerState.currentPage) {
        navigationBarOffsetState.floatValue = 0f
    }

    val nestedScrollConnection = remember(barHeightPx, isMainTab) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isMainTab) {
                    val delta = available.y
                    navigationBarOffsetState.floatValue =
                        (navigationBarOffsetState.floatValue - delta).coerceIn(0f, barHeightPx)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (isMainTab) {
                    val currentOffset = navigationBarOffsetState.floatValue
                    val targetOffset = if (currentOffset > barHeightPx / 2f) barHeightPx else 0f
                    if (reducedMotion) {
                        navigationBarOffsetState.floatValue = targetOffset
                    } else {
                        coroutineScope.launch {
                            Animatable(currentOffset).animateTo(
                                targetValue = targetOffset,
                                animationSpec = tween(
                                    durationMillis = NotePayMotion.navigationBarSettleDurationMillis
                                )
                            ) {
                                navigationBarOffsetState.floatValue = this.value
                            }
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
        val actionPerformed = result == SnackbarResult.ActionPerformed
        if (actionPerformed) feedback.onAction?.invoke()
        return actionPerformed
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
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = if (useNavigationRail) 16.dp else 104.dp)
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) { padding ->
        CompositionLocalProvider(
            LocalNotePayBackdrop provides backdrop,
            LocalAuthUser provides authUser,
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
                                val direction = tabTransitionDirection(
                                    initialState.destination.route,
                                    targetState.destination.route,
                                )
                                when {
                                    reducedMotion -> EnterTransition.None
                                    direction == null -> fadeIn(
                                        tween(NotePayMotion.contentDurationMillis)
                                    )
                                    else -> fadeIn(
                                        tween(
                                            durationMillis = NotePayMotion.contentDurationMillis,
                                            delayMillis = NotePayMotion.contentFadeDelayMillis,
                                        )
                                    ) + slideInHorizontally(
                                        animationSpec = tween(NotePayMotion.contentDurationMillis),
                                        initialOffsetX = { direction * motionDistancePx },
                                    )
                                }
                            },
                            exitTransition = {
                                val direction = tabTransitionDirection(
                                    initialState.destination.route,
                                    targetState.destination.route,
                                )
                                when {
                                    reducedMotion -> ExitTransition.None
                                    direction == null -> fadeOut(
                                        tween(NotePayMotion.contentFadeOutDurationMillis)
                                    )
                                    else -> fadeOut(
                                        tween(NotePayMotion.contentFadeOutDurationMillis)
                                    ) + slideOutHorizontally(
                                        animationSpec = tween(NotePayMotion.contentFadeOutDurationMillis),
                                        targetOffsetX = { -direction * motionDistancePx },
                                    )
                                }
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
                                        targetOffsetX = { it },
                                    ) + fadeOut(tween(NotePayMotion.contentFadeOutDurationMillis))
                                }
                            },
                        ) {
                            composable(Route.Welcome.path) {
                                OnboardingRoute(
                                    onComplete = {
                                        navController.navigate(Route.Home.path) {
                                            popUpTo(Route.Welcome.path) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(Route.Home.path) {
                                MainTabPager(
                                    pagerState = pagerState,
                                    navController = navController,
                                    showFeedback = ::showFeedback,
                                    reducedMotion = reducedMotion,
                                    userScrollEnabled = !useNavigationRail,
                                    contentStartInset = if (useNavigationRail) {
                                        NotePayAdaptive.navigationRailInset
                                    } else {
                                        0.dp
                                    },
                                )
                            }
                            transactionGraph(navController, ::showFeedback)
                            statsScreen(
                                navController = navController,
                                onNavigateToTab = navigateToRootTab,
                            )
                            billSplitGraph(navController, ::showFeedback)
                            subscriptionScreen(navController)
                            walletGraph(
                                navController = navController,
                                showFeedback = ::showFeedback,
                                onNavigateToTab = navigateToRootTab,
                            )
                            utilitiesGraph(
                                navController = navController,
                                onNavigateToTab = navigateToRootTab,
                            )
                            debtGraph(navController)
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
                            expanded = false,
                            contentDescription = stringResource(R.string.quick_add_open_menu),
                            onClick = {
                                navController.navigate(Route.AddTransaction.path) {
                                    launchSingleTop = true
                                }
                            },
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
                            navigationBarOffsetProvider = { navigationBarOffsetState.floatValue },
                            showQuickAddSheet = false,
                            useNavigationGlass = useNavigationGlass,
                            reducedMotion = reducedMotion,
                            backdrop = backdrop,
                            onTabSelected = navigateToRootTab,
                            onToggleQuickAdd = {
                                navController.navigate(Route.AddTransaction.path) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }

            }
        }
    }
}
