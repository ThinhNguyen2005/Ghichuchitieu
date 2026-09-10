package com.notepay.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
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
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        text.forEachIndexed { index, char ->
            if (char.isDigit()) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        // Trượt lên mượt mà khi đổi số
                        (slideInVertically { height -> height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> -height } + fadeOut()) using
                                SizeTransform(clip = false)
                    },
                    label = "DigitTicker_$index"
                ) { targetChar ->
                    Text(
                        text = targetChar.toString(),
                        style = style,
                        color = color,
                        fontWeight = fontWeight ?: style.fontWeight,
                    )
                }
            } else {
                Text(
                    text = char.toString(),
                    style = style,
                    color = color,
                    fontWeight = fontWeight ?: style.fontWeight,
                )
            }
        }
    }
}
