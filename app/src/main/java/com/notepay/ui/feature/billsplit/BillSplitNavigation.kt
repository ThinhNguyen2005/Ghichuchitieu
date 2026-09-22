package com.notepay.ui.feature.billsplit

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.navigation.Route

fun NavGraphBuilder.billSplitGraph(
    navController: NavController,
    navigationBarOffset: Float,
    showFeedback: suspend (UiFeedback) -> Boolean,
) {
    composable(
        route = "bill-split?showCreate={showCreate}",
        arguments = listOf(navArgument("showCreate") { type = NavType.BoolType; defaultValue = false })
    ) { backStackEntry ->
        val arguments = backStackEntry.arguments
        val showCreateArg = arguments?.getBoolean("showCreate") ?: false
        val isHandled = backStackEntry.savedStateHandle.get<Boolean>("showCreateHandled") ?: false

        if (showCreateArg && !isHandled) {
            backStackEntry.savedStateHandle["showCreate"] = true
            backStackEntry.savedStateHandle["showCreateHandled"] = true
        } else if (!showCreateArg) {
            backStackEntry.savedStateHandle["showCreateHandled"] = false
        }

        val showCreateFlow = backStackEntry.savedStateHandle.getStateFlow("showCreate", false)
        val showCreate by showCreateFlow.collectAsState()
        BillSplitScreen(
            onDebtorClick = { debtorName ->
                navController.navigate(Route.DebtorDetail(debtorName).path)
            },
            onFeedback = showFeedback,
            navigationBarOffset = navigationBarOffset,
            initialShowCreate = showCreate,
            onClearShowCreate = {
                backStackEntry.savedStateHandle["showCreate"] = false
            }
        )
    }
    composable(
        route = Route.DebtorDetail.ROUTE,
        arguments = listOf(navArgument(Route.DebtorDetail.ARG_NAME) { type = NavType.StringType }),
    ) { backStackEntry ->
        val debtorName = backStackEntry.arguments?.getString(Route.DebtorDetail.ARG_NAME).orEmpty()
        DebtorDetailScreen(
            debtorName = debtorName,
            onBack = { navController.popBackStack() },
            onFeedback = showFeedback,
        )
    }
}
