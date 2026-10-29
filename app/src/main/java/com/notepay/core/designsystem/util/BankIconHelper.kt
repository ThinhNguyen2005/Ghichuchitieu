package com.notepay.ui.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

object BankIconHelper {

    /**
     * Lấy Icon ứng dụng chính thức từ hệ thống Android nếu máy đã cài đặt ứng dụng ngân hàng/ví đó.
     *
     * @param context Context Android.
     * @param packageName Tên package của ứng dụng (VD: com.tpb.mb.gprsandroid, com.mservice.momotransfer...).
     * @return [Drawable] của ứng dụng nếu đã cài, hoặc `null` nếu máy chưa cài.
     */
    fun getInstalledAppIcon(context: Context, packageName: String): Drawable? {
        if (packageName.isBlank()) return null
        val pm = context.packageManager
        try {
            return pm.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
    }

    /**
     * Tìm Icon ứng dụng bằng Mã BIN ngân hàng (VD: 970423 -> TPBank).
     */
    fun getInstalledAppIconByBankBin(context: Context, bankBin: String): Drawable? {
        val packageName = getPackageNameByBankBin(bankBin) ?: return null
        return getInstalledAppIcon(context, packageName)
    }

    /**
     * Ánh xạ mã BIN ngân hàng chuẩn sang Package name phổ biến nhất.
     */
    fun getPackageNameByBankBin(bankBin: String): String? {
        return when (bankBin) {
            "970423" -> "com.tpb.mb.gprsandroid" // TPBank
            "970436" -> "com.VCB" // Vietcombank
            "970415" -> "com.vietinbank.ipay" // VietinBank
            "970418" -> "com.vnpay.bidv" // BIDV
            "970405" -> "com.vnpay.Agribank3g" // Agribank
            "970422" -> "com.mbmobile" // MB Bank
            "970407" -> "vn.com.techcombank.bb.app" // Techcombank
            "970416" -> "mobile.acb.com.vn" // ACB
            "970432" -> "com.vnpay.vpbankonline" // VPBank
            "970403" -> "src.com.sacombank" // Sacombank
            "970441" -> "com.vib.myvib2" // VIB
            "970431" -> "com.msb.digibank.retail" // MSB
            "970448" -> "vn.com.ocb.awe" // OCB
            "970449" -> "vn.lienviet.app" // LPBank
            "970424" -> "com.shinhan.global.vn.bank" // Shinhan Bank
            else -> null
        }
    }
}
