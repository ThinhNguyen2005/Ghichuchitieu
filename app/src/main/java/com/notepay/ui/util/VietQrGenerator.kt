package com.notepay.ui.util

import com.notepay.util.StringUtils
import java.net.URLEncoder
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
}
