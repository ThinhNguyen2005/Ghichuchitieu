package com.notepay.feature.assets

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.feature.assets.main.TransferError
import com.notepay.feature.assets.main.TransferValidation
import com.notepay.feature.assets.main.WalletAssetItem
import com.notepay.feature.assets.main.parseWholeVnd
import com.notepay.feature.assets.main.validateTransfer
import org.junit.Test

class TransferValidationTest {

    private val wallets = listOf(
        asset(id = 1, name = "Tiền mặt", balanceVnd = 500_000),
        asset(id = 2, name = "Ngân hàng", balanceVnd = 100_000),
    )

    @Test
    fun `validateTransfer accepts a distinct pair with a positive amount`() {
        val result = validateTransfer(
            fromWalletId = 1,
            toWalletId = 2,
            amountInput = "5000",
            wallets = wallets,
        )

        assertThat(result).isInstanceOf(TransferValidation.Valid::class.java)
        val valid = result as TransferValidation.Valid
        assertThat(valid.from.wallet.id).isEqualTo(1L)
        assertThat(valid.to.wallet.id).isEqualTo(2L)
        assertThat(valid.amount).isEqualTo(Money(500_000))
    }

    @Test
    fun `validateTransfer rejects invalid wallet pairs and amounts`() {
        data class Case(
            val fromId: Long?,
            val toId: Long?,
            val amount: String,
            val walletError: TransferError?,
            val amountError: TransferError?,
        )
        listOf(
            Case(1, 1, "5000", TransferError.InvalidWalletPair, null),
            Case(99, 2, "5000", TransferError.InvalidWalletPair, null),
            Case(null, 2, "5000", TransferError.InvalidWalletPair, null),
            Case(1, 2, "", null, TransferError.InvalidAmount),
            Case(1, 1, "0", TransferError.InvalidWalletPair, TransferError.InvalidAmount),
        ).forEach { c ->
            val validation = validateTransfer(c.fromId, c.toId, c.amount, wallets)
            assertThat(validation).isInstanceOf(TransferValidation.Invalid::class.java)
            val invalid = validation as TransferValidation.Invalid
            assertThat(invalid.walletError).isEqualTo(c.walletError)
            assertThat(invalid.amountError).isEqualTo(c.amountError)
        }
    }

    @Test
    fun `parseWholeVnd converts positive amounts and rejects invalid or overflowing inputs`() {
        assertThat(parseWholeVnd("5000")).isEqualTo(Money(500_000))
        listOf("0", "-5", "", "12a", Long.MAX_VALUE.toString()).forEach { raw ->
            assertThat(parseWholeVnd(raw)).isNull()
        }
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
