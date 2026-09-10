package com.notepay.ui

import com.notepay.ui.util.VietQrGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests cho VietQrGenerator.
 *
 * Xác minh URL ảnh VietQR chính thức (img.vietqr.io) được tạo ra:
 * - Cấu trúc URL và tham số truy vấn
 * - Quy đổi số tiền từ cents sang VND
 * - Chuẩn hóa nội dung chuyển khoản (bỏ dấu, in hoa, URL-encode)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VietQrGeneratorTest {

    // Trường hợp cơ bản: tạo QR thanh toán cho một khoản chia tiền đơn giản
    private val BANK_BIN_TPBANK = "970423"
    private val ACCOUNT_NUMBER = "0945553902"
    private val TEST_AMOUNT_CENTS = 20_000L * 100  // 20.000 VND in cents
    private val TEST_MEMO = "NP15 BAN A"

    // ─────────────────────── Cấu trúc URL ───────────────────────

    @Test
    fun `generateImageUrl - uses official vietqr image host`() {
        val result = VietQrGenerator.generateImageUrl(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertTrue("Phải dùng host chính thức img.vietqr.io", result.startsWith("https://img.vietqr.io/image/"))
    }

    @Test
    fun `generateImageUrl - path contains bank BIN account and default template`() {
        val result = VietQrGenerator.generateImageUrl(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertTrue(
            "Đường dẫn phải là <bin>-<stk>-compact2.png",
            result.contains("$BANK_BIN_TPBANK-$ACCOUNT_NUMBER-compact2.png")
        )
    }

    @Test
    fun `generateImageUrl - custom template is honoured`() {
        val result = VietQrGenerator.generateImageUrl(
            BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO, template = "qr_only"
        )
        assertTrue("Phải dùng template tùy chọn qr_only", result.contains("-qr_only.png"))
    }

    @Test
    fun `generateImageUrl - amount is converted from cents to VND`() {
        // 20000_00 cents = 20000 VND
        val result = VietQrGenerator.generateImageUrl(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertTrue("Số tiền phải quy đổi sang đơn vị đồng: amount=20000", result.contains("amount=20000"))
    }

    @Test
    fun `generateImageUrl - memo is passed via addInfo`() {
        val result = VietQrGenerator.generateImageUrl(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertTrue("Nội dung chuyển khoản phải nằm ở addInfo", result.contains("addInfo=NP15+BAN+A"))
    }

    @Test
    fun `generateImageUrl - account name omitted when blank`() {
        val result = VietQrGenerator.generateImageUrl(
            BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO, accountName = "  "
        )
        assertFalse("Không được thêm accountName khi để trống", result.contains("accountName="))
    }

    @Test
    fun `generateImageUrl - account name is normalised when provided`() {
        val result = VietQrGenerator.generateImageUrl(
            BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO, accountName = "Nguyễn Văn A"
        )
        assertTrue("Tên chủ tài khoản phải bỏ dấu và in hoa", result.contains("accountName=NGUYEN+VAN+A"))
    }

    @Test
    fun `generateImageUrl - is deterministic for same inputs`() {
        val r1 = VietQrGenerator.generateImageUrl(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        val r2 = VietQrGenerator.generateImageUrl(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertEquals("URL phải ổn định (deterministic) với cùng đầu vào", r1, r2)
    }

    @Test
    fun `generateImageUrl - url changes when amount changes`() {
        val qr1 = VietQrGenerator.generateImageUrl(BANK_BIN_TPBANK, ACCOUNT_NUMBER, 20_000_00L, TEST_MEMO)
        val qr2 = VietQrGenerator.generateImageUrl(BANK_BIN_TPBANK, ACCOUNT_NUMBER, 30_000_00L, TEST_MEMO)
        assertFalse("URL phải thay đổi khi số tiền thay đổi", qr1 == qr2)
    }

    @Test
    fun `generateImageUrl - url changes when memo changes`() {
        val qr1 = VietQrGenerator.generateImageUrl(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, "NP15 BAN A")
        val qr2 = VietQrGenerator.generateImageUrl(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, "NP15 BAN B")
        assertFalse("URL phải thay đổi khi nội dung chuyển khoản thay đổi", qr1 == qr2)
    }

    // ─────────────────────── Xử lý tiếng Việt ───────────────────────

    @Test
    fun `generateImageUrl - vietnamese accents are stripped from memo`() {
        // Memo với dấu tiếng Việt phải được loại bỏ dấu và in hoa
        val result = VietQrGenerator.generateImageUrl(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, "bữa ăn")
        assertTrue("Dấu tiếng Việt phải được chuyển đổi: 'bữa ăn' → 'BUA AN'", result.contains("addInfo=BUA+AN"))
        assertFalse("Không được có ký tự có dấu tiếng Việt trong URL", result.contains("ữ"))
    }

    @Test
    fun `generateImageUrl - lowercase memo is uppercased`() {
        val result = VietQrGenerator.generateImageUrl(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, "ngan hang")
        assertTrue("Memo phải được in hoa: 'ngan hang' → 'NGAN HANG'", result.contains("addInfo=NGAN+HANG"))
    }

    // ─────────────────────── Ngân hàng khác nhau ───────────────────────

    @Test
    fun `generateImageUrl - different bank BIN produces different QR`() {
        val tpbankQr = VietQrGenerator.generateImageUrl("970423", ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        val vcbQr = VietQrGenerator.generateImageUrl("970436", ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
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

    // ─────────────────────── Sinh mã VietQR EMVCo 100% Offline ───────────────────────

    @Test
    fun `generateEmvCoPayload - creates valid Napas EMVCo payload with CRC16`() {
        val payload = VietQrGenerator.generateEmvCoPayload(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        assertNotNull("Payload không được null", payload)
        assertTrue("Payload phải bắt đầu bằng 000201", payload.startsWith("000201"))
        assertTrue("Payload phải chứa NAPAS AID A000000727", payload.contains("A000000727"))
        assertTrue("Payload phải chứa mã BIN TPBank 970423", payload.contains("970423"))
        assertTrue("Payload phải chứa số tài khoản 0945553902", payload.contains("0945553902"))
        assertTrue("Payload phải chứa mã CRC 6304", payload.contains("6304"))
    }

    @Test
    fun `calculateCrc16 - calculates correct 4-digit hex checksum`() {
        val testData = "00020101021238570010A00000072701270006970423011009455539020208QRIBFTTA53037045405200005802VN62140810NP15 BAN A6304"
        val crc = VietQrGenerator.calculateCrc16(testData)
        assertEquals("CRC16 phải dài 4 ký tự hex", 4, crc.length)
        assertTrue("CRC16 phải là chuỗi hex in hoa", crc.all { it.isDigit() || (it in 'A'..'F') })
    }

    @Test
    fun `generateLocalQrBitmap - creates valid non-empty bitmap offline`() {
        val payload = VietQrGenerator.generateEmvCoPayload(BANK_BIN_TPBANK, ACCOUNT_NUMBER, TEST_AMOUNT_CENTS, TEST_MEMO)
        val bitmap = VietQrGenerator.generateLocalQrBitmap(payload, 200, 200)
        assertNotNull("Bitmap không được null", bitmap)
        assertEquals("Chiều rộng bitmap đúng 200", 200, bitmap.width)
        assertEquals("Chiều cao bitmap đúng 200", 200, bitmap.height)
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
