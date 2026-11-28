package com.notepay.feature.debt

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.feature.debt.detail.DebtDetailScreen
import com.notepay.feature.debt.management.DebtManagementScreen
import com.notepay.core.navigation.PredictiveBackDestination
import com.notepay.core.navigation.Route

fun NavGraphBuilder.debtGraph(
    navController: NavController,
    showFeedback: suspend (UiFeedback) -> Boolean,
    onFeedbackAfterReturn: (NavBackStackEntry, UiFeedback) -> Unit,
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
                onFeedback = showFeedback,
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
    ) { entry ->
        PredictiveBackDestination(onBack = { navController.popBackStack() }) {
            DebtDetailScreen(
                onBack = { navController.popBackStack() },
                onFeedback = showFeedback,
                onDeleted = { feedback -> onFeedbackAfterReturn(entry, feedback) },
            )
        }
    }
}
