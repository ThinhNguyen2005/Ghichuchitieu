package com.notepay.core.designsystem.component.effect

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import com.notepay.core.navigation.NotePayEmphasizedAccelerate
import com.notepay.core.navigation.NotePayEmphasizedDecelerate
import com.notepay.core.navigation.rememberNotePayReducedMotion

/**
 * Hiệu ứng cuộn số (Rolling Number / Ticker) cho chuỗi tiền tệ hoặc số liệu tài chính.
 * Mỗi chữ số khi thay đổi sẽ trượt dọc và fade mượt mà.
 */
@Composable
fun RollingNumberTicker(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    if (reducedMotion) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            fontWeight = fontWeight ?: style.fontWeight,
        )
        return
    }

    val animatedStyle = style.copy(textMotion = TextMotion.Animated)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        text.forEachIndexed { index, char ->
            if (char.isDigit()) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = tween(durationMillis = 200, easing = NotePayEmphasizedDecelerate),
                        ) { height -> height } + fadeIn(
                            animationSpec = tween(durationMillis = 200, easing = NotePayEmphasizedDecelerate),
                        )) togetherWith (slideOutVertically(
                            animationSpec = tween(durationMillis = 140, easing = NotePayEmphasizedAccelerate),
                        ) { height -> -height } + fadeOut(
                            animationSpec = tween(durationMillis = 140, easing = NotePayEmphasizedAccelerate),
                        )) using SizeTransform(clip = false)
                    },
                    label = "DigitTicker_$index",
                ) { targetChar ->
                    Text(
                        text = targetChar.toString(),
                        style = animatedStyle,
                        color = color,
                        fontWeight = fontWeight ?: style.fontWeight,
                    )
                }
            } else {
                Text(
                    text = char.toString(),
                    style = animatedStyle,
                    color = color,
                    fontWeight = fontWeight ?: style.fontWeight,
                )
            }
        }
    }
}
