package com.notepay.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.notepay.R

val FrauncesFontFamily = FontFamily(
    Font(R.font.fraunces_variable)
)

val InterFontFamily = FontFamily(
    Font(R.font.inter_variable)
)

/**
 * Lớp kiểu chữ ngữ nghĩa của v4 "Sand & Ink" (DESIGN.md §3).
 *
 * Tích hợp toàn bộ thang chữ NotePay vào AppTheme.typography để các component
 * không phải trộn lẫn giữa MaterialTheme và AppTheme.
 * Số tiền luôn dùng Inter + `tnum`, không dùng font serif.
 * Toàn bộ kiểu chữ đều tuân thủ kích thước tối thiểu >= 12sp.
 */
@Immutable
data class AppTypography(
    val displaySerif: TextStyle,
    val eyebrow: TextStyle,
    val amountHero: TextStyle,
    val amountCard: TextStyle,
    val amountRow: TextStyle,
    val displayLarge: TextStyle = NotePayTypography.displayLarge,
    val displayMedium: TextStyle = NotePayTypography.displayMedium,
    val displaySmall: TextStyle = NotePayTypography.displaySmall,
    val headlineLarge: TextStyle = NotePayTypography.headlineLarge,
    val headlineMedium: TextStyle = NotePayTypography.headlineMedium,
    val headlineSmall: TextStyle = NotePayTypography.headlineSmall,
    val titleLarge: TextStyle = NotePayTypography.titleLarge,
    val titleMedium: TextStyle = NotePayTypography.titleMedium,
    val titleSmall: TextStyle = NotePayTypography.titleSmall,
    val bodyLarge: TextStyle = NotePayTypography.bodyLarge,
    val bodyMedium: TextStyle = NotePayTypography.bodyMedium,
    val bodySmall: TextStyle = NotePayTypography.bodySmall,
    val labelLarge: TextStyle = NotePayTypography.labelLarge,
    val labelMedium: TextStyle = NotePayTypography.labelMedium,
    val labelSmall: TextStyle = NotePayTypography.labelSmall,
)

val DefaultAppTypography = AppTypography(
    displaySerif = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontSize = 26.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 30.sp
    ),
    eyebrow = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 12.sp, // Tối thiểu 12sp theo chuẩn WCAG
        fontWeight = FontWeight.SemiBold,
        lineHeight = 16.sp,
        letterSpacing = 0.08.em
    ),
    amountHero = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 44.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 48.sp,
        letterSpacing = (-0.03).em,
        fontFeatureSettings = "tnum"
    ),
    amountCard = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp,
        fontFeatureSettings = "tnum"
    ),
    amountRow = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum"
    )
)

val LocalAppTypography = staticCompositionLocalOf { DefaultAppTypography }
