package com.notepay.ui.feature.stats

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.notepay.ui.navigation.Route

fun NavGraphBuilder.statsScreen(
    navController: NavController,
    onNavigateToTab: ((Int) -> Unit)? = null,
) {
    composable(Route.Stats.path) {
        if (onNavigateToTab != null) {
            LaunchedEffect(Unit) {
                onNavigateToTab(1)
                navController.popBackStack(Route.Home.path, inclusive = false)
            }
        } else {
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
}
