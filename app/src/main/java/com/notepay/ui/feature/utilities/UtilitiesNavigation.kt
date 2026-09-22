package com.notepay.ui.feature.utilities

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.notepay.ui.feature.backup.BackupRestoreScreen
import com.notepay.ui.feature.home.AppSettingsScreen
import com.notepay.ui.navigation.Route

fun NavGraphBuilder.utilitiesGraph(
    navController: NavController,
) {
    composable(Route.Utilities.path) {
        UtilitiesScreen(
            onNavigateToBillSplit = {
                navController.navigate(Route.BillSplit.path) {
                    launchSingleTop = true
                }
            },
            onNavigateToSubscription = {
                navController.navigate(Route.Subscription.path) {
                    launchSingleTop = true
                }
            }
        )
    }
    composable(Route.BackupRestore.path) {
        BackupRestoreScreen(onBack = { navController.popBackStack() })
    }
    composable(Route.AppSettings.path) {
        AppSettingsScreen(
            onBack = { navController.popBackStack() },
            onNavigateToBackupRestore = { navController.navigate(Route.BackupRestore.path) },
        )
    }
}
