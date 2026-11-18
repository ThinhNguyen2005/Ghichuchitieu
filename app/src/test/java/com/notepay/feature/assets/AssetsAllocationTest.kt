package com.notepay.feature.assets

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.feature.assets.main.MaxAllocationSegments
import com.notepay.feature.assets.main.WalletAssetItem
import com.notepay.feature.assets.main.buildAllocation
import com.notepay.feature.assets.main.groupAllocation
import org.junit.Test

class AssetsAllocationTest {

    @Test
    fun `buildAllocation ignores wallets without a positive balance`() {
        val allocation = buildAllocation(
            listOf(
                asset(id = 1, name = "Tiền mặt", balanceVnd = 300_000),
                asset(id = 2, name = "Đang âm", balanceVnd = -50_000),
                asset(id = 3, name = "Ví rỗng", balanceVnd = 0),
            ),
        )

        assertThat(allocation.map { it.name }).containsExactly("Tiền mặt").inOrder()
        assertThat(allocation.single().percentage).isWithin(0.0001f).of(1f)
    }

    @Test
    fun `buildAllocation returns nothing instead of splitting evenly when no balance is positive`() {
        val allocation = buildAllocation(
            listOf(
                asset(id = 1, name = "Đang âm", balanceVnd = -50_000),
                asset(id = 2, name = "Ví rỗng", balanceVnd = 0),
            ),
        )

        assertThat(allocation).isEmpty()
    }

    @Test
    fun `buildAllocation sorts by balance and percentages cover the whole positive total`() {
        val allocation = buildAllocation(
            listOf(
                asset(id = 1, name = "Nhỏ", balanceVnd = 200_000),
                asset(id = 2, name = "Lớn", balanceVnd = 500_000),
                asset(id = 3, name = "Vừa", balanceVnd = 300_000),
            ),
        )

        assertThat(allocation.map { it.name }).containsExactly("Lớn", "Vừa", "Nhỏ").inOrder()
        val percentages = allocation.map { it.percentage }
        assertThat(percentages[0]).isWithin(0.0001f).of(0.5f)
        assertThat(percentages[1]).isWithin(0.0001f).of(0.3f)
        assertThat(percentages[2]).isWithin(0.0001f).of(0.2f)
        assertThat(allocation.sumOf { it.percentage.toDouble() }).isWithin(1e-6).of(1.0)
    }

    @Test
    fun `groupAllocation keeps every segment when there are at most three wallets`() {
        val segments = groupAllocation(
            buildAllocation(
                listOf(
                    asset(id = 1, name = "Tiền mặt", balanceVnd = 300_000),
                    asset(id = 2, name = "Ngân hàng", balanceVnd = 200_000),
                    asset(id = 3, name = "Ví điện tử", balanceVnd = 100_000),
                ),
            ),
        )

        assertThat(segments).hasSize(MaxAllocationSegments)
        assertThat(segments.none { it.isOther }).isTrue()
        assertThat(segments.map { it.name }).containsExactly("Tiền mặt", "Ngân hàng", "Ví điện tử").inOrder()
    }

    @Test
    fun `groupAllocation merges the wallets after the third into a single other segment`() {
        val segments = groupAllocation(
            buildAllocation(
                listOf(
                    asset(id = 1, name = "Lớn", balanceVnd = 400_000),
                    asset(id = 2, name = "Vừa", balanceVnd = 300_000),
                    asset(id = 3, name = "Nhỏ", balanceVnd = 200_000),
                    asset(id = 4, name = "Rất nhỏ", balanceVnd = 100_000),
                ),
            ),
        )

        assertThat(segments).hasSize(MaxAllocationSegments + 1)
        val other = segments.last()
        assertThat(other.isOther).isTrue()
        assertThat(other.name).isNull()
        assertThat(other.colorKey).isNull()
        assertThat(other.percentage).isWithin(0.0001f).of(0.1f)
    }

    private fun asset(id: Long, name: String, balanceVnd: Long): WalletAssetItem = WalletAssetItem(
        wallet = Wallet(
            id = id,
            name = name,
            initialBalance = Money(balanceVnd * 100L),
            iconKey = Wallet.ICON_CASH,
            colorKey = Wallet.COLOR_PRIMARY,
        ),
        balance = Money(balanceVnd * 100L),
        monthlyIncome = Money.ZERO,
        monthlyExpense = Money.ZERO,
        transactionCount = 0,
    )
}
