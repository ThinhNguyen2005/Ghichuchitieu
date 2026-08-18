package com.notepay.ui.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.notepay.util.StringUtils
import java.net.URLEncoder
import java.util.EnumMap
import java.util.Locale

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
        template: String = "compact2",
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
     * Sinh chuỗi mã VietQR chuẩn EMVCo / Napas 100% Offline.
     */
    fun generateEmvCoPayload(
        bankBin: String,
        accountNumber: String,
        amountCents: Long = 0L,
        memo: String = "",
        accountName: String? = null,
    ): String {
        val cleanBin = bankBin.trim()
        val cleanAccount = accountNumber.trim()
        val majorAmount = amountCents / 100

        // Sub-tag 01 (Thông tin ngân hàng & số tài khoản)
        val binTlv = "00" + String.format(Locale.ROOT, "%02d", cleanBin.length) + cleanBin
        val accTlv = "01" + String.format(Locale.ROOT, "%02d", cleanAccount.length) + cleanAccount
        val subTag01Value = binTlv + accTlv
        val subTag01Tlv = "01" + String.format(Locale.ROOT, "%02d", subTag01Value.length) + subTag01Value

        // Tag 38 (Merchant Account Information - NAPAS)
        // GUID: A000000727 (NAPAS), Service: QRIBFTTA (Chuyển nhanh Napas 247)
        val tag38Value = "0010A000000727" + subTag01Tlv + "0208QRIBFTTA"
        val tag38Tlv = "38" + String.format(Locale.ROOT, "%02d", tag38Value.length) + tag38Value

        val sb = StringBuilder()
        sb.append("000201") // Payload Format Indicator
        sb.append(if (majorAmount > 0) "010212" else "010211") // Point of Initiation Method: 12 (Dynamic), 11 (Static)
        sb.append(tag38Tlv)
        sb.append("5303704") // Currency: 704 (VND)

        if (majorAmount > 0) {
            val amountStr = majorAmount.toString()
            sb.append("54").append(String.format(Locale.ROOT, "%02d", amountStr.length)).append(amountStr)
        }

        sb.append("5802VN") // Country Code

        if (!accountName.isNullOrBlank()) {
            val cleanName = StringUtils.removeVietnameseAccents(accountName).uppercase(Locale.ROOT)
            if (cleanName.isNotBlank()) {
                sb.append("59").append(String.format(Locale.ROOT, "%02d", cleanName.length)).append(cleanName)
            }
        }

        if (memo.isNotBlank()) {
            val cleanMemo = StringUtils.removeVietnameseAccents(memo).uppercase(Locale.ROOT)
            val subTag08Tlv = "08" + String.format(Locale.ROOT, "%02d", cleanMemo.length) + cleanMemo
            sb.append("62").append(String.format(Locale.ROOT, "%02d", subTag08Tlv.length)).append(subTag08Tlv)
        }

        sb.append("6304") // Tag CRC 63, length 04

        val crc = calculateCrc16(sb.toString())
        sb.append(crc)

        return sb.toString()
    }

    /**
     * Tính checksum CRC16-CCITT chuẩn EMVCo (Polynomial 0x1021, Initial 0xFFFF).
     */
    fun calculateCrc16(data: String): String {
        var crc = 0xFFFF
        val polynomial = 0x1021
        val bytes = data.toByteArray(Charsets.ISO_8859_1)
        for (b in bytes) {
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
        return String.format(Locale.ROOT, "%04X", crc)
    }

    /**
     * Tạo Bitmap mã QR 100% Offline từ dữ liệu bằng ZXing.
     */
    fun generateLocalQrBitmap(
        content: String,
        width: Int = 512,
        height: Int = 512,
    ): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.MARGIN] = 1
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
}
