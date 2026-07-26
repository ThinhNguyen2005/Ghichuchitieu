package com.notepay.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.notepay.R

object WalletUiHelper {
    fun getIcon(key: String): ImageVector = when (key) {
        "cash" -> Icons.Rounded.Payments
        "bank" -> Icons.Rounded.AccountBalance
        "momo" -> Icons.Rounded.AccountBalanceWallet
        "card" -> Icons.Rounded.CreditCard
        "savings" -> Icons.Rounded.Savings
        "atm" -> Icons.Rounded.LocalAtm
        "business" -> Icons.Rounded.Storefront
        "work" -> Icons.Rounded.Work
        "crypto" -> Icons.AutoMirrored.Rounded.TrendingUp
        "home" -> Icons.Rounded.Home
        "car" -> Icons.Rounded.DirectionsCar
        "shopping" -> Icons.Rounded.ShoppingBag
        "gift" -> Icons.Rounded.CardGiftcard
        else -> Icons.Rounded.AccountBalanceWallet
    }

    val iconList = listOf(
        Triple("cash", Icons.Rounded.Payments, R.string.icon_label_cash),
        Triple("bank", Icons.Rounded.AccountBalance, R.string.icon_label_bank),
        Triple("momo", Icons.Rounded.AccountBalanceWallet, R.string.icon_label_ewallet),
        Triple("card", Icons.Rounded.CreditCard, R.string.icon_label_credit_card),
        Triple("savings", Icons.Rounded.Savings, R.string.icon_label_savings),
        Triple("atm", Icons.Rounded.LocalAtm, R.string.icon_label_atm),
        Triple("business", Icons.Rounded.Storefront, R.string.icon_label_business),
        Triple("work", Icons.Rounded.Work, R.string.icon_label_work),
        Triple("crypto", Icons.AutoMirrored.Rounded.TrendingUp, R.string.icon_label_crypto),
        Triple("home", Icons.Rounded.Home, R.string.icon_label_home),
        Triple("car", Icons.Rounded.DirectionsCar, R.string.icon_label_car),
        Triple("shopping", Icons.Rounded.ShoppingBag, R.string.icon_label_shopping),
        Triple("gift", Icons.Rounded.CardGiftcard, R.string.icon_label_gift)
    )

    fun getColor(key: String): Color = when (key) {
        "primary" -> Color(0xFF007AFF) // Deep iOS Blue
        "secondary" -> Color(0xFF34C759) // Apple Green
        "tertiary" -> Color(0xFFFF9500) // Amber Orange
        "emerald" -> Color(0xFF00A86B)
        "ocean" -> Color(0xFF007791)
        "coral" -> Color(0xFFFF6F61)
        "teal" -> Color(0xFF008080)
        "slate" -> Color(0xFF4A5568)
        "brown" -> Color(0xFF8B5A2B)
        "rose" -> Color(0xFFE31B23)
        else -> Color(0xFF007AFF)
    }

    val colorList = listOf(
        "primary" to Color(0xFF007AFF),
        "secondary" to Color(0xFF34C759),
        "tertiary" to Color(0xFFFF9500),
        "emerald" to Color(0xFF00A86B),
        "ocean" to Color(0xFF007791),
        "coral" to Color(0xFFFF6F61),
        "teal" to Color(0xFF008080),
        "slate" to Color(0xFF4A5568),
        "brown" to Color(0xFF8B5A2B),
        "rose" to Color(0xFFE31B23)
    )
}
