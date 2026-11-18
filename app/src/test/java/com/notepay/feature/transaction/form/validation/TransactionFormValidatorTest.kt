package com.notepay.feature.transaction.form.validation

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Transaction
import com.notepay.domain.money.Money
import com.notepay.feature.transaction.form.calculator.AmountParser
import org.junit.Test

class TransactionFormValidatorTest {

    @Test
    fun `validate reports amount wallet and note problems together`() {
        val errors = TransactionFormValidator.validate(
            amount = null,
            walletId = null,
            note = "a".repeat(Transaction.MAX_NOTE_LENGTH + 1),
        )

        assertThat(errors).containsExactly(
            FieldError.AMOUNT_EMPTY,
            FieldError.WALLET_MISSING,
            FieldError.NOTE_TOO_LONG,
        )
    }

    @Test
    fun `validate accepts a positive amount with a wallet and a short note`() {
        val errors = TransactionFormValidator.validate(
            amount = Money(50_000L),
            walletId = 1L,
            note = "Cơm trưa",
        )

        assertThat(errors).isEmpty()
    }

    @Test
    fun `amountErrors keeps unrelated errors and replaces stale amount ones`() {
        val stale = setOf(FieldError.AMOUNT_EMPTY, FieldError.WALLET_MISSING)

        val valid = TransactionFormValidator.amountErrors(stale, AmountParser.parse("120000"))
        assertThat(valid).containsExactly(FieldError.WALLET_MISSING)

        val invalid = TransactionFormValidator.amountErrors(valid, AmountParser.parse(""))
        assertThat(invalid).containsExactly(FieldError.WALLET_MISSING, FieldError.AMOUNT_EMPTY)
    }

    @Test
    fun `amountErrors from the keypad flags an empty value`() {
        assertThat(TransactionFormValidator.amountErrors(emptySet(), amountInCents = 0L))
            .containsExactly(FieldError.AMOUNT_EMPTY)
        assertThat(TransactionFormValidator.amountErrors(setOf(FieldError.AMOUNT_EMPTY), amountInCents = 25_000L))
            .isEmpty()
    }

    @Test
    fun `noteErrors only flags notes longer than the domain limit`() {
        assertThat(TransactionFormValidator.noteErrors(emptySet(), Transaction.MAX_NOTE_LENGTH)).isEmpty()
        assertThat(TransactionFormValidator.noteErrors(emptySet(), Transaction.MAX_NOTE_LENGTH + 1))
            .containsExactly(FieldError.NOTE_TOO_LONG)
        assertThat(
            TransactionFormValidator.noteErrors(setOf(FieldError.NOTE_TOO_LONG), Transaction.MAX_NOTE_LENGTH),
        ).isEmpty()
    }

    @Test
    fun `positiveAmountInCents reads formatted input and rejects empty values`() {
        assertThat(TransactionFormValidator.positiveAmountInCents("1.250.000")).isEqualTo(125_000_000L)
        assertThat(TransactionFormValidator.positiveAmountInCents("")).isNull()
        assertThat(TransactionFormValidator.positiveAmountInCents("0")).isNull()
    }
}
