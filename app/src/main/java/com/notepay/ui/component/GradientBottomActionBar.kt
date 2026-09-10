package com.notepay.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Thanh hành động cố định ở đáy màn hình.
 *
 * Trước đây dùng gradient trong suốt 55–88% nên nội dung cuộn phía dưới lộ mờ qua thanh,
 * gây rối mắt. Nay dùng nền đặc + đường kẻ mảnh ở mép trên để tách bạch rõ vùng nội dung
 * và vùng hành động.
 */
@Composable
fun GradientBottomActionBar(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = 16.dp,
        top = 20.dp,
        end = 16.dp,
        bottom = 10.dp,
    ),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(contentPadding),
            ) {
                content()
            }
        }
    }
}
