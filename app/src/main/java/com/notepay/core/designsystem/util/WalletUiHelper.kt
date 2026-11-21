package com.notepay.core.designsystem.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.notepay.R
import com.notepay.domain.model.AccountNature
import com.notepay.domain.model.WalletType

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
        "crypto" -> Icons.Rounded.CurrencyBitcoin
        "home" -> Icons.Rounded.Home
        "car" -> Icons.Rounded.DirectionsCar
        "shopping" -> Icons.Rounded.ShoppingBag
        "gift" -> Icons.Rounded.CardGiftcard
        "investment", "fund" -> Icons.AutoMirrored.Rounded.TrendingUp
        "stock" -> Icons.AutoMirrored.Rounded.ShowChart
        "gold" -> Icons.Rounded.MonetizationOn
        "credit_line" -> Icons.Rounded.CreditScore
        "loan" -> Icons.AutoMirrored.Rounded.ReceiptLong
        "mortgage" -> Icons.Rounded.HomeWork
        "other_liability" -> Icons.Rounded.Receipt
        "other_asset" -> Icons.Rounded.Inventory2
        else -> Icons.Rounded.AccountBalanceWallet
    }

    val iconList = listOf(
        Triple("cash", Icons.Rounded.Payments, R.string.icon_label_cash),
        Triple("bank", Icons.Rounded.AccountBalance, R.string.transfer_bank),
        Triple("momo", Icons.Rounded.AccountBalanceWallet, R.string.icon_label_ewallet),
        Triple("card", Icons.Rounded.CreditCard, R.string.icon_label_credit_card),
        Triple("savings", Icons.Rounded.Savings, R.string.icon_label_savings),
        Triple("investment", Icons.AutoMirrored.Rounded.TrendingUp, R.string.wallet_type_fund),
        Triple("stock", Icons.AutoMirrored.Rounded.ShowChart, R.string.wallet_type_stock),
        Triple("crypto", Icons.Rounded.CurrencyBitcoin, R.string.wallet_type_crypto),
        Triple("gold", Icons.Rounded.MonetizationOn, R.string.wallet_type_gold),
        Triple("home", Icons.Rounded.Home, R.string.icon_label_home),
        Triple("car", Icons.Rounded.DirectionsCar, R.string.icon_label_car),
        Triple("credit_line", Icons.Rounded.CreditScore, R.string.wallet_type_credit_line),
        Triple("loan", Icons.AutoMirrored.Rounded.ReceiptLong, R.string.wallet_type_loan),
        Triple("mortgage", Icons.Rounded.HomeWork, R.string.wallet_type_mortgage),
        Triple("other_asset", Icons.Rounded.Inventory2, R.string.wallet_type_other_asset),
        Triple("other_liability", Icons.Rounded.Receipt, R.string.wallet_type_other_liability),
        Triple("shopping", Icons.Rounded.ShoppingBag, R.string.icon_label_shopping),
        Triple("gift", Icons.Rounded.CardGiftcard, R.string.icon_label_gift),
    )

    fun getWalletTypeTitleRes(type: WalletType): Int = when (type) {
        WalletType.CASH -> R.string.wallet_type_cash
        WalletType.BANK -> R.string.wallet_type_bank
        WalletType.EWALLET -> R.string.wallet_type_ewallet
        WalletType.SAVINGS -> R.string.wallet_type_savings
        WalletType.FUND -> R.string.wallet_type_fund
        WalletType.STOCK -> R.string.wallet_type_stock
        WalletType.CRYPTO -> R.string.wallet_type_crypto
        WalletType.GOLD -> R.string.wallet_type_gold
        WalletType.REAL_ESTATE -> R.string.wallet_type_real_estate
        WalletType.VEHICLE -> R.string.wallet_type_vehicle
        WalletType.OTHER_ASSET -> R.string.wallet_type_other_asset
        WalletType.CREDIT_CARD -> R.string.wallet_type_credit_card
        WalletType.CREDIT_LINE -> R.string.wallet_type_credit_line
        WalletType.LOAN -> R.string.wallet_type_loan
        WalletType.MORTGAGE -> R.string.wallet_type_mortgage
        WalletType.OTHER_LIABILITY -> R.string.wallet_type_other_liability
    }

    fun getWalletTypeIcon(type: WalletType): ImageVector = getIcon(type.defaultIconKey)

    fun getNatureTitleRes(nature: AccountNature): Int = when (nature) {
        AccountNature.ASSET -> R.string.account_nature_asset
        AccountNature.LIABILITY -> R.string.account_nature_liability
    }

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
