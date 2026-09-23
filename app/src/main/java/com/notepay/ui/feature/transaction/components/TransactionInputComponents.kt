package com.notepay.ui.feature.transaction.components

import com.notepay.ui.theme.AppTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Category
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.feature.transaction.AmountParser
import com.notepay.ui.formatter.VietnameseMoneyWordsFormatter
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.sp

/**
 * Hiển thị số tiền / biểu thức đã được format sẵn từ CalculatorEngine.
 * KHÔNG format lại — tránh bug double-format (vd "5.000" → "5.0.00").
 * Hiển thị thêm dòng đọc số bằng chữ tiếng Việt phía dưới.
 */
@Composable
fun TransactionAmountDisplay(
    amountInput: String,
    modifier: Modifier = Modifier
) {
    // Parse số thuần từ chuỗi đã format (vd "5.054.542" → 5054542L)
    val rawNumber = remember(amountInput) {
        amountInput.replace(".", "").toLongOrNull() ?: 0L
    }
    val zeroWords = stringResource(R.string.number_words_zero)
    val currencySuffix = stringResource(R.string.number_words_currency_suffix)
    val amountInWords = remember(rawNumber, zeroWords, currencySuffix) {
        if (rawNumber > 0L) {
            runCatching {
                VietnameseMoneyWordsFormatter.format(rawNumber, zeroWords) + " " + currencySuffix
            }.getOrDefault("")
        } else ""
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.amount_input_title),
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
            // amountInput đã được format bởi CalculatorEngine.displayExpression → hiển thị thẳng
            Text(
                text = amountInput.ifBlank { "0" },
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = stringResource(R.string.currency_vnd_symbol),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        // Dòng đọc số bằng chữ (chỉ hiển khi có số thuần)
        if (amountInWords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amountInWords,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
    val allVisible = remember(categories, isIncome) {
        categories.filter { it == Category.OTHER || it.isIncome == isIncome }
            .take(8) // 2 hàng × 4 danh mục
    }
    val row1 = allVisible.take(4)
    val row2 = allVisible.drop(4)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.category_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.action_see_all),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .clip(AppTheme.shapes.corner8)
                    .clickable(onClick = onSeeAllClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Hàng 1
        CategoryRow(row1, selectedCategory, onCategoryChanged)

        if (row2.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            // Hàng 2
            CategoryRow(row2, selectedCategory, onCategoryChanged)
        }
    }
}

@Composable
private fun CategoryRow(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategoryChanged: (Category) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category == selectedCategory
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.1f)
                    .clip(AppTheme.shapes.corner16)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        shape = AppTheme.shapes.corner16
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
        // Placeholder để giữ cân bằng nếu hàng cuối < 4 items
        repeat(4 - categories.size) {
            Spacer(modifier = Modifier.weight(1f))
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
            .clip(AppTheme.shapes.corner16)
            .clickable(onClick = onClick),
        shape = AppTheme.shapes.corner16,
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
            .clip(AppTheme.shapes.corner12)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                shape = AppTheme.shapes.corner12
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
                    contentDescription = stringResource(R.string.content_description_backspace),
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
                    text = stringResource(R.string.numeric_keypad_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.content_description_collapse_keypad),
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
    val nextValue = when (key) {
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
    return if (nextValue.length > AmountParser.MAX_DIGITS) currentValue else nextValue
}

/**
 * Chip hiển thị tóm tắt ghi chú hiện tại — bấm vào để mở NoteQuickEntrySheet.
 * Chỉ hiển thị khi note không rỗng.
 */
@Composable
fun NotePreviewChip(
    note: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.corner12)
            .clickable(onClick = onClick),
        shape = AppTheme.shapes.corner12,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
