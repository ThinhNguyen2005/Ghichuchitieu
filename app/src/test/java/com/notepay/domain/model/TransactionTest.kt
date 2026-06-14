package com.notepay.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith

class TransactionTest {

    @Test
    fun `valid transaction constructs successfully`() {
        val t = TestTransactionFactory.expense()
        assertThat(t.amount).isEqualTo(Money(50_000_00))
        assertThat(t.type).isEqualTo(TransactionType.EXPENSE)
    }

    @Test
    fun `negative amount throws at construction`() {
        assertFailsWith<IllegalArgumentException> {
            TestTransactionFactory.expense(amount = Money(-50_000_00L))
        }
    }

    @Test
    fun `note longer than 200 chars throws`() {
        val longNote = "a".repeat(201)
        assertFailsWith<IllegalArgumentException> {
            TestTransactionFactory.expense(note = longNote)
        }
    }

    @Test
    fun `note exactly 200 chars is allowed`() {
        val boundary = "a".repeat(200)
        val t = TestTransactionFactory.expense(note = boundary)
        assertThat(t.note).hasLength(200)
    }

    @Test
    fun `negative walletId throws`() {
        assertFailsWith<IllegalArgumentException> {
            TestTransactionFactory.expense(walletId = -1L)
        }
    }
}
