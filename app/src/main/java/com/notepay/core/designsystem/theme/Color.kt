package com.notepay.core.designsystem.theme

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
// Theme Palettes: Emerald, Ocean, Lavender
// ==========================================
val EmeraldInk = Color(0xFF0D5C43)
val EmeraldOnInk = Color(0xFFFFFFFF)
val EmeraldLightBg = Color(0xFFEFF6F2)
val EmeraldLightSurface = Color(0xFFFFFFFF)
val EmeraldLightField = Color(0xFFE6F2EC)
val EmeraldLightBorder = Color(0xFFB3CCC0)
val EmeraldLightBorderSoft = Color(0xFFC8DDD2)
val EmeraldSecondary = Color(0xFF2F6B58)
val EmeraldAccentLight = Color(0xFFA7F3D0)
val EmeraldOnAccentLight = Color(0xFF063B2B)

val EmeraldDarkBg = Color(0xFF0E1613)
val EmeraldDarkSurface = Color(0xFF16221D)
val EmeraldDarkLow = Color(0xFF121C18)
val EmeraldDarkCard = Color(0xFF1E2E27)
val EmeraldDarkInk = Color(0xFF144D39)
val EmeraldDarkAccent = Color(0xFF5EEAD4)
val EmeraldDarkOnAccent = Color(0xFF052E26)
val EmeraldDarkText = Color(0xFFEAF4EF)
val EmeraldDarkTextSecondary = Color(0xFFA2C0B3)
val EmeraldDarkBorder = Color(0xFF2C4238)

val OceanInk = Color(0xFF154A8A)
val OceanOnInk = Color(0xFFFFFFFF)
val OceanLightBg = Color(0xFFEDF3FA)
val OceanLightSurface = Color(0xFFFFFFFF)
val OceanLightField = Color(0xFFE4EEF9)
val OceanLightBorder = Color(0xFFB4C8DE)
val OceanLightBorderSoft = Color(0xFFC9D9EB)
val OceanSecondary = Color(0xFF3B5F8A)
val OceanAccentLight = Color(0xFFBBD9FF)
val OceanOnAccentLight = Color(0xFF0C294D)

val OceanDarkBg = Color(0xFF0D141E)
val OceanDarkSurface = Color(0xFF151F2D)
val OceanDarkLow = Color(0xFF111925)
val OceanDarkCard = Color(0xFF1D2A3D)
val OceanDarkInk = Color(0xFF1B3B66)
val OceanDarkAccent = Color(0xFF7CB8FF)
val OceanDarkOnAccent = Color(0xFF08203E)
val OceanDarkText = Color(0xFFEAF2FC)
val OceanDarkTextSecondary = Color(0xFFA3B8D4)
val OceanDarkBorder = Color(0xFF2A3B54)

val LavenderInk = Color(0xFF54338A)
val LavenderOnInk = Color(0xFFFFFFFF)
val LavenderLightBg = Color(0xFFF4F0FA)
val LavenderLightSurface = Color(0xFFFFFFFF)
val LavenderLightField = Color(0xFFECE5F7)
val LavenderLightBorder = Color(0xFFC5B9DB)
val LavenderLightBorderSoft = Color(0xFFD7CEE8)
val LavenderSecondary = Color(0xFF6B5291)
val LavenderAccentLight = Color(0xFFDEC8FF)
val LavenderOnAccentLight = Color(0xFF2B1452)

val LavenderDarkBg = Color(0xFF14101D)
val LavenderDarkSurface = Color(0xFF1D1729)
val LavenderDarkLow = Color(0xFF181322)
val LavenderDarkCard = Color(0xFF271F37)
val LavenderDarkInk = Color(0xFF3E266B)
val LavenderDarkAccent = Color(0xFFC9A8FF)
val LavenderDarkOnAccent = Color(0xFF220E42)
val LavenderDarkText = Color(0xFFF2ECFC)
val LavenderDarkTextSecondary = Color(0xFFBCB0D4)
val LavenderDarkBorder = Color(0xFF382D4F)

val EmeraldLightColorScheme: ColorScheme = lightColorScheme(
    primary = EmeraldInk,
    onPrimary = EmeraldOnInk,
    primaryContainer = EmeraldAccentLight,
    onPrimaryContainer = EmeraldOnAccentLight,
    secondary = EmeraldSecondary,
    onSecondary = EmeraldOnInk,
    secondaryContainer = EmeraldLightField,
    onSecondaryContainer = SandLightTextPrimary,
    tertiary = EmeraldSecondary,
    onTertiary = EmeraldOnInk,
    background = EmeraldLightBg,
    onBackground = SandLightTextPrimary,
    surface = EmeraldLightSurface,
    onSurface = SandLightTextPrimary,
    surfaceVariant = EmeraldLightField,
    onSurfaceVariant = SandLightTextSecondary,
    surfaceContainerLowest = EmeraldLightSurface,
    surfaceContainerLow = EmeraldLightField,
    surfaceContainer = EmeraldLightField,
    surfaceContainerHigh = EmeraldLightSurface,
    surfaceContainerHighest = EmeraldLightBorderSoft,
    outline = EmeraldLightBorder,
    outlineVariant = EmeraldLightBorderSoft,
    error = SandExpense,
    onError = SandLightOnError,
    errorContainer = SandLightErrorContainer,
    onErrorContainer = SandExpense,
)

val EmeraldDarkColorScheme: ColorScheme = darkColorScheme(
    primary = EmeraldDarkAccent,
    onPrimary = EmeraldDarkOnAccent,
    primaryContainer = EmeraldDarkInk,
    onPrimaryContainer = EmeraldDarkText,
    secondary = EmeraldDarkTextSecondary,
    onSecondary = EmeraldDarkBg,
    secondaryContainer = EmeraldDarkCard,
    onSecondaryContainer = EmeraldDarkText,
    tertiary = EmeraldDarkAccent,
    onTertiary = EmeraldDarkOnAccent,
    background = EmeraldDarkBg,
    onBackground = EmeraldDarkText,
    surface = EmeraldDarkSurface,
    onSurface = EmeraldDarkText,
    surfaceVariant = EmeraldDarkCard,
    onSurfaceVariant = EmeraldDarkTextSecondary,
    surfaceContainerLowest = EmeraldDarkLow,
    surfaceContainerLow = EmeraldDarkSurface,
    surfaceContainer = EmeraldDarkCard,
    surfaceContainerHigh = EmeraldDarkBorder,
    surfaceContainerHighest = EmeraldDarkBorder,
    outline = EmeraldDarkBorder,
    outlineVariant = EmeraldDarkBorder,
    error = SandDarkExpense,
    onError = SandDarkOnExpense,
    errorContainer = SandDarkErrorContainer,
    onErrorContainer = SandDarkOnErrorContainer,
)

val OceanLightColorScheme: ColorScheme = lightColorScheme(
    primary = OceanInk,
    onPrimary = OceanOnInk,
    primaryContainer = OceanAccentLight,
    onPrimaryContainer = OceanOnAccentLight,
    secondary = OceanSecondary,
    onSecondary = OceanOnInk,
    secondaryContainer = OceanLightField,
    onSecondaryContainer = SandLightTextPrimary,
    tertiary = OceanSecondary,
    onTertiary = OceanOnInk,
    background = OceanLightBg,
    onBackground = SandLightTextPrimary,
    surface = OceanLightSurface,
    onSurface = SandLightTextPrimary,
    surfaceVariant = OceanLightField,
    onSurfaceVariant = SandLightTextSecondary,
    surfaceContainerLowest = OceanLightSurface,
    surfaceContainerLow = OceanLightField,
    surfaceContainer = OceanLightField,
    surfaceContainerHigh = OceanLightSurface,
    surfaceContainerHighest = OceanLightBorderSoft,
    outline = OceanLightBorder,
    outlineVariant = OceanLightBorderSoft,
    error = SandExpense,
    onError = SandLightOnError,
    errorContainer = SandLightErrorContainer,
    onErrorContainer = SandExpense,
)

val OceanDarkColorScheme: ColorScheme = darkColorScheme(
    primary = OceanDarkAccent,
    onPrimary = OceanDarkOnAccent,
    primaryContainer = OceanDarkInk,
    onPrimaryContainer = OceanDarkText,
    secondary = OceanDarkTextSecondary,
    onSecondary = OceanDarkBg,
    secondaryContainer = OceanDarkCard,
    onSecondaryContainer = OceanDarkText,
    tertiary = OceanDarkAccent,
    onTertiary = OceanDarkOnAccent,
    background = OceanDarkBg,
    onBackground = OceanDarkText,
    surface = OceanDarkSurface,
    onSurface = OceanDarkText,
    surfaceVariant = OceanDarkCard,
    onSurfaceVariant = OceanDarkTextSecondary,
    surfaceContainerLowest = OceanDarkLow,
    surfaceContainerLow = OceanDarkSurface,
    surfaceContainer = OceanDarkCard,
    surfaceContainerHigh = OceanDarkBorder,
    surfaceContainerHighest = OceanDarkBorder,
    outline = OceanDarkBorder,
    outlineVariant = OceanDarkBorder,
    error = SandDarkExpense,
    onError = SandDarkOnExpense,
    errorContainer = SandDarkErrorContainer,
    onErrorContainer = SandDarkOnErrorContainer,
)

val LavenderLightColorScheme: ColorScheme = lightColorScheme(
    primary = LavenderInk,
    onPrimary = LavenderOnInk,
    primaryContainer = LavenderAccentLight,
    onPrimaryContainer = LavenderOnAccentLight,
    secondary = LavenderSecondary,
    onSecondary = LavenderOnInk,
    secondaryContainer = LavenderLightField,
    onSecondaryContainer = SandLightTextPrimary,
    tertiary = LavenderSecondary,
    onTertiary = LavenderOnInk,
    background = LavenderLightBg,
    onBackground = SandLightTextPrimary,
    surface = LavenderLightSurface,
    onSurface = SandLightTextPrimary,
    surfaceVariant = LavenderLightField,
    onSurfaceVariant = SandLightTextSecondary,
    surfaceContainerLowest = LavenderLightSurface,
    surfaceContainerLow = LavenderLightField,
    surfaceContainer = LavenderLightField,
    surfaceContainerHigh = LavenderLightSurface,
    surfaceContainerHighest = LavenderLightBorderSoft,
    outline = LavenderLightBorder,
    outlineVariant = LavenderLightBorderSoft,
    error = SandExpense,
    onError = SandLightOnError,
    errorContainer = SandLightErrorContainer,
    onErrorContainer = SandExpense,
)

val LavenderDarkColorScheme: ColorScheme = darkColorScheme(
    primary = LavenderDarkAccent,
    onPrimary = LavenderDarkOnAccent,
    primaryContainer = LavenderDarkInk,
    onPrimaryContainer = LavenderDarkText,
    secondary = LavenderDarkTextSecondary,
    onSecondary = LavenderDarkBg,
    secondaryContainer = LavenderDarkCard,
    onSecondaryContainer = LavenderDarkText,
    tertiary = LavenderDarkAccent,
    onTertiary = LavenderDarkOnAccent,
    background = LavenderDarkBg,
    onBackground = LavenderDarkText,
    surface = LavenderDarkSurface,
    onSurface = LavenderDarkText,
    surfaceVariant = LavenderDarkCard,
    onSurfaceVariant = LavenderDarkTextSecondary,
    surfaceContainerLowest = LavenderDarkLow,
    surfaceContainerLow = LavenderDarkSurface,
    surfaceContainer = LavenderDarkCard,
    surfaceContainerHigh = LavenderDarkBorder,
    surfaceContainerHighest = LavenderDarkBorder,
    outline = LavenderDarkBorder,
    outlineVariant = LavenderDarkBorder,
    error = SandDarkExpense,
    onError = SandDarkOnExpense,
    errorContainer = SandDarkErrorContainer,
    onErrorContainer = SandDarkOnErrorContainer,
)

// Streak tier flame tokens
val StreakSparkFlame = Color(0xFFFF8A1E)
val StreakBlazeFlame = Color(0xFFFF5E1A)
val StreakInfernoFlame = Color(0xFFFF3D00)
val StreakFlameCore = Color(0xFFFFDF6B)
val StreakInfernoDarkFlame = Color(0xFFFF7A29)

// ==========================================
// Theme Resolvers (mặc định "sand")
// ==========================================
fun getLightColorScheme(themeColor: String = "sand", context: Context? = null): ColorScheme {
    return when (themeColor) {
        "emerald" -> EmeraldLightColorScheme
        "ocean" -> OceanLightColorScheme
        "lavender" -> LavenderLightColorScheme
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
        "emerald" -> EmeraldDarkColorScheme
        "ocean" -> OceanDarkColorScheme
        "lavender" -> LavenderDarkColorScheme
        "ios" -> IosDarkColorScheme
        "dynamic" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
            dynamicDarkColorScheme(context)
        } else {
            SandDarkColorScheme
        }
        else -> SandDarkColorScheme
    }
}

// ==========================================
// Bảng màu "Mint" — đặc khu cho màn Home
// ==========================================
// Các token dưới đây chưa phải theme toàn cục: chỉ `HomePalette` (feature/home/theme)
// đọc chúng, nên màn khác và thanh điều hướng vẫn dùng v4 Sand & Ink. Khi Home được
// duyệt chính thức, các token này sẽ chuyển lên AppColors và section này bị xóa.

// Light — nền lạnh rất nhạt, thẻ trắng, nhấn xanh lá
val MintLightBackground = Color(0xFFF7F9FB)
val MintLightCard = Color(0xFFFFFFFF)
val MintLightField = Color(0xFFF2F4F6)
val MintLightChip = Color(0xFFE6E8EA)
val MintLightTrack = Color(0xFFECEEF0)
val MintLightText = Color(0xFF191C1E)
val MintLightTextVariant = Color(0xFF3C4A42)
val MintLightDivider = Color(0xFFBBCABF)
val MintPrimary = Color(0xFF006C49)                 // 5.9:1 trên MintLightCard
val MintOnPrimary = Color(0xFFFFFFFF)
val MintPrimaryContainer = Color(0xFF10B981)        // đoạn thanh ngân sách, khối icon
val MintBanner = Color(0xFFDAE2FD)                  // fixed của vai phụ: banner AI
val MintOnBanner = Color(0xFF131B2E)
val MintOnBannerMuted = Color(0xFF5C647A)
val MintLightError = Color(0xFFBA1A1A)
val MintLightErrorContainer = Color(0xFFFFDAD6)
val MintLightOnErrorContainer = Color(0xFF93000A)

// Dark — cùng ngữ nghĩa nhưng đảo sáng/tối để giữ tương phản >= 4.5:1
val MintDarkBackground = Color(0xFF101312)
val MintDarkCard = Color(0xFF1A1E1D)
val MintDarkField = Color(0xFF202524)
val MintDarkChip = Color(0xFF2A302E)
val MintDarkTrack = Color(0xFF242A28)
val MintDarkText = Color(0xFFE2E5E3)
val MintDarkTextVariant = Color(0xFFAFC3B7)
val MintDarkDivider = Color(0xFF2E3533)
val MintDarkPrimary = Color(0xFF57D9A3)
val MintDarkOnPrimary = Color(0xFF00382A)
val MintDarkPrimaryContainer = Color(0xFF146C4A)
val MintDarkBanner = Color(0xFF22304A)
val MintOnDarkBanner = Color(0xFFDCE6F8)
val MintOnDarkBannerMuted = Color(0xFFA8B6CC)
val MintDarkError = Color(0xFFFF6F6F)
val MintDarkErrorContainer = Color(0xFF5C2320)
val MintDarkOnErrorContainer = Color(0xFFFFB4AB)

// ==========================================
// Home iOS Surface, Shadows & Background Presets
// ==========================================
val HomeShadowLight = Color(0xFF1A1F2C)
val HomeShadowDark = Color(0xFF000000)

val HomeHeroCardLightTop = Color(0xFFFFFFFF)
val HomeHeroCardLightBottom = Color(0xFFFAF8F4)
val HomeHeroCardLightRim = Color(0xE6FFFFFF)
val HomeHeroCardDarkTop = Color(0xFF282B32)
val HomeHeroCardDarkBottom = Color(0xFF1E2127)
val HomeHeroCardDarkRim = Color(0x24FFFFFF)

// Warm Silk
val HomePresetWarmSilkLightTop = Color(0xFFFBF5EC)
val HomePresetWarmSilkLightMid = Color(0xFFF5E6D3)
val HomePresetWarmSilkLightBottom = Color(0xFFF3F1EE)
val HomePresetWarmSilkDarkTop = Color(0xFF181615)
val HomePresetWarmSilkDarkMid = Color(0xFF271F19)
val HomePresetWarmSilkDarkBottom = Color(0xFF141414)

// Ocean Mist
val HomePresetOceanMistLightTop = Color(0xFFEAF3FF)
val HomePresetOceanMistLightMid = Color(0xFFD9ECF8)
val HomePresetOceanMistLightBottom = Color(0xFFF2F5F8)
val HomePresetOceanMistDarkTop = Color(0xFF0E1824)
val HomePresetOceanMistDarkMid = Color(0xFF15293D)
val HomePresetOceanMistDarkBottom = Color(0xFF11151B)

// Sage Morning
val HomePresetSageMorningLightTop = Color(0xFFEDF6F0)
val HomePresetSageMorningLightMid = Color(0xFFDCECE2)
val HomePresetSageMorningLightBottom = Color(0xFFF2F5F3)
val HomePresetSageMorningDarkTop = Color(0xFF101C17)
val HomePresetSageMorningDarkMid = Color(0xFF182D24)
val HomePresetSageMorningDarkBottom = Color(0xFF121614)

// Sunset Bloom
val HomePresetSunsetBloomLightTop = Color(0xFFFFF1EC)
val HomePresetSunsetBloomLightMid = Color(0xFFFCE0D4)
val HomePresetSunsetBloomLightBottom = Color(0xFFF1ECF8)
val HomePresetSunsetBloomDarkTop = Color(0xFF1E1420)
val HomePresetSunsetBloomDarkMid = Color(0xFF2D1C25)
val HomePresetSunsetBloomDarkBottom = Color(0xFF141318)

// Aurora Lavender
val HomePresetAuroraLavenderLightTop = Color(0xFFF2EFFF)
val HomePresetAuroraLavenderLightMid = Color(0xFFE4E5FC)
val HomePresetAuroraLavenderLightBottom = Color(0xFFF4F3F8)
val HomePresetAuroraLavenderDarkTop = Color(0xFF141427)
val HomePresetAuroraLavenderDarkMid = Color(0xFF211E3D)
val HomePresetAuroraLavenderDarkBottom = Color(0xFF13131A)

// Minimal Canvas
val HomePresetMinimalCanvasLightTop = Color(0xFFF2F2F7)
val HomePresetMinimalCanvasLightMid = Color(0xFFEFEFF4)
val HomePresetMinimalCanvasLightBottom = Color(0xFFF2F2F7)
val HomePresetMinimalCanvasDarkTop = Color(0xFF121316)
val HomePresetMinimalCanvasDarkMid = Color(0xFF16181D)
val HomePresetMinimalCanvasDarkBottom = Color(0xFF121316)

