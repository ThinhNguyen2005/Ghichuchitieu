package com.notepay.domain.ingestion

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.TransactionType
import org.junit.Test

class TransactionAnalyzerTest {

    @Test
    fun `analyze parses bank transaction notification`() {
        val payload = RawTransactionPayload(
            rawText = "GD: +100,000 VND tai Vietcombank. ND: Chuyen tien luong",
            source = TransactionInputSource.NOTIFICATION,
            title = "Vietcombank"
        )
        val result = TransactionAnalyzer.analyze(payload)
        assertThat(result).isInstanceOf(ParsedTransactionResult.Success::class.java)

        val success = result as ParsedTransactionResult.Success
        assertThat(success.amount.amountInCents).isEqualTo(10_000_000L)
        assertThat(success.type).isEqualTo(TransactionType.INCOME)
        assertThat(success.note).isEqualTo("Chuyen tien luong")
        assertThat(success.suggestedCategoryId).isEqualTo("SALARY")
    }

    @Test
    fun `analyze parses Momo payment notification`() {
        val payload = RawTransactionPayload(
            rawText = "Thanh toán 150.000đ cho Phở Thìn",
            source = TransactionInputSource.NOTIFICATION
        )
        val result = TransactionAnalyzer.analyze(payload)
        assertThat(result).isInstanceOf(ParsedTransactionResult.Success::class.java)

        val success = result as ParsedTransactionResult.Success
        assertThat(success.amount.amountInCents).isEqualTo(15_000_000L)
        assertThat(success.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(success.note).isEqualTo("Thanh toán cho Phở Thìn")
        assertThat(success.suggestedCategoryId).isEqualTo("FOOD")
    }

    @Test
    fun `analyze parses natural Vietnamese phrase`() {
        val payload = RawTransactionPayload(
            rawText = "chuyển 50k ăn sáng",
            source = TransactionInputSource.PASTED_TEXT
        )
        val result = TransactionAnalyzer.analyze(payload)
        assertThat(result).isInstanceOf(ParsedTransactionResult.Success::class.java)

        val success = result as ParsedTransactionResult.Success
        assertThat(success.amount.amountInCents).isEqualTo(5_000_000L)
        assertThat(success.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(success.note).isEqualTo("ăn sáng")
        assertThat(success.suggestedCategoryId).isEqualTo("FOOD")
    }

    @Test
    fun `analyze returns Unrecognized for invalid text`() {
        val payload = RawTransactionPayload(
            rawText = "chỉ là một câu nói linh tinh",
            source = TransactionInputSource.MANUAL_INPUT
        )
        val result = TransactionAnalyzer.analyze(payload)
        assertThat(result).isInstanceOf(ParsedTransactionResult.Unrecognized::class.java)
    }
}
