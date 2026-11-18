package com.notepay.domain.model

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.money.Money
import org.junit.Test
import kotlin.test.assertFailsWith

class TransactionTest {

    @Test
    fun `note policy truncates to transaction maximum length`() {
        val longNote = "a".repeat(Transaction.MAX_NOTE_LENGTH + 20)
        val truncated = TransactionNotePolicy.truncate(longNote)

        assertThat(truncated.length).isEqualTo(Transaction.MAX_NOTE_LENGTH)
        assertThat(truncated).endsWith("...")
    }

    @Test
    fun `valid transaction and exact max note boundary construct successfully`() {
        val t = TestTransactionFactory.expense(note = "a".repeat(Transaction.MAX_NOTE_LENGTH))
        assertThat(t.amount).isEqualTo(Money(50_000_00))
        assertThat(t.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(t.note).hasLength(Transaction.MAX_NOTE_LENGTH)
    }

    @Test
    fun `invalid constructor arguments throw IllegalArgumentException`() {
        val invalidBuilders: List<() -> Transaction> = listOf(
            { TestTransactionFactory.expense(amount = Money(-50_000_00L)) },
            { TestTransactionFactory.expense(amount = Money(0L)) },
            { TestTransactionFactory.expense(note = "a".repeat(Transaction.MAX_NOTE_LENGTH + 1)) },
            { TestTransactionFactory.expense(walletId = -1L) },
        )

        invalidBuilders.forEach { build ->
            assertFailsWith<IllegalArgumentException> { build() }
        }
    }
}
