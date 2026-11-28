package com.notepay.core.navigation

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.notepay.R
import com.notepay.core.designsystem.component.action.CreateNewBottomSheet
import com.notepay.core.designsystem.component.action.QuickAddAction
import com.notepay.core.designsystem.theme.NotePayTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotePayMotionAndNavigationContractTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun quickAddSheetNavigatesToAddTransactionWithForwardTransition() {
        val openSheetButton = "Open quick add"
        val expenseTitle = context.getString(R.string.quick_add_expense_title)
        val addTransactionContent = "AddTransactionStub"
        var navControllerRef: NavHostController? = null

        composeRule.setContent {
            val navController = rememberNavController()
            navControllerRef = navController
            var showQuickAddSheet by remember { mutableStateOf(false) }
            var pendingAction by remember { mutableStateOf<QuickAddAction?>(null) }

            LaunchedEffect(pendingAction) {
                val action = pendingAction ?: return@LaunchedEffect
                pendingAction = null
                if (action == QuickAddAction.Transaction) {
                    navController.navigate(Route.AddTransaction.path) { launchSingleTop = true }
                }
            }

            NotePayTheme(darkTheme = false) {
                LightweightAddTransactionNavHost(
                    navController = navController,
                    homeContent = {
                        Button(onClick = { showQuickAddSheet = true }) {
                            Text(openSheetButton)
                        }
                    },
                    addTransactionContent = { Text(addTransactionContent) },
                )
                if (showQuickAddSheet) {
                    CreateNewBottomSheet(
                        onDismissRequest = { showQuickAddSheet = false },
                        onAction = { action ->
                            showQuickAddSheet = false
                            pendingAction = action
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithText(openSheetButton).performClick()
        composeRule.onNodeWithText(expenseTitle).assertIsDisplayed().performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) {
            navControllerRef?.currentBackStackEntry?.destination?.route
                ?.startsWith(Route.AddTransaction.path) == true
        }
        composeRule.runOnIdle {
            assertEquals(Route.AddTransaction.ROUTE, navControllerRef?.currentBackStackEntry?.destination?.route)
        }
        composeRule.onNodeWithText(expenseTitle).assertDoesNotExist()
        composeRule.onNodeWithText(addTransactionContent).assertIsDisplayed()
    }

    @Test
    fun bottomBarTabsAndFloatingAddToggleExposeSelectionAndDescriptions() {
        val homeLabel = context.getString(R.string.nav_home)
        val assetsLabel = context.getString(R.string.nav_assets)
        val openMenuDesc = context.getString(R.string.quick_add_open_menu)
        val closeMenuDesc = context.getString(R.string.quick_add_close_menu)
        var currentRoute by mutableStateOf(Route.Home.path)
        var showQuickAdd by mutableStateOf(false)
        val selectedRoutes = mutableListOf<Route>()

        composeRule.setContent {
            NotePayTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    NotePayBottomBar(
                        currentRoute = currentRoute,
                        showQuickAddSheet = showQuickAdd,
                        useNavigationGlass = false,
                        reducedMotion = false,
                        backdrop = rememberLayerBackdrop(),
                        onTabSelected = { route ->
                            selectedRoutes.add(route)
                            currentRoute = route.path
                        },
                        onToggleQuickAdd = { showQuickAdd = !showQuickAdd },
                    )
                }
            }
        }

        composeRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertCountEquals(4)
        composeRule.onNodeWithText(homeLabel)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
        composeRule.onNodeWithText(assetsLabel).assertIsNotSelected().performClick()

        composeRule.runOnIdle { assertEquals(listOf(Route.Assets), selectedRoutes) }
        composeRule.onNodeWithText(assetsLabel).assertIsSelected()
        composeRule.onNodeWithText(homeLabel).assertIsNotSelected()

        composeRule.onNodeWithContentDescription(openMenuDesc).assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(closeMenuDesc).assertIsDisplayed()
    }

    @Test
    fun motionRouteMathAndIndicatorOffsetFollowContract() {
        assertEquals(0, rootTabIndexForRoute(Route.Home.path))
        assertEquals(2, rootTabIndexForRoute("${Route.Assets.path}?walletId=1"))
        assertNull(rootTabIndexForRoute(Route.AddTransaction.path))
        assertNull(rootTabIndexForRoute(null))

        assertEquals(1, tabTransitionDirection(Route.Home.path, Route.Stats.path))
        assertEquals(-1, tabTransitionDirection(Route.Utilities.path, Route.Home.path))
        assertNull(tabTransitionDirection(Route.Home.path, Route.Home.path))
        assertNull(tabTransitionDirection(Route.Home.path, Route.AddTransaction.path))

        assertEquals(200f, tabIndicatorOffsetPx(2, 4, 100f), 0.001f)
        assertEquals(300f, tabIndicatorOffsetPx(9, 4, 100f), 0.001f)
        assertEquals(0f, tabIndicatorOffsetPx(0, 0, 100f), 0.001f)
    }

    @Test
    fun reducedMotionAndStaggeredEntranceRenderContentCleanly() {
        val staggeredLabel = "StaggeredBlock"
        val reportLabel = context.getString(R.string.nav_report)
        var currentRoute by mutableStateOf(Route.Home.path)

        composeRule.setContent {
            NotePayTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    NotePayStaggeredEntrance(index = 0) {
                        Text(staggeredLabel)
                    }
                    NotePayBottomBar(
                        currentRoute = currentRoute,
                        showQuickAddSheet = false,
                        useNavigationGlass = false,
                        reducedMotion = true,
                        backdrop = rememberLayerBackdrop(),
                        onTabSelected = { currentRoute = it.path },
                        onToggleQuickAdd = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText(staggeredLabel).assertIsDisplayed()
        composeRule.onNodeWithText(reportLabel).performClick()
        composeRule.onNodeWithText(reportLabel).assertIsSelected()
    }

    @Test
    fun predictiveBackDestinationPopsOnlyWhenCanGoBackIsTrue() {
        val homeLabel = "HomeDestination"
        val addLabel = "AddDestination"
        var canGoBack by mutableStateOf(false)
        var navControllerRef: NavHostController? = null
        var backDispatcher: OnBackPressedDispatcher? = null

        composeRule.setContent {
            val navController = rememberNavController()
            navControllerRef = navController
            backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

            LaunchedEffect(navController) {
                navController.navigate(Route.AddTransaction.path)
            }

            NotePayTheme(darkTheme = false) {
                LightweightAddTransactionNavHost(
                    navController = navController,
                    canGoBack = { canGoBack },
                    homeContent = { Text(homeLabel) },
                    addTransactionContent = { Text(addLabel) },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            navControllerRef?.currentBackStackEntry?.destination?.route
                ?.startsWith(Route.AddTransaction.path) == true
        }
        composeRule.onNodeWithText(addLabel).assertIsDisplayed()

        composeRule.runOnUiThread { backDispatcher?.onBackPressed() }
        composeRule.runOnIdle {
            assertEquals(Route.AddTransaction.ROUTE, navControllerRef?.currentBackStackEntry?.destination?.route)
        }

        canGoBack = true
        composeRule.runOnUiThread { backDispatcher?.onBackPressed() }
        composeRule.waitUntil(timeoutMillis = 3_000) {
            navControllerRef?.currentBackStackEntry?.destination?.route == Route.Home.path
        }
        composeRule.onNodeWithText(homeLabel).assertIsDisplayed()
    }

    @Composable
    private fun LightweightAddTransactionNavHost(
        navController: NavHostController,
        canGoBack: () -> Boolean = { navController.previousBackStackEntry != null },
        homeContent: @Composable () -> Unit,
        addTransactionContent: @Composable () -> Unit,
    ) {
        val motionDistancePx = with(LocalDensity.current) {
            NotePayMotion.contentTranslation.roundToPx()
        }
        val reducedMotion = rememberNotePayReducedMotion()
        NavHost(
            navController = navController,
            startDestination = Route.Home.path,
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
        ) {
            composable(Route.Home.path) {
                PredictiveBackDestination(canGoBack = { false }) {
                    homeContent()
                }
            }
            composable(
                route = Route.AddTransaction.ROUTE,
                arguments = listOf(
                    navArgument(Route.AddTransaction.ARG_LAUNCH_SCAN) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) {
                PredictiveBackDestination(
                    onBack = { navController.popBackStack() },
                    canGoBack = canGoBack,
                ) {
                    addTransactionContent()
                }
            }
        }
    }
}
