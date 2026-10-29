package com.notepay.feature.utilities

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.notepay.feature.backup.BackupRestoreScreen
import com.notepay.feature.category.CategoryManagementScreen
import com.notepay.feature.settings.AboutScreen
import com.notepay.feature.settings.ai.AiSettingsScreen
import com.notepay.feature.settings.appearance.AppearanceLanguageScreen
import com.notepay.feature.settings.notification.NotificationSettingsScreen
import com.notepay.feature.settings.currency.CurrencySettingsScreen
import com.notepay.core.navigation.PredictiveBackDestination
import com.notepay.core.navigation.Route

fun NavGraphBuilder.utilitiesGraph(
    navController: NavController,
    onNavigateToTab: ((Route) -> Unit)? = null,
) {
    composable(Route.Utilities.path) {
        if (onNavigateToTab != null) {
            LaunchedEffect(Unit) {
                // Graph này chính là tab Utilities; host tự quy đổi route -> trang pager.
                onNavigateToTab(Route.Utilities)
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
                onNavigateToNotificationSettings = {
                    navController.navigate(Route.NotificationSettings.path) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAbout = {
                    navController.navigate(Route.About.path) {
                        launchSingleTop = true
                    }
                },
                onNavigateToDebtManagement = {
                    navController.navigate(Route.DebtManagement.path) {
                        launchSingleTop = true
                    }
                },
            )
        }
    }
    composable(Route.CurrencySettings.path) {
        PredictiveBackDestination(onBack = { navController.popBackStack() }) {
            CurrencySettingsScreen(onBack = { navController.popBackStack() })
        }
    }
    composable(Route.CategoryManagement.path) {
        PredictiveBackDestination(onBack = { navController.popBackStack() }) {
            CategoryManagementScreen(onBack = { navController.popBackStack() })
        }
    }
    composable(Route.AppearanceLanguage.path) {
        PredictiveBackDestination(onBack = { navController.popBackStack() }) {
            AppearanceLanguageScreen(onBack = { navController.popBackStack() })
        }
    }
    composable(Route.AiSettings.path) {
        PredictiveBackDestination(onBack = { navController.popBackStack() }) {
            AiSettingsScreen(onBack = { navController.popBackStack() })
        }
    }
    composable(Route.BackupRestore.path) {
        PredictiveBackDestination(onBack = { navController.popBackStack() }) {
            BackupRestoreScreen(onBack = { navController.popBackStack() })
        }
    }
    composable(Route.NotificationSettings.path) {
        PredictiveBackDestination(onBack = { navController.popBackStack() }) {
            NotificationSettingsScreen(onBack = { navController.popBackStack() })
        }
    }
    composable(Route.About.path) {
        PredictiveBackDestination(onBack = { navController.popBackStack() }) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
