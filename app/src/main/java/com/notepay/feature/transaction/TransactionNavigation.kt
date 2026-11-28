package com.notepay.feature.transaction

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.notepay.core.navigation.PredictiveBackDestination
import com.notepay.feature.transaction.form.TransactionFormScreen
import com.notepay.feature.transaction.form.TransactionFormSaveResult
import com.notepay.feature.transaction.form.TransactionFormViewModel
import com.notepay.feature.transaction.detail.TransactionDetailScreen
import com.notepay.feature.transaction.list.TransactionListRoute
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.navigation.Route

fun NavGraphBuilder.transactionGraph(
    navController: NavController,
    showFeedback: suspend (UiFeedback) -> Boolean,
    onTransactionSaved: (NavBackStackEntry, TransactionFormSaveResult) -> Unit,
) {
    composable(
        route = Route.AddTransaction.ROUTE,
        arguments = listOf(
            navArgument(Route.AddTransaction.ARG_LAUNCH_SCAN) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
    ) { entry ->
        val launchScanArg = entry.arguments?.getBoolean(Route.AddTransaction.ARG_LAUNCH_SCAN) ?: false
        val isHandled = entry.savedStateHandle.get<Boolean>("launchScanHandled") ?: false
        if (launchScanArg && !isHandled) {
            entry.savedStateHandle["launchScan"] = true
            entry.savedStateHandle["launchScanHandled"] = true
        } else if (!launchScanArg) {
            entry.savedStateHandle["launchScanHandled"] = false
        }
        val launchScan by entry.savedStateHandle.getStateFlow("launchScan", false).collectAsStateWithLifecycle()

        val viewModel: TransactionFormViewModel = hiltViewModel()
        PredictiveBackDestination(
            onBack = { navController.popBackStack() },
            canGoBack = { !viewModel.state.value.isSaving },
        ) {
            TransactionFormScreen(
                onSaved = { onTransactionSaved(entry, it) },
                onBack = { navController.popBackStack() },
                onOpenAiSettings = {
                    navController.navigate(Route.AiSettings.path) { launchSingleTop = true }
                },
                initialLaunchScan = launchScan,
                onClearLaunchScan = { entry.savedStateHandle["launchScan"] = false },
                viewModel = viewModel,
            )
        }
    }
    composable(
        route = Route.EditTransaction.ROUTE,
        arguments = listOf(navArgument(Route.EditTransaction.ARG_ID) { type = NavType.LongType }),
    ) { entry ->
        val viewModel: TransactionFormViewModel = hiltViewModel()
        PredictiveBackDestination(
            onBack = { navController.popBackStack() },
            canGoBack = { !viewModel.state.value.isSaving },
        ) {
            TransactionFormScreen(
                onSaved = { onTransactionSaved(entry, it) },
                onBack = { navController.popBackStack() },
                onOpenAiSettings = {
                    navController.navigate(Route.AiSettings.path) { launchSingleTop = true }
                },
                viewModel = viewModel,
            )
        }
    }
    composable(
        route = Route.TransactionDetail.ROUTE,
        arguments = listOf(navArgument(Route.TransactionDetail.ARG_ID) { type = NavType.LongType }),
    ) {
        PredictiveBackDestination(onBack = { navController.popBackStack() }) {
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
                onFeedback = showFeedback,
            )
        }
    }
    composable(
        route = "${Route.TransactionList.path}?walletId={walletId}",
        arguments = listOf(
            navArgument("walletId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {
        PredictiveBackDestination(onBack = { navController.popBackStack() }) {
            TransactionListRoute(
                onBack = { navController.popBackStack() },
                onTransactionClick = { txId ->
                    navController.navigate(Route.TransactionDetail(txId).path)
                },
                onEditTransaction = { txId ->
                    navController.navigate(Route.EditTransaction(txId).path)
                },
                // Pane chi tiết ở chế độ Expanded dùng chung hai đích này với route chi tiết.
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
                onFeedback = showFeedback,
            )
        }
    }
}
