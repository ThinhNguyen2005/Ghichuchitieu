package com.notepay.ui.util

import java.net.URLEncoder
import java.util.Locale
import com.notepay.util.StringUtils

object VietQrGenerator {

    /**
     * Tạo URL ảnh VietQR chuẩn chính thức qua VietQR API (img.vietqr.io).
     *
     * @param bankBin Mã BIN ngân hàng (VD: 970423 cho TPBank, 970436 cho Vietcombank).
     * @param accountNumber Số tài khoản ngân hàng.
     * @param amountCents Số tiền (cents).
     * @param memo Nội dung chuyển khoản đối soát.
     * @param accountName Tên chủ tài khoản (tùy chọn).
     * @param template Khung giao diện VietQR: "compact2" (mặc định), "compact", "qr_only".
     */
    fun generateImageUrl(
        bankBin: String,
        accountNumber: String,
        amountCents: Long,
        memo: String,
        accountName: String? = null,
        template: String = "compact2"
    ): String {
        val majorAmount = amountCents / 100
        val cleanMemo = StringUtils.removeVietnameseAccents(memo).uppercase(Locale.ROOT)
        val encodedMemo = URLEncoder.encode(cleanMemo, "UTF-8")
        val baseUrl = "https://img.vietqr.io/image/$bankBin-$accountNumber-$template.png"
        
        val queryParams = mutableListOf("amount=$majorAmount", "addInfo=$encodedMemo")
        if (!accountName.isNullOrBlank()) {
            val cleanName = StringUtils.removeVietnameseAccents(accountName).uppercase(Locale.ROOT)
            queryParams.add("accountName=${URLEncoder.encode(cleanName, "UTF-8")}")
        }

        return "$baseUrl?${queryParams.joinToString("&")}"
    }

    /**
     * Sinh chuỗi VietQR chuẩn EMVCo 100% offline.
     */
    fun generate(
        bankBin: String,
        accountNumber: String,
        amountCents: Long,
        memo: String
    ): String {
        val tag00 = formatTag("00", "01")
        val tag01 = formatTag("01", "12")
        val guid = formatTag("00", "A000000727")
        val providerInfo = formatTag("00", bankBin) + formatTag("01", accountNumber)
        val serviceCode = formatTag("02", "QRIBFTTA")
        val merchantAccountValue = guid + formatTag("01", providerInfo) + serviceCode
        val tag38 = formatTag("38", merchantAccountValue)
        val tag53 = formatTag("53", "704")
        val majorAmount = amountCents / 100
        val tag54 = formatTag("54", majorAmount.toString())
        val tag58 = formatTag("58", "VN")
        val cleanMemo = StringUtils.removeVietnameseAccents(memo).uppercase(Locale.ROOT)
        val tag62Value = formatTag("08", cleanMemo)
        val tag62 = formatTag("62", tag62Value)
        val payloadWithoutCrc = tag00 + tag01 + tag38 + tag53 + tag54 + tag58 + tag62 + "6304"
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
