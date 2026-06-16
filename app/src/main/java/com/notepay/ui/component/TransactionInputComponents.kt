package com.notepay.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.domain.model.Category
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.sp

/**
 * Hiển thị số tiền lớn, căn giữa kèm ký hiệu tiền tệ ₫ màu Primary nổi bật
 */
@Composable
fun TransactionAmountDisplay(
    amountInput: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Nhập số tiền",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            val formattedAmount = remember(amountInput) {
                if (amountInput.isEmpty() || amountInput == "0") "0" else {
                    val amountLong = amountInput.toLongOrNull() ?: 0L
                    val formatter = java.text.DecimalFormat("#,###")
                    formatter.format(amountLong).replace(",", ".")
                }
            }
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "đ",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

/**
 * Dải chọn nhanh danh mục dạng ô vuông (square) có border như hình thiết kế
 */
@Composable
fun CategoryQuickSelectionRow(
    categories: List<Category>,
    selectedCategory: Category?,
    isIncome: Boolean,
    onCategoryChanged: (Category) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleCategories = remember(categories, isIncome) {
        categories.filter { it == Category.OTHER || it.isIncome == isIncome }
            .take(4) // Hiển thị 4 danh mục phổ biến nhất
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Danh mục",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Xem tất cả",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSeeAllClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            visibleCategories.forEach { category ->
                val isSelected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.1f) // Hơi chữ nhật nhẹ cho cân đối
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onCategoryChanged(category) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CategoryAvatar(category = category, size = 32.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ô thông tin dạng Card tròn bo góc chứa Icon bên trái, nhãn trên và giá trị lớn bên dưới
 */
@Composable
fun TransactionInputField(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value.ifBlank { placeholder },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}



@Immutable
sealed interface KeypadKey {
    data class Number(val value: Int) : KeypadKey
    data object DotThreeZeros : KeypadKey // Phím ".000"
    data object Backspace : KeypadKey     // Phím xóa
}

// Khai báo layout cố định của bàn phím dưới dạng một hằng số (tránh khởi tạo lại khi recompose)
val KeypadLayout = listOf(
    listOf(KeypadKey.Number(1), KeypadKey.Number(2), KeypadKey.Number(3)),
    listOf(KeypadKey.Number(4), KeypadKey.Number(5), KeypadKey.Number(6)),
    listOf(KeypadKey.Number(7), KeypadKey.Number(8), KeypadKey.Number(9)),
    listOf(KeypadKey.DotThreeZeros, KeypadKey.Number(0), KeypadKey.Backspace)
)

@Composable
fun RowScope.KeypadButton(
    key: KeypadKey,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .weight(1f)
            .aspectRatio(1.5f) // Giúp các phím giữ tỷ lệ cân đối trên mọi màn hình
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            is KeypadKey.Number -> {
                Text(
                    text = key.value.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            KeypadKey.DotThreeZeros -> {
                Text(
                    text = ".000",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            KeypadKey.Backspace -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Backspace",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Bàn phím số tự chế dạng lưới tối ưu hóa tỷ lệ và hiệu năng
 */
@Composable
fun NumericKeypad(
    onKeyPress: (KeypadKey) -> Unit,
    onCollapse: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (onCollapse != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bàn phím nhập số",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Thu gọn bàn phím",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        KeypadLayout.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp) // Khoảng cách giữa các cột
            ) {
                rowKeys.forEach { key ->
                    KeypadButton(
                        key = key,
                        onClick = { onKeyPress(key) }
                    )
                }
            }
        }
    }
}

/**
 * Hàm xử lý chuỗi nhập tiền tệ thuần khiết, dễ kiểm thử
 */
fun handleKeyInput(currentValue: String, key: KeypadKey): String {
    return when (key) {
        is KeypadKey.Number -> {
            if (currentValue == "0") key.value.toString() else currentValue + key.value
        }
        KeypadKey.DotThreeZeros -> {
            if (currentValue.isEmpty() || currentValue == "0") "0" else currentValue + "000"
        }
        KeypadKey.Backspace -> {
            if (currentValue.isNotEmpty()) currentValue.dropLast(1) else ""
        }
    }
}
