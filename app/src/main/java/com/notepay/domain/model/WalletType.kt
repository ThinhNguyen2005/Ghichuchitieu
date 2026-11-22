package com.notepay.domain.model

/**
 * Danh mục loại tài khoản / tài sản và nợ phải trả.
 */
enum class WalletType(
    val nature: AccountNature,
    val defaultIconKey: String,
) {
    // === NHÓM TÀI SẢN (ASSETS) ===
    CASH(AccountNature.ASSET, "cash"),
    BANK(AccountNature.ASSET, "bank"),
    EWALLET(AccountNature.ASSET, "momo"),
    SAVINGS(AccountNature.ASSET, "savings"),
    FUND(AccountNature.ASSET, "investment"),
    STOCK(AccountNature.ASSET, "stock"),
    CRYPTO(AccountNature.ASSET, "crypto"),
    GOLD(AccountNature.ASSET, "gold"),
    REAL_ESTATE(AccountNature.ASSET, "home"),
    VEHICLE(AccountNature.ASSET, "car"),
    OTHER_ASSET(AccountNature.ASSET, "other_asset"),

    // === NHÓM NỢ PHẢI TRẢ / TÍN DỤNG (LIABILITIES) ===
    CREDIT_CARD(AccountNature.LIABILITY, "card"),
    CREDIT_LINE(AccountNature.LIABILITY, "credit_line"),
    LOAN(AccountNature.LIABILITY, "loan"),
    MORTGAGE(AccountNature.LIABILITY, "mortgage"),
    OTHER_LIABILITY(AccountNature.LIABILITY, "other_liability");

    val isLiability: Boolean get() = nature == AccountNature.LIABILITY
    val isAsset: Boolean get() = nature == AccountNature.ASSET
    val isInvestment: Boolean get() = this in setOf(FUND, STOCK, CRYPTO, GOLD, REAL_ESTATE)
    val isCreditCardOrLine: Boolean get() = this in setOf(CREDIT_CARD, CREDIT_LINE)

    companion object {
        fun fromString(value: String?, fallbackIconKey: String? = null): WalletType {
            if (value != null) {
                entries.find { it.name.equals(value, ignoreCase = true) }?.let { return it }
            }
            return when (fallbackIconKey) {
                "card" -> CREDIT_CARD
                "bank" -> BANK
                "momo" -> EWALLET
                "savings" -> SAVINGS
                "crypto" -> CRYPTO
                "home" -> REAL_ESTATE
                "car" -> VEHICLE
                "investment" -> FUND
                "stock" -> STOCK
                "gold" -> GOLD
                "other_asset" -> OTHER_ASSET
                "credit_line" -> CREDIT_LINE
                "loan" -> LOAN
                "mortgage" -> MORTGAGE
                "other_liability" -> OTHER_LIABILITY
                else -> CASH
            }
        }
    }
}
