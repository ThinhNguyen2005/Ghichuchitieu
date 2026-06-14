package com.notepay.domain.notification

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType
import org.junit.Test

class NotificationParserTest {

    @Test
    fun parseVietcombankIncome() {
        val body = "VCB: GD +500,000 VND luc 15:30. So du 1,200,000 VND. ND: Luong thang"
        val result = NotificationParser.parse("Vietcombank", body)

        assertThat(result).isNotNull()
        assertThat(result!!.amount).isEqualTo(Money(500_000_00)) // 500,000 đ
        assertThat(result.type).isEqualTo(TransactionType.INCOME)
        assertThat(result.note).isEqualTo("Luong thang")
    }

    @Test
    fun parseTechcombankExpense() {
        val body = "TCB: GD -150,000 VND. ND: Mua Circle K"
        val result = NotificationParser.parse("Techcombank", body)

        assertThat(result).isNotNull()
        assertThat(result!!.amount).isEqualTo(Money(150_000_00)) // 150,000 đ
        assertThat(result.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(result.note).isEqualTo("Mua Circle K")
    }

    @Test
    fun parseTPBankIncome() {
        val body = """
            (TPBank): 14/06/26;05:51
            TK: xxxx1234
            PS: +100,000VND
            SD: 2,500,000VND
            SD KHA DUNG: 2,500,000VND
            ND: Chuyen khoan mua banh mi
            SO GD: 987654321
            05:51
        """.trimIndent()
        val result = NotificationParser.parse("TPBank", body)

        assertThat(result).isNotNull()
        assertThat(result!!.amount).isEqualTo(Money(100_000_00)) // 100,000 đ
        assertThat(result.type).isEqualTo(TransactionType.INCOME)
        assertThat(result.note).isEqualTo("Chuyen khoan mua banh mi")
    }

    @Test
    fun parseTPBankExpense() {
        val body = """
            (TPBank): 14/06/26;05:51
            TK: xxxx1234
            PS: -50,000VND
            SD: 2,450,000VND
            SD KHA DUNG: 2,450,000VND
            ND: Mua ca phe chieu
            SO GD: 987654322
            05:51
        """.trimIndent()
        val result = NotificationParser.parse("TPBank", body)

        assertThat(result).isNotNull()
        assertThat(result!!.amount).isEqualTo(Money(50_000_00)) // 50,000 đ
        assertThat(result.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(result.note).isEqualTo("Mua ca phe chieu")
    }

    @Test
    fun parseMomoPayment() {
        val body = "Momo: Bạn đã thanh toán 45,000đ cho Circle K. Mã giao dịch 123."
        val result = NotificationParser.parse("Momo", body)

        assertThat(result).isNotNull()
        assertThat(result!!.amount).isEqualTo(Money(45_000_00)) // 45,000 đ
        assertThat(result.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(result.note).isEqualTo("Thanh toán cho Circle K")
    }

    @Test
    fun parseMomoReceive() {
        val body = "Momo: Bạn đã nhận 100,000đ từ Nguyen Van A. Lời nhắn: Tra tien com"
        val result = NotificationParser.parse("Momo", body)

        assertThat(result).isNotNull()
        assertThat(result!!.amount).isEqualTo(Money(100_000_00)) // 100,000 đ
        assertThat(result.type).isEqualTo(TransactionType.INCOME)
        assertThat(result.note).isEqualTo("Tra tien com")
    }

    @Test
    fun parseInvalidNotificationReturnsNull() {
        val body = "Momo: Chúc bạn một ngày mới vui vẻ!"
        val result = NotificationParser.parse("Momo", body)

        assertThat(result).isNull()
    }
}
