package com.notepay.ui.feature.home

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.notepay.ui.navigation.Route

fun NavGraphBuilder.homeScreen(
    navController: NavController,
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
            onTransactionClick = { txId ->
                navController.navigate(Route.TransactionDetail(txId).path)
            },
        )
    }
}
