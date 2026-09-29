package com.notepay.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.notepay.R

val FrauncesFontFamily = FontFamily(
    Font(R.font.fraunces_variable)
)

val ManropeFontFamily = FontFamily(
    Font(R.font.manrope_variable)
)

val InterFontFamily = FontFamily(
    Font(R.font.inter_variable)
)

val NotePayNumberFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

data class AppTypography(
    val display: TextStyle,
    val displayMedium: TextStyle,
    val displaySemibold: TextStyle,
    
    val headline: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSemibold: TextStyle,
    
    val title: TextStyle,
    val titleMedium: TextStyle,
    val titleSemibold: TextStyle,
    
    val body: TextStyle,
    val bodyMedium: TextStyle,
    val bodySemibold: TextStyle,
    
    val caption: TextStyle,
    val captionMedium: TextStyle,
    val captionSemibold: TextStyle,
    
    val numberFontFamily: FontFamily,
    val displayFontFamily: FontFamily = FrauncesFontFamily,
    val contentFontFamily: FontFamily = ManropeFontFamily,
)

val DefaultAppTypography = AppTypography(
    display = TextStyle(fontFamily = FrauncesFontFamily, fontSize = 32.sp, fontWeight = FontWeight.Normal, lineHeight = 38.sp),
    displayMedium = TextStyle(fontFamily = FrauncesFontFamily, fontSize = 32.sp, fontWeight = FontWeight.Medium, lineHeight = 38.sp),
    displaySemibold = TextStyle(fontFamily = FrauncesFontFamily, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, lineHeight = 38.sp),
    
    headline = TextStyle(fontFamily = FrauncesFontFamily, fontSize = 22.sp, fontWeight = FontWeight.Normal, lineHeight = 28.sp),
    headlineMedium = TextStyle(fontFamily = FrauncesFontFamily, fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp),
    headlineSemibold = TextStyle(fontFamily = FrauncesFontFamily, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    
    title = TextStyle(fontFamily = ManropeFontFamily, fontSize = 19.sp, fontWeight = FontWeight.Normal, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = ManropeFontFamily, fontSize = 19.sp, fontWeight = FontWeight.Medium, lineHeight = 25.sp),
    titleSemibold = TextStyle(fontFamily = ManropeFontFamily, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, lineHeight = 25.sp),
    
    body = TextStyle(fontFamily = ManropeFontFamily, fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = ManropeFontFamily, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp),
    bodySemibold = TextStyle(fontFamily = ManropeFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    
    caption = TextStyle(fontFamily = ManropeFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    captionMedium = TextStyle(fontFamily = ManropeFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    captionSemibold = TextStyle(fontFamily = ManropeFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp),
    
    numberFontFamily = NotePayNumberFontFamily,
    displayFontFamily = FrauncesFontFamily,
    contentFontFamily = ManropeFontFamily,
)

val LocalAppTypography = staticCompositionLocalOf { DefaultAppTypography }
