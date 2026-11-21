package com.notepay.core.designsystem.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.util.concurrent.ConcurrentHashMap

object BankIconHelper {

    private sealed interface CachedIcon {
        data class Found(val drawable: Drawable) : CachedIcon
        data object Missing : CachedIcon
    }

    sealed interface CacheLookup {
        data class Hit(val drawable: Drawable?) : CacheLookup
        data object Miss : CacheLookup
    }

    private val iconCache = ConcurrentHashMap<String, CachedIcon>()

    /**
     * Tra cứu nhanh icon đã đệm trong RAM cho ví mà không chạm vào PackageManager hay I/O đĩa.
     */
    fun lookupCachedWalletIcon(linkedPackageName: String?, bankBin: String?): CacheLookup {
        val primaryPkg = linkedPackageName?.trim()?.takeIf { it.isNotEmpty() }
        val fallbackPkg = bankBin?.trim()?.takeIf { it.isNotEmpty() }?.let(::getPackageNameByBankBin)
        if (primaryPkg == null && fallbackPkg == null) return CacheLookup.Hit(null)

        if (primaryPkg != null) {
            when (val cached = iconCache[primaryPkg]) {
                is CachedIcon.Found -> return CacheLookup.Hit(cached.drawable)
                CachedIcon.Missing -> if (fallbackPkg == null || fallbackPkg == primaryPkg) {
                    return CacheLookup.Hit(null)
                }
                null -> return CacheLookup.Miss
            }
        }
        if (fallbackPkg != null) {
            return when (val cached = iconCache[fallbackPkg]) {
                is CachedIcon.Found -> CacheLookup.Hit(cached.drawable)
                CachedIcon.Missing -> CacheLookup.Hit(null)
                null -> CacheLookup.Miss
            }
        }
        return CacheLookup.Hit(null)
    }

    /**
     * Tải icon ứng dụng cho ví (ưu tiên package liên kết, sau đó theo mã BIN ngân hàng).
     * Nên gọi trên background dispatcher khi cache chưa có.
     */
    fun resolveWalletIcon(
        context: Context,
        linkedPackageName: String?,
        bankBin: String?,
    ): Drawable? {
        val primary = linkedPackageName?.trim()?.takeIf { it.isNotEmpty() }?.let { pkg ->
            getInstalledAppIcon(context, pkg)
        }
        if (primary != null) return primary
        val bin = bankBin?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return getInstalledAppIconByBankBin(context, bin)
    }

    /**
     * Lấy Icon ứng dụng chính thức từ hệ thống Android nếu máy đã cài đặt ứng dụng ngân hàng/ví đó.
     * Kết quả (bao gồm cả trường hợp chưa cài) được đệm trong RAM để tránh lặp lại I/O khi cuộn.
     */
    fun getInstalledAppIcon(context: Context, packageName: String): Drawable? {
        val normalized = packageName.trim()
        if (normalized.isEmpty()) return null
        iconCache[normalized]?.let { cached ->
            return (cached as? CachedIcon.Found)?.drawable
        }
        val pm = (context.applicationContext ?: context).packageManager
        val loaded = try {
            pm.getApplicationIcon(normalized)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Exception) {
            null
        }
        iconCache[normalized] = if (loaded != null) CachedIcon.Found(loaded) else CachedIcon.Missing
        return loaded
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
