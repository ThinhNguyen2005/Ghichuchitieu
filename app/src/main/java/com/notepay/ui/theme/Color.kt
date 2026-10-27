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
// NotePay Design Specification v4:
// Sand & Ink (Editorial) — xem DESIGN.md
// ==========================================

// Light Mode Canvas & Surface
val SandInk = Color(0xFF0B0B0B)                  // Thẻ nhấn, nút chính, thanh điều hướng
val SandLightBackground = Color(0xFFEFEFEF)      // Nền trang trung tính
val SandLightSurface = Color(0xFFFFFFFF)         // Thẻ nội dung
val SandLightSurfaceField = Color(0xFFF4F1EC)    // Ô nhập liệu, dải thu/chi
val SandLightSurfaceLow = Color(0xFFF7F7F7)      // Vùng lõm nhẹ
val SandLightSurfaceDim = Color(0xFFE7E7E7)

val SandLightTextPrimary = Color(0xFF111111)
val SandLightTextSecondary = Color(0xFF5C5C5C)   // 5.8:1 trên background
val SandLightTextMuted = Color(0xFF767676)       // >= 4.5:1 trên surface

val SandLightBorder = Color(0xFFBDBDBD)
val SandLightBorderSoft = Color(0xFFCFCFCF)
val SandLightBorderStrong = Color(0xFF8A8A8A)
val SandLightHairline = Color(0x1A111111)

// Light Mode Brand
val Sand = Color(0xFFF2D3A8)                     // Điểm sáng: mục chọn, đoạn thanh, huy hiệu
val SandOnSand = Color(0xFF111111)
val SandGraphite = Color(0xFF3A3733)
val SandClay = Color(0xFFC98A63)
val SandSky = Color(0xFF7CC4FF)                  // Chuỗi dữ liệu thứ tư
val SandLightTertiaryContainer = Color(0xFFF5E9DC)
val SandLightOnTertiaryContainer = Color(0xFF3A2A1C)
val SandLightErrorContainer = Color(0xFFF7E4DF)
val SandLightOnError = Color(0xFFFFFFFF)

// Dark Mode Canvas & Surface
val SandDarkInk = Color(0xFF262626)              // Bề mặt ink ở dark mode
val SandDarkInkBorder = Color(0xFF3A3A3A)        // Viền 1px bắt buộc kèm dark-ink
val SandDarkBackground = Color(0xFF141414)
val SandDarkSurface = Color(0xFF1D1D1D)
val SandDarkSurfaceLow = Color(0xFF191919)
val SandDarkCard = Color(0xFF242424)
val SandDarkSurfaceHigh = Color(0xFF2A2A2A)

val SandDarkTextPrimary = Color(0xFFF2F2F2)
val SandDarkTextSecondary = Color(0xFFA8A8A8)
val SandDarkTextMuted = Color(0xFF8A8A8A)

val SandDarkBorder = Color(0xFF3A3A3A)
val SandDarkBorderStrong = Color(0xFF4A4A4A)
val SandDarkHairline = Color(0x1AF2F2F2)

// Dark Mode Brand
val SandDarkPrimaryContainer = Color(0xFF3A2F1F)
val SandDarkSecondary = Color(0xFFB8AFA4)
val SandDarkTertiary = Color(0xFFE0B48F)
val SandDarkTertiaryContainer = Color(0xFF3A2A1C)
val SandDarkOnTertiaryContainer = Color(0xFFF5E9DC)
val SandDarkGraphite = Color(0xFF8A8279)
val SandDarkClay = Color(0xFFD19A72)

// Semantic Financial Flow
val SandIncome = Color(0xFF2F6B3A)
val SandDarkIncome = Color(0xFF7FCB8C)
val SandExpense = Color(0xFFA63B2E)
val SandDarkExpense = Color(0xFFFF8F7E)

// Thu/chi trên thẻ ink: biến thể sáng hơn để đủ tương phản trên nền #0B0B0B
val SandIncomeOnInk = Color(0xFF8FD99B)          // 11.8:1 trên ink
val SandExpenseOnInk = Color(0xFFFF9C8C)         // 9.7:1 trên ink
val SandDarkOnExpense = Color(0xFF2A1215)
val SandDarkErrorContainer = Color(0xFF3D1B14)
val SandDarkOnErrorContainer = Color(0xFFFF9C8C)
val SandWarning = Color(0xFF8A5A00)
val SandDarkWarning = Color(0xFFF2C26B)

// ==========================================
// NotePay ColorSchemes v4 (Sand & Ink)
// ==========================================
val SandLightColorScheme: ColorScheme = lightColorScheme(
    primary = SandInk,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Sand,
    onPrimaryContainer = SandOnSand,
    secondary = SandGraphite,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = SandLightSurfaceField,
    onSecondaryContainer = SandLightTextPrimary,
    tertiary = SandClay,
    onTertiary = SandOnSand,
    tertiaryContainer = SandLightTertiaryContainer,
    onTertiaryContainer = SandLightOnTertiaryContainer,
    background = SandLightBackground,
    onBackground = SandLightTextPrimary,
    surface = SandLightSurface,
    onSurface = SandLightTextPrimary,
    surfaceVariant = SandLightSurfaceField,
    onSurfaceVariant = SandLightTextSecondary,
    surfaceDim = SandLightSurfaceDim,
    surfaceBright = SandLightSurface,
    surfaceContainerLowest = SandLightSurface,
    surfaceContainerLow = SandLightSurfaceLow,
    surfaceContainer = SandLightSurfaceField,
    surfaceContainerHigh = SandLightSurface,
    surfaceContainerHighest = SandLightBorderSoft,
    inverseSurface = SandInk,
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Sand,
    surfaceTint = SandLightSurface,
    outline = SandLightBorder,
    outlineVariant = SandLightBorderSoft,
    error = SandExpense,
    onError = SandLightOnError,
    errorContainer = SandLightErrorContainer,
    onErrorContainer = SandExpense,
    scrim = Color(0xFF000000)
)

val SandDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Sand,
    onPrimary = SandOnSand,
    primaryContainer = SandDarkPrimaryContainer,
    onPrimaryContainer = Sand,
    secondary = SandDarkSecondary,
    onSecondary = SandDarkBackground,
    secondaryContainer = SandDarkSurfaceHigh,
    onSecondaryContainer = SandDarkTextPrimary,
    tertiary = SandDarkTertiary,
    onTertiary = SandDarkBackground,
    tertiaryContainer = SandDarkTertiaryContainer,
    onTertiaryContainer = SandDarkOnTertiaryContainer,
    background = SandDarkBackground,
    onBackground = SandDarkTextPrimary,
    surface = SandDarkSurface,
    onSurface = SandDarkTextPrimary,
    surfaceVariant = SandDarkCard,
    onSurfaceVariant = SandDarkTextSecondary,
    surfaceDim = SandDarkBackground,
    surfaceBright = SandDarkSurfaceHigh,
    surfaceContainerLowest = SandDarkSurfaceLow,
    surfaceContainerLow = SandDarkSurface,
    surfaceContainer = SandDarkCard,
    surfaceContainerHigh = SandDarkSurfaceHigh,
    surfaceContainerHighest = SandDarkBorder,
    inverseSurface = SandDarkTextPrimary,
    inverseOnSurface = SandDarkBackground,
    inversePrimary = SandInk,
    surfaceTint = SandDarkSurface,
    outline = SandDarkBorderStrong,
    outlineVariant = SandDarkBorder,
    error = SandDarkExpense,
    onError = SandDarkOnExpense,
    errorContainer = SandDarkErrorContainer,
    onErrorContainer = SandDarkOnErrorContainer,
    scrim = Color(0xFF000000)
)

// ==========================================
// Legacy iOS Monochrome Tokens (lựa chọn "Đơn sắc iOS")
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
// Theme Resolvers (mặc định "sand")
// ==========================================
fun getLightColorScheme(themeColor: String = "sand", context: Context? = null): ColorScheme {
    return when (themeColor) {
        "ios" -> IosLightColorScheme
        "dynamic" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
            dynamicLightColorScheme(context)
        } else {
            SandLightColorScheme
        }
        // "sand" (mặc định), "ledger" (id v3 đã lưu), "green" (backup cũ) và mọi id lạ
        else -> SandLightColorScheme
    }
}

fun getDarkColorScheme(themeColor: String = "sand", context: Context? = null): ColorScheme {
    return when (themeColor) {
        "ios" -> IosDarkColorScheme
        "dynamic" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
            dynamicDarkColorScheme(context)
        } else {
            SandDarkColorScheme
        }
        else -> SandDarkColorScheme
    }
}
