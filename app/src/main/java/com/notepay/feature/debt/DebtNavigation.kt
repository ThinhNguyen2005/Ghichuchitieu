package com.notepay.feature.debt

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.notepay.feature.debt.detail.DebtDetailScreen
import com.notepay.feature.debt.management.DebtManagementScreen
import com.notepay.core.navigation.PredictiveBackDestination
import com.notepay.core.navigation.Route

fun NavGraphBuilder.debtGraph(
    navController: NavController,
) {
    composable(Route.DebtManagement.path) {
        PredictiveBackDestination(onBack = { navController.popBackStack() }) {
            DebtManagementScreen(
                onBack = { navController.popBackStack() },
                onDebtClick = { debtId ->
                    navController.navigate(Route.DebtDetail(debtId).path)
                },
            )
        }
    }

    composable(
        route = Route.DebtDetail.ROUTE,
        arguments = listOf(
            navArgument(Route.DebtDetail.ARG_ID) { type = NavType.LongType }
        )
    ) {
        PredictiveBackDestination(onBack = { navController.popBackStack() }) {
            DebtDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
