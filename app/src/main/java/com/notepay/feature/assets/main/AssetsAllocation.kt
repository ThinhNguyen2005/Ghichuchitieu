package com.notepay.feature.assets.main

import androidx.compose.runtime.Immutable

/** Số ví được liệt kê riêng; phần còn lại gộp thành một đoạn "Khác". */
const val MaxAllocationSegments = 3

/**
 * Phân bổ **số dư dương**: chỉ ví có số dư > 0 mới góp mặt.
 *
 * Không có ví dương thì trả về rỗng thay vì chia đều giả tạo — tỷ trọng đều không mô tả
 * bất kỳ số dư thật nào.
 */
fun buildAllocation(wallets: List<WalletAssetItem>): List<WalletAllocationItem> {
    val positiveWallets = wallets
        .filter { it.balance.amountInCents > 0L }
        .sortedByDescending { it.balance.amountInCents }
    val totalCents = positiveWallets.sumOf { it.balance.amountInCents }
    if (totalCents <= 0L) return emptyList()

    return positiveWallets.map { item ->
        WalletAllocationItem(
            walletId = item.wallet.id,
            name = item.wallet.name,
            colorKey = item.wallet.colorKey,
            iconKey = item.wallet.iconKey,
            balance = item.balance,
            percentage = item.balance.amountInCents.toDouble()
                .div(totalCents.toDouble())
                .toFloat(),
        )
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
