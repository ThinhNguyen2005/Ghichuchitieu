package com.notepay.platform

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/**
 * Quản lý ngôn ngữ và Locale tập trung cho toàn bộ ứng dụng NotePay.
 *
 * Khắc phục triệt để hiện tượng nhấp nháy / Activity relaunch loop:
 * 1. Không set lại Locale nếu ngôn ngữ hiện tại của hệ thống hoặc app đã khớp.
 * 2. Tuyệt đối không can thiệp Locale trong LaunchedEffect của Activity lifecycle.
 * 3. Đồng bộ mã ngôn ngữ: dùng "vi-VN" cho tiếng Việt trên Android để khớp với hệ thống,
 *    tránh xung đột vi vs vi-VN giữa app và OS (đặc biệt là Xiaomi HyperOS/MIUI).
 */
object LocaleHelper {

    const val LANG_VI = "vi"
    const val LANG_EN = "en"
    const val LANG_SYSTEM = "system"

    private const val PREFS_NAME = "notepay_settings"
    private const val KEY_APP_LANGUAGE = "app_language"

    /**
     * Chuẩn hóa bất kỳ chuỗi locale / language tag nào về chuẩn nội bộ của app ("vi", "en", "system").
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

            // Nếu per-app locales đã được set và ngôn ngữ đầu tiên khớp với target -> Đã khớp
            if (!appLocales.isEmpty) {
                val currentFirst = appLocales[0] ?: return false
                if (normalizeLanguageCode(currentFirst.language) == target) {
                    return true
                }
            }

            // Nếu target là "vi" và ngôn ngữ hệ thống hiện tại cũng là "vi",
            // đồng thời appLocales đang trống -> Đang dùng vi-VN của hệ thống -> Đã khớp!
            val systemLocale = Locale.getDefault()
            if (normalizeLanguageCode(systemLocale.language) == target && appLocales.isEmpty) {
                return true
            }

            return false
        } else {
            if (target == LANG_SYSTEM) {
                return true
            }

            @Suppress("DEPRECATION")
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
     * Chỉ gọi khi người dùng chủ động chọn đổi ngôn ngữ trong cài đặt.
     */
    fun applyLocale(context: Context, targetLanguage: String) {
        val target = normalizeLanguageCode(targetLanguage)

        // Lưu vào SharedPreferences cho cold-start
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_APP_LANGUAGE, target) }

        // Tuyệt đối không set lại nếu đã khớp để tránh recreate() liên tục
        if (isAlreadyApplied(context, target)) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
            val localeList = when (target) {
                LANG_VI -> LocaleList.forLanguageTags("vi-VN")
                LANG_EN -> LocaleList.forLanguageTags("en")
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

    /**
     * Khởi tạo Locale khi khởi động ứng dụng (chỉ chạy 1 lần duy nhất trong Application.onCreate).
     * Trên Android 13+, LocaleManager tự động duy trì cấu hình nên không cần can thiệp.
     */
    fun initializeOnAppStart(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedLang = prefs.getString(KEY_APP_LANGUAGE, null)
            if (!savedLang.isNullOrBlank() && savedLang != LANG_SYSTEM) {
                applyLocale(context, savedLang)
            }
        }
    }
}
