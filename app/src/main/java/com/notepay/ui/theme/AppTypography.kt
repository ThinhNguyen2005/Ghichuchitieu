package com.notepay.ui.theme

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
 * Thang chữ chung (title / body / label) nằm ở [NotePayTypography]; lớp này chỉ giữ những
 * style đặc thù của v4: tiêu đề serif, nhãn eyebrow và bộ số tiền bật `tnum`.
 * Số tiền luôn dùng Inter + `tnum`, không dùng font serif.
 */
@Immutable
data class AppTypography(
    val displaySerif: TextStyle,
    val eyebrow: TextStyle,
    val amountHero: TextStyle,
    val amountCard: TextStyle,
    val amountRow: TextStyle
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
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 14.sp,
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
