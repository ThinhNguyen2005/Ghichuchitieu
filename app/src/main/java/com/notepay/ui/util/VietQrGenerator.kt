package com.notepay.ui.util

import java.util.Locale
import com.notepay.util.StringUtils

object VietQrGenerator {

    /**
     * Sinh chuỗi VietQR chuẩn EMVCo 100% offline.
     *
     * @param bankBin BIN của ngân hàng nhận (vd: "970423" cho TPBank, "970436" cho Vietcombank).
     * @param accountNumber Số tài khoản ngân hàng nhận.
     * @param amountCents Số tiền cần chuyển (cents).
     * @param memo Cú pháp nội dung chuyển tiền (đối soát).
     */
    fun generate(
        bankBin: String,
        accountNumber: String,
        amountCents: Long,
        memo: String
    ): String {
        // Tag 00: Payload Format Indicator (Cố định "01")
        val tag00 = formatTag("00", "01")
        
        // Tag 01: Point of Initiation Method ("12" cho QR động có số tiền)
        val tag01 = formatTag("01", "12")
        
        // Tag 38: Merchant Account Information
        val guid = formatTag("00", "A000000727")
        val providerInfo = formatTag("00", bankBin) + formatTag("01", accountNumber)
        val serviceCode = formatTag("02", "QRIBFTTA")
        val merchantAccountValue = guid + formatTag("01", providerInfo) + serviceCode
        val tag38 = formatTag("38", merchantAccountValue)
        
        // Tag 53: Transaction Currency ("704" cho VND)
        val tag53 = formatTag("53", "704")
        
        // Tag 54: Transaction Amount (Đổi cents thành đơn vị đồng nguyên)
        val majorAmount = amountCents / 100
        val tag54 = formatTag("54", majorAmount.toString())
        
        // Tag 58: Country Code ("VN")
        val tag58 = formatTag("58", "VN")
        
        // Tag 62: Additional Data Field Template (Tag 08 chứa nội dung chuyển khoản)
        // Loại bỏ dấu tiếng Việt để tránh lỗi hiển thị/parse trên một số ngân hàng
        val cleanMemo = StringUtils.removeVietnameseAccents(memo).uppercase(Locale.ROOT)
        val tag62Value = formatTag("08", cleanMemo)
        val tag62 = formatTag("62", tag62Value)
        
        // Tổng hợp payload
        val payloadWithoutCrc = tag00 + tag01 + tag38 + tag53 + tag54 + tag58 + tag62 + "6304"
        
        // Tính toán CRC-16
        val crc = calculateCRC16(payloadWithoutCrc)
        
        return payloadWithoutCrc + crc
    }

    private fun formatTag(id: String, value: String): String {
        val length = String.format(Locale.US, "%02d", value.length)
        return "$id$length$value"
    }

    private fun calculateCRC16(data: String): String {
        var crc = 0xFFFF
        val polynomial = 0x1021
        for (b in data.toByteArray(Charsets.US_ASCII)) {
            for (i in 0 until 8) {
                val bit = (b.toInt() shr (7 - i) and 1) == 1
                val c15 = (crc shr 15 and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) {
                    crc = crc xor polynomial
                }
            }
        }
        crc = crc and 0xFFFF
        return String.format(Locale.US, "%04X", crc)
    }
}
