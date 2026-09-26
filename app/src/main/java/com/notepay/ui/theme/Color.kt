package com.notepay.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ==========================================
// NotePay Design Specification v3:
// Ledger Ink & Warm Paper (DESIGN.md)
// ==========================================

// Light Mode Canvas & Surface (Warm Paper)
val LedgerLightBackground = Color(0xFFF7F3EC)       // Giấy ngà ấm
val LedgerLightSurface = Color(0xFFFFFDF8)          // Bề mặt card - trắng ngà
val LedgerLightSurfaceSunken = Color(0xFFEFE8DC)    // Vùng lõm (input/unfocused)
val LedgerLightSurfaceRaised = Color(0xFFFFFFFF)    // Bề mặt nổi (modal/sheet)

val LedgerLightTextPrimary = Color(0xFF1F1B16)      // Mực đen ấm
val LedgerLightTextSecondary = Color(0xFF6B6255)    // Nâu xám ấm
val LedgerLightTextMuted = Color(0xFFA69C8C)        // Placeholder / disabled

val LedgerLightBorder = Color(0xFFDCD3C2)           // Viền giấy ấm
val LedgerLightBorderStrong = Color(0xFFC7BBA4)     // Viền nhấn (dashed divider ledger)
val LedgerLightHairline = Color(0x1A1F1B16)         // rgba(31, 27, 22, 0.10)

// Light Mode Brand Accent
val LedgerPrimary = Color(0xFFB5502E)               // Đất nung (terracotta ink) - Chữ ký NotePay
val LedgerOnPrimary = Color(0xFFFFF8F0)
val LedgerPrimaryContainer = Color(0xFFF3DFCF)      // Nền nhạt accent
val LedgerOnPrimaryContainer = Color(0xFF5C2413)

val LedgerSecondary = Color(0xFF3E4A3E)             // Xanh rêu đậm
val LedgerOnSecondary = Color(0xFFF7F3EC)
val LedgerSecondaryContainer = Color(0xFFDCE3D5)
val LedgerOnSecondaryContainer = Color(0xFF1F2A1F)

val LedgerCta = Color(0xFFB5502E)                   // Nút hành động chính
val LedgerOnCta = Color(0xFFFFF8F0)

// Dark Mode Canvas & Surface (Ink Night)
val LedgerDarkBackground = Color(0xFF15130F)        // Đen ấm ngả nâu
val LedgerDarkSurface = Color(0xFF211E18)
val LedgerDarkSurfaceSunken = Color(0xFF1A1712)
val LedgerDarkSurfaceRaised = Color(0xFF2A261E)

val LedgerDarkTextPrimary = Color(0xFFF3ECDF)       // Trắng ngà ấm
val LedgerDarkTextSecondary = Color(0xFFB7AC97)
val LedgerDarkTextMuted = Color(0xFF7C7263)

val LedgerDarkBorder = Color(0xFF3A3327)
val LedgerDarkBorderStrong = Color(0xFF4E4433)
val LedgerDarkHairline = Color(0x17F3ECDF)          // rgba(243, 236, 223, 0.09)

// Dark Mode Brand Accent
val LedgerDarkPrimary = Color(0xFFE08A5C)           // Đất nung sáng cho nền tối
val LedgerDarkOnPrimary = Color(0xFF2E1206)
val LedgerDarkPrimaryContainer = Color(0xFF4A2415)
val LedgerDarkOnPrimaryContainer = Color(0xFFF3DFCF)

val LedgerDarkSecondary = Color(0xFF8FA485)
val LedgerDarkOnSecondary = Color(0xFF12190F)
val LedgerDarkSecondaryContainer = Color(0xFF2B3527)
val LedgerDarkOnSecondaryContainer = Color(0xFFDCE3D5)

val LedgerDarkCta = Color(0xFFE08A5C)
val LedgerDarkOnCta = Color(0xFF2E1206)

// Semantic Financial Flow (Desaturated, warm tones)
val LedgerIncome = Color(0xFF4C7A4A)                // Xanh lá rêu đục
val LedgerIncomeContainer = Color(0xFFE1EBDC)
val LedgerDarkIncome = Color(0xFF7FAE78)
val LedgerDarkIncomeContainer = Color(0xFF233021)

val LedgerExpense = Color(0xFFA63B2E)               // Đỏ gạch đất
val LedgerExpenseContainer = Color(0xFFF3DAD2)
val LedgerDarkExpense = Color(0xFFD9705A)
val LedgerDarkExpenseContainer = Color(0xFF3D1B14)

val LedgerWarning = Color(0xFFB8862E)               // Vàng đất mù tạt
val LedgerWarningContainer = Color(0xFFF3E6C8)
val LedgerDarkWarning = Color(0xFFD9AC5C)
val LedgerDarkWarningContainer = Color(0xFF3B2C0F)

// ==========================================
// NotePay Ledger ColorSchemes (Default v3)
// ==========================================
val LedgerLightColorScheme: ColorScheme = lightColorScheme(
    primary = LedgerPrimary,
    onPrimary = LedgerOnPrimary,
    primaryContainer = LedgerPrimaryContainer,
    onPrimaryContainer = LedgerOnPrimaryContainer,
    secondary = LedgerSecondary,
    onSecondary = LedgerOnSecondary,
    secondaryContainer = LedgerSecondaryContainer,
    onSecondaryContainer = LedgerOnSecondaryContainer,
    background = LedgerLightBackground,
    onBackground = LedgerLightTextPrimary,
    surface = LedgerLightSurface,
    onSurface = LedgerLightTextPrimary,
    surfaceVariant = LedgerLightSurfaceSunken,
    onSurfaceVariant = LedgerLightTextSecondary,
    surfaceContainer = LedgerLightSurfaceSunken,
    surfaceContainerHighest = LedgerLightBorder,
    outline = LedgerLightTextMuted,
    outlineVariant = LedgerLightBorder,
    error = LedgerExpense,
    onError = LedgerOnPrimary,
    errorContainer = LedgerExpenseContainer,
    onErrorContainer = LedgerExpense,
)

val LedgerDarkColorScheme: ColorScheme = darkColorScheme(
    primary = LedgerDarkPrimary,
    onPrimary = LedgerDarkOnPrimary,
    primaryContainer = LedgerDarkPrimaryContainer,
    onPrimaryContainer = LedgerDarkOnPrimaryContainer,
    secondary = LedgerDarkSecondary,
    onSecondary = LedgerDarkOnSecondary,
    secondaryContainer = LedgerDarkSecondaryContainer,
    onSecondaryContainer = LedgerDarkOnSecondaryContainer,
    background = LedgerDarkBackground,
    onBackground = LedgerDarkTextPrimary,
    surface = LedgerDarkSurface,
    onSurface = LedgerDarkTextPrimary,
    surfaceVariant = LedgerDarkSurfaceSunken,
    onSurfaceVariant = LedgerDarkTextSecondary,
    surfaceContainer = LedgerDarkSurfaceSunken,
    surfaceContainerHighest = LedgerDarkBorder,
    outline = LedgerDarkTextMuted,
    outlineVariant = LedgerDarkBorder,
    error = LedgerDarkExpense,
    onError = LedgerDarkOnPrimary,
    errorContainer = LedgerDarkExpenseContainer,
    onErrorContainer = LedgerDarkExpense,
)

// ==========================================
// Legacy iOS Monochrome Tokens (For option)
// ==========================================
val IosBlack = Color(0xFF000000)
val IosWhite = Color(0xFFFFFFFF)

val IosLightBackground = Color(0xFFF2F2F7)
val IosLightSurface = Color(0xFFFFFFFF)
val IosLightSurfaceContainer = Color(0xFFE5E5EA)
val IosLightBorder = Color(0xFFD1D1D6)
val IosLightTextSecondary = Color(0xFF6C6C70)
val IosLightTextMuted = Color(0xFF8E8E93)

val IosDarkBackground = Color(0xFF000000)
val IosDarkSurface = Color(0xFF1C1C1E)
val IosDarkSurfaceContainer = Color(0xFF2C2C2E)
val IosDarkBorder = Color(0xFF38383A)
val IosDarkTextSecondary = Color(0xFF8E8E93)
val IosDarkTextMuted = Color(0xFF636366)

val IosSuccessLight = Color(0xFF34C759)
val IosSuccessDark = Color(0xFF30D158)
val IosExpenseLight = Color(0xFFFF3B30)
val IosExpenseDark = Color(0xFFFF453A)

val IosLightColorScheme: ColorScheme = lightColorScheme(
    primary = IosBlack,
    onPrimary = IosWhite,
    primaryContainer = IosLightSurfaceContainer,
    onPrimaryContainer = IosBlack,
    secondary = Color(0xFF3A3A3C),
    onSecondary = IosWhite,
    background = IosLightBackground,
    onBackground = IosBlack,
    surface = IosLightSurface,
    onSurface = IosBlack,
    surfaceVariant = IosLightSurfaceContainer,
    onSurfaceVariant = IosLightTextSecondary,
    surfaceContainer = IosLightSurfaceContainer,
    surfaceContainerHighest = IosLightBorder,
    outline = IosLightTextMuted,
    outlineVariant = IosLightBorder,
    error = IosExpenseLight,
    onError = IosWhite,
)

val IosDarkColorScheme: ColorScheme = darkColorScheme(
    primary = IosWhite,
    onPrimary = IosBlack,
    primaryContainer = IosDarkSurfaceContainer,
    onPrimaryContainer = IosWhite,
    secondary = Color(0xFF8E8E93),
    onSecondary = IosBlack,
    background = IosDarkBackground,
    onBackground = IosWhite,
    surface = IosDarkSurface,
    onSurface = IosWhite,
    surfaceVariant = IosDarkSurfaceContainer,
    onSurfaceVariant = IosDarkTextSecondary,
    surfaceContainer = IosDarkSurfaceContainer,
    surfaceContainerHighest = IosDarkBorder,
    outline = IosDarkTextMuted,
    outlineVariant = IosDarkBorder,
    error = IosExpenseDark,
    onError = IosBlack,
)

// ==========================================
// Theme Resolvers (Default to Ledger Ink & Warm Paper)
// ==========================================
fun getLightColorScheme(themeColor: String = "ledger", context: Context? = null): ColorScheme {
    return when (themeColor) {
        "ios" -> IosLightColorScheme
        "dynamic" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
            dynamicLightColorScheme(context)
        } else {
            LedgerLightColorScheme
        }
        else -> LedgerLightColorScheme
    }
}

fun getDarkColorScheme(themeColor: String = "ledger", context: Context? = null): ColorScheme {
    return when (themeColor) {
        "ios" -> IosDarkColorScheme
        "dynamic" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
            dynamicDarkColorScheme(context)
        } else {
            LedgerDarkColorScheme
        }
        else -> LedgerDarkColorScheme
    }
}
