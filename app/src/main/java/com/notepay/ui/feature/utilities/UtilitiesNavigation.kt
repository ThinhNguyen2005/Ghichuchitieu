package com.notepay.ui.feature.utilities

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.notepay.ui.feature.backup.BackupRestoreScreen
import com.notepay.ui.feature.home.AppSettingsScreen
import com.notepay.ui.navigation.Route

fun NavGraphBuilder.utilitiesGraph(
    navController: NavController,
    onNavigateToTab: ((Int) -> Unit)? = null,
) {
    composable(Route.Utilities.path) {
        if (onNavigateToTab != null) {
            LaunchedEffect(Unit) {
                onNavigateToTab(3)
                navController.popBackStack(Route.Home.path, inclusive = false)
            }
        } else {
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
                },
                onNavigateToAssets = {
                    navController.navigate(Route.Assets.path) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAppSettings = {
                    navController.navigate(Route.AppSettings.path) {
                        launchSingleTop = true
                    }
                },
                onNavigateToBackupRestore = {
                    navController.navigate(Route.BackupRestore.path) {
                        launchSingleTop = true
                    }
                },
            )
        }
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
