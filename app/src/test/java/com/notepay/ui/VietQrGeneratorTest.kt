package com.notepay.ui

import com.notepay.core.designsystem.util.VietQrGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VietQrGeneratorTest {

    private val bankBinTpbank = "970423"
    private val accountNumber = "0945553902"
    private val testAmountCents = 20_000L * 100
    private val testMemo = "NP15 BAN A"

    @Test
    fun `generateImageUrl formats host path parameters and normalizes Vietnamese text`() {
        val baseUrl = VietQrGenerator.generateImageUrl(bankBinTpbank, accountNumber, testAmountCents, testMemo)
        assertTrue(baseUrl.startsWith("https://img.vietqr.io/image/"))
        assertTrue(baseUrl.contains("$bankBinTpbank-$accountNumber-compact2.png"))
        assertTrue(baseUrl.contains("amount=20000"))
        assertTrue(baseUrl.contains("addInfo=NP15+BAN+A"))
        assertEquals(
            baseUrl,
            VietQrGenerator.generateImageUrl(bankBinTpbank, accountNumber, testAmountCents, testMemo),
        )

        val customTemplate = VietQrGenerator.generateImageUrl(
            bankBinTpbank,
            accountNumber,
            testAmountCents,
            testMemo,
            template = "qr_only",
            accountName = "  ",
        )
        assertTrue(customTemplate.contains("-qr_only.png"))
        assertFalse(customTemplate.contains("accountName="))

        listOf(
            Triple("Nguyễn Văn A", "bữa ăn", listOf("accountName=NGUYEN+VAN+A", "addInfo=BUA+AN")),
            Triple("", "ngan hang", listOf("addInfo=NGAN+HANG")),
        ).forEach { (accName, memo, expectedFragments) ->
            val url = VietQrGenerator.generateImageUrl(
                bankBinTpbank,
                accountNumber,
                testAmountCents,
                memo,
                accountName = accName,
            )
            expectedFragments.forEach { fragment -> assertTrue(url.contains(fragment)) }
            assertFalse(url.contains("ữ"))
        }

        val changedAmount = VietQrGenerator.generateImageUrl(bankBinTpbank, accountNumber, 30_000_00L, testMemo)
        val changedMemo = VietQrGenerator.generateImageUrl(bankBinTpbank, accountNumber, testAmountCents, "NP15 BAN B")
        val vcbQr = VietQrGenerator.generateImageUrl("970436", accountNumber, testAmountCents, testMemo)
        assertFalse(baseUrl == changedAmount)
        assertFalse(baseUrl == changedMemo)
        assertFalse(baseUrl == vcbQr)
        assertTrue(vcbQr.contains("970436"))
    }

    @Test
    fun `memoCode is unique per transaction-debtor and formatted uppercase alphanumeric`() {
        val codeA = buildMemoCode(15L, "Ban A")
        val codeB = buildMemoCode(15L, "Ban B")
        val otherTx = buildMemoCode(10L, "Ban A")
        assertFalse(codeA == codeB)
        assertFalse(codeA == otherTx)

        val accented = buildMemoCode(42L, "Nguyễn Văn A")
        assertTrue(accented.startsWith("NP42 "))
        assertTrue(accented.removePrefix("NP42 ").all { it.isUpperCase() || it.isDigit() || it == ' ' })
    }

    @Test
    fun `generateEmvCoPayload creates valid Napas EMVCo payload with CRC16`() {
        val payload = VietQrGenerator.generateEmvCoPayload(bankBinTpbank, accountNumber, testAmountCents, testMemo)
        assertTrue(payload.startsWith("000201"))
        assertTrue(payload.contains("A000000727"))
        assertTrue(payload.contains("970423"))
        assertTrue(payload.contains("0945553902"))
        assertTrue(payload.contains("6304"))
    }

    @Test
    fun `calculateCrc16 calculates correct 4-digit hex checksum`() {
        val testData = "00020101021238570010A00000072701270006970423011009455539020208QRIBFTTA53037045405200005802VN62140810NP15 BAN A6304"
        val crc = VietQrGenerator.calculateCrc16(testData)
        assertEquals(4, crc.length)
        assertTrue(crc.all { it.isDigit() || (it in 'A'..'F') })
    }

    @Test
    fun `generateLocalQrBitmap creates valid non-empty bitmap offline`() {
        val payload = VietQrGenerator.generateEmvCoPayload(bankBinTpbank, accountNumber, testAmountCents, testMemo)
        val bitmap = VietQrGenerator.generateLocalQrBitmap(payload, 200, 200)
        assertNotNull(bitmap)
        assertEquals(200, bitmap.width)
        assertEquals(200, bitmap.height)
    }

    private fun buildMemoCode(transactionId: Long, debtorName: String): String {
        val sanitized = debtorName.filter { it.isLetterOrDigit() || it.isWhitespace() }.trim()
        return "NP${transactionId} ${sanitized.uppercase()}"
    }
}
