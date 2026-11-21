package com.notepay.feature.assets.main

import androidx.compose.runtime.Immutable
import com.notepay.domain.model.CurrencyConverter
import com.notepay.domain.money.Money
import java.math.BigDecimal

enum class AllocationMode {
    ASSET,
    LIABILITY,
}

/** Số ví được liệt kê riêng; phần còn lại gộp thành một đoạn "Khác". */
const val MaxAllocationSegments = 3

/**
 * Phân bổ **số dư dương**: chỉ ví có số dư > 0 mới góp mặt.
 *
 * Chuẩn toán học: 100% tổng phân bổ chỉ tính trên các ví dương (cho Asset)
 * hoặc dư nợ dương (cho Liability), không để số dư âm bóp méo tỷ trọng.
 */
fun buildAllocation(
    wallets: List<WalletAssetItem>,
    mode: AllocationMode = AllocationMode.ASSET,
    rates: Map<String, BigDecimal>? = null,
): List<WalletAllocationItem> {
    return when (mode) {
        AllocationMode.ASSET -> {
            val positiveAssets = wallets
                .filter { it.wallet.isAsset && !it.wallet.isArchived && it.wallet.includeInTotalBalance }
                .map { item ->
                    val vndCents = CurrencyConverter.convertToVndCents(
                        item.balance.amountInCents,
                        item.wallet.currencyCode,
                        rates,
                    )
                    item to vndCents
                }
                .filter { it.second > 0L }
                .sortedByDescending { it.second }
            val totalCents = positiveAssets.sumOf { it.second }
            if (totalCents <= 0L) return emptyList()

            positiveAssets.map { (item, vndCents) ->
                WalletAllocationItem(
                    walletId = item.wallet.id,
                    name = item.wallet.name,
                    colorKey = item.wallet.colorKey,
                    iconKey = item.wallet.iconKey,
                    balance = item.balance,
                    convertedBalanceVnd = Money(vndCents),
                    percentage = vndCents.toDouble()
                        .div(totalCents.toDouble())
                        .toFloat(),
                )
            }
        }
        AllocationMode.LIABILITY -> {
            val liabilities = wallets
                .filter { it.wallet.isLiability && !it.wallet.isArchived && it.wallet.includeInTotalBalance }
                .map { item ->
                    val rawDebt = if (item.balance.amountInCents < 0L) -item.balance.amountInCents else item.balance.amountInCents
                    val vndCents = CurrencyConverter.convertToVndCents(
                        rawDebt,
                        item.wallet.currencyCode,
                        rates,
                    )
                    item to vndCents
                }
                .filter { it.second > 0L }
                .sortedByDescending { it.second }
            val totalDebtCents = liabilities.sumOf { it.second }
            if (totalDebtCents <= 0L) return emptyList()

            liabilities.map { (item, vndCents) ->
                WalletAllocationItem(
                    walletId = item.wallet.id,
                    name = item.wallet.name,
                    colorKey = item.wallet.colorKey,
                    iconKey = item.wallet.iconKey,
                    balance = item.balance,
                    convertedBalanceVnd = Money(vndCents),
                    percentage = vndCents.toDouble()
                        .div(totalDebtCents.toDouble())
                        .toFloat(),
                )
            }
        }
    }
}

/**
 * Một đoạn của thanh phân bổ.
 *
 * [colorKey] và [name] null nghĩa là đoạn gộp "Khác" (màu xám trung tính). Thanh vẽ và
 * chú giải cùng đọc danh sách này, nên màu trên thanh luôn có nhãn tương ứng và tỷ trọng
 * hiển thị đúng bằng bề rộng được vẽ.
 */
@Immutable
data class AllocationSegment(
    val colorKey: String?,
    val name: String?,
    val percentage: Float,
    val isOther: Boolean,
)

/** Gộp các ví xếp sau top [MaxAllocationSegments] thành một đoạn "Khác" duy nhất. */
fun groupAllocation(items: List<WalletAllocationItem>): List<AllocationSegment> {
    val head = items.take(MaxAllocationSegments).map { item ->
        AllocationSegment(
            colorKey = item.colorKey,
            name = item.name,
            percentage = item.percentage,
            isOther = false,
        )
    }
    val rest = items.drop(MaxAllocationSegments)
    if (rest.isEmpty()) return head

    return head + AllocationSegment(
        colorKey = null,
        name = null,
        percentage = rest.sumOf { it.percentage.toDouble() }.toFloat(),
        isOther = true,
    )
}
