package com.notepay.ui.feature.wallet

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
) {
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
