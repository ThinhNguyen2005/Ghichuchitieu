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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Category
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.component.LiquidGlassPanel
import com.notepay.ui.component.customCategoryIconOptions
import com.notepay.ui.theme.AppTheme

/**
 * Lưới chọn danh mục 3 cột dùng chung cho cả AddTransaction và EditTransaction.
 *
 * - Hiển thị đúng các danh mục theo `type` (chi tiêu hoặc thu nhập).
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
    onCreateCategory: ((displayName: String, colorArgb: Long, iconId: String, isIncome: Boolean) -> Unit)? = null,
) {
    val allVisible = categories.filter { it.isIncome == isIncome }
    var query by remember(isIncome) { mutableStateOf("") }
    val visible = if (allVisible.size > 8) {
        allVisible.filter { it.displayName.contains(query, ignoreCase = true) }
    } else {
        allVisible
    }

    var showAddDialog by remember { mutableStateOf(false) }

    val canAdd = onCreateCategory != null
    val items: List<CategoryGridItem> = buildList {
        visible.forEach { add(CategoryGridItem.CategoryItem(it)) }
        if (canAdd) add(CategoryGridItem.AddButton)
    }
    val chunkedRows = items.chunked(3)

    LiquidGlassPanel(
        modifier = modifier
            .fillMaxWidth(),
        shape = AppTheme.shapes.corner20,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Danh mục",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "${visible.size} lựa chọn",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "Chọn danh mục phù hợp nhất với giao dịch này",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (allVisible.size > 8) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.action_search)) },
            )
        }

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
    }

    if (showAddDialog && onCreateCategory != null) {
        AddCategoryDialog(
            isIncome = isIncome,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color, iconId ->
                onCreateCategory(name, color, iconId, isIncome)
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
        modifier = modifier.heightIn(min = 56.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            labelColor = MaterialTheme.colorScheme.onSurface,
            iconColor = MaterialTheme.colorScheme.primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.68f),
            selectedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.68f),
        ),
    )
}

@Composable
private fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = Color(category.colorArgb)
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
            CategoryAvatar(
                category = category,
                size = 28.dp,
                iconSize = 16.dp,
            )
        },
        modifier = modifier.heightIn(min = 56.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            labelColor = MaterialTheme.colorScheme.onSurface,
            iconColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = accent.copy(alpha = 0.20f),
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
            selectedLeadingIconColor = accent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = accent.copy(alpha = 0.90f),
            selectedBorderWidth = 2.dp,
        ),
    )
}

@Composable
private fun AddCategoryDialog(
    isIncome: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Long, iconId: String) -> Unit,
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
    var selectedIconId by remember { mutableStateOf(customCategoryIconOptions.first().id) }

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

                Text("Chọn biểu tượng", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(customCategoryIconOptions, key = { it.id }) { option ->
                        val isSelected = selectedIconId == option.id
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color(selectedColor).copy(alpha = 0.20f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(selectedColor) else Color.Transparent,
                                    shape = CircleShape,
                                )
                                .clickable { selectedIconId = option.id },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                tint = if (isSelected) Color(selectedColor) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor, selectedIconId) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
