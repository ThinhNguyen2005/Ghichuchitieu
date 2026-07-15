package com.notepay.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.notepay.R

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
    
    val numberFontFamily: FontFamily
)

val DefaultAppTypography = AppTypography(
    display = TextStyle(fontFamily = InterFontFamily, fontSize = 34.sp, fontWeight = FontWeight.Normal, lineHeight = 41.sp),
    displayMedium = TextStyle(fontFamily = InterFontFamily, fontSize = 34.sp, fontWeight = FontWeight.Medium, lineHeight = 41.sp),
    displaySemibold = TextStyle(fontFamily = InterFontFamily, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, lineHeight = 41.sp),
    
    headline = TextStyle(fontFamily = InterFontFamily, fontSize = 22.sp, fontWeight = FontWeight.Normal, lineHeight = 28.sp),
    headlineMedium = TextStyle(fontFamily = InterFontFamily, fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp),
    headlineSemibold = TextStyle(fontFamily = InterFontFamily, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    
    title = TextStyle(fontFamily = InterFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Normal, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = InterFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Medium, lineHeight = 25.sp),
    titleSemibold = TextStyle(fontFamily = InterFontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 25.sp),
    
    body = TextStyle(fontFamily = InterFontFamily, fontSize = 17.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = InterFontFamily, fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp),
    bodySemibold = TextStyle(fontFamily = InterFontFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    
    caption = TextStyle(fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    captionMedium = TextStyle(fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    captionSemibold = TextStyle(fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp),
    
    numberFontFamily = NotePayNumberFontFamily
)

val LocalAppTypography = staticCompositionLocalOf { DefaultAppTypography }

