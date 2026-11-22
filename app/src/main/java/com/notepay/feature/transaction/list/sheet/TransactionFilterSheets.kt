package com.notepay.feature.transaction.list.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notepay.R
import com.notepay.core.designsystem.util.localizedName
import com.notepay.domain.model.Category
import com.notepay.domain.model.TransactionType
import com.notepay.feature.transaction.component.DateRangePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateRangeBottomSheet(
    currentPreset: DateRangePreset,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelectPreset: (DateRangePreset) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 36.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.filter_time_period),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = stringResource(R.string.action_close),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            DateRangePreset.entries.forEach { preset ->
                val isSelected = preset == currentPreset
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectPreset(preset) }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(preset.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FilterBottomSheet(
    currentType: TransactionType?,
    isTransferOnly: Boolean,
    currentCategoryIds: Set<String>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
    onApply: (TransactionType?, Boolean, Set<String>) -> Unit,
) {
    var selectedType by remember { mutableStateOf(currentType) }
    var selectedTransferOnly by remember { mutableStateOf(isTransferOnly) }
    var selectedCategories by remember { mutableStateOf(currentCategoryIds) }

    val expenseCategories = remember { Category.getAll().filter { !it.isIncome } }
    val incomeCategories = remember { Category.getAll().filter { it.isIncome } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 36.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.filter_advanced),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            selectedType = null
                            selectedTransferOnly = false
                            selectedCategories = emptySet()
                            onClearAll()
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.filter_clear_all),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.filter_classification),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val isAllSelected = selectedType == null && !selectedTransferOnly
                FilterChip(
                    selected = isAllSelected,
                    onClick = {
                        selectedType = null
                        selectedTransferOnly = false
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    label = { Text(stringResource(R.string.filter_type_all)) },
                )
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE && !selectedTransferOnly,
                    onClick = {
                        selectedType = TransactionType.EXPENSE
                        selectedTransferOnly = false
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    label = { Text(stringResource(R.string.filter_type_expense)) },
                )
                FilterChip(
                    selected = selectedType == TransactionType.INCOME && !selectedTransferOnly,
                    onClick = {
                        selectedType = TransactionType.INCOME
                        selectedTransferOnly = false
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    label = { Text(stringResource(R.string.filter_type_income)) },
                )
                FilterChip(
                    selected = selectedTransferOnly,
                    onClick = {
                        selectedType = null
                        selectedTransferOnly = true
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    label = { Text(stringResource(R.string.filter_type_transfer)) },
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.filter_category_expense),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                expenseCategories.take(12).forEach { category ->
                    val isCatSelected = category.id in selectedCategories
                    FilterChip(
                        selected = isCatSelected,
                        onClick = {
                            selectedCategories = if (isCatSelected) {
                                selectedCategories - category.id
                            } else {
                                selectedCategories + category.id
                            }
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(category.colorArgb)),
                            )
                        },
                        label = { Text(category.localizedName(), fontSize = 12.sp) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.filter_category_income),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                incomeCategories.forEach { category ->
                    val isCatSelected = category.id in selectedCategories
                    FilterChip(
                        selected = isCatSelected,
                        onClick = {
                            selectedCategories = if (isCatSelected) {
                                selectedCategories - category.id
                            } else {
                                selectedCategories + category.id
                            }
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(category.colorArgb)),
                            )
                        },
                        label = { Text(category.localizedName(), fontSize = 12.sp) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onApply(selectedType, selectedTransferOnly, selectedCategories)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.filter_apply),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}
