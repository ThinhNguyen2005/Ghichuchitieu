package com.notepay.feature.transaction.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notepay.R
import com.notepay.core.designsystem.component.effect.NotePayModalBottomSheet
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.localizedName
import com.notepay.core.navigation.rememberNotePayHaptic
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.domain.model.Category
import com.notepay.feature.category.mapper.categoryIcon
import com.notepay.feature.category.mapper.customCategoryIconOptions

// ─── Nhóm danh mục (UI-only, không thuộc domain) ─────────────────────────────

private enum class CategoryGroup(val labelRes: Int) {
    ALL(R.string.category_group_all),
    FOOD_TRANSPORT(R.string.category_group_food_transport),
    HOME_BILLS(R.string.category_group_home_bills),
    LIFESTYLE(R.string.category_group_lifestyle),
    FAMILY_HEALTH(R.string.category_group_family_health),
    FINANCE(R.string.category_group_finance),
}

@Composable
private fun CategoryGroup.accentColor(): Color = when (this) {
    CategoryGroup.ALL -> MaterialTheme.colorScheme.primary
    CategoryGroup.FOOD_TRANSPORT -> AppTheme.colors.warning
    CategoryGroup.HOME_BILLS -> AppTheme.colors.chartSeries[3]
    CategoryGroup.LIFESTYLE -> MaterialTheme.colorScheme.tertiary
    CategoryGroup.FAMILY_HEALTH -> AppTheme.colors.error
    CategoryGroup.FINANCE -> AppTheme.colors.success
}

/** Phân nhóm category.id -> CategoryGroup (UI-only mapping). */
private fun Category.uiGroup(): CategoryGroup = when (id) {
    "FOOD", "COFFEE", "TRANSPORT", "GAS" -> CategoryGroup.FOOD_TRANSPORT
    "BILL", "ELECTRICITY", "WATER", "INTERNET", "HOME", "REPAIR" -> CategoryGroup.HOME_BILLS
    "SHOPPING", "CLOTHES", "BEAUTY", "ENTERTAINMENT", "SPORTS", "TRAVEL" -> CategoryGroup.LIFESTYLE
    "HEALTH", "FAMILY", "CHILDREN", "PETS", "EDUCATION" -> CategoryGroup.FAMILY_HEALTH
    "SAVINGS", "DEBT_LOAN", "CHARITY", "INSURANCE", "TAX", "INVESTMENT" -> CategoryGroup.FINANCE
    else -> CategoryGroup.ALL
}

/** Màu accent lấy từ group nếu là category mặc định, hoặc colorArgb nếu là custom. */
@Composable
private fun Category.accentColor(): Color =
    if (isCustom) Color(colorArgb) else uiGroup().accentColor()

private fun preferredContentOnAccent(accent: Color): Color =
    if (accent.luminance() > 0.52f) Color(0xFF1B1B1F) else Color.White

// ─── CategoryGridPicker chính ─────────────────────────────────────────────────

/**
 * Lưới chọn danh mục đáp ứng độ rộng và font scale (icon trên – tên dưới).
 * Hỗ trợ hai chế độ: giới hạn chiều cao trong ModalBottomSheet ([fillAvailableHeight] = false)
 * hoặc chiếm phần còn lại của màn hình trong bước chọn danh mục ([fillAvailableHeight] = true).
 */
@Composable
fun CategoryGridPicker(
    categories: List<Category>,
    selectedCategory: Category?,
    isIncome: Boolean,
    onCategoryChanged: (Category) -> Unit,
    modifier: Modifier = Modifier,
    onCreateCategory: ((displayName: String, colorArgb: Long, iconId: String, isIncome: Boolean) -> Unit)? = null,
    fillAvailableHeight: Boolean = false,
    isLoading: Boolean = false,
    showHeader: Boolean = true,
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val fontScale = LocalConfiguration.current.fontScale
    val allVisible = remember(categories, isIncome) { categories.filter { it.isIncome == isIncome } }

    var query by rememberSaveable(isIncome) { mutableStateOf("") }
    var selectedGroupName by rememberSaveable(isIncome) { mutableStateOf(CategoryGroup.ALL.name) }
    val selectedGroup = CategoryGroup.entries.firstOrNull { it.name == selectedGroupName } ?: CategoryGroup.ALL
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    val filtered by remember(query, selectedGroup, allVisible) {
        derivedStateOf {
            allVisible.filter { cat ->
                val matchGroup = selectedGroup == CategoryGroup.ALL || cat.uiGroup() == selectedGroup
                val matchQuery = query.isBlank() ||
                    cat.localizedName(context).contains(query.trim(), ignoreCase = true)
                matchGroup && matchQuery
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columnCount = when {
            fontScale >= 1.7f -> 2
            maxWidth < 340.dp || fontScale >= 1.3f -> 3
            else -> 4
        }
        Column(
            modifier = if (fillAvailableHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showHeader) {
                CategoryGridHeader(count = filtered.size)
            }

            CategoryGridSearchField(
                query = query,
                onQueryChange = { query = it },
                onSearchDone = { focusManager.clearFocus() },
            )

            CategoryGroupFilterRow(
                selectedGroup = selectedGroup,
                onSelectGroup = {
                    selectedGroupName = it.name
                    focusManager.clearFocus()
                },
            )

            val gridContainerModifier = if (fillAvailableHeight) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            } else {
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            }

            Box(modifier = gridContainerModifier) {
                CategoryGridBody(
                    allVisibleCount = allVisible.size,
                    filtered = filtered,
                    selectedCategory = selectedCategory,
                    isLoading = isLoading,
                    columnCount = columnCount,
                    gridState = gridState,
                    onCategoryChanged = onCategoryChanged,
                    onClearFilters = {
                        query = ""
                        selectedGroupName = CategoryGroup.ALL.name
                    },
                    onOpenCreateDialog = if (onCreateCategory != null) {
                        { showAddDialog = true }
                    } else {
                        null
                    },
                )
            }
        }
    }

    if (showAddDialog && onCreateCategory != null) {
        AddCategoryBottomSheet(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color, iconId ->
                onCreateCategory(name, color, iconId, isIncome)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun CategoryGridHeader(count: Int) {
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
            color = AppTheme.colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = pluralStringResource(R.plurals.category_choices_format, count, count),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun CategoryGridSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchDone: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = stringResource(R.string.category_search_placeholder),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
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
                IconButton(onClick = { onQueryChange("") }) {
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
        keyboardActions = KeyboardActions(onSearch = { onSearchDone() }),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
    )
}

@Composable
private fun CategoryGroupFilterRow(
    selectedGroup: CategoryGroup,
    onSelectGroup: (CategoryGroup) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(CategoryGroup.entries, key = { it.name }) { group ->
            val isChipSelected = selectedGroup == group
            val groupColor = group.accentColor()
            FilterChip(
                selected = isChipSelected,
                onClick = { onSelectGroup(group) },
                modifier = Modifier.minimumInteractiveComponentSize(),
                label = {
                    Text(
                        text = stringResource(group.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = groupColor.copy(alpha = 0.18f),
                    selectedLabelColor = groupColor,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isChipSelected,
                    borderColor = Color.Transparent,
                    selectedBorderColor = groupColor.copy(alpha = 0.5f),
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp,
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryGridBody(
    allVisibleCount: Int,
    filtered: List<Category>,
    selectedCategory: Category?,
    isLoading: Boolean,
    columnCount: Int,
    gridState: LazyGridState,
    onCategoryChanged: (Category) -> Unit,
    onClearFilters: () -> Unit,
    onOpenCreateDialog: (() -> Unit)?,
) {
    when {
        isLoading && allVisibleCount == 0 -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                Text(
                    text = stringResource(R.string.category_loading_state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }

        allVisibleCount == 0 -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.category_empty_state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                if (onOpenCreateDialog != null) {
                    Button(
                        onClick = onOpenCreateDialog,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text(stringResource(R.string.category_dialog_title))
                    }
                }
            }
        }

        filtered.isEmpty() -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.category_search_empty_state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onClearFilters,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text(stringResource(R.string.category_clear_filters))
                    }
                    if (onOpenCreateDialog != null) {
                        TextButton(
                            onClick = onOpenCreateDialog,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        ) {
                            Text(stringResource(R.string.category_dialog_title))
                        }
                    }
                }
            }
        }

        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = filtered,
                    key = { it.id },
                    contentType = { "category" },
                ) { category ->
                    CategoryGridCell(
                        category = category,
                        isSelectedProvider = { category.id == selectedCategory?.id },
                        onClick = { onCategoryChanged(category) },
                    )
                }

                if (onOpenCreateDialog != null) {
                    item(key = "__add__", contentType = "add") {
                        AddCategoryCell(onClick = onOpenCreateDialog)
                    }
                }
            }
        }
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
    val reducedMotion = rememberNotePayReducedMotion()
    val performHaptic = rememberNotePayHaptic()
    val isSelected = isSelectedProvider()
    val accent = category.accentColor()
    val labelText = category.localizedName()

    // Scale spring chạy trên RenderNode (GPU), tôn trọng Cài đặt giảm chuyển động
    val scale = remember { Animatable(1f) }
    LaunchedEffect(isSelected, reducedMotion) {
        if (reducedMotion) {
            scale.snapTo(1f)
        } else if (isSelected) {
            scale.animateTo(
                targetValue = 1.04f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        } else {
            scale.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    val bubbleFill = if (isSelected) accent else accent.copy(alpha = 0.14f)
    val iconTint = if (isSelected) preferredContentOnAccent(accent) else accent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(AppTheme.shapes.corner16)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .semantics(mergeDescendants = true) {
                selected = isSelected
                contentDescription = labelText
            }
            .clickable(
                role = Role.Button,
                onClickLabel = labelText,
            ) {
                performHaptic(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .drawWithCache {
                    val selectionStroke = Stroke(width = 2.dp.toPx())
                    val radiusOffsetPx = 3.dp.toPx()
                    onDrawBehind {
                        if (isSelected) {
                            drawCircle(
                                color = accent,
                                radius = size.minDimension / 2f + radiusOffsetPx,
                                style = selectionStroke,
                            )
                        }
                    }
                }
                .background(
                    color = bubbleFill,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )

            if (isSelected) {
                val badgeBg = MaterialTheme.colorScheme.surface
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .background(badgeBg, CircleShape)
                        .padding(1.5.dp)
                        .background(accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = preferredContentOnAccent(accent),
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }

        Text(
            text = labelText,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
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
    val labelText = stringResource(R.string.category_add_more)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(AppTheme.shapes.corner16)
            .semantics(mergeDescendants = true) {
                contentDescription = labelText
            }
            .clickable(
                role = Role.Button,
                onClickLabel = labelText,
                onClick = onClick,
            )
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
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
            text = labelText,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                lineHeight = 15.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─── BottomSheet tạo danh mục tùy biến ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCategoryBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Long, iconId: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }

    val colors = remember {
        listOf(
            0xFFE57373L, 0xFFF06292L, 0xFFBA68C8L, 0xFF9575CDL,
            0xFF64B5F6L, 0xFF4FC3F7L, 0xFF4DB6ACL, 0xFF81C784L,
            0xFFFFB74DL, 0xFF90A4AEL,
        )
    }
    var selectedColor by remember { mutableLongStateOf(colors.first()) }
    var selectedIconId by remember { mutableStateOf(customCategoryIconOptions.first().id) }

    NotePayModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.category_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.category_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.category_color_title),
                style = MaterialTheme.typography.titleSmall,
                color = AppTheme.colors.textPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                colors.take(6).forEach { colorVal ->
                    val isSel = selectedColor == colorVal
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable { selectedColor = colorVal },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (isSel) 3.dp else 0.dp,
                                    color = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.category_icon_title),
                style = MaterialTheme.typography.titleSmall,
                color = AppTheme.colors.textPrimary,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(customCategoryIconOptions, key = { it.id }) { option ->
                    val isSel = selectedIconId == option.id
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable { selectedIconId = option.id },
                        contentAlignment = Alignment.Center,
                    ) {
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
                                ),
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor, selectedIconId) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.action_add))
                }
            }
        }
    }
}
