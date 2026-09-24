package com.notepay.ui.feature.utilities

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.notepay.ui.feature.backup.BackupRestoreScreen
import com.notepay.ui.feature.category.CategoryManagementScreen
import com.notepay.ui.feature.home.AppSettingsScreen
import com.notepay.ui.feature.settings.ai.AiSettingsScreen
import com.notepay.ui.feature.settings.appearance.AppearanceLanguageScreen
import com.notepay.ui.feature.settings.currency.CurrencySettingsScreen
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
                onNavigateToCurrencySettings = {
                    navController.navigate(Route.CurrencySettings.path) {
                        launchSingleTop = true
                    }
                },
                onNavigateToCategoryManagement = {
                    navController.navigate(Route.CategoryManagement.path) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAppearanceLanguage = {
                    navController.navigate(Route.AppearanceLanguage.path) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAiSettings = {
                    navController.navigate(Route.AiSettings.path) {
                        launchSingleTop = true
                    }
                },
                onNavigateToBackupRestore = {
                    navController.navigate(Route.BackupRestore.path) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAppSettings = {
                    navController.navigate(Route.AppSettings.path) {
                        launchSingleTop = true
                    }
                },
            )
        }
    }
    composable(Route.CurrencySettings.path) {
        CurrencySettingsScreen(onBack = { navController.popBackStack() })
    }
    composable(Route.CategoryManagement.path) {
        CategoryManagementScreen(onBack = { navController.popBackStack() })
    }
    composable(Route.AppearanceLanguage.path) {
        AppearanceLanguageScreen(onBack = { navController.popBackStack() })
    }
    composable(Route.AiSettings.path) {
        AiSettingsScreen(onBack = { navController.popBackStack() })
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
