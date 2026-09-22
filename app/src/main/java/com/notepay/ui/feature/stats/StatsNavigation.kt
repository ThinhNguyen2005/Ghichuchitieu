package com.notepay.ui.feature.stats

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.notepay.ui.navigation.Route

fun NavGraphBuilder.statsScreen(
    navController: NavController,
) {
    composable(Route.Stats.path) {
        StatsScreen(
            onAddTransaction = { navController.navigate(Route.AddTransaction.path) },
            onTransactionClick = { txId ->
                navController.navigate(Route.TransactionDetail(txId).path)
            },
            onConfigureLocalModel = {
                navController.navigate(Route.AppSettings.path)
            },
        )
    }
}
