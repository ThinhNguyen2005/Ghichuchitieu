package com.notepay.ui.navigation

sealed interface Route {
    val path: String

    data object Home : Route { override val path = "home" }
    data object AddTransaction : Route { override val path = "add-transaction" }
    data object TransactionList : Route { override val path = "list" }
    data object Stats : Route { override val path = "stats" }
    data object BillSplit : Route { override val path = "bill-split" }
    data object Subscription : Route { override val path = "subscription" }
    data object AddWallet : Route { override val path = "add-wallet" }
    data object Utilities : Route { override val path = "utilities" }
    data object AddDummy : Route { override val path = "add-dummy" }
    data object NotificationSettings : Route { override val path = "notification-settings" }
    data class EditTransaction(val id: Long) : Route {
        override val path = "edit-transaction/$id"
        companion object {
            const val ROUTE = "edit-transaction/{id}"
            const val ARG_ID = "id"
        }
    }
    data class TransactionDetail(val id: Long) : Route {
        override val path = "transaction-detail/$id"
        companion object {
            const val ROUTE = "transaction-detail/{id}"
            const val ARG_ID = "id"
        }
    }
    data class DebtorDetail(val name: String) : Route {
        override val path = "debtor-detail/${android.net.Uri.encode(name)}"
        companion object {
            const val ROUTE = "debtor-detail/{name}"
            const val ARG_NAME = "name"
        }
    }
}
