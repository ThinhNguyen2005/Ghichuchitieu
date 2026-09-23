package com.notepay.ui.feature.wallet

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.navigation.Route

fun NavGraphBuilder.walletGraph(
    navController: NavController,
    showFeedback: suspend (UiFeedback) -> Boolean,
    onNavigateToTab: ((Int) -> Unit)? = null,
) {
    composable(Route.Assets.path) {
        if (onNavigateToTab != null) {
            LaunchedEffect(Unit) {
                onNavigateToTab(2)
                navController.popBackStack(Route.Home.path, inclusive = false)
            }
        } else {
            AssetsScreen(
                onAddWallet = { navController.navigate(Route.AddWallet.path) },
                onEditWallet = { walletId -> navController.navigate(Route.EditWallet(walletId).path) },
                onWalletClick = { walletId ->
                    navController.navigate("${Route.TransactionList.path}?walletId=$walletId")
                },
                onFeedback = showFeedback,
            )
        }
    }
    composable(Route.AddWallet.path) {
        AddWalletScreen(
            onSaved = { feedback ->
                navController.popBackStack()
                showFeedback(feedback)
            },
            onBack = { navController.popBackStack() },
            onFeedback = showFeedback,
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
            onFeedback = showFeedback,
        )
    }
}
