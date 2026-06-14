package com.notepay.ui.feature.addtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.domain.model.Category
import com.notepay.ui.component.categoryIcon

/**
 * Lưới chọn danh mục 3 cột dùng chung cho cả AddTransaction và EditTransaction.
 *
 * - Hiển thị các danh mục theo `type` (chi tiêu/thu nhập), kèm danh mục OTHER.
 * - Mỗi danh mục là một FilterChip có icon tròn (avatar) màu của danh mục đó.
 * - Có nút "+ Thêm..." ở cuối để mở dialog tạo danh mục tùy biến.
 * - Khi [onCreateCategory] = null thì nút Thêm bị ẩn (dùng cho chế độ read-only).
 */
@Composable
fun CategoryGridPicker(
    categories: List<Category>,
    selectedCategory: Category?,
    isIncome: Boolean,
    onCategoryChanged: (Category) -> Unit,
    modifier: Modifier = Modifier,
    onCreateCategory: ((displayName: String, colorArgb: Long, isIncome: Boolean) -> Unit)? = null,
) {
    val visible = categories.filter {
        it == Category.OTHER || it.isIncome == isIncome
    }

    var showAddDialog by remember { mutableStateOf(false) }

    val canAdd = onCreateCategory != null
    val items: List<CategoryGridItem> = buildList {
        visible.forEach { add(CategoryGridItem.CategoryItem(it)) }
        if (canAdd) add(CategoryGridItem.AddButton)
    }
    val chunkedRows = items.chunked(3)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Danh mục",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        chunkedRows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        when (item) {
                            is CategoryGridItem.CategoryItem -> CategoryChip(
                                category = item.category,
                                selected = item.category == selectedCategory,
                                onClick = { onCategoryChanged(item.category) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            is CategoryGridItem.AddButton -> AddCategoryChip(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                val emptySlots = 3 - rowItems.size
                repeat(emptySlots) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (showAddDialog && onCreateCategory != null) {
        AddCategoryDialog(
            isIncome = isIncome,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color ->
                onCreateCategory(name, color, isIncome)
                showAddDialog = false
            },
        )
    }
}

private sealed interface CategoryGridItem {
    data class CategoryItem(val category: Category) : CategoryGridItem
    data object AddButton : CategoryGridItem
}

@Composable
private fun AddCategoryChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text("Thêm...", maxLines = 1) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        modifier = modifier.heightIn(min = 48.dp),
    )
}

@Composable
private fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                category.displayName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(category.colorArgb).copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    categoryIcon(category),
                    contentDescription = null,
                    tint = Color(category.colorArgb),
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        modifier = modifier.heightIn(min = 48.dp),
    )
}

@Composable
private fun AddCategoryDialog(
    isIncome: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    val colors = listOf(
        0xFFE57373L, // Red
        0xFFF06292L, // Pink
        0xFFBA68C8L, // Purple
        0xFF9575CDL, // Deep Purple
        0xFF64B5F6L, // Blue
        0xFF4FC3F7L, // Light Blue
        0xFF4DB6ACL, // Teal
        0xFF81C784L, // Green
        0xFFFFB74DL, // Orange
        0xFF90A4AEL, // Blue Grey
    )
    var selectedColor by remember { mutableStateOf(colors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo danh mục mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên danh mục") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Chọn màu sắc", style = MaterialTheme.typography.titleSmall)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val displayColors = colors.take(6)
                    displayColors.forEach { colorVal ->
                        val isSelected = selectedColor == colorVal
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .clickable { selectedColor = colorVal }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor) },
                enabled = name.isNotBlank(),
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
    )
}
