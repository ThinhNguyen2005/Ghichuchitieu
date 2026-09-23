package com.notepay.ui.feature.subscription

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

fun NavGraphBuilder.subscriptionScreen(
    navController: NavController,
    navigationBarOffset: Float = 0f,
) {
    composable(
        route = "subscription?showCreate={showCreate}",
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
        SubscriptionScreen(
            navigationBarOffset = navigationBarOffset,
            initialShowCreate = showCreate,
            onBack = { navController.popBackStack() },
            onClearShowCreate = {
                backStackEntry.savedStateHandle["showCreate"] = false
            }
        )
    }
}
