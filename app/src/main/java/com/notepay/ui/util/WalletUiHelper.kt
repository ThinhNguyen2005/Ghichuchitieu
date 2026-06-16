package com.notepay.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

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
        Triple("cash", Icons.Rounded.Payments, "Tiền mặt"),
        Triple("bank", Icons.Rounded.AccountBalance, "Ngân hàng"),
        Triple("momo", Icons.Rounded.AccountBalanceWallet, "Ví điện tử"),
        Triple("card", Icons.Rounded.CreditCard, "Thẻ tín dụng"),
        Triple("savings", Icons.Rounded.Savings, "Tiết kiệm"),
        Triple("atm", Icons.Rounded.LocalAtm, "Thẻ ATM"),
        Triple("business", Icons.Rounded.Storefront, "Kinh doanh"),
        Triple("work", Icons.Rounded.Work, "Công việc"),
        Triple("crypto", Icons.AutoMirrored.Rounded.TrendingUp, "Đầu tư"),
        Triple("home", Icons.Rounded.Home, "Nhà cửa"),
        Triple("car", Icons.Rounded.DirectionsCar, "Xe cộ"),
        Triple("shopping", Icons.Rounded.ShoppingBag, "Mua sắm"),
        Triple("gift", Icons.Rounded.CardGiftcard, "Quà tặng")
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
