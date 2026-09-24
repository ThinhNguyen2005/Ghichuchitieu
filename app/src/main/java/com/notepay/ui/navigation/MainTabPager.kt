package com.notepay.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.notepay.ui.feature.home.HomeScreen
import com.notepay.ui.feature.stats.StatsScreen
import com.notepay.ui.feature.utilities.UtilitiesScreen
import com.notepay.ui.feature.wallet.AssetsScreen
import com.notepay.ui.feedback.UiFeedback
import kotlinx.coroutines.launch

@Composable
fun MainTabPager(
    pagerState: PagerState,
    navController: NavHostController,
    showFeedback: suspend (UiFeedback) -> Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            if (reducedMotion) {
                pagerState.scrollToPage(0)
            } else {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        key = { page -> bottomTabs.getOrNull(page)?.route?.path ?: page },
        modifier = modifier.fillMaxSize(),
    ) { page ->
        when (page) {
            0 -> HomeScreen(
                onSeeAll = {
                    navController.navigate(Route.TransactionList.path) {
                        launchSingleTop = true
                    }
                },
                onAddWallet = { navController.navigate(Route.AddWallet.path) },
                onEditWallet = { walletId -> navController.navigate(Route.EditWallet(walletId).path) },
                onNavigateToReminders = { navController.navigate(Route.Subscription.path) },
                onNavigateToAppSettings = { navController.navigate(Route.AppSettings.path) },
                onTransactionClick = { txId ->
                    navController.navigate(Route.TransactionDetail(txId).path)
                },
                onEditTransaction = { txId ->
                    navController.navigate(Route.EditTransaction(txId).path)
                },
            )
            1 -> StatsScreen(
                onAddTransaction = { navController.navigate(Route.AddTransaction.path) },
                onTransactionClick = { txId ->
                    navController.navigate(Route.TransactionDetail(txId).path)
                },
                onConfigureLocalModel = {
                    navController.navigate(Route.AppSettings.path)
                },
            )
            2 -> AssetsScreen(
                onAddWallet = { navController.navigate(Route.AddWallet.path) },
                onEditWallet = { walletId -> navController.navigate(Route.EditWallet(walletId).path) },
                onWalletClick = { walletId ->
                    navController.navigate("${Route.TransactionList.path}?walletId=$walletId")
                },
                onFeedback = showFeedback,
            )
            3 -> UtilitiesScreen(
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
                    coroutineScope.launch {
                        if (reducedMotion) pagerState.scrollToPage(2)
                        else pagerState.animateScrollToPage(2)
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
}
