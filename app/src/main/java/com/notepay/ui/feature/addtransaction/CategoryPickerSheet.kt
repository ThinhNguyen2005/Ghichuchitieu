package com.notepay.ui.feature.addtransaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notepay.domain.model.Category

/**
 * BottomSheet chứa lưới chọn và thêm danh mục tùy biến, dùng chung cho cả màn hình Thêm và Chỉnh sửa giao dịch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<Category>,
    selectedCategory: Category?,
    isIncome: Boolean,
    onCategoryChanged: (Category) -> Unit,
    onDismiss: () -> Unit,
    onCreateCategory: (
        displayName: String,
        colorArgb: Long,
        iconId: String,
        isIncome: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            CategoryGridPicker(
                categories = categories,
                selectedCategory = selectedCategory,
                isIncome = isIncome,
                onCategoryChanged = onCategoryChanged,

                onCreateCategory = onCreateCategory
            )
        }
    }
}
