package com.notepay.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.notepay.platform.OsCompatHelper
import com.notepay.ui.component.LocalNotePayBackdrop
import com.notepay.ui.feature.billsplit.billSplitGraph
import com.notepay.ui.feature.home.homeScreen
import com.notepay.ui.feature.stats.statsScreen
import com.notepay.ui.feature.subscription.subscriptionScreen
import com.notepay.ui.feature.transaction.transactionGraph
import com.notepay.ui.feature.utilities.utilitiesGraph
import com.notepay.ui.feature.wallet.walletGraph
import com.notepay.ui.feedback.FeedbackDuration
import com.notepay.ui.feedback.UiFeedback
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NotePayNavHost(
    navController: NavHostController = rememberNavController(),
    liquidGlassEnabled: Boolean = false,
) {
    val useNavigationGlass = liquidGlassEnabled && OsCompatHelper.liquidGlassCompatibility(
        isHardwareAccelerated = LocalView.current.isHardwareAccelerated,
    ).isSupported
    val reducedMotion = rememberNotePayReducedMotion()
    val motionDistancePx = with(LocalDensity.current) {
        NotePayMotion.contentTranslation.roundToPx()
    }
    val systemBackground = MaterialTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(systemBackground)
        drawContent()
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isMainTab = isMainTabRoute(currentRoute)
    val snackbarHostState = remember { SnackbarHostState() }
    var showQuickAddSheet by rememberSaveable { mutableStateOf(false) }

    val barHeightPx = with(LocalDensity.current) { 104.dp.toPx() }
    var navigationBarOffset by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentRoute) {
        navigationBarOffset = 0f
        showQuickAddSheet = false
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
                    if (reducedMotion) {
                        navigationBarOffset = targetOffset
                    } else {
                        coroutineScope.launch {
                            Animatable(navigationBarOffset).animateTo(
                                targetValue = targetOffset,
                                animationSpec = tween(
                                    durationMillis = NotePayMotion.navigationBarSettleDurationMillis
                                )
                            ) {
                                navigationBarOffset = this.value
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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 104.dp)
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) { padding ->
        CompositionLocalProvider(LocalNotePayBackdrop provides backdrop) {
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
                            startDestination = Route.Home.path,
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
                        ) {
                            homeScreen(navController)
                            transactionGraph(navController, ::showFeedback)
                            statsScreen(navController)
                            billSplitGraph(navController, navigationBarOffset, ::showFeedback)
                            subscriptionScreen(navController, navigationBarOffset)
                            walletGraph(navController, ::showFeedback)
                            utilitiesGraph(navController)
                        }
                    }
                }

                if (isMainTab) {
                    NotePayBottomBar(
                        currentRoute = currentRoute,
                        navigationBarOffset = navigationBarOffset,
                        showQuickAddSheet = showQuickAddSheet,
                        useNavigationGlass = useNavigationGlass,
                        reducedMotion = reducedMotion,
                        backdrop = backdrop,
                        onTabSelected = { route ->
                            navController.navigate(route.path) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onToggleQuickAdd = {
                            showQuickAddSheet = !showQuickAddSheet
                        },
                    )
                }

                QuickAddSheet(
                    visible = showQuickAddSheet,
                    onDismissRequest = { showQuickAddSheet = false },
                    onAddExpense = {
                        showQuickAddSheet = false
                        navController.navigate(Route.AddTransaction.path) {
                            launchSingleTop = true
                        }
                    },
                    onAddBillSplit = {
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
                        }
                    },
                    onAddSubscription = {
                        showQuickAddSheet = false
                        val isAlreadyOnSubscription = currentRoute?.startsWith("subscription") == true
                        if (isAlreadyOnSubscription) {
                            navController.currentBackStackEntry?.savedStateHandle?.set("showCreate", true)
                        } else {
                            navController.navigate("subscription?showCreate=true") {
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
        }
    }
}
