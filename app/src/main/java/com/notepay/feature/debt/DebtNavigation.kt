package com.notepay.feature.debt

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    composable(
        route = Route.DebtManagement.ROUTE,
        arguments = listOf(
            navArgument(Route.DebtManagement.ARG_SHOW_CREATE) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
    ) { backStackEntry ->
        val showCreateArg = backStackEntry.arguments?.getBoolean(Route.DebtManagement.ARG_SHOW_CREATE) ?: false
        val isHandled = backStackEntry.savedStateHandle.get<Boolean>("showCreateHandled") ?: false

        if (showCreateArg && !isHandled) {
            backStackEntry.savedStateHandle["showCreate"] = true
            backStackEntry.savedStateHandle["showCreateHandled"] = true
        } else if (!showCreateArg) {
            backStackEntry.savedStateHandle["showCreateHandled"] = false
        }

        val showCreateFlow = backStackEntry.savedStateHandle.getStateFlow("showCreate", false)
        val showCreate by showCreateFlow.collectAsStateWithLifecycle()

        PredictiveBackDestination(onBack = { navController.popBackStack() }) {
            DebtManagementScreen(
                onBack = { navController.popBackStack() },
                onDebtClick = { debtId ->
                    navController.navigate(Route.DebtDetail(debtId).path)
                },
                initialShowCreate = showCreate,
                onClearShowCreate = {
                    backStackEntry.savedStateHandle["showCreate"] = false
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
