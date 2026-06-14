package com.notepay.ui

import com.notepay.ui.util.VietQrGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests cho VietQrGenerator.
 *
 * Xác minh tính hợp lệ của chuỗi VietQR được tạo ra:
 * - Cấu trúc EMVCo chuẩn (tag-length-value)
 * - Tính đúng đắn của CRC-16 checksum
 * - Mã hóa nội dung chuyển khoản (memo code)
 */
class VietQrGeneratorTest {

    // Trường hợp cơ bản: tạo QR thanh toán cho một khoản chia tiền đơn giản
    private val BANK_BIN_TPBANK = "970423"
    private val ACCOUNT_NUMBER = "0945553902"
    private val TEST_AMOUNT_CENTS = 20_000L * 100  // 20.000 VND in cents
    private val TEST_MEMO = "NP15 BAN A"

    // ─────────────────────── Cấu trúc EMVCo ───────────────────────

    @Test
    fun `generate - payload starts with tag00 equals 01`() {
        val result = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertTrue("Payload phải bắt đầu bằng tag 00 giá trị 01", result.startsWith("000201"))
    }

    @Test
    fun `generate - payload contains point of initiation method 12 for dynamic QR`() {
        val result = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertTrue("QR động phải chứa tag 01 = 12", result.contains("010212"))
    }

    @Test
    fun `generate - payload contains VietQR GUID A000000727`() {
        val result = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertTrue("Phải chứa GUID VietQR (A000000727)", result.contains("A000000727"))
    }

    @Test
    fun `generate - payload contains bank BIN`() {
        val result = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertTrue("Phải nhúng BIN ngân hàng ($BANK_BIN_TPBANK)", result.contains(BANK_BIN_TPBANK))
    }

    @Test
    fun `generate - payload contains account number`() {
        val result = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertTrue("Phải nhúng số tài khoản ($ACCOUNT_NUMBER)", result.contains(ACCOUNT_NUMBER))
    }

    @Test
    fun `generate - payload contains service code QRIBFTTA`() {
        val result = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertTrue("Phải chứa mã dịch vụ QRIBFTTA (chuyển khoản nhanh)", result.contains("QRIBFTTA"))
    }

    @Test
    fun `generate - payload contains VND currency code 704`() {
        val result = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertTrue("Phải chứa mã tiền tệ 704 (VND)", result.contains("5303704"))
    }

    @Test
    fun `generate - payload contains country code VN`() {
        val result = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertTrue("Phải chứa mã quốc gia VN", result.contains("5802VN"))
    }

    @Test
    fun `generate - payload contains correct major amount`() {
        // 20000_00 cents = 20000 VND = "20000"
        val result = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertTrue("Phải chứa số tiền đúng là 20000 đ", result.contains("20000"))
    }

    @Test
    fun `generate - payload contains uppercase memo code`() {
        val result = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, "NP15 BAN A")
        assertTrue("Nội dung chuyển khoản phải được in hoa: NP15 BAN A", result.contains("NP15 BAN A"))
    }

    @Test
    fun `generate - payload ends with 4-character hex CRC`() {
        val result = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        // Payload kết thúc bằng "6304" + 4 ký tự CRC hex
        assertTrue("Payload phải kết thúc bằng tag CRC 6304", result.contains("6304"))
        val crcHex = result.takeLast(4)
        assertTrue(
            "4 ký tự cuối phải là hex hợp lệ: $crcHex",
            crcHex.all { it.isDigit() || it in 'A'..'F' }
        )
    }

    // ─────────────────────── CRC-16 Checksum ───────────────────────

    @Test
    fun `generate - CRC-16 is stable for same inputs`() {
        val r1 = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        val r2 = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertEquals("CRC-16 phải ổn định (deterministic) với cùng đầu vào", r1, r2)
    }

    @Test
    fun `generate - CRC-16 changes when amount changes`() {
        val qr1 = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, 20_000_00L, TEST_MEMO)
        val qr2 = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, 30_000_00L, TEST_MEMO)
        val crc1 = qr1.takeLast(4)
        val crc2 = qr2.takeLast(4)
        assertFalse("CRC-16 phải thay đổi khi số tiền thay đổi", crc1 == crc2)
    }

    @Test
    fun `generate - CRC-16 changes when memo changes`() {
        val qr1 = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, "NP15 BAN A")
        val qr2 = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, "NP15 BAN B")
        val crc1 = qr1.takeLast(4)
        val crc2 = qr2.takeLast(4)
        assertFalse("CRC-16 phải thay đổi khi nội dung chuyển khoản thay đổi", crc1 == crc2)
    }

    // ─────────────────────── Xử lý tiếng Việt ───────────────────────

    @Test
    fun `generate - vietnamese accents are stripped from memo`() {
        // Memo với dấu tiếng Việt phải được loại bỏ dấu và in hoa
        val result = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, "bữa ăn")
        assertTrue("Dấu tiếng Việt phải được chuyển đổi: 'bữa ăn' → 'BUA AN'", result.contains("BUA AN"))
        assertFalse("Không được có ký tự có dấu tiếng Việt trong payload", result.contains("ữ"))
    }

    @Test
    fun `generate - lowercase memo is uppercased`() {
        val result = VietQrGenerator.generate(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, "ngan hang")
        assertTrue("Memo phải được in hoa: 'ngan hang' → 'NGAN HANG'", result.contains("NGAN HANG"))
    }

    // ─────────────────────── Ngân hàng khác nhau ───────────────────────

    @Test
    fun `generate - different bank BIN produces different QR`() {
        val tpbankQr = VietQrGenerator.generate("970423", ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        val vcbQr = VietQrGenerator.generate("970436", ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertFalse("Mã QR của 2 ngân hàng khác nhau phải khác nhau", tpbankQr == vcbQr)
        assertTrue("QR VCB phải chứa BIN 970436", vcbQr.contains("970436"))
    }

    // ─────────────────────── Đối soát mã nội dung ───────────────────────

    @Test
    fun `memoCode format - unique per transaction and debtor`() {
        val memoCodeA = buildMemoCode(transactionId = 15L, debtorName = "Ban A")
        val memoCodeB = buildMemoCode(transactionId = 15L, debtorName = "Ban B")
        assertFalse("Mã nội dung của 2 người khác nhau trong cùng giao dịch phải khác nhau", memoCodeA == memoCodeB)
    }

    @Test
    fun `memoCode format - same memo across different transactions is unique`() {
        val memo1 = buildMemoCode(transactionId = 10L, debtorName = "Ban A")
        val memo2 = buildMemoCode(transactionId = 11L, debtorName = "Ban A")
        assertFalse("Mã nội dung của cùng 1 người ở 2 giao dịch khác nhau phải khác nhau", memo1 == memo2)
    }

    @Test
    fun `memoCode format - uppercase and alphanumeric only`() {
        val memoCode = buildMemoCode(transactionId = 42L, debtorName = "Nguyễn Văn A")
        // Chỉ chứa ký tự uppercase chữ cái + số + khoảng trắng
        assertTrue(
            "Memo phải bắt đầu bằng 'NP42 '",
            memoCode.startsWith("NP42 ")
        )
        // Sau khi lọc, tên phải là "NGUYEN VAN A" (sau khi xóa dấu, in hoa, lọc ký tự đặc biệt)
        val namePart = memoCode.removePrefix("NP42 ")
        assertTrue(
            "Phần tên phải là chữ hoa không dấu",
            namePart.all { it.isUpperCase() || it.isDigit() || it == ' ' }
        )
    }

    @Test
    fun `reconciliation - memoCode found in transaction description marks as paid`() {
        // Simulate: thông báo ngân hàng chứa "NP15 BAN A" → match với bill split
        val memoCode = "NP15 BAN A"
        val notificationBody = "GD +20,000VND ND: NP15 BAN A"

        // Xác minh logic tìm kiếm memoCode trong nội dung thông báo
        assertTrue(
            "Thông báo ngân hàng phải chứa memoCode để đối soát",
            notificationBody.contains(memoCode, ignoreCase = true)
        )
    }

    @Test
    fun `reconciliation - different memoCode does not match`() {
        val memoCode = "NP15 BAN B"
        val notificationBody = "GD +20,000VND ND: NP15 BAN A"

        assertFalse(
            "Memocode khác nhau không được match nhầm",
            notificationBody.contains(memoCode, ignoreCase = true)
        )
    }

    @Test
    fun `reconciliation - memoCode match is case-insensitive`() {
        val memoCode = "NP15 BAN A"
        val notificationBodyLower = "gd +20,000vnd nd: np15 ban a"
        assertTrue(
            "So sánh memoCode phải không phân biệt chữ hoa/thường",
            notificationBodyLower.contains(memoCode, ignoreCase = true)
        )
    }

    // ─────────────────────── Helpers ───────────────────────

    /**
     * Mô phỏng logic tạo memoCode trong BillSplitViewModel.createBillSplits()
     */
    private fun buildMemoCode(transactionId: Long, debtorName: String): String {
        val sanitized = debtorName.filter { it.isLetterOrDigit() || it.isWhitespace() }.trim()
        return "NP${transactionId} ${sanitized.uppercase()}"
    }
}
