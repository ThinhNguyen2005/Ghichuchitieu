package com.notepay.ui.feature.debt

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.notepay.ui.navigation.Route

fun NavGraphBuilder.debtGraph(
    navController: NavController,
) {
    composable(Route.DebtManagement.path) {
        DebtManagementScreen(
            onBack = { navController.popBackStack() },
            onDebtClick = { debtId ->
                navController.navigate(Route.DebtDetail(debtId).path)
            },
        )
    }

    composable(
        route = Route.DebtDetail.ROUTE,
        arguments = listOf(
            navArgument(Route.DebtDetail.ARG_ID) { type = NavType.LongType }
        )
    ) {
        DebtDetailScreen(
            onBack = { navController.popBackStack() },
        )
    }
}
