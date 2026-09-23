package com.notepay.ui.feature.transaction.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notepay.R
import com.notepay.domain.model.Category
import com.notepay.ui.component.categoryIcon
import com.notepay.ui.component.customCategoryIconOptions
import com.notepay.ui.theme.AppTheme

// ─── Nhóm danh mục (UI-only, không thuộc domain) ─────────────────────────────

private enum class CategoryGroup(val labelRes: Int, val baseColor: Color) {
    ALL(R.string.category_group_all, Color(0xFF64B5F6)),
    FOOD_TRANSPORT(R.string.category_group_food_transport, Color(0xFFF59E0B)),
    HOME_BILLS(R.string.category_group_home_bills, Color(0xFF3B82F6)),
    LIFESTYLE(R.string.category_group_lifestyle, Color(0xFF8B5CF6)),
    FAMILY_HEALTH(R.string.category_group_family_health, Color(0xFFEF4444)),
    FINANCE(R.string.category_group_finance, Color(0xFF10B981)),
}

/** Phân nhóm category.id -> CategoryGroup (UI-only mapping). */
private fun Category.uiGroup(): CategoryGroup = when (id) {
    "FOOD", "COFFEE", "TRANSPORT", "GAS" -> CategoryGroup.FOOD_TRANSPORT
    "BILL", "ELECTRICITY", "WATER", "INTERNET", "HOME", "REPAIR" -> CategoryGroup.HOME_BILLS
    "SHOPPING", "CLOTHES", "BEAUTY", "ENTERTAINMENT", "SPORTS", "TRAVEL" -> CategoryGroup.LIFESTYLE
    "HEALTH", "FAMILY", "CHILDREN", "PETS", "EDUCATION" -> CategoryGroup.FAMILY_HEALTH
    "SAVINGS", "DEBT_LOAN", "CHARITY", "INSURANCE", "TAX", "INVESTMENT" -> CategoryGroup.FINANCE
    else -> CategoryGroup.ALL // custom / income categories hiện ở Tất cả
}

/** Màu accent lấy từ group nếu là category mặc định, hoặc colorArgb nếu là custom. */
private fun Category.accentColor(): Color =
    if (isCustom) Color(colorArgb) else uiGroup().baseColor

// ─── CategoryGridPicker chính ─────────────────────────────────────────────────

/**
 * Lưới chọn danh mục 4 cột (icon trên – tên dưới).
 * Tối ưu render cấp 3: drawBehind + graphicsLayer để vẽ viền / scale trên GPU.
 * Deferred state reading: mỗi item nhận lambda isSelectedProvider để bỏ qua recomposition toàn lưới.
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
    val focusManager = LocalFocusManager.current
    val allVisible = remember(categories, isIncome) { categories.filter { it.isIncome == isIncome } }

    var query by remember(isIncome) { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(CategoryGroup.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Tìm kiếm + lọc nhóm — derivedStateOf không gây recompose thừa khi state khác thay đổi
    val filtered by remember(query, selectedGroup, allVisible) {
        derivedStateOf {
            allVisible.filter { cat ->
                val matchGroup = selectedGroup == CategoryGroup.ALL || cat.uiGroup() == selectedGroup
                val matchQuery = query.isBlank() || cat.displayName.contains(query.trim(), ignoreCase = true)
                matchGroup && matchQuery
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.category_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.category_choices_format, filtered.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Ô tìm kiếm
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.category_search_placeholder), fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = stringResource(R.string.content_description_clear_text),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            singleLine = true,
            shape = AppTheme.shapes.corner12,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            ),
        )

        // Filter Chips nhóm danh mục (cuộn ngang)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(CategoryGroup.entries, key = { it.name }) { group ->
                val isChipSelected = selectedGroup == group
                FilterChip(
                    selected = isChipSelected,
                    onClick = {
                        selectedGroup = group
                        focusManager.clearFocus()
                    },
                    label = { Text(stringResource(group.labelRes), fontSize = 12.sp) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = group.baseColor.copy(alpha = 0.18f),
                        selectedLabelColor = group.baseColor,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isChipSelected,
                        borderColor = Color.Transparent,
                        selectedBorderColor = group.baseColor.copy(alpha = 0.5f),
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp,
                    ),
                )
            }
        }

        // Lưới 4 cột — icon trên, tên dưới
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(
                items = filtered,
                key = { it.id },
                contentType = { "category" },
            ) { category ->
                CategoryGridCell(
                    category = category,
                    // Deferred reading: chỉ đọc state khi vẽ → 26 items khác bỏ qua recomposition
                    isSelectedProvider = { category.id == selectedCategory?.id },
                    onClick = { onCategoryChanged(category) },
                )
            }

            // Nút "+ Thêm" ở cuối nếu có callback
            if (onCreateCategory != null) {
                item(key = "__add__", contentType = "add") {
                    AddCategoryCell(onClick = { showAddDialog = true })
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

// ─── Ô danh mục đơn lẻ (Icon trên – Tên dưới) ───────────────────────────────

@Composable
private fun CategoryGridCell(
    category: Category,
    isSelectedProvider: () -> Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val isSelected = isSelectedProvider()
    val accent = category.accentColor()

    // Scale spring chạy trên RenderNode (GPU), không layout lại
    val scale = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            scale.animateTo(
                targetValue = 1.08f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        } else {
            scale.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                // Toàn bộ hiệu ứng co giãn đẩy thẳng vào RenderNode
                scaleX = scale.value
                scaleY = scale.value
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        // Icon tròn + viền phát sáng khi được chọn (vẽ ở Draw Phase – không recompose)
        Box(
            modifier = Modifier
                .size(52.dp)
                .drawBehind {
                    if (isSelected) {
                        drawCircle(
                            color = accent,
                            radius = size.minDimension / 2f + 2.5.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
                .background(
                    color = if (isSelected) accent.copy(alpha = 0.26f) else accent.copy(alpha = 0.11f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = if (isSelected) Color.White else accent,
                modifier = Modifier.size(24.dp),
            )

            // Badge check góc dưới phải
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .background(accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
        }

        // Tên danh mục — 1 dòng, căn giữa, không bao giờ bị cắt "..." do 4 cột rộng hơn
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─── Ô "+ Thêm" ──────────────────────────────────────────────────────────────

@Composable
private fun AddCategoryCell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = CircleShape,
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }

        Text(
            text = stringResource(R.string.category_add_more),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─── Dialog tạo danh mục tùy biến (giữ nguyên, tương thích) ─────────────────

@Composable
private fun AddCategoryDialog(
    isIncome: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Long, iconId: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    val colors = listOf(
        0xFFE57373L, 0xFFF06292L, 0xFFBA68C8L, 0xFF9575CDL,
        0xFF64B5F6L, 0xFF4FC3F7L, 0xFF4DB6ACL, 0xFF81C784L,
        0xFFFFB74DL, 0xFF90A4AEL,
    )
    var selectedColor by remember { mutableStateOf(colors.first()) }
    var selectedIconId by remember { mutableStateOf(customCategoryIconOptions.first().id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.category_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(stringResource(R.string.category_color_title), style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    colors.take(6).forEach { colorVal ->
                        val isSel = selectedColor == colorVal
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .clickable { selectedColor = colorVal }
                                .border(
                                    width = if (isSel) 3.dp else 0.dp,
                                    color = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape,
                                ),
                        )
                    }
                }

                Text(stringResource(R.string.category_icon_title), style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(customCategoryIconOptions, key = { it.id }) { option ->
                        val isSel = selectedIconId == option.id
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSel) Color(selectedColor).copy(alpha = 0.20f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) Color(selectedColor) else Color.Transparent,
                                    shape = CircleShape,
                                )
                                .clickable { selectedIconId = option.id },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = stringResource(option.labelRes),
                                tint = if (isSel) Color(selectedColor) else MaterialTheme.colorScheme.onSurfaceVariant,
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
