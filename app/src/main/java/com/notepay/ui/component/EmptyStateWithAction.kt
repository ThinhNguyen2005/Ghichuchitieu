package com.notepay.ui.component

import com.notepay.ui.theme.AppTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Empty state duy nhất của ứng dụng: hỗ trợ hiển thị icon tròn nền nhạt + tiêu đề + mô tả và nút hành động tùy chọn.
 *
 * @param title tiêu đề hoặc nội dung thông báo chính.
 * @param description mô tả chi tiết (tùy chọn).
 * @param icon icon lớn ở giữa (mặc định là FolderOpen).
 * @param actionLabel nhãn nút hành động (tùy chọn).
 * @param onClick callback khi nhấn nút (tùy chọn).
 */
@Composable
fun EmptyStateWithAction(
    title: String,
    modifier: Modifier = Modifier,
    description: String = "",
    icon: ImageVector = Icons.Outlined.FolderOpen,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconBackground: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
    actionLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    // Tự động phân tách tiêu đề và mô tả nếu tiêu đề chứa ký tự xuống dòng
    val (displayTitle, displayDescription) = remember(title, description) {
        if (description.isEmpty() && (title.contains("\n") || title.contains("\\n"))) {
            val delimiter = if (title.contains("\n")) "\n" else "\\n"
            val parts = title.split(delimiter, limit = 2)
            parts.getOrElse(0) { title } to parts.getOrElse(1) { "" }.replace("\\n", "\n")
        } else {
            title to description
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(AppTheme.shapes.circle)
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = displayTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (displayDescription.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = displayDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onClick != null) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onClick,
                shape = AppTheme.shapes.corner12,
            ) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
