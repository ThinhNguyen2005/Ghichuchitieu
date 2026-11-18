package com.notepay.domain.notification

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import org.junit.Test

class NotificationParserTest {

    @Test
    fun `parses valid bank and Momo notifications across income and expense`() {
        data class Case(
            val title: String,
            val body: String,
            val expectedAmount: Money,
            val expectedType: TransactionType,
            val expectedNote: String,
        )
        listOf(
            Case(
                title = "Vietcombank",
                body = "VCB: GD +500,000 VND luc 15:30. So du 1,200,000 VND. ND: Luong thang",
                expectedAmount = Money(500_000_00),
                expectedType = TransactionType.INCOME,
                expectedNote = "Luong thang",
            ),
            Case(
                title = "Techcombank",
                body = "TCB: GD -150,000 VND. ND: Mua Circle K",
                expectedAmount = Money(150_000_00),
                expectedType = TransactionType.EXPENSE,
                expectedNote = "Mua Circle K",
            ),
            Case(
                title = "TPBank",
                body = """
                    (TPBank): 14/06/26;05:51
                    TK: xxxx1234
                    PS: +100,000VND
                    SD: 2,500,000VND
                    SD KHA DUNG: 2,500,000VND
                    ND: Chuyen khoan mua banh mi
                    SO GD: 987654321
                    05:51
                """.trimIndent(),
                expectedAmount = Money(100_000_00),
                expectedType = TransactionType.INCOME,
                expectedNote = "Chuyen khoan mua banh mi",
            ),
            Case(
                title = "TPBank",
                body = """
                    (TPBank): 14/06/26;05:51
                    TK: xxxx1234
                    PS: -50,000VND
                    SD: 2,450,000VND
                    SD KHA DUNG: 2,450,000VND
                    ND: Mua ca phe chieu
                    SO GD: 987654322
                    05:51
                """.trimIndent(),
                expectedAmount = Money(50_000_00),
                expectedType = TransactionType.EXPENSE,
                expectedNote = "Mua ca phe chieu",
            ),
            Case(
                title = "Momo",
                body = "Momo: Bạn đã thanh toán 45,000đ cho Circle K. Mã giao dịch 123.",
                expectedAmount = Money(45_000_00),
                expectedType = TransactionType.EXPENSE,
                expectedNote = "Thanh toán cho Circle K",
            ),
            Case(
                title = "Momo",
                body = "Momo: Bạn đã nhận 100,000đ từ Nguyen Van A. Lời nhắn: Tra tien com",
                expectedAmount = Money(100_000_00),
                expectedType = TransactionType.INCOME,
                expectedNote = "Tra tien com",
            ),
        ).forEach { c ->
            val result = NotificationParser.parse(c.title, c.body)
            assertThat(result).isNotNull()
            assertThat(result!!.amount).isEqualTo(c.expectedAmount)
            assertThat(result.type).isEqualTo(c.expectedType)
            assertThat(result.note).isEqualTo(c.expectedNote)
        }
    }

    @Test
    fun `parseMomoMerchantNoteUsesTransactionNotePolicy`() {
        val merchant = "A".repeat(Transaction.MAX_NOTE_LENGTH + 20)
        val result = NotificationParser.parse(
            "Momo",
            "Momo: Bạn đã thanh toán 45,000đ cho $merchant.",
        )

        assertThat(result).isNotNull()
        assertThat(result!!.note).hasLength(Transaction.MAX_NOTE_LENGTH)
        assertThat(result.note).endsWith("...")
    }

    @Test
    fun `parseInvalidNotificationReturnsNull`() {
        val result = NotificationParser.parse("Momo", "Momo: Chúc bạn một ngày mới vui vẻ!")
        assertThat(result).isNull()
    }

    @Test
    fun `amount overflow is rejected while largest safe cents boundary succeeds`() {
        val largestSafe = Long.MAX_VALUE / 100L
        val safeResult = NotificationParser.parse("VCB", "VCB: GD +$largestSafe VND")
        assertThat(safeResult).isNotNull()
        assertThat(safeResult!!.amount.amountInCents).isEqualTo(largestSafe * 100L)

        val tooLarge = largestSafe + 1L
        assertThat(NotificationParser.parse("VCB", "VCB: GD +$tooLarge VND")).isNull()
    }
}
