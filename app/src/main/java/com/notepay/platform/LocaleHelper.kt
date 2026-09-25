package com.notepay.platform

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Quản lý ngôn ngữ và Locale tập trung cho toàn bộ ứng dụng NotePay.
 *
 * Áp dụng triệt để các nguyên tắc:
 * 1. Tránh set lại Locale nếu ngôn ngữ hiện tại của hệ thống hoặc app đã khớp (ngăn chặn recreate() lặp vòng).
 * 2. Đồng bộ chuẩn mã ngôn ngữ ("vi", "en", "system"), xử lý nhất quán các biến thể ("vi-VN", "vi_VN").
 * 3. Tương thích chuẩn Android 13+ (LocaleManager / Per-App Language Preferences) và fallback an toàn cho Android < 13.
 */
object LocaleHelper {

    const val LANG_VI = "vi"
    const val LANG_EN = "en"
    const val LANG_SYSTEM = "system"

    /**
     * Chuẩn hóa bất kỳ chuỗi locale / language tag nào về chuẩn nội bộ của app ("vi", "en", "system").
     *
     * Ví dụ:
     * - "vi", "vi-VN", "vi_VN", "vi-Latn-VN" -> "vi"
     * - "en", "en-US", "en_US", "en-GB" -> "en"
     * - null, rỗng, "system" -> "system"
     */
    fun normalizeLanguageCode(rawCode: String?): String {
        if (rawCode.isNullOrBlank()) return LANG_SYSTEM
        val trimmed = rawCode.trim()
        if (trimmed.equals(LANG_SYSTEM, ignoreCase = true)) return LANG_SYSTEM

        val baseLang = trimmed.substringBefore('-').substringBefore('_').lowercase(Locale.ROOT)
        return when (baseLang) {
            LANG_VI -> LANG_VI
            LANG_EN -> LANG_EN
            else -> LANG_SYSTEM
        }
    }

    /**
     * Kiểm tra xem ngôn ngữ mục tiêu [targetLanguage] đã đang được áp dụng hay chưa.
     * Trả về true nếu đã khớp -> KHÔNG CẦN set lại, tránh kích hoạt Activity recreate() lặp vòng.
     */
    fun isAlreadyApplied(context: Context, targetLanguage: String): Boolean {
        val target = normalizeLanguageCode(targetLanguage)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java) ?: return false
            val appLocales = localeManager.applicationLocales

            if (target == LANG_SYSTEM) {
                // Nếu mục tiêu là mặc định hệ thống và per-app locales đang trống -> Đã khớp
                return appLocales.isEmpty
            }

            if (appLocales.isEmpty) {
                // App đang dùng mặc định hệ thống, nhưng mục tiêu là ngôn ngữ cụ thể -> Chưa khớp
                return false
            }

            // Kiểm tra ngôn ngữ đầu tiên trong danh sách per-app locales
            val currentFirst = appLocales[0] ?: return false
            return normalizeLanguageCode(currentFirst.language) == target
        } else {
            if (target == LANG_SYSTEM) {
                return true
            }

            val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            } ?: Locale.getDefault()

            return normalizeLanguageCode(currentLocale.language) == target
        }
    }

    /**
     * Áp dụng ngôn ngữ cho ứng dụng một cách an toàn.
     * Nếu ngôn ngữ đã khớp với trạng thái hiện tại, hàm sẽ return ngay lập tức.
     */
    fun applyLocale(context: Context, targetLanguage: String) {
        val target = normalizeLanguageCode(targetLanguage)

        // Tuyệt đối không set lại nếu đã khớp để tránh recreate() liên tục
        if (isAlreadyApplied(context, target)) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
            val localeList = when (target) {
                LANG_VI -> LocaleList.forLanguageTags(LANG_VI)
                LANG_EN -> LocaleList.forLanguageTags(LANG_EN)
                else -> LocaleList.getEmptyLocaleList()
            }
            localeManager.applicationLocales = localeList
        } else {
            val targetLocale = when (target) {
                LANG_VI -> Locale.forLanguageTag("vi-VN")
                LANG_EN -> Locale.US
                else -> Locale.getDefault()
            }
            Locale.setDefault(targetLocale)

            @Suppress("DEPRECATION")
            val config = context.resources.configuration
            config.setLocale(targetLocale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}
