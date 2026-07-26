package com.notepay.domain.model

/**
 * Thông tin ngân hàng từ VietQR API (api.vietqr.io/v2/banks).
 */
data class VietQrBank(
    val id: Int,
    val bin: String,
    val code: String,
    val shortName: String,
    val name: String,
    val logoUrl: String,
    val transferSupported: Boolean,
)
