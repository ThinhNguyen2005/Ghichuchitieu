package com.notepay.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notepay.ui.feature.addtransaction.AddTransactionScreen
import com.notepay.ui.feature.addtransaction.EditTransactionScreen
import com.notepay.ui.feature.billsplit.BillSplitScreen
import com.notepay.ui.feature.billsplit.DebtorDetailScreen
import com.notepay.ui.feature.detail.TransactionDetailScreen
import com.notepay.ui.feature.home.HomeScreen
import com.notepay.ui.feature.list.TransactionListScreen
import com.notepay.ui.feature.stats.StatsScreen
import com.notepay.ui.feature.subscription.SubscriptionScreen
import com.notepay.ui.feature.wallet.AddWalletScreen


private data class BottomTab(val route: Route, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Route.Home, "Trang chủ", Icons.Rounded.Home),
    BottomTab(Route.TransactionList, "Danh sách", Icons.Rounded.Receipt),
    BottomTab(Route.Stats, "Thống kê", Icons.Rounded.BarChart),
    BottomTab(Route.BillSplit, "Chia tiền", Icons.Rounded.CallSplit),
    BottomTab(Route.Subscription, "Nhắc nhở", Icons.Rounded.NotificationsActive),
)

@Composable
fun NotePayNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isMainTab = currentRoute in bottomTabs.map { it.route.path }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isMainTab) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route.path,
                            onClick = {
                                navController.navigate(tab.route.path) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.path,
            modifier = Modifier.padding(padding),
        ) {
            composable(Route.Home.path) {
                HomeScreen(
                    onAddTransaction = { navController.navigate(Route.AddTransaction.path) },
                    onSeeAll = { navController.navigate(Route.TransactionList.path) },
                    onAddWallet = { navController.navigate(Route.AddWallet.path) },
                    onTransactionClick = { txId ->
                        navController.navigate(Route.EditTransaction(txId).path)
                    },
                )
            }
            composable(Route.AddTransaction.path) {
                AddTransactionScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Route.EditTransaction.ROUTE,
                arguments = listOf(navArgument(Route.EditTransaction.ARG_ID) { type = NavType.LongType }),
            ) {
                EditTransactionScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Route.TransactionDetail.ROUTE,
                arguments = listOf(navArgument(Route.TransactionDetail.ARG_ID) { type = NavType.LongType }),
            ) {
                TransactionDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Route.EditTransaction(id).path) },
                    onCreateBillSplit = {
                        navController.navigate(Route.BillSplit.path) {
                            launchSingleTop = true
                        }
                    },
                    onCreateSubscription = { _, _ ->
                        navController.navigate(Route.Subscription.path) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Route.TransactionList.path) {
                TransactionListScreen(
                    onTransactionClick = { txId ->
                        navController.navigate(Route.EditTransaction(txId).path)
                    },
                )
            }
            composable(Route.Stats.path) {
                StatsScreen(
                    onAddTransaction = { navController.navigate(Route.AddTransaction.path) },
                )
            }
            composable(Route.AddWallet.path) {
                AddWalletScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Route.BillSplit.path) {
                BillSplitScreen(
                    onDebtorClick = { debtorName ->
                        navController.navigate(Route.DebtorDetail(debtorName).path)
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
                )
            }
            composable(Route.Subscription.path) {
                SubscriptionScreen()
            }
        }
    }
}
