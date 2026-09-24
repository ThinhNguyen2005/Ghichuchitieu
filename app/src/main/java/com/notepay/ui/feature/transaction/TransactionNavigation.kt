package com.notepay.ui.feature.transaction

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.notepay.ui.feature.transaction.add.AddTransactionScreen
import com.notepay.ui.feature.transaction.detail.TransactionDetailScreen
import com.notepay.ui.feature.transaction.edit.EditTransactionScreen
import com.notepay.ui.feature.transaction.list.TransactionListScreen
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.navigation.Route

fun NavGraphBuilder.transactionGraph(
    navController: NavController,
    showFeedback: suspend (UiFeedback) -> Boolean,
) {
    composable(Route.AddTransaction.path) {
        AddTransactionScreen(
            onSaved = { navController.popBackStack() },
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
            onFeedback = showFeedback,
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
        TransactionListScreen(
            onBack = { navController.popBackStack() },
            onTransactionClick = { txId ->
                navController.navigate(Route.TransactionDetail(txId).path)
            },
            onEditTransaction = { txId ->
                navController.navigate(Route.EditTransaction(txId).path)
            },
            onFeedback = showFeedback,
        )
    }
}
